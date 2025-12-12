package com.dayve22.Chronos.controller;

import com.dayve22.Chronos.payload.ApiResponse;
import com.dayve22.Chronos.payload.JobRequest;
import com.dayve22.Chronos.service.JobService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.quartz.SchedulerException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/jobs")
@RequiredArgsConstructor
public class JobController {

    private final JobService jobService;

    @PostMapping("/schedule")
    public ResponseEntity<ApiResponse> scheduleJob(@Valid @RequestBody JobRequest jobRequest) {
        boolean success = jobService.scheduleJob(jobRequest);
        if (success) {
            return ResponseEntity.ok(new ApiResponse(true, "Job scheduled successfully"));
        } else {
            return ResponseEntity.badRequest().body(new ApiResponse(false, "Job scheduling failed. Check logs."));
        }
    }

    @PostMapping("/pause")
    public ResponseEntity<ApiResponse> pauseJob(@RequestParam String name, @RequestParam String group) {
        try {
            jobService.pauseJob(name, group);
            return ResponseEntity.ok(new ApiResponse(true, "Job paused"));
        } catch (SchedulerException e) {
            return ResponseEntity.internalServerError().body(new ApiResponse(false, "Error pausing job"));
        }
    }

    @PostMapping("/resume")
    public ResponseEntity<ApiResponse> resumeJob(@RequestParam String name, @RequestParam String group) {
        try {
            jobService.resumeJob(name, group);
            return ResponseEntity.ok(new ApiResponse(true, "Job resumed"));
        } catch (SchedulerException e) {
            return ResponseEntity.internalServerError().body(new ApiResponse(false, "Error resuming job"));
        }
    }

    @DeleteMapping("/delete")
    public ResponseEntity<ApiResponse> deleteJob(@RequestParam String name, @RequestParam String group) {
        try {
            boolean deleted = jobService.deleteJob(name, group);
            return ResponseEntity.ok(new ApiResponse(deleted, deleted ? "Job deleted" : "Job not found"));
        } catch (SchedulerException e) {
            return ResponseEntity.internalServerError().body(new ApiResponse(false, "Error deleting job"));
        }
    }
}