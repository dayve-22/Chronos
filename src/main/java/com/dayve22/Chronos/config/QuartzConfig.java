package com.dayve22.Chronos.config;

import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
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

    // Standard Constructor Injection (Spring automatically passes the correct beans)
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
        factory.setOverwriteExistingJobs(true);

        Properties props = new Properties();

        props.put("org.quartz.scheduler.instanceName", "ChronosScheduler");
        props.put("org.quartz.scheduler.instanceId", "AUTO");

        props.put("org.quartz.threadPool.class", "org.quartz.simpl.SimpleThreadPool");
        props.put("org.quartz.threadPool.threadCount", "10");

        props.put("org.quartz.jobStore.class",
                "org.springframework.scheduling.quartz.LocalDataSourceJobStore");
        props.put("org.quartz.jobStore.driverDelegateClass",
                "org.quartz.impl.jdbcjobstore.PostgreSQLDelegate");
        props.put("org.quartz.jobStore.tablePrefix", "QRTZ_");
        props.put("org.quartz.jobStore.isClustered", "false");
        props.put("org.quartz.jobStore.acquireTriggersWithinLock", "true");

        factory.setQuartzProperties(props);
        return factory;
    }
}