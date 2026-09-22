package com.wordflow.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.ThreadPoolExecutor;

/**
 * 业务异步线程池配置。
 */
@Slf4j
@Configuration
public class AsyncConfig {

    /** AI 批改任务线程池名称前缀 */
    public static final String AI_TASK_EXECUTOR = "aiTaskExecutor";

    /** 核心线程数：AI 批改是 IO 密集型任务，核心线程数按 CPU 核心数配置 */
    private static final int CORE_POOL_SIZE = Runtime.getRuntime().availableProcessors();
    private static final int MAX_POOL_SIZE = CORE_POOL_SIZE * 4;
    private static final int QUEUE_CAPACITY = 200;
    private static final int KEEP_ALIVE_SECONDS = 60;

    @Bean(name = AI_TASK_EXECUTOR)
    public ThreadPoolTaskExecutor aiTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(CORE_POOL_SIZE);
        executor.setMaxPoolSize(MAX_POOL_SIZE);
        executor.setQueueCapacity(QUEUE_CAPACITY);
        executor.setKeepAliveSeconds(KEEP_ALIVE_SECONDS);
        executor.setThreadNamePrefix("wordflow-ai-");
        // 队列满时由调用线程执行，形成背压，避免任务被静默丢弃
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);
        executor.initialize();
        log.info("AI 批改线程池初始化完成：core={}, max={}, queue={}",
                CORE_POOL_SIZE, MAX_POOL_SIZE, QUEUE_CAPACITY);
        return executor;
    }
}
