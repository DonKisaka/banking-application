package com.banking_application.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;


@Configuration
public class SchedulingConfig {

    private static final Logger log = LoggerFactory.getLogger(SchedulingConfig.class);

    @Bean
    @ConditionalOnProperty(name = "scheduling.enabled", havingValue = "true", matchIfMissing = true)
    public TaskScheduler taskScheduler() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(3);
        scheduler.setThreadNamePrefix("scheduled-");
        scheduler.setWaitForTasksToCompleteOnShutdown(true);
        scheduler.setAwaitTerminationSeconds(30);
        scheduler.initialize();
        log.info("Task scheduler initialized with pool size 3");
        return scheduler;
    }
}
