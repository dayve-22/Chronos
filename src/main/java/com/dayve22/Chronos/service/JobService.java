package com.dayve22.Chronos.service;

import com.dayve22.Chronos.payload.JobRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.quartz.*;
import org.quartz.impl.matchers.GroupMatcher;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.Set;

@Service
@Slf4j
@RequiredArgsConstructor
public class JobService {

    private final Scheduler scheduler;

    private static final int MAX_JOBS_PER_USER = 20;


    public boolean scheduleJob(JobRequest jobRequest, String username) {
        try {
            // 1. QUOTA CHECK: Count existing jobs for this user
            int currentJobCount = scheduler.getJobKeys(GroupMatcher.jobGroupEquals(username)).size();
            if (currentJobCount >= MAX_JOBS_PER_USER) {
                log.warn("User '{}' has reached the job quota of {}", username, MAX_JOBS_PER_USER);
                throw new RuntimeException("Quota Exceeded! You can only have " + MAX_JOBS_PER_USER + " active jobs.");
            }


            Class<? extends Job> jobClass = jobRequest.getJobClass();

            JobDetail jobDetail = JobBuilder.newJob(jobClass)
                    .withIdentity(jobRequest.getJobName(), username)
                    .storeDurably()
                    .requestRecovery()
                    .build();


            if (jobRequest.getCommand() != null) {
                jobDetail.getJobDataMap().put("command", jobRequest.getCommand());
            }
            jobDetail.getJobDataMap().put("retries", 0);


            Trigger trigger;

            if (jobRequest.getCronExpression() != null && !jobRequest.getCronExpression().isEmpty()) {
                trigger = TriggerBuilder.newTrigger()
                        .withIdentity(jobRequest.getJobName() + "_trigger", username)
                        .withSchedule(CronScheduleBuilder.cronSchedule(jobRequest.getCronExpression()))
                        .build();
            } else {
                SimpleScheduleBuilder scheduleBuilder = SimpleScheduleBuilder.simpleSchedule()
                        .withIntervalInSeconds(jobRequest.getRepeatIntervalInSeconds().intValue());

                if (jobRequest.getRepeatCount() != null && jobRequest.getRepeatCount() == -1) {
                    scheduleBuilder.repeatForever();
                } else if (jobRequest.getRepeatCount() != null) {
                    scheduleBuilder.withRepeatCount(jobRequest.getRepeatCount());
                } else {
                    scheduleBuilder.repeatForever();
                }

                // Handle start time
                Date startDate = (jobRequest.getStartTime() != null)
                        ? jobRequest.getStartDateAt()
                        : new Date();

                trigger = TriggerBuilder.newTrigger()
                        .withIdentity(jobRequest.getJobName() + "_trigger", username)
                        .startAt(startDate)
                        .withSchedule(scheduleBuilder)
                        .build();
            }

            if (scheduler.checkExists(jobDetail.getKey())) {
                log.warn("Job '{}' already exists for user '{}'", jobRequest.getJobName(), username);
                return false;
            }

            Date dt = scheduler.scheduleJob(jobDetail, trigger);
            log.info("Job '{}' scheduled successfully for user '{}' at {}", jobRequest.getJobName(), username, dt);
            return true;

        } catch (SchedulerException e) {
            log.error("Error scheduling job for user '{}'", username, e);
            throw new RuntimeException("Error scheduling job: " + e.getMessage());
        }
    }

    /**
     * Pauses a job.
     */
    public void pauseJob(String jobName, String username) throws SchedulerException {
        JobKey key = JobKey.jobKey(jobName, username);
        if (scheduler.checkExists(key)) {
            scheduler.pauseJob(key);
            log.info("Paused job '{}' for user '{}'", jobName, username);
        }
    }


    public void resumeJob(String jobName, String username) throws SchedulerException {
        JobKey key = JobKey.jobKey(jobName, username);
        if (scheduler.checkExists(key)) {
            scheduler.resumeJob(key);
            log.info("Resumed job '{}' for user '{}'", jobName, username);
        }
    }


    public boolean deleteJob(String jobName, String username) throws SchedulerException {
        JobKey key = JobKey.jobKey(jobName, username);
        if (scheduler.checkExists(key)) {
            boolean deleted = scheduler.deleteJob(key);
            log.info("Deleted job '{}' for user '{}'", jobName, username);
            return deleted;
        }
        return false;
    }

    public Set<JobKey> listJobs(String username) throws SchedulerException {
        return scheduler.getJobKeys(GroupMatcher.jobGroupEquals(username));
    }
}