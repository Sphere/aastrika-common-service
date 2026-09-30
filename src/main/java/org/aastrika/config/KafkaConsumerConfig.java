package org.aastrika.config;

import org.aastrika.exception.NotEnrolledException;
import org.apache.kafka.common.TopicPartition;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.FixedBackOff;

/**
 * Error handling for the rating consumer. Transient failures are retried a couple of times, then
 * (like the Flink job's failed side-output) the record is published to the dead-letter topic.
 * {@link NotEnrolledException} is non-retryable — it goes straight to the DLQ.
 */
@Configuration
@ConditionalOnProperty(name = "ratings.aggregation.enabled", havingValue = "true", matchIfMissing = true)
public class KafkaConsumerConfig {

    @Bean
    public DefaultErrorHandler kafkaErrorHandler(
            KafkaTemplate<String, String> kafkaTemplate,
            @Value("${ratings.failed-topic:rating.failedRating}") String failedTopic) {
        DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(
                kafkaTemplate, (record, exception) -> new TopicPartition(failedTopic, 0));
        // retry transient errors twice (1s apart), then send to the DLQ
        DefaultErrorHandler handler = new DefaultErrorHandler(recoverer, new FixedBackOff(1000L, 2L));
        handler.addNotRetryableExceptions(NotEnrolledException.class);
        return handler;
    }
}
