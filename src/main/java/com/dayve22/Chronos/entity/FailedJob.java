package com.dayve22.Chronos.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
public class FailedJob {
    @Id
    @GeneratedValue
    private Long id;

    @ManyToOne
    private Job originalJob;

    private LocalDateTime failedAt;

    @Column(columnDefinition = "TEXT")
    private String errorMessage;

    @Column(columnDefinition = "TEXT")
    private String stackTrace;

    private Boolean reviewed = false;
}
