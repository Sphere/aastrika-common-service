package org.aastrika.messaging;

import java.util.concurrent.TimeUnit;

import org.aastrika.dto.event.RatingMessage;
import org.aastrika.exception.ApiException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Publishes rating events to Kafka. Unlike the source's fire-and-forget {@code Producer}, this sends
 * <b>synchronously</b> (waits for the broker ack) so a delivery failure surfaces to the caller as an
 * error rather than being silently dropped.
 */
@Component
public class RatingEventPublisher {

    private static final String API_ID = "api.ratings.update";
    private static final long SEND_TIMEOUT_SECONDS = 10;

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final String topic;

    public RatingEventPublisher(
            KafkaTemplate<String, String> kafkaTemplate,
            @Value("${ratings.rating-event-topic:dev.rating.event}") String topic) {
        this.kafkaTemplate = kafkaTemplate;
        this.topic = topic;
    }

    public void publish(RatingMessage message) {
        try {
            String json = objectMapper.writeValueAsString(message);
            kafkaTemplate.send(topic, json).get(SEND_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ApiException(API_ID, HttpStatus.INTERNAL_SERVER_ERROR,
                    "Interrupted while publishing rating event");
        } catch (Exception e) {
            throw new ApiException(API_ID, HttpStatus.INTERNAL_SERVER_ERROR,
                    "Failed to publish rating event: " + e.getMessage());
        }
    }
}
