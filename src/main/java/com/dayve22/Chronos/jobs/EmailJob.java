package com.dayve22.Chronos.jobs;

import lombok.extern.slf4j.Slf4j;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.scheduling.quartz.QuartzJobBean;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class EmailJob extends QuartzJobBean {

    @Override
    protected void executeInternal(JobExecutionContext context) throws JobExecutionException {
        String jobName = context.getJobDetail().getKey().getName();
        log.info("Starting Job: {}", jobName);

        try {
            log.info("Sending Email batch...");
            Thread.sleep(2000);

            if (Math.random() < 0.2) {
                throw new RuntimeException("SMTP Server not responding");
            }

            log.info("Job {} completed successfully.", jobName);

        } catch (Exception e) {
            log.error("Job failed: {}", e.getMessage());

            JobExecutionException je = new JobExecutionException(e);

            int retries = context.getJobDetail().getJobDataMap().getIntValue("retries");
            if (retries < 3) {
                log.info("Retrying job... Attempt {}", retries + 1);
                context.getJobDetail().getJobDataMap().put("retries", retries + 1);
                je.setRefireImmediately(true);
                throw je;
            } else {
                log.error("Job failed after 3 retries.");
            }
        }
    }
}
