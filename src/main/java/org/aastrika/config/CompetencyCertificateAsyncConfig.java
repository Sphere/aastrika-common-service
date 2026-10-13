package org.aastrika.config;

import java.util.concurrent.Executor;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import lombok.extern.slf4j.Slf4j;

/**
 * Executor for the competency-certificate task started after a passbook update.
 *
 * <p>Unlike the bulk-upload pool this does not use {@code CallerRunsPolicy}: a full queue must
 * never push certificate work onto the passbook request thread. A dropped task is retried
 * naturally on the learner's next passbook update for that competency.
 *
 * <p>{@code @EnableAsync} is already declared on {@link BulkUploadAsyncConfig} and is application-wide.
 */
@Configuration
@Slf4j
public class CompetencyCertificateAsyncConfig {

    public static final String EXECUTOR = "competencyCertificateExecutor";

    @Bean(EXECUTOR)
    public Executor competencyCertificateExecutor(
            @Value("${competency-certificate.worker.core-pool-size:1}") int corePoolSize,
            @Value("${competency-certificate.worker.max-pool-size:2}") int maxPoolSize,
            @Value("${competency-certificate.worker.queue-capacity:200}") int queueCapacity,
            @Value("${competency-certificate.worker.shutdown-await-seconds:30}") int awaitSeconds) {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(corePoolSize);
        executor.setMaxPoolSize(maxPoolSize);
        executor.setQueueCapacity(queueCapacity);
        executor.setThreadNamePrefix("competency-cert-");
        executor.setRejectedExecutionHandler((task, pool) ->
                log.warn("Competency certificate queue full; task dropped (retried on next passbook update)"));
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(awaitSeconds);
        executor.initialize();
        return executor;
    }
}
