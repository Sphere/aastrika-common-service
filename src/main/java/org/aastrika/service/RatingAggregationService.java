package org.aastrika.service;

import org.aastrika.dto.event.RatingMessage;

/**
 * Consumes a rating event and maintains the aggregate tables ({@code ratings_summary} +
 * {@code ratings_lookup}) — the in-service port of the Flink rating job. Invoked by the Kafka
 * consumer, single-threaded, so per-activity read-modify-write stays correct.
 */
public interface RatingAggregationService {

    /**
     * @throws org.aastrika.exception.NotEnrolledException if the user isn't enrolled in the activity
     *                                                     (routed to the dead-letter topic)
     */
    void aggregate(RatingMessage event);
}
