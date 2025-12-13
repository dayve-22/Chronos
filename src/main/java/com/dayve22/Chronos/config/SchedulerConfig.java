package com.dayve22.Chronos.config;

import com.dayve22.Chronos.listener.GlobalJobListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.quartz.autoconfigure.SchedulerFactoryBeanCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.quartz.SchedulerFactoryBean;

import javax.sql.DataSource;
import java.util.Properties;

@Configuration
public class SchedulerConfig {

    @Bean
    public SchedulerFactoryBean schedulerFactoryBean(DataSource dataSource) {
        SchedulerFactoryBean factory = new SchedulerFactoryBean();
        factory.setDataSource(dataSource);

        // Define Quartz Properties Manually
        Properties props = new Properties();

        // --- THE CRITICAL FIX ---
        // This tells Quartz: "We are using Postgres, handle the data correctly!"
        props.put("org.quartz.jobStore.driverDelegateClass", "org.quartz.impl.jdbcjobstore.PostgreSQLDelegate");

        // Standard settings
        props.put("org.quartz.jobStore.tablePrefix", "QRTZ_");
        props.put("org.quartz.jobStore.isClustered", "true");
        props.put("org.quartz.jobStore.misfireThreshold", "60000");
        props.put("org.quartz.threadPool.threadCount", "10");

        factory.setQuartzProperties(props);
        return factory;
    }
}
