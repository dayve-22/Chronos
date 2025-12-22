package com.dayve22.Chronos.entity;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
public class JobMetrics {

    private final Counter jobsExecuted;
    private final Counter jobsFailed;
    private final Timer jobExecutionTime;

    public JobMetrics(MeterRegistry registry) {
        this.jobsExecuted = Counter.builder("jobs.executed")
                .tag("type", "all")
                .register(registry);

        this.jobsFailed = Counter.builder("jobs.failed")
                .register(registry);

        this.jobExecutionTime = Timer.builder("jobs.execution.time")
                .register(registry);
    }

    public void recordExecution(String jobType, Duration duration) {
        jobsExecuted.increment();
        jobExecutionTime.record(duration);
    }
}