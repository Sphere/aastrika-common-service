package org.aastrika.service.impl;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.UUID;

import org.aastrika.dto.event.RatingMessage;
import org.aastrika.entity.RatingSummary;
import org.aastrika.entity.RatingSummaryKey;
import org.aastrika.exception.NotEnrolledException;
import org.aastrika.client.UserEnrolmentDao;
import org.aastrika.repository.RatingLookupRepository;
import org.aastrika.repository.RatingSummaryRepository;
import org.aastrika.service.RatingAggregationService;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;

/**
 * Port of the Flink {@code RatingFunction}: per rating event, gate on enrolment then maintain
 * {@code ratings_summary} (star counts, sum/total, latest-50 reviews) and {@code ratings_lookup}.
 * Delta-based and non-idempotent by design (parity) — the Kafka consumer runs single-threaded so
 * per-activity read-modify-write is serialized.
 */
@Service
@Slf4j
public class RatingAggregationServiceImpl implements RatingAggregationService {

    private static final int REVIEW_MIN_LENGTH = 100;   // reviews longer than this feed latest50reviews
    private static final int MAX_REVIEWS = 50;

    private final RatingSummaryRepository summaryRepository;
    private final RatingLookupRepository lookupRepository;
    private final UserEnrolmentDao userEnrolmentDao;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public RatingAggregationServiceImpl(RatingSummaryRepository summaryRepository,
                                        RatingLookupRepository lookupRepository,
                                        UserEnrolmentDao userEnrolmentDao) {
        this.summaryRepository = summaryRepository;
        this.lookupRepository = lookupRepository;
        this.userEnrolmentDao = userEnrolmentDao;
    }

    @Override
    public void aggregate(RatingMessage event) {
        String activityId = event.getActivity_id();
        String activityType = event.getActivity_Type();
        String userId = event.getUser_id();

        // 1) enrolment gate — only enrolled users' ratings aggregate
        if (!userEnrolmentDao.isEnrolled(userId, activityId)) {
            throw new NotEnrolledException("User " + userId + " not enrolled in activity " + activityId);
        }

        RatingMessage.UpdatedValues updated = event.getUpdatedValues();
        RatingMessage.UpdatedValues prev = event.getPrevValues();

        float newRating = nz(updated.getRating());
        float prevRating = (prev == null) ? 0f : nz(prev.getRating());
        float delta = (prev == null) ? newRating : newRating - prevRating;
        String review = updated.getReview();
        int reviewLen = (review == null) ? 0 : review.length();

        RatingSummaryKey key = new RatingSummaryKey(activityId, activityType);
        RatingSummary summary = summaryRepository.findById(key).orElse(null);

        // 2) summary maintenance — rating changed OR a long (new/edited) review
        if (delta != 0f || reviewLen > REVIEW_MIN_LENGTH) {
            float[] stars;
            float sum;
            float total;
            String latest50;
            if (summary != null) {
                stars = updateStarCounts(summary, prevRating, newRating);
                latest50 = summary.getLatest50Reviews();
                if (delta != 0f) {
                    sum = nz(summary.getSumOfTotalRatings()) + delta;
                    total = nz(summary.getTotalNumberOfRatings()) + (prev == null ? 1f : 0f);
                } else {
                    sum = nz(summary.getSumOfTotalRatings());
                    total = nz(summary.getTotalNumberOfRatings());
                }
            } else {
                stars = updateStarCounts(null, 0f, newRating);
                sum = newRating;
                total = 1f;
                latest50 = null;
            }
            writeSummary(event, key, stars, sum, total, latest50);
        }

        // 3) short-review-only edit (rating unchanged): refresh summary (mainly the reviews list)
        if (review != null && reviewLen < REVIEW_MIN_LENGTH && delta == 0f && summary != null) {
            float[] stars = updateStarCounts(summary, prevRating, newRating);
            writeSummary(event, key, stars, nz(summary.getSumOfTotalRatings()),
                    nz(summary.getTotalNumberOfRatings()), summary.getLatest50Reviews());
        }

        // 4) lookup upkeep — remove the pre-update star row, insert the post-update one
        if (prev != null && lookupRepository.existsForStar(activityId, activityType, prevRating)) {
            lookupRepository.delete(activityId, activityType, prevRating, UUID.fromString(prev.getUpdatedOn()));
        }
        lookupRepository.insert(activityId, activityType, newRating,
                UUID.fromString(updated.getUpdatedOn()), review, userId);
    }

    private void writeSummary(RatingMessage event, RatingSummaryKey key, float[] stars,
                              float sum, float total, String existingLatest50) {
        RatingSummary s = new RatingSummary();
        s.setKey(key);
        s.setSumOfTotalRatings(sum);
        s.setTotalNumberOfRatings(total);
        s.setTotalCount1Stars(stars[0]);
        s.setTotalCount2Stars(stars[1]);
        s.setTotalCount3Stars(stars[2]);
        s.setTotalCount4Stars(stars[3]);
        s.setTotalCount5Stars(stars[4]);
        s.setLatest50Reviews(updateTop50Reviews(existingLatest50, event));
        summaryRepository.save(s);   // Cassandra upsert (covers both insert and update)
    }

    /** Star-count buckets (index 0..4 → 1★..5★): increment the new bucket, decrement the old. */
    private float[] updateStarCounts(RatingSummary summary, float prevRating, float newRating) {
        float[] counts = new float[5];
        if (summary != null) {
            counts[0] = nz(summary.getTotalCount1Stars());
            counts[1] = nz(summary.getTotalCount2Stars());
            counts[2] = nz(summary.getTotalCount3Stars());
            counts[3] = nz(summary.getTotalCount4Stars());
            counts[4] = nz(summary.getTotalCount5Stars());
        }
        int newBucket = (int) Math.floor(newRating);
        int oldBucket = (int) Math.floor(prevRating);
        if (newBucket >= 1 && newBucket <= 5 && newBucket != oldBucket) {
            counts[newBucket - 1] += 1f;
        }
        if (oldBucket >= 1 && oldBucket <= 5 && counts[oldBucket - 1] != 0f && newBucket != oldBucket) {
            counts[oldBucket - 1] -= 1f;
        }
        return counts;
    }

    /**
     * Maintains the latest-50-reviews JSON: drop this user's prior entry, cap at 50 (oldest out),
     * and add the new review only if it's longer than {@value #REVIEW_MIN_LENGTH} chars.
     */
    private String updateTop50Reviews(String existingJson, RatingMessage event) {
        RatingMessage.UpdatedValues updated = event.getUpdatedValues();
        int reviewLen = (updated.getReview() == null) ? 0 : updated.getReview().length();

        Deque<ReviewNode> queue = new ArrayDeque<>();
        if (existingJson != null && !existingJson.isBlank()) {
            try {
                ReviewNode[] nodes = objectMapper.readValue(existingJson, ReviewNode[].class);
                for (ReviewNode node : nodes) {
                    if (!event.getUser_id().equals(node.userId)) {
                        queue.addLast(node);
                    }
                }
            } catch (Exception e) {
                log.warn("Failed to parse latest50reviews for {}: {}", event.getActivity_id(), e.getMessage());
            }
            if (queue.size() >= MAX_REVIEWS) {
                queue.pollFirst();
            }
        }

        if (reviewLen > REVIEW_MIN_LENGTH) {
            queue.addLast(new ReviewNode("review", event.getUser_id(), updated.getUpdatedOn(),
                    updated.getRating(), updated.getReview()));
            return toJson(queue);
        }
        return queue.isEmpty() ? null : toJson(queue);
    }

    private String toJson(Deque<ReviewNode> queue) {
        try {
            return objectMapper.writeValueAsString(new ArrayList<>(queue));
        } catch (Exception e) {
            log.warn("Failed to serialize latest50reviews: {}", e.getMessage());
            return null;
        }
    }

    private static float nz(Float value) {
        return value == null ? 0f : value;
    }

    /** One entry in the {@code latest50reviews} JSON array (field names match the read side). */
    static class ReviewNode {
        public String objectType;
        @JsonProperty("user_id")
        public String userId;
        public String date;
        public Float rating;
        public String review;

        ReviewNode() {
        }

        ReviewNode(String objectType, String userId, String date, Float rating, String review) {
            this.objectType = objectType;
            this.userId = userId;
            this.date = date;
            this.rating = rating;
            this.review = review;
        }
    }
}
