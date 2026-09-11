package org.aastrika.service.impl;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

import org.aastrika.common.Constants;
import org.aastrika.dto.event.RatingMessage;
import org.aastrika.dto.request.RatingsLookupRequest;
import org.aastrika.dto.request.RatingsReadRequest;
import org.aastrika.dto.request.RequestRating;
import org.aastrika.dto.response.AppResponse;
import org.aastrika.dto.response.RatingInfo;
import org.aastrika.dto.response.RatingInfoV3;
import org.aastrika.dto.response.RatingLookupResponse;
import org.aastrika.dto.response.RatingLookupResponseV2;
import org.aastrika.dto.response.RatingSummaryResponse;
import org.aastrika.dto.response.RatingSummaryResponseV2;
import org.aastrika.entity.Rating;
import org.aastrika.entity.RatingKey;
import org.aastrika.entity.RatingSummary;
import org.aastrika.entity.RatingSummaryKey;
import org.aastrika.entity.User;
import org.aastrika.exception.ApiException;
import org.aastrika.messaging.RatingEventPublisher;
import org.aastrika.repository.RatingLookupRepository;
import org.aastrika.repository.RatingRepository;
import org.aastrika.repository.RatingSummaryRepository;
import org.aastrika.repository.UserRepository;
import org.aastrika.service.RatingService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import com.datastax.oss.driver.api.core.uuid.Uuids;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class RatingServiceImpl implements RatingService {

    private static final String READ_API_ID = "api.ratings.read";
    private static final String UPDATE_API_ID = "api.ratings.update";
    private static final String SUMMARY_API_ID = "api.ratings.summary";
    private static final String LOOKUP_API_ID = "api.ratings.lookup";

    private final RatingRepository ratingRepository;
    private final RatingSummaryRepository ratingSummaryRepository;
    private final RatingLookupRepository ratingLookupRepository;
    private final UserRepository userRepository;
    private final RatingEventPublisher ratingEventPublisher;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public RatingServiceImpl(
            RatingRepository ratingRepository,
            RatingSummaryRepository ratingSummaryRepository,
            RatingLookupRepository ratingLookupRepository,
            UserRepository userRepository,
            RatingEventPublisher ratingEventPublisher) {
        this.ratingRepository = ratingRepository;
        this.ratingSummaryRepository = ratingSummaryRepository;
        this.ratingLookupRepository = ratingLookupRepository;
        this.userRepository = userRepository;
        this.ratingEventPublisher = ratingEventPublisher;
    }

    @Override
    public AppResponse<Map<String, Object>> upsertRating(RequestRating request) {
        UUID now = Uuids.timeBased();
        RatingKey key = new RatingKey(request.getActivityId(), request.getActivityType(), request.getUserId());
        Rating existing = ratingRepository.findById(key).orElse(null);

        RatingMessage event;
        if (existing != null) {
            // Read-modify-write preserves columns the request doesn't touch (the source did partial CQL updates).
            UUID prevUpdatedOn = existing.getUpdatedOn();
            Float prevRating = existing.getRating();
            String prevReview = existing.getReview();

            if (request.getComment() == null) {
                existing.setRating(request.getRating());
                existing.setReview(request.getReview());
                existing.setUpdatedOn(now);
            }
            if (request.getComment() != null && request.getCommentBy() != null) {
                existing.setComment(request.getComment());
                existing.setCommentBy(request.getCommentBy());
                existing.setCommentUpdatedOn(now);
            }
            if (request.getRecommended() != null) {
                existing.setRecommended(request.getRecommended());
            }
            ratingRepository.save(existing);

            event = new RatingMessage("ratingUpdate", request.getActivityId(), request.getActivityType(),
                    request.getUserId(), String.valueOf(existing.getCreatedOn()));
            event.setPrevValues(values(String.valueOf(prevUpdatedOn), prevRating, prevReview));
            event.setUpdatedValues(values(String.valueOf(now), request.getRating(), request.getReview()));
        } else {
            Rating rating = new Rating();
            rating.setKey(key);
            rating.setCreatedOn(now);
            rating.setUpdatedOn(now);
            rating.setRating(request.getRating());
            rating.setReview(request.getReview());
            rating.setRecommended(request.getRecommended());
            ratingRepository.save(rating);

            event = new RatingMessage("ratingAdd", request.getActivityId(), request.getActivityType(),
                    request.getUserId(), String.valueOf(now));
            event.setUpdatedValues(values(String.valueOf(now), request.getRating(), request.getReview()));
        }

        // Only a rating add/update feeds the aggregation pipeline — a comment-only change does not.
        if (request.getComment() == null && request.getCommentBy() == null) {
            ratingEventPublisher.publish(event);
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("message", "Successful");
        return AppResponse.success(UPDATE_API_ID, result, HttpStatus.OK);
    }

    @Override
    public AppResponse<Map<String, Object>> readRatings(RatingsReadRequest request) {
        RatingsReadRequest.Payload payload = request.getRequest();
        List<RatingInfo> content = ratingRepository
                .findByKeyActivityIdAndKeyActivityTypeAndKeyUserIdIn(
                        payload.getActivityId(), payload.getActivityType(), payload.getUserIds())
                .stream()
                .map(RatingServiceImpl::toRatingInfo)
                .toList();

        Map<String, Object> result = new LinkedHashMap<>();
        result.put(Constants.COUNT, content.size());
        result.put(Constants.CONTENT, content);
        return AppResponse.success(READ_API_ID, result, HttpStatus.OK);
    }

    @Override
    public AppResponse<Map<String, Object>> readRatingsV3(RatingsReadRequest request) {
        RatingsReadRequest.Payload payload = request.getRequest();
        List<RatingInfoV3> content = ratingRepository
                .findByKeyActivityIdAndKeyActivityTypeAndKeyUserIdIn(
                        payload.getActivityId(), payload.getActivityType(), payload.getUserIds())
                .stream()
                .map(RatingServiceImpl::toRatingInfoV3)
                .toList();

        Map<String, Object> result = new LinkedHashMap<>();
        result.put(Constants.COUNT, content.size());
        result.put(Constants.CONTENT, content);
        return AppResponse.success(READ_API_ID, result, HttpStatus.OK);
    }

    @Override
    public AppResponse<Map<String, Object>> getRatingSummary(String activityId, String activityType) {
        RatingSummary summary = ratingSummaryRepository
                .findById(new RatingSummaryKey(activityId, activityType))
                .orElse(null);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("message", "Successful");
        result.put(Constants.RESPONSE, summary == null ? null : toSummaryResponse(summary));
        return AppResponse.success(SUMMARY_API_ID, result, HttpStatus.OK);
    }

    private RatingSummaryResponse toSummaryResponse(RatingSummary summary) {
        return RatingSummaryResponse.builder()
                .activityId(summary.getKey().getActivityId())
                .activityType(summary.getKey().getActivityType())
                .totalCount1Stars(summary.getTotalCount1Stars())
                .totalCount2Stars(summary.getTotalCount2Stars())
                .totalCount3Stars(summary.getTotalCount3Stars())
                .totalCount4Stars(summary.getTotalCount4Stars())
                .totalCount5Stars(summary.getTotalCount5Stars())
                .totalNumberOfRatings(summary.getTotalNumberOfRatings())
                .sumOfTotalRatings(summary.getSumOfTotalRatings())
                .latest50Reviews(parseReviews(summary.getLatest50Reviews()))
                .build();
    }

    /** Parses the {@code latest50reviews} JSON array and enriches each entry with the reviewer's name. */
    private List<RatingSummaryResponse.Review> parseReviews(String latest50ReviewsJson) {
        if (latest50ReviewsJson == null || latest50ReviewsJson.isBlank()) {
            return List.of();
        }
        JsonNode array;
        try {
            array = objectMapper.readTree(latest50ReviewsJson);
        } catch (Exception e) {
            log.warn("Failed to parse latest50reviews JSON; returning summary without reviews: {}", e.getMessage());
            return List.of();
        }

        List<String> userIds = new ArrayList<>();
        for (JsonNode node : array) {
            if (node.hasNonNull("user_id")) {
                userIds.add(node.get("user_id").asText());
            }
        }
        Map<String, String> firstNames = userRepository.findAllById(userIds).stream()
                .collect(Collectors.toMap(User::getId, u -> u.getFirstName() == null ? "" : u.getFirstName(), (a, b) -> a));

        List<RatingSummaryResponse.Review> reviews = new ArrayList<>();
        for (JsonNode node : array) {
            String userId = node.hasNonNull("user_id") ? node.get("user_id").asText() : null;
            reviews.add(RatingSummaryResponse.Review.builder()
                    .objectType("review")
                    .userId(userId)
                    .date(reviewDate(node.path("date").asText(null)))
                    .rating(node.hasNonNull("rating") ? (float) node.get("rating").asDouble() : null)
                    .review(node.hasNonNull("review") ? node.get("review").asText() : null)
                    .firstName(firstNames.getOrDefault(userId, ""))
                    .build());
        }
        return reviews;
    }

    @Override
    public AppResponse<Map<String, Object>> getRatingSummaryV2(String activityId, String activityType) {
        RatingSummary summary = ratingSummaryRepository
                .findById(new RatingSummaryKey(activityId, activityType))
                .orElse(null);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("message", "Successful");
        result.put(Constants.RESPONSE, summary == null ? null : toSummaryResponseV2(summary));
        return AppResponse.success(SUMMARY_API_ID, result, HttpStatus.OK);
    }

    private RatingSummaryResponseV2 toSummaryResponseV2(RatingSummary summary) {
        String rawReviews = summary.getLatest50Reviews();
        // Null only when the source column itself is null (matches the source's null check), not
        // merely when parsing yields no entries.
        String latest50ReviewsJson = null;
        if (rawReviews != null) {
            List<ReviewNodeV2> nodes = parseReviews(rawReviews).stream()
                    .map(r -> ReviewNodeV2.builder()
                            .objectType(r.getObjectType())
                            .userId(r.getUserId())
                            .date(r.getDate() == null ? null : r.getDate().toEpochMilli())
                            .rating(r.getRating())
                            .review(r.getReview())
                            .firstName(r.getFirstName())
                            .build())
                    .toList();
            try {
                latest50ReviewsJson = objectMapper.writeValueAsString(nodes);
            } catch (JsonProcessingException e) {
                log.warn("Failed to serialize latest50Reviews for v2: {}", e.getMessage());
            }
        }

        return RatingSummaryResponseV2.builder()
                .activityId(summary.getKey().getActivityId())
                .activityType(summary.getKey().getActivityType())
                .totalCount1Stars(summary.getTotalCount1Stars())
                .totalCount2Stars(summary.getTotalCount2Stars())
                .totalCount3Stars(summary.getTotalCount3Stars())
                .totalCount4Stars(summary.getTotalCount4Stars())
                .totalCount5Stars(summary.getTotalCount5Stars())
                .totalNumberOfRatings(summary.getTotalNumberOfRatings())
                .sumOfTotalRatings(summary.getSumOfTotalRatings())
                .latest50Reviews(latest50ReviewsJson)
                .build();
    }

    /** Wire shape embedded inside v2's double-encoded {@code latest50Reviews} string (source parity). */
    @Data
    @Builder
    private static class ReviewNodeV2 {
        private String objectType;
        @JsonProperty("user_id")
        private String userId;
        private Long date;
        private Float rating;
        private String review;
        private String firstName;
    }

    @Override
    public AppResponse<Map<String, Object>> ratingLookUp(RatingsLookupRequest request) {
        List<RatingLookupRepository.Row> rows = ratingLookupRepository.findPage(
                request.getActivityId(), request.getActivityType(),
                request.getRating(), request.getUpdateOn(), request.getLimit());

        List<String> userIds = rows.stream()
                .map(RatingLookupRepository.Row::userId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        Map<String, String> firstNames = userIds.isEmpty() ? Map.of()
                : userRepository.findAllById(userIds).stream()
                        .collect(Collectors.toMap(User::getId,
                                u -> u.getFirstName() == null ? "" : u.getFirstName(), (a, b) -> a));

        List<RatingLookupResponse> content = new ArrayList<>();
        for (RatingLookupRepository.Row row : rows) {
            content.add(RatingLookupResponse.builder()
                    .activityId(row.activityId())
                    .activityType(row.activityType())
                    .userId(row.userId())
                    .firstName(firstNames.getOrDefault(row.userId(), ""))
                    .rating(row.rating())
                    .review(row.review())
                    .updatedOn(row.updatedOn() == null ? null
                            : Instant.ofEpochMilli(Uuids.unixTimestamp(row.updatedOn())))
                    .updatedOnUuid(row.updatedOn() == null ? null : row.updatedOn().toString())
                    .build());
        }
        // With no rating filter the DB can't order across star partitions — sort newest-first here.
        content.sort(Comparator.comparing(RatingLookupResponse::getUpdatedOn,
                Comparator.nullsLast(Comparator.reverseOrder())));

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("message", "Successful");
        result.put(Constants.RESPONSE, content);
        return AppResponse.success(LOOKUP_API_ID, result, HttpStatus.OK);
    }

    @Override
    @SuppressWarnings("unchecked")
    public AppResponse<Map<String, Object>> ratingLookUpV2(RatingsLookupRequest request) {
        List<RatingLookupResponse> content =
                (List<RatingLookupResponse>) ratingLookUp(request).getResult().get(Constants.RESPONSE);
        List<RatingLookupResponseV2> contentV2 = content.stream()
                .map(r -> RatingLookupResponseV2.builder()
                        .activityId(r.getActivityId())
                        .activityType(r.getActivityType())
                        .userId(r.getUserId())
                        .firstName(r.getFirstName())
                        .rating(r.getRating())
                        .review(r.getReview())
                        .updatedOn(r.getUpdatedOn())
                        .updatedOnUuid(r.getUpdatedOnUuid())
                        .build())
                .toList();

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("message", "Successful");
        result.put(Constants.RESPONSE, contentV2);
        return AppResponse.success(LOOKUP_API_ID, result, HttpStatus.OK);
    }

    private static Instant reviewDate(String dateTimeUuid) {
        if (dateTimeUuid == null || dateTimeUuid.isBlank()) {
            return null;
        }
        try {
            return Instant.ofEpochMilli(Uuids.unixTimestamp(UUID.fromString(dateTimeUuid)));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private static RatingMessage.UpdatedValues values(String updatedOn, Float rating, String review) {
        RatingMessage.UpdatedValues values = new RatingMessage.UpdatedValues();
        values.setUpdatedOn(updatedOn);
        values.setRating(rating);
        values.setReview(review);
        return values;
    }

    private static RatingInfo toRatingInfo(Rating rating) {
        return RatingInfo.builder()
                .activityId(rating.getKey().getActivityId())
                .activityType(rating.getKey().getActivityType())
                .userId(rating.getKey().getUserId())
                .rating(rating.getRating())
                .review(rating.getReview())
                .comment(rating.getComment())
                .commentBy(rating.getCommentBy())
                .recommended(rating.getRecommended())
                .createdOn(toInstant(rating.getCreatedOn()))
                .updatedOn(toInstant(rating.getUpdatedOn()))
                .commentUpdatedOn(toInstant(rating.getCommentUpdatedOn()))
                .build();
    }

    /** Recover the embedded timestamp of a v1 (time-based) UUID as an {@link Instant}. */
    private static Instant toInstant(UUID timeUuid) {
        return timeUuid == null ? null : Instant.ofEpochMilli(Uuids.unixTimestamp(timeUuid));
    }

    private static RatingInfoV3 toRatingInfoV3(Rating rating) {
        return RatingInfoV3.builder()
                .activityId(rating.getKey().getActivityId())
                .activityType(rating.getKey().getActivityType())
                .userId(rating.getKey().getUserId())
                .rating(rating.getRating())
                .review(rating.getReview())
                .comment(rating.getComment())
                .commentBy(rating.getCommentBy())
                .recommended(rating.getRecommended())
                .createdOn(rating.getCreatedOn() == null ? null : rating.getCreatedOn().toString())
                .updatedOn(rating.getUpdatedOn() == null ? null : rating.getUpdatedOn().toString())
                .commentUpdatedOn(rating.getCommentUpdatedOn() == null ? null : rating.getCommentUpdatedOn().toString())
                .build();
    }
}
