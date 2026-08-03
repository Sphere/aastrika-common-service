package org.aastrika.exception;

/**
 * Signals that a rating event's user is not enrolled in the activity, so it must not be aggregated.
 * Treated as a non-retryable failure by the Kafka error handler and routed to the dead-letter topic
 * (mirroring the Flink job's failed side-output for the enrolment gate).
 */
public class NotEnrolledException extends RuntimeException {

    public NotEnrolledException(String message) {
        super(message);
    }
}
