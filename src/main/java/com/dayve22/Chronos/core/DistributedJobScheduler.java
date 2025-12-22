package com.dayve22.Chronos.core;

import com.dayve22.Chronos.entity.*;
import com.dayve22.Chronos.executor.JobExecutor;
import com.dayve22.Chronos.repository.JobExecutionRepository;
import com.dayve22.Chronos.repository.JobRepository;
import jakarta.persistence.OptimisticLockException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.concurrent.*;

@Service
public class DistributedJobScheduler {

    private static final Logger logger = LoggerFactory.getLogger(DistributedJobScheduler.class);
    private static final int BATCH_SIZE = 100;
    private static final int LOOK_AHEAD_SECONDS = 10;

    @Autowired
    private JobRepository jobRepository;

    @Autowired
    @Lazy
    private DistributedJobScheduler self; // Required for @Transactional to work

    @Autowired
    private JobExecutionRepository jobExecutionRepository;

    @Autowired
    private JobExecutor jobExecutor;

    @Autowired
    private ScheduleCalculator scheduleCalculator;

    // --- FIX 1: In-Memory Set to track jobs currently being processed ---
    private final Set<Long> processingJobIds = ConcurrentHashMap.newKeySet();

    private final ExecutorService executorService = new ThreadPoolExecutor(
            Runtime.getRuntime().availableProcessors() * 2,
            Runtime.getRuntime().availableProcessors() * 4,
            60L, TimeUnit.SECONDS,
            new LinkedBlockingQueue<>(1000),
            new ThreadPoolExecutor.CallerRunsPolicy()
    );

    private final ScheduledExecutorService highFrequencyExecutor =
            Executors.newScheduledThreadPool(Runtime.getRuntime().availableProcessors());

    @Scheduled(fixedRate = 1000)
    public void pollAndScheduleJobs() {
        try {
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime lookAhead = now.plusSeconds(LOOK_AHEAD_SECONDS);

            logger.info("Polling for jobs... Now: {}, LookAhead: {}", now, lookAhead);

            // --- FIX 2: Updated to match your reverted Repository signature (3 args) ---
            List<Job> jobsToRun = jobRepository.findJobsToRun(now, lookAhead, BATCH_SIZE);

            logger.info("Found {} jobs to schedule", jobsToRun.size());

            for (Job job : jobsToRun) {
                // --- FIX 3: Check if job is already "In Flight" ---
                if (processingJobIds.contains(job.getId())) {
                    logger.debug("Skipping job {} (already in process)", job.getId());
                    continue;
                }

                // Mark job as processing
                processingJobIds.add(job.getId());

                logger.info("Scheduling job: {} - {}, next run: {}",
                        job.getId(), job.getName(), job.getNextRunTime());
                scheduleJobExecution(job);
            }

        } catch (Exception e) {
            logger.error("Error in job polling", e);
        }
    }

    private void scheduleJobExecution(Job job) {
        long delay = calculateDelay(job.getNextRunTime());
        final long delayMillis = Math.max(0, delay);
        final Long jobId = job.getId();

        if (job.getIntervalSeconds() != null && job.getIntervalSeconds() < 30) {
            highFrequencyExecutor.schedule(() -> self.executeJob(jobId), delayMillis, TimeUnit.MILLISECONDS);
        } else {
            executorService.submit(() -> {
                try {
                    if (delayMillis > 0) {
                        Thread.sleep(delayMillis);
                    }
                    self.executeJob(jobId);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    logger.warn("Job execution interrupted for job: {}", jobId);
                    // Important: remove from set if interrupted before execution
                    processingJobIds.remove(jobId);
                }
            });
        }
    }

    @Transactional
    public void executeJob(Long jobId) {
        try {
            Job job = jobRepository.findByIdWithLock(jobId);

            if (job == null) {
                logger.warn("Job not found: {}", jobId);
                processingJobIds.remove(jobId); // Clean up
                return;
            }

            LocalDateTime now = LocalDateTime.now();

            if (job.getNextRunTime().isAfter(now.plusSeconds(1)) ||
                    job.getStatus() != JobStatus.ACTIVE) {
                processingJobIds.remove(jobId); // Clean up
                return;
            }

            logger.info("Executing job: {} - {}", jobId, job.getName());

            JobExecution execution = new JobExecution();
            execution.setJob(job);
            execution.setStartTime(now);
            execution.setStatus(ExecutionStatus.RUNNING);
            execution.setAttemptNumber(job.getRetryCount() + 1);
            jobExecutionRepository.save(execution);

            updateNextRunTime(job);
            job.setLastRunTime(now);
            jobRepository.save(job);

            // Execute asynchronously
            CompletableFuture.runAsync(() -> {
                try {
                    jobExecutor.execute(job, execution);
                    handleSuccessfulExecution(execution);
                } catch (Exception e) {
                    handleFailedExecution(job, execution, e);
                } finally {
                    // --- FIX 4: CRITICAL - Remove from set after async execution finishes ---
                    processingJobIds.remove(jobId);
                    logger.debug("Job {} processing complete. Removed from lock set.", jobId);
                }
            }, executorService);

        } catch (OptimisticLockException e) {
            logger.debug("Job {} already picked up by another instance", jobId);
            processingJobIds.remove(jobId);
        } catch (Exception e) {
            logger.error("Error executing job: {}", jobId, e);
            processingJobIds.remove(jobId);
        }
    }

    // ... handleSuccessfulExecution, handleFailedExecution, updateNextRunTime, etc. remain the same ...

    @Transactional
    protected void handleSuccessfulExecution(JobExecution execution) {
        // (Keep your existing implementation)
        execution.setEndTime(LocalDateTime.now());
        execution.setStatus(ExecutionStatus.SUCCESS);
        jobExecutionRepository.save(execution);
        Job job = execution.getJob();
        job.setRetryCount(0);
        if (job.getScheduleType() == ScheduleType.ONE_TIME) {
            job.setStatus(JobStatus.COMPLETED);
        }
        jobRepository.save(job);
    }

    @Transactional
    protected void handleFailedExecution(Job job, JobExecution execution, Exception e) {
        // (Keep your existing implementation)
        execution.setEndTime(LocalDateTime.now());
        execution.setStatus(ExecutionStatus.FAILED);
        execution.setErrorMessage(e.getMessage());
        jobExecutionRepository.save(execution);
        job = jobRepository.findById(job.getId()).orElse(null);
        if (job == null) return;
        job.setRetryCount(job.getRetryCount() + 1);
        if (job.getRetryCount() >= job.getMaxRetries()) {
            job.setStatus(JobStatus.FAILED);
        } else {
            int delaySeconds = job.getRetryDelaySeconds() * (int) Math.pow(2, job.getRetryCount() - 1);
            job.setNextRunTime(LocalDateTime.now().plusSeconds(delaySeconds));
        }
        jobRepository.save(job);
    }

    private void updateNextRunTime(Job job) {
        if (job.getScheduleType() == ScheduleType.ONE_TIME) return;
        LocalDateTime nextRun = scheduleCalculator.calculateNextRunTime(job);
        job.setNextRunTime(nextRun);
    }

    private long calculateDelay(LocalDateTime nextRunTime) {
        return java.time.Duration.between(LocalDateTime.now(), nextRunTime).toMillis();
    }

    public void shutdown() {
        // (Keep your existing implementation)
        executorService.shutdown();
        highFrequencyExecutor.shutdown();
    }
}