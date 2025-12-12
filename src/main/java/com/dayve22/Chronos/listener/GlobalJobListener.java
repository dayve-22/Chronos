package com.dayve22.Chronos.listener;

import com.dayve22.Chronos.entity.ExecutionLog;
import com.dayve22.Chronos.repository.ExecutionLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.JobListener;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
@Slf4j
@RequiredArgsConstructor
public class GlobalJobListener implements JobListener {

    private final ExecutionLogRepository logRepository;

    @Override
    public String getName() {
        return "GlobalJobListener";
    }

    @Override
    public void jobToBeExecuted(JobExecutionContext context) {
        // Can log start time here
    }

    @Override
    public void jobExecutionVetoed(JobExecutionContext context) {
        // If job was cancelled by a trigger
    }

    @Override
    public void jobWasExecuted(JobExecutionContext context, JobExecutionException jobException) {
        ExecutionLog executionLog = new ExecutionLog();
        executionLog.setJobName(context.getJobDetail().getKey().getName());
        executionLog.setJobGroup(context.getJobDetail().getKey().getGroup());
        executionLog.setStartTime(LocalDateTime.now());
        executionLog.setEndTime(LocalDateTime.now());

        if (jobException != null) {
            executionLog.setStatus("FAILED");
            executionLog.setErrorMessage(jobException.getMessage());
        } else {
            executionLog.setStatus("SUCCESS");
        }

        logRepository.save(executionLog);
        log.info("Saved execution log for job: {}", executionLog.getJobName());
    }
}
