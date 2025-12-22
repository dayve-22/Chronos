package com.dayve22.Chronos.entity;

public class JobUpdateRequest {
    private String name;
    private JobStatus status;
    private String cronExpression;
    private Integer intervalSeconds;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public JobStatus getStatus() { return status; }
    public void setStatus(JobStatus status) { this.status = status; }
    public String getCronExpression() { return cronExpression; }
    public void setCronExpression(String cronExpression) { this.cronExpression = cronExpression; }
    public Integer getIntervalSeconds() { return intervalSeconds; }
    public void setIntervalSeconds(Integer intervalSeconds) { this.intervalSeconds = intervalSeconds; }
}
