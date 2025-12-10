package com.dayve22.Chronos.config;

import com.dayve22.Chronos.listener.GlobalJobListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.quartz.autoconfigure.SchedulerFactoryBeanCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SchedulerConfig {
    @Autowired
    private GlobalJobListener globalJobListener;

    @Bean
    public SchedulerFactoryBeanCustomizer schedulerFactoryBeanCustomizer() {
        return bean -> bean.setGlobalJobListeners(globalJobListener);
    }
}
