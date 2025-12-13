package com.dayve22.Chronos.service;

import com.dayve22.Chronos.entity.ExecutionLog;
import com.dayve22.Chronos.payload.JobRequest;
import com.dayve22.Chronos.repository.ExecutionLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.quartz.*;
import org.quartz.impl.matchers.GroupMatcher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

@Service
@Slf4j
@RequiredArgsConstructor
public class JobService {

    private final Scheduler scheduler;
    private final ExecutionLogRepository logRepository;

    private static final int MAX_JOBS_PER_USER = 20;

    @Transactional  // ← ADD THIS
    public boolean scheduleJob(JobRequest jobRequest, String username) {
        try {
            // Check quota first
            int currentJobCount = scheduler.getJobKeys(GroupMatcher.jobGroupEquals(username)).size();
            if (currentJobCount >= MAX_JOBS_PER_USER) {
                log.warn("User '{}' has reached the job quota of {}", username, MAX_JOBS_PER_USER);
                throw new RuntimeException("Quota Exceeded! You can only have " + MAX_JOBS_PER_USER + " active jobs.");
            }

            // Check if job already exists BEFORE creating it
            JobKey jobKey = new JobKey(jobRequest.getJobName(), username);
            if (scheduler.checkExists(jobKey)) {
                log.warn("Job '{}' already exists for user '{}'", jobRequest.getJobName(), username);
                return false;
            }

            Class<? extends Job> jobClass = jobRequest.getJobClass();

            // Build job WITHOUT storeDurably()
            JobDetail jobDetail = JobBuilder.newJob(jobClass)
                    .withIdentity(jobRequest.getJobName(), username)
                    .storeDurably(false)  // ← CHANGE THIS: Don't store separately
                    .requestRecovery(true)
                    .build();

            // Add job data
            if (jobRequest.getCommand() != null) {
                jobDetail.getJobDataMap().put("command", jobRequest.getCommand());
            }
            jobDetail.getJobDataMap().put("retries", 0);

            // Build trigger
            Trigger trigger;

            if (jobRequest.getCronExpression() != null && !jobRequest.getCronExpression().isEmpty()) {
                trigger = TriggerBuilder.newTrigger()
                        .withIdentity(jobRequest.getJobName() + "_trigger", username)
                        .withSchedule(CronScheduleBuilder.cronSchedule(jobRequest.getCronExpression())
                                .withMisfireHandlingInstructionFireAndProceed())  // ← ADD MISFIRE HANDLING
                        .build();
            } else {
                SimpleScheduleBuilder scheduleBuilder = SimpleScheduleBuilder.simpleSchedule()
                        .withIntervalInSeconds(jobRequest.getRepeatIntervalInSeconds().intValue())
                        .withMisfireHandlingInstructionFireNow();  // ← ADD MISFIRE HANDLING

                if (jobRequest.getRepeatCount() != null && jobRequest.getRepeatCount() == -1) {
                    scheduleBuilder.repeatForever();
                } else if (jobRequest.getRepeatCount() != null) {
                    scheduleBuilder.withRepeatCount(jobRequest.getRepeatCount());
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

            // Schedule job and trigger together atomically
            Date scheduledTime = scheduler.scheduleJob(jobDetail, trigger);

            log.info("Job '{}' scheduled successfully for user '{}' at {}",
                    jobRequest.getJobName(), username, scheduledTime);

            return true;

        } catch (SchedulerException e) {
            log.error("Error scheduling job for user '{}'", username, e);
            throw new RuntimeException("Error scheduling job: " + e.getMessage());
        }
    }

    @Transactional  // ← ADD THIS
    public void pauseJob(String jobName, String username) throws SchedulerException {
        JobKey key = JobKey.jobKey(jobName, username);
        if (scheduler.checkExists(key)) {
            scheduler.pauseJob(key);
            log.info("Paused job '{}' for user '{}'", jobName, username);
        }
    }

    @Transactional  // ← ADD THIS
    public void resumeJob(String jobName, String username) throws SchedulerException {
        JobKey key = JobKey.jobKey(jobName, username);
        if (scheduler.checkExists(key)) {
            scheduler.resumeJob(key);
            log.info("Resumed job '{}' for user '{}'", jobName, username);
        }
    }

    @Transactional  // ← ADD THIS
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

    public List<Map<String, Object>> getUserJobs(String username) {
        List<Map<String, Object>> jobList = new ArrayList<>();
        try {
            Set<JobKey> jobKeys = scheduler.getJobKeys(GroupMatcher.jobGroupEquals(username));

            for (JobKey jobKey : jobKeys) {
                JobDetail jobDetail = scheduler.getJobDetail(jobKey);
                List<? extends Trigger> triggers = scheduler.getTriggersOfJob(jobKey);

                Map<String, Object> jobMap = new HashMap<>();
                jobMap.put("jobName", jobKey.getName());
                jobMap.put("description", jobDetail.getDescription());
                jobMap.put("jobClass", jobDetail.getJobClass().getName());

                if(jobDetail.getJobDataMap().containsKey("command")) {
                    jobMap.put("command", jobDetail.getJobDataMap().getString("command"));
                }

                if (!triggers.isEmpty()) {
                    jobMap.put("nextFireTime", triggers.get(0).getNextFireTime());
                    jobMap.put("state", scheduler.getTriggerState(triggers.get(0).getKey()));
                }

                jobList.add(jobMap);
            }
        } catch (SchedulerException e) {
            log.error("Error listing jobs", e);
        }
        return jobList;
    }

    public List<ExecutionLog> getJobHistory(String username, LocalDateTime fromDate, LocalDateTime toDate) {
        return logRepository.findByJobGroupAndStartTimeBetween(username, fromDate, toDate);
    }
}