-- 比较、计错、消费放在同一脚本，避免两个请求同时使用同一个验证码。
local code = redis.call('HGET', KEYS[1], 'code')
if not code then return 0 end
local time = redis.call('TIME')
local now = tonumber(time[1]) * 1000 + math.floor(tonumber(time[2]) / 1000)
if tonumber(redis.call('HGET', KEYS[1], 'expiresAt') or '0') <= now then
    redis.call('HDEL', KEYS[1], 'code')
    return 0
end
if code ~= ARGV[1] then
    local attempts = redis.call('HINCRBY', KEYS[1], 'attempts', 1)
    if attempts >= tonumber(ARGV[2]) then redis.call('HDEL', KEYS[1], 'code') end
    return 0
end
redis.call('HDEL', KEYS[1], 'code')
return 1
