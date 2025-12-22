package com.dayve22.Chronos.core;

import com.dayve22.Chronos.entity.*;
import com.dayve22.Chronos.executor.JobExecutor;
import com.dayve22.Chronos.repository.JobExecutionRepository;
import com.dayve22.Chronos.repository.JobRepository;
import com.dayve22.Chronos.service.NotificationService;
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
    private RedisJobScheduler redisScheduler;
    @Autowired
    private JobRepository jobRepository;
    @Autowired
    private NotificationService notificationService;
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

            List<String> jobIds = redisScheduler.pollDueJobs(BATCH_SIZE);

            if (jobIds.isEmpty()) return;

            logger.info("Redis popped {} jobs to run", jobIds.size());

            // 2. Fetch full details from DB and Run
            for (String idStr : jobIds) {
                Long jobId = Long.valueOf(idStr);

                // Submit to thread pool
                executorService.submit(() -> self.executeJob(jobId));
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
        if (job.getStatus() == JobStatus.ACTIVE) {
            redisScheduler.scheduleJob(job.getId(), job.getNextRunTime());
        }
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
            notificationService.notifyJobFailure(job, e.getMessage());
        } else {
            int delaySeconds = job.getRetryDelaySeconds() * (int) Math.pow(2, job.getRetryCount() - 1);
            job.setNextRunTime(LocalDateTime.now().plusSeconds(delaySeconds));

            logger.info("Scheduling retry {} for job {} in {} seconds",
                    job.getRetryCount(), job.getId(), delaySeconds);

            redisScheduler.scheduleJob(job.getId(), job.getNextRunTime());
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