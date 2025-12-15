package com.dayve22.Chronos.controller;

import com.dayve22.Chronos.dto.ApiResponse;
import com.dayve22.Chronos.dto.JobRequest;
import com.dayve22.Chronos.service.JobService;
import lombok.RequiredArgsConstructor;
import org.quartz.SchedulerException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/jobs")
@RequiredArgsConstructor
public class JobController {

    private final JobService jobService;

    @PostMapping
    public ResponseEntity<ApiResponse> scheduleJob(@RequestBody JobRequest request) {
        try {
            String jobId;
            if ("RECURRING".equalsIgnoreCase(request.getType())) {
                if (request.getCronExpression() == null) {
                    throw new IllegalArgumentException("Cron expression required for recurring jobs");
                }
                jobId = jobService.scheduleCronJob(request.getCommand(), request.getCronExpression());
            } else {
                if (request.getExecuteAt() == null) {
                    throw new IllegalArgumentException("Execution time required for one-time jobs");
                }
                jobId = jobService.scheduleOneTimeJob(request.getCommand(), request.getExecuteAt());
            }
            return ResponseEntity.ok(new ApiResponse(true, "Job Scheduled Successfully", jobId));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new ApiResponse(false, e.getMessage(), null));
        }
    }

    // 2. Pause a Job
    @PutMapping("/{jobId}/pause")
    public ResponseEntity<ApiResponse> pauseJob(@PathVariable String jobId) {
        try {
            jobService.pauseJob(jobId);
            return ResponseEntity.ok(new ApiResponse(true, "Job Paused", null));
        } catch (SchedulerException e) {
            return ResponseEntity.internalServerError().body(new ApiResponse(false, e.getMessage(), null));
        }
    }

    // 3. Resume a Job
    @PutMapping("/{jobId}/resume")
    public ResponseEntity<ApiResponse> resumeJob(@PathVariable String jobId) {
        try {
            jobService.resumeJob(jobId);
            return ResponseEntity.ok(new ApiResponse(true, "Job Resumed", null));
        } catch (SchedulerException e) {
            return ResponseEntity.internalServerError().body(new ApiResponse(false, e.getMessage(), null));
        }
    }

    // 4. Delete/Stop a Job
    @DeleteMapping("/{jobId}")
    public ResponseEntity<ApiResponse> deleteJob(@PathVariable String jobId) {
        try {
            jobService.deleteJob(jobId);
            return ResponseEntity.ok(new ApiResponse(true, "Job Deleted", null));
        } catch (SchedulerException e) {
            return ResponseEntity.internalServerError().body(new ApiResponse(false, e.getMessage(), null));
        }
    }
}