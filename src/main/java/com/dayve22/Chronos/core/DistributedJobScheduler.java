package com.dayve22.Chronos.core;

import com.dayve22.Chronos.entity.*;
import com.dayve22.Chronos.executor.JobExecutor;
import com.dayve22.Chronos.repository.JobExecutionRepository;
import com.dayve22.Chronos.repository.JobRepository;
import jakarta.persistence.OptimisticLockException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.aop.framework.AopContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.*;

@Service
public class DistributedJobScheduler {

    private static final Logger logger = LoggerFactory.getLogger(DistributedJobScheduler.class);
    private static final int BATCH_SIZE = 100;
    private static final int LOOK_AHEAD_SECONDS = 10;

    @Autowired
    private JobRepository jobRepository;

    @Autowired
    private JobExecutionRepository jobExecutionRepository;

    @Autowired
    private JobExecutor jobExecutor;

    @Autowired
    private ScheduleCalculator scheduleCalculator;

    // Use a thread pool with core size based on available processors
    // Max pool size should be higher to handle bursts
    private final ExecutorService executorService = new ThreadPoolExecutor(
            Runtime.getRuntime().availableProcessors() * 2,
            Runtime.getRuntime().availableProcessors() * 4,
            60L, TimeUnit.SECONDS,
            new LinkedBlockingQueue<>(1000),
            new ThreadPoolExecutor.CallerRunsPolicy()
    );

    // High-frequency job scheduler (for jobs running every few seconds)
    private final ScheduledExecutorService highFrequencyExecutor =
            Executors.newScheduledThreadPool(Runtime.getRuntime().availableProcessors());

    // Polling scheduler - runs every second to fetch jobs
    @Scheduled(fixedRate = 1000)
    public void pollAndScheduleJobs() {
        try {
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime lookAhead = now.plusSeconds(LOOK_AHEAD_SECONDS);

            logger.info("Polling for jobs... Now: {}, LookAhead: {}", now, lookAhead);

            // Fetch jobs that need to run in the next window
            List<Job> jobsToRun = jobRepository.findJobsToRun(now, lookAhead, BATCH_SIZE);

            logger.info("Found {} jobs to schedule", jobsToRun.size());

            for (Job job : jobsToRun) {
                logger.info("Scheduling job: {} - {}, next run: {}",
                        job.getId(), job.getName(), job.getNextRunTime());
                scheduleJobExecution(job);
            }

        } catch (Exception e) {
            logger.error("Error in job polling", e);
        }
    }
    private DistributedJobScheduler currentProxy() {
        return (DistributedJobScheduler) AopContext.currentProxy();
    }
    private void scheduleJobExecution(Job job) {
        long delay = calculateDelay(job.getNextRunTime());
        final long delayMillis = Math.max(0, delay); // Run immediately if past due

        final Long jobId = job.getId();

        if (job.getIntervalSeconds() != null && job.getIntervalSeconds() < 30) {
            highFrequencyExecutor.schedule(() -> currentProxy().executeJob(jobId), delayMillis, TimeUnit.MILLISECONDS);
        } else {
            executorService.submit(() -> {
                try {
                    if (delayMillis > 0) Thread.sleep(delayMillis);

                    currentProxy().executeJob(jobId);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    logger.warn("Job execution interrupted for job: {}", jobId);
                }
            });
        }
    }

    @Transactional
    public void executeJob(Long jobId) {
        try {
            // Use pessimistic locking to prevent duplicate execution
            Job job = jobRepository.findByIdWithLock(jobId);

            if (job == null) {
                logger.warn("Job not found: {}", jobId);
                return;
            }

            LocalDateTime now = LocalDateTime.now();

            // Double-check the job should still run (another instance might have picked it up)
            if (job.getNextRunTime().isAfter(now.plusSeconds(1)) ||
                    job.getStatus() != JobStatus.ACTIVE) {
                return;
            }

            logger.info("Executing job: {} - {}", jobId, job.getName());

            // Create execution record
            JobExecution execution = new JobExecution();
            execution.setJob(job);
            execution.setStartTime(now);
            execution.setStatus(ExecutionStatus.RUNNING);
            execution.setAttemptNumber(job.getRetryCount() + 1);
            jobExecutionRepository.save(execution);

            // Update job's next run time BEFORE execution
            updateNextRunTime(job);
            job.setLastRunTime(now);
            jobRepository.save(job);

            // Execute the job asynchronously
            CompletableFuture.runAsync(() -> {
                try {
                    jobExecutor.execute(job, execution);
                    handleSuccessfulExecution(execution);
                } catch (Exception e) {
                    handleFailedExecution(job, execution, e);
                }
            }, executorService);

        } catch (OptimisticLockException e) {
            logger.debug("Job {} already picked up by another instance", jobId);
        } catch (Exception e) {
            logger.error("Error executing job: {}", jobId, e);
        }
    }

    @Transactional
    protected void handleSuccessfulExecution(JobExecution execution) {
        execution.setEndTime(LocalDateTime.now());
        execution.setStatus(ExecutionStatus.SUCCESS);
        jobExecutionRepository.save(execution);

        Job job = execution.getJob();
        job.setRetryCount(0); // Reset retry count on success

        // Mark one-time jobs as completed
        if (job.getScheduleType() == ScheduleType.ONE_TIME) {
            job.setStatus(JobStatus.COMPLETED);
        }

        jobRepository.save(job);
    }

    @Transactional
    protected void handleFailedExecution(Job job, JobExecution execution, Exception e) {
        execution.setEndTime(LocalDateTime.now());
        execution.setStatus(ExecutionStatus.FAILED);
        execution.setErrorMessage(e.getMessage());
        jobExecutionRepository.save(execution);

        job = jobRepository.findById(job.getId()).orElse(null);
        if (job == null) return;

        job.setRetryCount(job.getRetryCount() + 1);

        if (job.getRetryCount() >= job.getMaxRetries()) {
            logger.error("Job {} failed after {} retries", job.getId(), job.getMaxRetries());
            job.setStatus(JobStatus.FAILED);
        } else {
            // Schedule retry with exponential backoff
            int delaySeconds = job.getRetryDelaySeconds() * (int) Math.pow(2, job.getRetryCount() - 1);
            job.setNextRunTime(LocalDateTime.now().plusSeconds(delaySeconds));
            logger.info("Scheduling retry {} for job {} in {} seconds",
                    job.getRetryCount(), job.getId(), delaySeconds);
        }

        jobRepository.save(job);
    }

    private void updateNextRunTime(Job job) {
        if (job.getScheduleType() == ScheduleType.ONE_TIME) {
            return; // One-time jobs don't need next run time
        }

        LocalDateTime nextRun = scheduleCalculator.calculateNextRunTime(job);
        job.setNextRunTime(nextRun);
    }

    private long calculateDelay(LocalDateTime nextRunTime) {
        return java.time.Duration.between(LocalDateTime.now(), nextRunTime).toMillis();
    }

    public void shutdown() {
        executorService.shutdown();
        highFrequencyExecutor.shutdown();
        try {
            if (!executorService.awaitTermination(60, TimeUnit.SECONDS)) {
                executorService.shutdownNow();
            }
            if (!highFrequencyExecutor.awaitTermination(60, TimeUnit.SECONDS)) {
                highFrequencyExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executorService.shutdownNow();
            highFrequencyExecutor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}