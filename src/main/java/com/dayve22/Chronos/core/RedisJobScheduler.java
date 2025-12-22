package com.dayve22.Chronos.core;

import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@Service
public class RedisJobScheduler {

    @Autowired
    private RedissonClient redisson;

    @Autowired
    private StringRedisTemplate redis;


    private final String POP_SCRIPT =
            "local jobs = redis.call('ZRANGEBYSCORE', KEYS[1], 0, ARGV[1], 'LIMIT', 0, ARGV[2]) " +
                    "if #jobs > 0 then " +
                    "  redis.call('ZREM', KEYS[1], unpack(jobs)) " +
                    "end " +
                    "return jobs";

    private final DefaultRedisScript<List> popScript;

    public RedisJobScheduler() {
        this.popScript = new DefaultRedisScript<>();
        this.popScript.setScriptText(POP_SCRIPT);
        this.popScript.setResultType(List.class);
    }

    public void scheduleJob(Long jobId, LocalDateTime runAt) {
        double score = runAt.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
        redis.opsForZSet().add("job:schedule", jobId.toString(), score);
    }


    public List<String> pollDueJobs(int limit) {
        long now = System.currentTimeMillis();

        // Execute Lua script
        return redis.execute(
                popScript,
                Collections.singletonList("job:schedule"), // KEYS[1]
                String.valueOf(now),                       // ARGV[1] (max score)
                String.valueOf(limit)                      // ARGV[2] (limit)
        );
    }


    public boolean tryLockJob(Long jobId) {
        RLock lock = redisson.getLock("job:lock:" + jobId);
        try {
            return lock.tryLock(0, 30, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            return false;
        }
    }

    public void removeJob(Long jobId) {
        redis.opsForZSet().remove("job:schedule", jobId.toString());
    }
}