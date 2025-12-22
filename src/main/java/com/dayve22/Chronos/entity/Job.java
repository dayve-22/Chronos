package com.dayve22.Chronos.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "jobs", indexes = {
        @Index(name = "idx_next_run", columnList = "nextRunTime"),
        @Index(name = "idx_user_status", columnList = "user_id,status")
})
public class Job {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private JobType type; // COMMAND, EMAIL

    @Column(columnDefinition = "TEXT")
    private String jobData; // JSON string containing job-specific data

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ScheduleType scheduleType; // ONE_TIME, RECURRING

    private String cronExpression; // For recurring jobs
    private Integer intervalSeconds; // For simple recurring jobs

    @Column(nullable = false)
    private LocalDateTime nextRunTime;

    private LocalDateTime lastRunTime;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private JobStatus status; // ACTIVE, PAUSED, COMPLETED, FAILED

    private Integer retryCount = 0;
    private Integer maxRetries = 3;
    private Integer retryDelaySeconds = 60;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @Version
    private Long version; // Optimistic locking

    // Getters and setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public JobType getType() { return type; }
    public void setType(JobType type) { this.type = type; }
    public String getJobData() { return jobData; }
    public void setJobData(String jobData) { this.jobData = jobData; }
    public ScheduleType getScheduleType() { return scheduleType; }
    public void setScheduleType(ScheduleType scheduleType) { this.scheduleType = scheduleType; }
    public String getCronExpression() { return cronExpression; }
    public void setCronExpression(String cronExpression) { this.cronExpression = cronExpression; }
    public Integer getIntervalSeconds() { return intervalSeconds; }
    public void setIntervalSeconds(Integer intervalSeconds) { this.intervalSeconds = intervalSeconds; }
    public LocalDateTime getNextRunTime() { return nextRunTime; }
    public void setNextRunTime(LocalDateTime nextRunTime) { this.nextRunTime = nextRunTime; }
    public LocalDateTime getLastRunTime() { return lastRunTime; }
    public void setLastRunTime(LocalDateTime lastRunTime) { this.lastRunTime = lastRunTime; }
    public JobStatus getStatus() { return status; }
    public void setStatus(JobStatus status) { this.status = status; }
    public Integer getRetryCount() { return retryCount; }
    public void setRetryCount(Integer retryCount) { this.retryCount = retryCount; }
    public Integer getMaxRetries() { return maxRetries; }
    public void setMaxRetries(Integer maxRetries) { this.maxRetries = maxRetries; }
    public Integer getRetryDelaySeconds() { return retryDelaySeconds; }
    public void setRetryDelaySeconds(Integer retryDelaySeconds) { this.retryDelaySeconds = retryDelaySeconds; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    public Long getVersion() { return version; }
    public void setVersion(Long version) { this.version = version; }
}
