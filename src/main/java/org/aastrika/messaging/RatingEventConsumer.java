package org.aastrika.messaging;

import org.aastrika.dto.event.RatingMessage;
import org.aastrika.service.RatingAggregationService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;

/**
 * In-service replacement for the Flink rating job: consumes {@code dev.rating.event} and drives the
 * aggregation. Single-threaded (concurrency 1) over the single-partition topic, so per-activity
 * read-modify-write stays correct. Failures are handled by the container error handler
 * ({@code KafkaConsumerConfig}) which routes to the dead-letter topic.
 *
 * <p>Toggle with {@code ratings.aggregation.enabled} (default true).
 */
@Component
@ConditionalOnProperty(name = "ratings.aggregation.enabled", havingValue = "true", matchIfMissing = true)
@Slf4j
public class RatingEventConsumer {

    private final RatingAggregationService aggregationService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public RatingEventConsumer(RatingAggregationService aggregationService) {
        this.aggregationService = aggregationService;
    }

    @KafkaListener(
            topics = "${ratings.rating-event-topic:dev.rating.event}",
            groupId = "${spring.kafka.consumer.group-id:aastrika-rating-aggregator}",
            concurrency = "1")
    public void onMessage(String message) throws Exception {
        RatingMessage event = objectMapper.readValue(message, RatingMessage.class);
        aggregationService.aggregate(event);
    }
}
