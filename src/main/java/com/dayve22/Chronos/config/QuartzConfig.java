package com.dayve22.Chronos.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource; // Import for loading SQL
import org.springframework.jdbc.datasource.init.DataSourceInitializer; // Import for Init
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator; // Import for Populator
import org.springframework.scheduling.quartz.SchedulerFactoryBean;
import org.springframework.scheduling.quartz.SpringBeanJobFactory;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;
import java.util.Properties;

@Configuration
public class QuartzConfig {

    private final ApplicationContext applicationContext;
    private final DataSource dataSource;
    private final PlatformTransactionManager transactionManager;

    public QuartzConfig(ApplicationContext applicationContext,
                        DataSource dataSource,
                        PlatformTransactionManager transactionManager) {
        this.applicationContext = applicationContext;
        this.dataSource = dataSource;
        this.transactionManager = transactionManager;
    }

    @Bean
    public SpringBeanJobFactory springBeanJobFactory() {
        SpringBeanJobFactory jobFactory = new SpringBeanJobFactory();
        jobFactory.setApplicationContext(applicationContext);
        return jobFactory;
    }

    @Bean
    public SchedulerFactoryBean scheduler(SpringBeanJobFactory jobFactory) {
        SchedulerFactoryBean factory = new SchedulerFactoryBean();
        factory.setJobFactory(jobFactory);
        factory.setDataSource(dataSource);
        factory.setTransactionManager(transactionManager);
        factory.setAutoStartup(true);
        factory.setWaitForJobsToCompleteOnShutdown(true);
        factory.setOverwriteExistingJobs(false);

        Properties props = new Properties();
        props.put("org.quartz.scheduler.instanceName", "ChronosScheduler");
        props.put("org.quartz.scheduler.instanceId", "AUTO");
        props.put("org.quartz.threadPool.class", "org.quartz.simpl.SimpleThreadPool");
        props.put("org.quartz.threadPool.threadCount", "10");
        props.put("org.quartz.jobStore.class", "org.springframework.scheduling.quartz.LocalDataSourceJobStore");
        props.put("org.quartz.jobStore.driverDelegateClass", "org.quartz.impl.jdbcjobstore.PostgreSQLDelegate");
        props.put("org.quartz.jobStore.tablePrefix", "QRTZ_");
        props.put("org.quartz.jobStore.isClustered", "false");

        factory.setQuartzProperties(props);
        return factory;
    }

    // --- ADD THIS BEAN TO REPLACE 'spring.quartz.jdbc.initialize-schema' ---
    @Bean
    public DataSourceInitializer quartzDataSourceInitializer(
            @Value("classpath:org/quartz/impl/jdbcjobstore/tables_postgres.sql") Resource script) {

        ResourceDatabasePopulator populator = new ResourceDatabasePopulator();
        populator.addScript(script);
        populator.setIgnoreFailedDrops(true);

        // This prevents crashing if tables already exist
        populator.setContinueOnError(true);

        DataSourceInitializer initializer = new DataSourceInitializer();
        initializer.setDataSource(dataSource);
        initializer.setDatabasePopulator(populator);
        return initializer;
    }
}