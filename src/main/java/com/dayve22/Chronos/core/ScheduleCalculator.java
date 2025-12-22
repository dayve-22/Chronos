package com.dayve22.Chronos.core;

import com.dayve22.Chronos.entity.Job;
import org.springframework.scheduling.support.CronExpression;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;

@Component
public class ScheduleCalculator {

    public LocalDateTime calculateNextRunTime(Job job) {
        if (job.getCronExpression() != null && !job.getCronExpression().isEmpty()) {
            return calculateFromCron(job.getCronExpression(), job.getLastRunTime());
        } else if (job.getIntervalSeconds() != null) {
            return calculateFromInterval(job.getIntervalSeconds(), job.getLastRunTime());
        }
        throw new IllegalArgumentException("Job must have either cron expression or interval");
    }

    private LocalDateTime calculateFromCron(String cronExpr, LocalDateTime lastRun) {
        CronExpression cron = CronExpression.parse(cronExpr);
        LocalDateTime base = lastRun != null ? lastRun : LocalDateTime.now();
        ZonedDateTime next = cron.next(base.atZone(ZoneId.systemDefault()));
        return next != null ? next.toLocalDateTime() : null;
    }

    private LocalDateTime calculateFromInterval(int intervalSeconds, LocalDateTime lastRun) {
        LocalDateTime base = lastRun != null ? lastRun : LocalDateTime.now();
        return base.plusSeconds(intervalSeconds);
    }
}