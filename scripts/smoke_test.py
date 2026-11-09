"""Run with Python 3 in the same network environment as the Java services."""
import argparse
import json
import urllib.request


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--api-base", default="http://127.0.0.1:8080")
    parser.add_argument("--provider-base", default="http://127.0.0.1:8081")
    args = parser.parse_args()
    opener = urllib.request.build_opener(urllib.request.ProxyHandler({}))

    def get(url):
        with opener.open(url, timeout=10) as response:
            return json.load(response)

    for base in (args.api_base, args.provider_base):
        if get(base + "/actuator/health").get("status") != "UP":
            raise SystemExit("Service unhealthy: " + base)
    result = get(args.api_base + "/api/system/user-service")
    expected = {"serviceName": "live-user-provider", "database": "live_platform",
                "databaseProduct": "MySQL", "probeValue": "stage01-ready"}
    if (result.get("code") != "OK" or not result.get("requestId") or
            any(result.get("data", {}).get(key) != value for key, value in expected.items())):
        raise SystemExit("Unexpected RPC/database response: " + json.dumps(result))
    print(json.dumps(result, ensure_ascii=False, indent=2))
    print("PASS: HTTP -> Dubbo -> user provider -> MySQL")


if __name__ == "__main__":
    main()
