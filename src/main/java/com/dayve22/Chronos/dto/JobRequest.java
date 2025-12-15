package com.dayve22.Chronos.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class JobRequest {
    private String command;       // The shell command to run
    private String type;          // "ONCE" or "RECURRING"
    private LocalDateTime executeAt; // For ONCE type (ISO-8601 format)
    private String cronExpression;   // For RECURRING type
}
