package com.dayve22.Chronos.entity;

import com.dayve22.Chronos.executor.CommandJobData;
import com.dayve22.Chronos.executor.EmailJobData;

import java.time.LocalDateTime;

public class JobCreateRequest {
    private String name;
    private JobType type;
    private ScheduleType scheduleType;
    private LocalDateTime runAt;
    private String cronExpression;
    private Integer intervalSeconds;
    private Integer maxRetries;
    private Integer retryDelaySeconds;
    private CommandJobData commandData;
    private EmailJobData emailData;

    // Getters and setters
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public JobType getType() { return type; }
    public void setType(JobType type) { this.type = type; }
    public ScheduleType getScheduleType() { return scheduleType; }
    public void setScheduleType(ScheduleType scheduleType) { this.scheduleType = scheduleType; }
    public LocalDateTime getRunAt() { return runAt; }
    public void setRunAt(LocalDateTime runAt) { this.runAt = runAt; }
    public String getCronExpression() { return cronExpression; }
    public void setCronExpression(String cronExpression) { this.cronExpression = cronExpression; }
    public Integer getIntervalSeconds() { return intervalSeconds; }
    public void setIntervalSeconds(Integer intervalSeconds) { this.intervalSeconds = intervalSeconds; }
    public Integer getMaxRetries() { return maxRetries; }
    public void setMaxRetries(Integer maxRetries) { this.maxRetries = maxRetries; }
    public Integer getRetryDelaySeconds() { return retryDelaySeconds; }
    public void setRetryDelaySeconds(Integer retryDelaySeconds) { this.retryDelaySeconds = retryDelaySeconds; }
    public CommandJobData getCommandData() { return commandData; }
    public void setCommandData(CommandJobData commandData) { this.commandData = commandData; }
    public EmailJobData getEmailData() { return emailData; }
    public void setEmailData(EmailJobData emailData) { this.emailData = emailData; }
}
