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
            @AuthenticationPrincipal UserDetails userDetails,  // ✅ Changed from User to UserDetails
            @RequestBody JobCreateRequest request) throws Exception {
        Job job = jobService.createJob(userDetails.getUsername(), request);  // ✅ Pass username
        return ResponseEntity.ok(job);
    }

    @GetMapping
    public ResponseEntity<List<Job>> getUserJobs(@AuthenticationPrincipal User user) {
        List<Job> jobs = jobService.getUserJobs(user.getId());
        return ResponseEntity.ok(jobs);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Job> getJob(
            @AuthenticationPrincipal User user,
            @PathVariable Long id) {
        Job job = jobService.getJob(id, user.getId());
        return ResponseEntity.ok(job);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Job> updateJob(
            @AuthenticationPrincipal User user,
            @PathVariable Long id,
            @RequestBody JobUpdateRequest request) throws Exception {
        Job job = jobService.updateJob(id, user.getId(), request);
        return ResponseEntity.ok(job);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteJob(
            @AuthenticationPrincipal User user,
            @PathVariable Long id) {
        jobService.deleteJob(id, user.getId());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/executions")
    public ResponseEntity<List<JobExecution>> getJobExecutions(
            @AuthenticationPrincipal User user,
            @PathVariable Long id) {
        List<JobExecution> executions = jobService.getJobExecutions(id, user.getId());
        return ResponseEntity.ok(executions);
    }
}
