package org.aastrika.client;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

/**
 * Reads the course-id lists that drive the additional-tag job (#7) from Redis (DB 12, set via
 * {@code spring.data.redis.database}). These keys are populated by an external ETL, not this service.
 */
@Component
public class RatingTagRedisReader {

    private static final String MOST_ENROLLED_KEY = "lhp_mostEnrolledTag";
    private static final String MOST_TRENDING_KEY = "lhp_trending";
    private static final String ACROSS_COURSES = "across:courses";
    private static final String ACROSS_PROGRAMS = "across:programs";

    private final StringRedisTemplate redis;

    public RatingTagRedisReader(StringRedisTemplate redis) {
        this.redis = redis;
    }

    /** {@code lhp_mostEnrolledTag} — a single comma-separated string of course ids. */
    public List<String> mostEnrolled() {
        return splitCsv(redis.opsForValue().get(MOST_ENROLLED_KEY));
    }

    /**
     * {@code lhp_trending} — a hash whose {@code across:courses} and {@code across:programs} fields are
     * each comma-separated course ids; combined here, with revision-copy ids ({@code _rc}) dropped.
     */
    public List<String> mostTrending() {
        List<Object> values = redis.opsForHash().multiGet(MOST_TRENDING_KEY, List.of(ACROSS_COURSES, ACROSS_PROGRAMS));
        List<String> combined = new ArrayList<>();
        for (Object value : values) {
            for (String id : splitCsv(value == null ? null : value.toString())) {
                if (!id.contains("_rc")) {
                    combined.add(id);
                }
            }
        }
        return combined;
    }

    private static List<String> splitCsv(String csv) {
        if (csv == null || csv.isBlank()) {
            return List.of();
        }
        return new ArrayList<>(Arrays.asList(csv.split(",")));
    }
}
