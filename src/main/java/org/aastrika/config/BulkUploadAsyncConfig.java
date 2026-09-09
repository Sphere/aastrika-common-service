package org.aastrika.config;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * Executor for bulk-upload processing. Deliberately small and bounded:
 * provisioning one user is four learner-service calls plus two existence checks, so a wide pool
 * would hammer that service rather than finish sooner.
 *
 * <p>{@code CallerRunsPolicy} means that if the queue fills, the submitting thread runs the task
 * instead of it being dropped — the source used {@code CompletableFuture.runAsync} on the common
 * ForkJoinPool, whose daemon threads are abandoned on shutdown, silently losing in-flight jobs.
 * {@code setWaitForTasksToCompleteOnShutdown} lets a rolling deploy drain instead.
 */
@Configuration
@EnableAsync
public class BulkUploadAsyncConfig {

    public static final String EXECUTOR = "bulkUploadExecutor";

    @Bean(EXECUTOR)
    public Executor bulkUploadExecutor(
            @Value("${bulk-upload.worker.core-pool-size:1}") int corePoolSize,
            @Value("${bulk-upload.worker.max-pool-size:2}") int maxPoolSize,
            @Value("${bulk-upload.worker.queue-capacity:50}") int queueCapacity,
            @Value("${bulk-upload.worker.shutdown-await-seconds:60}") int awaitSeconds) {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(corePoolSize);
        executor.setMaxPoolSize(maxPoolSize);
        executor.setQueueCapacity(queueCapacity);
        executor.setThreadNamePrefix("bulk-upload-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(awaitSeconds);
        executor.initialize();
        return executor;
    }
}
