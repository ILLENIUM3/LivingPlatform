-- KEYS[1] 为锁，KEYS[2] 为缓存；ARGV 为令牌、JSON、TTL（秒）。
-- 防止查询过慢、租约已过期的旧请求覆盖后来者回填的数据。
if redis.call('GET', KEYS[1]) == ARGV[1] then
    redis.call('SET', KEYS[2], ARGV[2], 'EX', ARGV[3])
    return 1
end
return 0
