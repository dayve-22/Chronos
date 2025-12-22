package com.dayve22.Chronos.config;

import com.dayve22.Chronos.core.RedisJobScheduler;
import com.dayve22.Chronos.entity.Job;
import com.dayve22.Chronos.entity.JobStatus;
import com.dayve22.Chronos.repository.JobRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class RedisDataSync implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(RedisDataSync.class);

    @Autowired
    private JobRepository jobRepository;

    @Autowired
    private RedisJobScheduler redisScheduler;

    @Override
    public void run(String... args) {
        logger.info("Syncing jobs from Database to Redis...");

        // List<Job> findByStatus(JobStatus status);
        List<Job> activeJobs = jobRepository.findByStatus(JobStatus.ACTIVE);

        int count = 0;
        for (Job job : activeJobs) {
            if (job.getNextRunTime() != null) {
                redisScheduler.scheduleJob(job.getId(), job.getNextRunTime());
                count++;
            }
        }

        logger.info("Synced {} jobs to Redis successfully.", count);
    }
}