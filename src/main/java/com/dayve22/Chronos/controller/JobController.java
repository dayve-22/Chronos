package com.dayve22.Chronos.controller;

import com.dayve22.Chronos.entity.*;
import com.dayve22.Chronos.service.JobService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/jobs")
public class JobController {

    @Autowired
    private JobService jobService;

    @PostMapping
    public ResponseEntity<Job> createJob(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody JobCreateRequest request) throws Exception {
        Job job = jobService.createJob(userDetails.getUsername(), request);
        return ResponseEntity.ok(job);
    }

    @GetMapping
    public ResponseEntity<List<Job>> getUserJobs(@AuthenticationPrincipal UserDetails userDetails) {
        List<Job> jobs = jobService.getUserJobs(userDetails.getUsername());
        return ResponseEntity.ok(jobs);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Job> getJob(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long id) {
        Job job = jobService.getJob(id, userDetails.getUsername());
        return ResponseEntity.ok(job);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Job> updateJob(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long id,
            @RequestBody JobUpdateRequest request) throws Exception {
        Job job = jobService.updateJob(id, userDetails.getUsername(), request);
        return ResponseEntity.ok(job);
    }

    @PostMapping("/{id}/pause")
    public ResponseEntity<Job> pauseJob(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long id) {
        Job job = jobService.pauseJob(id, userDetails.getUsername());
        return ResponseEntity.ok(job);
    }

    @PostMapping("/{id}/resume")
    public ResponseEntity<Job> resumeJob(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long id) {
        Job job = jobService.resumeJob(id, userDetails.getUsername());
        return ResponseEntity.ok(job);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteJob(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long id) {
        jobService.deleteJob(id, userDetails.getUsername());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/executions")
    public ResponseEntity<List<JobExecution>> getJobExecutions(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long id) {
        List<JobExecution> executions = jobService.getJobExecutions(id, userDetails.getUsername());
        return ResponseEntity.ok(executions);
    }
}
