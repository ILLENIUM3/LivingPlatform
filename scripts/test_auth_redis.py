"""验证实际 Lua；仅启动临时 Unix socket Redis，不连接项目实例。需要 redis-server/redis-cli。"""
import concurrent.futures
from pathlib import Path
import subprocess
import tempfile
import time
import unittest


SCRIPTS = Path(__file__).resolve().parents[1] / "backend/live-user-provider/src/main/resources/redis"


class AuthRedisTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.temp = tempfile.TemporaryDirectory(prefix="live-auth-")
        cls.socket = str(Path(cls.temp.name) / "redis.sock")
        cls.log = open(Path(cls.temp.name) / "redis.log", "w+")
        cls.process = subprocess.Popen([
            "redis-server", "--port", "0", "--unixsocket", cls.socket,
            "--unixsocketperm", "700", "--save", "", "--appendonly", "no",
            "--maxmemory-policy", "noeviction",
        ], stdout=cls.log, stderr=cls.log)
        cls.addClassCleanup(cls.stop)
        for _ in range(100):
            if cls.process.poll() is not None:
                cls.log.seek(0)
                raise RuntimeError("Temporary Redis failed: " + cls.log.read())
            if Path(cls.socket).exists() and cls.command("PING") == "PONG":
                return
            time.sleep(0.02)
        raise RuntimeError("Temporary Redis did not start")

    @classmethod
    def stop(cls):
        cls.process.terminate()
        cls.process.wait(timeout=5)
        cls.log.close()
        cls.temp.cleanup()

    @classmethod
    def command(cls, *args):
        return subprocess.check_output(
            ["redis-cli", "-s", cls.socket, "--raw", *map(str, args)], text=True
        ).strip()

    def setUp(self):
        self.key = "test:" + self._testMethodName

    def script(self, file, *args):
        return self.command("--eval", SCRIPTS / file, self.key, ",", *args)

    def issue(self, code="012345", ttl=300, cooldown=60):
        return self.script("issue-login-code.lua", code, ttl, cooldown)

    def consume(self, code="012345"):
        return self.script("consume-login-code.lua", code, 5)

    def test_only_one_concurrent_request_consumes_code(self):
        self.assertEqual(self.issue(), "1")
        with concurrent.futures.ThreadPoolExecutor(max_workers=8) as pool:
            results = list(pool.map(lambda _: self.consume(), range(8)))
        self.assertEqual(results.count("1"), 1)
        self.assertEqual(results.count("0"), 7)

    def test_only_one_concurrent_send_wins(self):
        with concurrent.futures.ThreadPoolExecutor(max_workers=8) as pool:
            results = list(pool.map(lambda _: self.issue(), range(8)))
        self.assertEqual(results.count("1"), 1)

    def test_consumption_preserves_cooldown_and_ttl(self):
        self.assertEqual(self.issue(), "1")
        self.assertEqual(self.consume(), "1")
        self.assertEqual(self.issue("654321"), "0")
        self.assertGreater(int(self.command("TTL", self.key)), 0)

    def test_attempt_limit_invalidates_even_correct_code(self):
        self.issue()
        for _ in range(5):
            self.assertEqual(self.consume("000000"), "0")
        self.assertEqual(self.consume(), "0")
        self.assertEqual(self.issue(), "0")

    def test_resend_replaces_old_code(self):
        self.issue()
        # 模拟冷却已过，避免测试等待一分钟；只修改临时实例中的测试 Key。
        self.command("HSET", self.key, "nextSendAt", "0")
        self.assertEqual(self.issue("654321"), "1")
        self.assertEqual(self.consume(), "0")
        self.assertEqual(self.consume("654321"), "1")

    def test_expiry_rejects_code_even_when_cooldown_hash_remains(self):
        self.assertEqual(self.issue(ttl=1, cooldown=60), "1")
        time.sleep(1.1)
        self.assertEqual(self.consume(), "0")
        self.assertEqual(self.issue(), "0")


if __name__ == "__main__":
    unittest.main(verbosity=2)
