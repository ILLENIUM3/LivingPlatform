-- 比较和删除是一个原子操作；旧持有者不得删除新持有者的锁。
if redis.call('GET', KEYS[1]) == ARGV[1] then
    return redis.call('DEL', KEYS[1])
end
return 0
