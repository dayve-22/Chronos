package com.dayve22.Chronos.listener;

import com.dayve22.Chronos.entity.JobExecutionLog;
import com.dayve22.Chronos.repository.JobLogRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.quartz.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;


@Component
public class JobLifecycleListener implements JobListener {

    private static final Logger log = LoggerFactory.getLogger(JobLifecycleListener.class);

    @Autowired
    private JobLogRepository jobLogRepository;

    @Autowired
    private Scheduler scheduler;

    // Register this listener after all beans are initialized
    @PostConstruct
    public void init() {
        try {
            scheduler.getListenerManager().addJobListener(this);
            log.info("✓ JobLifecycleListener registered successfully");
        } catch (SchedulerException e) {
            log.error("✗ Failed to register JobLifecycleListener", e);
        }
    }

    @Override
    public String getName() {
        return "JobLifecycleListener";
    }

    @Override
    public void jobToBeExecuted(JobExecutionContext context) {
        String jobId = context.getJobDetail().getKey().getName();
        log.info("⏱ Job about to execute: {}", jobId);

        try {
            JobExecutionLog jobLog = new JobExecutionLog();
            jobLog.setJobId(jobId);
            jobLog.setStatus("RUNNING");
            jobLog.setStartTime(LocalDateTime.now());
            jobLogRepository.save(jobLog);
        } catch (Exception e) {
            log.error("Failed to log job start", e);
        }
    }

    @Override
    public void jobExecutionVetoed(JobExecutionContext context) {
        String jobId = context.getJobDetail().getKey().getName();
        log.warn("⛔ Job execution vetoed: {}", jobId);

        try {
            JobExecutionLog jobLog = new JobExecutionLog();
            jobLog.setJobId(jobId);
            jobLog.setStatus("VETOED");
            jobLog.setStartTime(LocalDateTime.now());
            jobLog.setEndTime(LocalDateTime.now());
            jobLogRepository.save(jobLog);
        } catch (Exception e) {
            log.error("Failed to log vetoed job", e);
        }
    }

    @Override
    public void jobWasExecuted(JobExecutionContext context, JobExecutionException jobException) {
        String jobId = context.getJobDetail().getKey().getName();
        boolean success = (jobException == null);

        log.info("✓ Job executed: {} - Success: {}", jobId, success);

        try {
            // Find the most recent RUNNING log for this job
            JobExecutionLog jobLog = jobLogRepository.findTopByJobIdAndStatusOrderByStartTimeDesc(jobId, "RUNNING")
                    .orElseGet(() -> {
                        JobExecutionLog newLog = new JobExecutionLog();
                        newLog.setJobId(jobId);
                        newLog.setStartTime(LocalDateTime.now());
                        return newLog;
                    });

            jobLog.setEndTime(LocalDateTime.now());
            jobLog.setStatus(success ? "SUCCESS" : "FAILED");

            if (jobException != null) {
                jobLog.setErrorMessage(jobException.getMessage());
            }

            // Get result if available
            Object result = context.getResult();
            if (result != null) {
                jobLog.setOutput(result.toString());
            }

            jobLogRepository.save(jobLog);

        } catch (Exception e) {
            log.error("Failed to log job completion", e);
        }
    }
}