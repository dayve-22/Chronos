package com.dayve22.Chronos.service;

import com.dayve22.Chronos.core.ScheduleCalculator;
import com.dayve22.Chronos.entity.*;
import com.dayve22.Chronos.repository.JobExecutionRepository;
import com.dayve22.Chronos.repository.JobRepository;
import com.dayve22.Chronos.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional; // Ensure this is imported

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

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Transactional
    public Job createJob(String username, JobCreateRequest request) throws Exception {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Job job = new Job();
        job.setUser(user);
        job.setUser(user);
        job.setName(request.getName());
        job.setType(request.getType());
        job.setScheduleType(request.getScheduleType());
        job.setStatus(JobStatus.ACTIVE);
        job.setCreatedAt(LocalDateTime.now());
        job.setUpdatedAt(LocalDateTime.now());

        // Set schedule
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

        // Set retry configuration
        if (request.getMaxRetries() != null) {
            job.setMaxRetries(request.getMaxRetries());
        }
        if (request.getRetryDelaySeconds() != null) {
            job.setRetryDelaySeconds(request.getRetryDelaySeconds());
        }

        // Set job data based on type
        if (request.getType() == JobType.COMMAND) {
            job.setJobData(objectMapper.writeValueAsString(request.getCommandData()));
        } else if (request.getType() == JobType.EMAIL) {
            job.setJobData(objectMapper.writeValueAsString(request.getEmailData()));
        }

        return jobRepository.save(job);
    }

    public Job getJob(Long jobId, Long userId) {
        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new RuntimeException("Job not found"));

        if (!job.getUser().getId().equals(userId)) {
            throw new RuntimeException("Unauthorized access to job");
        }

        return job;
    }

    public List<Job> getUserJobs(Long userId) {
        return jobRepository.findByUserId(userId);
    }

    @Transactional
    public Job updateJob(Long jobId, Long userId, JobUpdateRequest request) throws Exception {
        Job job = getJob(jobId, userId);

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
    public void deleteJob(Long jobId, Long userId) {
        Job job = getJob(jobId, userId);
        jobRepository.delete(job);
    }

    public List<JobExecution> getJobExecutions(Long jobId, Long userId) {
        getJob(jobId, userId); // Check authorization
        return jobExecutionRepository.findTop10ByJobId(jobId);
    }
}