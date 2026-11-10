-- 一个手机号一个 Hash，发送冷却不会因登录成功而被删除。
local time = redis.call('TIME')
local now = tonumber(time[1]) * 1000 + math.floor(tonumber(time[2]) / 1000)
local nextSend = tonumber(redis.call('HGET', KEYS[1], 'nextSendAt') or '0')
if nextSend > now then return 0 end
local ttl = tonumber(ARGV[2])
local cooldown = tonumber(ARGV[3])
redis.call('HSET', KEYS[1], 'code', ARGV[1], 'attempts', 0,
    'expiresAt', now + ttl * 1000, 'nextSendAt', now + cooldown * 1000)
redis.call('EXPIRE', KEYS[1], math.max(ttl, cooldown))
return 1
