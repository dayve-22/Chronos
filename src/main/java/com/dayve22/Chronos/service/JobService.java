package com.dayve22.Chronos.service;

import com.dayve22.Chronos.payload.JobRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.quartz.*;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;

import java.util.Date;

@Service
@Slf4j
@RequiredArgsConstructor
public class JobService {

    private final Scheduler scheduler;
    private final ApplicationContext context;

    public boolean scheduleJob(JobRequest jobRequest) {
        Class<? extends Job> jobClass = jobRequest.getJobClass();

        JobDetail jobDetail = JobBuilder.newJob(jobClass)
                .withIdentity(jobRequest.getJobName(), jobRequest.getJobGroup())
                .storeDurably()
                .requestRecovery()
                .build();

        Trigger trigger;
        if (jobRequest.getCronExpression() != null) {
            trigger = TriggerBuilder.newTrigger()
                    .withIdentity(jobRequest.getJobName() + "_trigger", jobRequest.getJobGroup())
                    .withSchedule(CronScheduleBuilder.cronSchedule(jobRequest.getCronExpression()))
                    .build();
        } else {
            SimpleScheduleBuilder scheduleBuilder = SimpleScheduleBuilder.simpleSchedule()
                    .withIntervalInSeconds(jobRequest.getRepeatIntervalInSeconds().intValue());

            if(jobRequest.getRepeatCount() != null && jobRequest.getRepeatCount() == -1) {
                scheduleBuilder.repeatForever();
            } else {
                scheduleBuilder.withRepeatCount(jobRequest.getRepeatCount());
            }

            trigger = TriggerBuilder.newTrigger()
                    .withIdentity(jobRequest.getJobName() + "_trigger", jobRequest.getJobGroup())
                    .startAt(jobRequest.getStartDateAt())
                    .withSchedule(scheduleBuilder)
                    .build();
        }

        try {
            if (scheduler.checkExists(jobDetail.getKey())) {
                log.warn("Job already exists: {}", jobRequest.getJobName());
                return false;
            }
            Date dt = scheduler.scheduleJob(jobDetail, trigger);
            log.info("Job scheduled successfully for: {}", dt);
            return true;
        } catch (SchedulerException e) {
            log.error("Error scheduling job", e);
            return false;
        }
    }

    public void pauseJob(String name, String group) throws SchedulerException {
        scheduler.pauseJob(JobKey.jobKey(name, group));
    }

    public void resumeJob(String name, String group) throws SchedulerException {
        scheduler.resumeJob(JobKey.jobKey(name, group));
    }

    public boolean deleteJob(String name, String group) throws SchedulerException {
        return scheduler.deleteJob(JobKey.jobKey(name, group));
    }
}
