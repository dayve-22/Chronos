package com.dayve22.Chronos.controller;

import com.dayve22.Chronos.entity.JobExecutionLog;
import com.dayve22.Chronos.repository.JobLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/logs")
@RequiredArgsConstructor
public class LogController {

    private final JobLogRepository logRepository;

    @GetMapping
    public ResponseEntity<List<JobExecutionLog>> getAllLogs() {
        return ResponseEntity.ok(logRepository.findAll());
    }

//    @GetMapping("/{jobName}")
//    public ResponseEntity<List<JobExecutionLog>> getLogsByJob(@PathVariable String jobName) {
//        return ResponseEntity.ok(logRepository.findByJobName(jobName));
//    }
}