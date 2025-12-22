package com.dayve22.Chronos.core;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class JobEventListener {

    @Autowired
    private DistributedJobScheduler scheduler;

    @RedisMessageListener(topic = "job:new")
    public void onNewJob(String jobId) {
        scheduler.executeJob(Long.parseLong(jobId));
    }
}
