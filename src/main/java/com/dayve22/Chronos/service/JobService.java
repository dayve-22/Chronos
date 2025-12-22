package com.dayve22.Chronos.service;

import com.dayve22.Chronos.core.RedisJobScheduler; // 1. IMPORT THIS
import com.dayve22.Chronos.core.ScheduleCalculator;
import com.dayve22.Chronos.entity.*;
import com.dayve22.Chronos.repository.JobExecutionRepository;
import com.dayve22.Chronos.repository.JobRepository;
import com.dayve22.Chronos.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

@Service
public class JobService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JobRepository jobRepository;

    @Autowired
    private JobExecutionRepository jobExecutionRepository;

    @Autowired
    private ScheduleCalculator scheduleCalculator;

    @Autowired
    private RedisJobScheduler redisScheduler;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Transactional
    public Job createJob(String username, JobCreateRequest request) throws Exception {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Job job = new Job();
        job.setUser(user);
        job.setName(request.getName());
        job.setType(request.getType());
        job.setScheduleType(request.getScheduleType());
        job.setStatus(JobStatus.ACTIVE);
        job.setCreatedAt(LocalDateTime.now());
        job.setUpdatedAt(LocalDateTime.now());

        if (request.getScheduleType() == ScheduleType.ONE_TIME) {
            job.setNextRunTime(request.getRunAt());
        } else {
            if (request.getCronExpression() != null) {
                job.setCronExpression(request.getCronExpression());
            } else if (request.getIntervalSeconds() != null) {
                job.setIntervalSeconds(request.getIntervalSeconds());
            } else {
                throw new IllegalArgumentException("Recurring job must have cron or interval");
            }
            job.setNextRunTime(
                    request.getRunAt() != null ? request.getRunAt() : LocalDateTime.now()
            );
        }

        if (request.getMaxRetries() != null) job.setMaxRetries(request.getMaxRetries());
        if (request.getRetryDelaySeconds() != null) job.setRetryDelaySeconds(request.getRetryDelaySeconds());

        // Set job data
        if (request.getType() == JobType.COMMAND) {
            job.setJobData(objectMapper.writeValueAsString(request.getCommandData()));
        } else if (request.getType() == JobType.EMAIL) {
            job.setJobData(objectMapper.writeValueAsString(request.getEmailData()));
        }

        Job savedJob = jobRepository.save(job);

        if (savedJob.getStatus() == JobStatus.ACTIVE && savedJob.getNextRunTime() != null) {
            redisScheduler.scheduleJob(savedJob.getId(), savedJob.getNextRunTime());
        }

        return savedJob;
    }

    public Job getJob(Long jobId, String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new RuntimeException("Job not found"));

        if (!job.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("Unauthorized access to job");
        }

        return job;
    }

    public List<Job> getUserJobs(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));
        return jobRepository.findByUserId(user.getId());
    }

    @Transactional
    public Job updateJob(Long jobId, String username, JobUpdateRequest request) throws Exception {
        Job job = getJob(jobId, username);

        if (request.getName() != null) {
            job.setName(request.getName());
        }

        if (request.getStatus() != null) {
            job.setStatus(request.getStatus());
        }

        if (request.getCronExpression() != null) {
            job.setCronExpression(request.getCronExpression());
        }

        if (request.getIntervalSeconds() != null) {
            job.setIntervalSeconds(request.getIntervalSeconds());
        }

        job.setUpdatedAt(LocalDateTime.now());

        return jobRepository.save(job);
    }

    @Transactional
    public void deleteJob(Long jobId, String username) {
        Job job = getJob(jobId, username);
        jobRepository.delete(job);
    }

    @Transactional
    public Job pauseJob(Long jobId, String username) {
        Job job = getJob(jobId, username);

        if (job.getStatus() != JobStatus.ACTIVE) {
            throw new RuntimeException("Job is not active. Current status: " + job.getStatus());
        }

        job.setStatus(JobStatus.PAUSED);
        job.setUpdatedAt(LocalDateTime.now());

        return jobRepository.save(job);
    }

    @Transactional
    public Job resumeJob(Long jobId, String username) {
        Job job = getJob(jobId, username);

        if (job.getStatus() != JobStatus.PAUSED) {
            throw new RuntimeException("Job is not paused. Current status: " + job.getStatus());
        }

        job.setStatus(JobStatus.ACTIVE);
        job.setUpdatedAt(LocalDateTime.now());

        // If job was supposed to run while paused, schedule it to run immediately
        if (job.getNextRunTime().isBefore(LocalDateTime.now())) {
            job.setNextRunTime(LocalDateTime.now());
        }

        return jobRepository.save(job);
    }

    public List<JobExecution> getJobExecutions(Long jobId, String username) {
        getJob(jobId, username); // Check authorization
        return jobExecutionRepository.findTop10ByJobId(jobId);
    }
}