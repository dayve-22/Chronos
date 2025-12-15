package com.dayve22.Chronos.service;

import com.dayve22.Chronos.jobs.CommandExecutionJob;
import org.quartz.*;
import org.quartz.impl.matchers.GroupMatcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional; // Ensure this is imported

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;

@Service
public class JobService {
    private static final Logger log = LoggerFactory.getLogger(JobService.class);

    private final Scheduler scheduler;

    public JobService(Scheduler scheduler) {
        this.scheduler = scheduler;
    }

    // ADD THIS ANNOTATION
    @Transactional
    public String scheduleOneTimeJob(String command, LocalDateTime executeAt) throws SchedulerException {
        String jobId = UUID.randomUUID().toString();

        JobDetail jobDetail = JobBuilder.newJob(CommandExecutionJob.class)
                .withIdentity(jobId, "user-jobs")
                .usingJobData(CommandExecutionJob.DATA_COMMAND, command)
                .usingJobData(CommandExecutionJob.DATA_RETRIES_ALLOWED, 3)
                .storeDurably(false)
                .build();

        Trigger trigger = TriggerBuilder.newTrigger()
                .withIdentity(jobId, "one-time-triggers")
                .forJob(jobDetail)
                .startAt(Date.from(executeAt.atZone(ZoneId.systemDefault()).toInstant()))
                .build();

        // FIX: Use the atomic method. Do not call addJob() separately.
        scheduler.scheduleJob(jobDetail, trigger);

        log.info("Scheduled one-time job: {} at {}", jobId, executeAt);
        return jobId;
    }

    // ADD THIS ANNOTATION
    @Transactional
    public String scheduleCronJob(String command, String cronExpression) throws SchedulerException {
        String jobId = UUID.randomUUID().toString();

        try {
            CronScheduleBuilder.cronSchedule(cronExpression);
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid cron expression: " + cronExpression, e);
        }

        JobDetail jobDetail = JobBuilder.newJob(CommandExecutionJob.class)
                .withIdentity(jobId, "user-jobs")
                .usingJobData(CommandExecutionJob.DATA_COMMAND, command)
                .usingJobData(CommandExecutionJob.DATA_RETRIES_ALLOWED, 3)
                .storeDurably(true)
                .requestRecovery(true)
                .build();

        CronTrigger trigger = TriggerBuilder.newTrigger()
                .withIdentity(jobId, "cron-triggers")
                .forJob(jobDetail)
                .withSchedule(CronScheduleBuilder.cronSchedule(cronExpression)
                        .withMisfireHandlingInstructionDoNothing())
                .startNow()
                .build();

        // FIX: Use the atomic method. Do not call addJob() separately.
        scheduler.scheduleJob(jobDetail, trigger);

        log.info("Scheduled cron job: {} with expression: {}", jobId, cronExpression);
        return jobId;
    }

    // Management Methods
    public void deleteJob(String jobId) throws SchedulerException {
        JobKey jobKey = new JobKey(jobId, "user-jobs");
        if (scheduler.checkExists(jobKey)) {
            scheduler.deleteJob(jobKey);
            log.info("Deleted job: {}", jobId);
        } else {
            throw new SchedulerException("Job not found: " + jobId);
        }
    }

    public void pauseJob(String jobId) throws SchedulerException {
        JobKey jobKey = new JobKey(jobId, "user-jobs");
        if (scheduler.checkExists(jobKey)) {
            scheduler.pauseJob(jobKey);
            log.info("Paused job: {}", jobId);
        } else {
            throw new SchedulerException("Job not found: " + jobId);
        }
    }

    public void resumeJob(String jobId) throws SchedulerException {
        JobKey jobKey = new JobKey(jobId, "user-jobs");
        if (scheduler.checkExists(jobKey)) {
            scheduler.resumeJob(jobKey);
            log.info("Resumed job: {}", jobId);
        } else {
            throw new SchedulerException("Job not found: " + jobId);
        }
    }

    // Get job details
    public JobDetail getJobDetail(String jobId) throws SchedulerException {
        JobKey jobKey = new JobKey(jobId, "user-jobs");
        return scheduler.getJobDetail(jobKey);
    }

    // List all jobs
    public List<String> listAllJobs() throws SchedulerException {
        List<String> jobIds = new ArrayList<>();
        for (String groupName : scheduler.getJobGroupNames()) {
            for (JobKey jobKey : scheduler.getJobKeys(GroupMatcher.jobGroupEquals(groupName))) {
                jobIds.add(jobKey.getName());
            }
        }
        return jobIds;
    }
}