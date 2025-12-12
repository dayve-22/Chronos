package com.dayve22.Chronos.payload;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;
import java.time.ZoneId;

@Data
public class JobRequest {

    @NotNull
    private String jobName;

    @NotNull
    private String jobGroup;

    @NotNull
    private Class<? extends org.quartz.Job> jobClass;

    private String cronExpression;
    private Long repeatIntervalInSeconds;
    private Integer repeatCount;

    private LocalDateTime startTime;

    private String command;

    public java.util.Date getStartDateAt() {
        return java.util.Date.from(startTime.atZone(ZoneId.systemDefault()).toInstant());
    }
}
