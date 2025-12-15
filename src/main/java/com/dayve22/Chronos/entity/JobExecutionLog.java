package com.dayve22.Chronos.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "job_logs", indexes = {
        @Index(name = "idx_job_id_status", columnList = "jobId, status, startTime")
})
@Data
public class JobExecutionLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String jobId;

    @Column(nullable = false)
    private String status; // RUNNING, SUCCESS, FAILED, VETOED

    @Column(nullable = false)
    private LocalDateTime startTime;

    private LocalDateTime endTime;

    @Column(length = 10000)
    private String output;

    @Column(length = 5000)
    private String errorMessage;

    @Transient
    public Long getDurationMillis() {
        if (startTime != null && endTime != null) {
            return java.time.Duration.between(startTime, endTime).toMillis();
        }
        return null;
    }
}