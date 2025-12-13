package com.dayve22.Chronos.controller;

import com.dayve22.Chronos.entity.ExecutionLog;
import com.dayve22.Chronos.payload.ApiResponse;
import com.dayve22.Chronos.payload.JobRequest;
import com.dayve22.Chronos.service.JobService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.quartz.SchedulerException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/jobs")
@RequiredArgsConstructor
public class JobController {

    private final JobService jobService;

    @PostMapping("/schedule")
    public ResponseEntity<ApiResponse> scheduleJob(@Valid @RequestBody JobRequest jobRequest, Principal principal) {
        // Principal holds the username of the logged-in user (from JWT)
        String username = principal.getName();

        try {
            boolean success = jobService.scheduleJob(jobRequest, username);
            if (success) {
                return ResponseEntity.ok(new ApiResponse(true, "Job scheduled successfully"));
            } else {
                return ResponseEntity.badRequest().body(new ApiResponse(false, "Job already exists"));
            }
        } catch (RuntimeException e) {
            // Catch the Quota Exceeded exception
            return ResponseEntity.status(429).body(new ApiResponse(false, e.getMessage()));
        }
    }

    @DeleteMapping("/delete")
    public ResponseEntity<ApiResponse> deleteJob(@RequestParam String name, Principal principal) {
        try {
            String username = principal.getName();
            boolean deleted = jobService.deleteJob(name, username);
            return ResponseEntity.ok(new ApiResponse(deleted, deleted ? "Job deleted" : "Job not found"));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(new ApiResponse(false, "Error deleting job"));
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

    @GetMapping("/list")
    public ResponseEntity<List<Map<String, Object>>> listMyJobs(Principal principal) {
        String username = principal.getName();
        List<Map<String, Object>> jobs = jobService.getUserJobs(username);
        return ResponseEntity.ok(jobs);
    }


    @GetMapping("/history")
    public ResponseEntity<List<ExecutionLog>> getJobHistory(
            @RequestParam String start,
            @RequestParam String end,
            Principal principal) {

        String username = principal.getName();

        // Parse the date strings from the URL
        DateTimeFormatter formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
        LocalDateTime startDate = LocalDateTime.parse(start, formatter);
        LocalDateTime endDate = LocalDateTime.parse(end, formatter);

        List<ExecutionLog> logs = jobService.getJobHistory(username, startDate, endDate);
        return ResponseEntity.ok(logs);
    }

}