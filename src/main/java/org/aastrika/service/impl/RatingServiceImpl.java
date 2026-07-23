package org.aastrika.service.impl;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

import org.aastrika.client.ContentClient;
import org.aastrika.client.RatingTagRedisReader;
import org.aastrika.dto.event.RatingMessage;
import org.aastrika.dto.request.RatingsLookupRequest;
import org.aastrika.dto.request.RatingsReadRequest;
import org.aastrika.dto.request.RequestRating;
import org.aastrika.dto.response.AppResponse;
import org.aastrika.dto.response.RatingInfo;
import org.aastrika.dto.response.RatingLookupResponse;
import org.aastrika.dto.response.RatingSummaryResponse;
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
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class RatingServiceImpl implements RatingService {

    private static final String READ_API_ID = "api.ratings.read";
    private static final String UPDATE_API_ID = "api.ratings.update";
    private static final String SUMMARY_API_ID = "api.ratings.summary";
    private static final String LOOKUP_API_ID = "api.ratings.lookup";
    private static final String META_UPDATE_API_ID = "api.ratings.content.meta.update";
    private static final String ADDITIONAL_TAG_API_ID = "api.content.meta.update";

    private static final String MOST_ENROLLED = "mostEnrolled";
    private static final String MOST_TRENDING = "mostTrending";
    private static final List<String> CONTENT_FIELDS = List.of("versionKey", "identifier", "additionalTags");

    private final RatingRepository ratingRepository;
    private final RatingSummaryRepository ratingSummaryRepository;
    private final RatingLookupRepository ratingLookupRepository;
    private final UserRepository userRepository;
    private final RatingEventPublisher ratingEventPublisher;
    private final ContentClient contentClient;
    private final RatingTagRedisReader ratingTagRedisReader;
    private final int metaUpdateLimit;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public RatingServiceImpl(
            RatingRepository ratingRepository,
            RatingSummaryRepository ratingSummaryRepository,
            RatingLookupRepository ratingLookupRepository,
            UserRepository userRepository,
            RatingEventPublisher ratingEventPublisher,
            ContentClient contentClient,
            RatingTagRedisReader ratingTagRedisReader,
            @Value("${ratings.meta-update-limit:0}") int metaUpdateLimit) {
        this.ratingRepository = ratingRepository;
        this.ratingSummaryRepository = ratingSummaryRepository;
        this.ratingLookupRepository = ratingLookupRepository;
        this.userRepository = userRepository;
        this.ratingEventPublisher = ratingEventPublisher;
        this.contentClient = contentClient;
        this.ratingTagRedisReader = ratingTagRedisReader;
        this.metaUpdateLimit = metaUpdateLimit;
    }

    @Override
    public AppResponse<Map<String, Object>> getRating(String activityId, String activityType, String userId) {
        RatingKey key = new RatingKey(activityId, activityType, userId);
        RatingInfo ratingInfo = ratingRepository.findById(key).map(RatingServiceImpl::toRatingInfo).orElse(null);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("message", "Successful");
        result.put("response", ratingInfo);
        return AppResponse.success(READ_API_ID, result, HttpStatus.OK);
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
        List<RatingInfo> content = ratingRepository
                .findByKeyActivityIdAndKeyActivityTypeAndKeyUserIdIn(
                        request.getActivityId(), request.getActivityType(), request.getUserIds())
                .stream()
                .map(RatingServiceImpl::toRatingInfo)
                .toList();

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("count", content.size());
        result.put("content", content);
        return AppResponse.success(READ_API_ID, result, HttpStatus.OK);
    }

    @Override
    public AppResponse<Map<String, Object>> getRatingSummary(String activityId, String activityType) {
        RatingSummary summary = ratingSummaryRepository
                .findById(new RatingSummaryKey(activityId, activityType))
                .orElse(null);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("message", "Successful");
        result.put("response", summary == null ? null : toSummaryResponse(summary));
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
        result.put("response", content);
        return AppResponse.success(LOOKUP_API_ID, result, HttpStatus.OK);
    }

    @Override
    public AppResponse<Map<String, Object>> updateRatingsMetaData() {
        List<RatingSummary> summaries = ratingSummaryRepository.findAll();
        if (metaUpdateLimit > 0 && summaries.size() > metaUpdateLimit) {
            log.info("meta-update capped at {} of {} summary rows (ratings.meta-update-limit)",
                    metaUpdateLimit, summaries.size());
            summaries = summaries.subList(0, metaUpdateLimit);
        }

        int updated = 0;
        int errored = 0;
        for (RatingSummary summary : summaries) {
            String contentId = summary.getKey().getActivityId();
            Float total = summary.getTotalNumberOfRatings();
            Float sum = summary.getSumOfTotalRatings();
            if (total == null || total == 0f || sum == null) {
                errored++;
                continue;
            }
            Map<String, Object> content = contentClient.readContent(contentId, CONTENT_FIELDS);
            if (content == null || content.get("versionKey") == null) {
                errored++;
                continue;
            }
            Map<String, Object> values = new LinkedHashMap<>();
            values.put("versionKey", content.get("versionKey"));
            values.put("avgRating",
                    BigDecimal.valueOf(sum).divide(BigDecimal.valueOf(total), 1, RoundingMode.HALF_UP).floatValue());
            values.put("totalNoOfRating", total.intValue());
            values.put("countOf1StarRating", intValue(summary.getTotalCount1Stars()));
            values.put("countOf2StarRating", intValue(summary.getTotalCount2Stars()));
            values.put("countOf3StarRating", intValue(summary.getTotalCount3Stars()));
            values.put("countOf4StarRating", intValue(summary.getTotalCount4Stars()));
            values.put("countOf5StarRating", intValue(summary.getTotalCount5Stars()));

            if (contentClient.updateContentMeta(contentId, values)) {
                updated++;
            } else {
                errored++;
            }
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("totalNumberOfUpdatedContent", updated);
        result.put("totalNumberOfErrorContent", errored);
        return AppResponse.success(META_UPDATE_API_ID, result, HttpStatus.OK);
    }

    @Override
    public AppResponse<Map<String, Object>> updateAdditionalTag(String tag) {
        List<String> latestCourseList = courseListForTag(tag);

        List<String> currentlyTagged = contentClient.searchContent(tag).stream()
                .map(m -> (String) m.get("identifier"))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        int updated = 0;
        int errored = 0;

        // Add the tag to courses that qualify now but aren't tagged yet.
        for (String contentId : latestCourseList) {
            if (currentlyTagged.contains(contentId)) {
                continue;
            }
            if (applyTag(contentId, tag, false)) {
                updated++;
            } else {
                errored++;
            }
        }

        // Remove the tag from courses that are tagged but no longer qualify.
        List<String> staleTagged = new ArrayList<>(currentlyTagged);
        staleTagged.removeAll(latestCourseList);
        for (String contentId : staleTagged) {
            if (applyTag(contentId, tag, true)) {
                updated++;
            } else {
                errored++;
            }
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("totalNumberOfUpdatedContent", updated);
        result.put("totalNumberOfErrorContent", errored);
        return AppResponse.success(ADDITIONAL_TAG_API_ID, result, HttpStatus.OK);
    }

    private List<String> courseListForTag(String tag) {
        if (MOST_ENROLLED.equalsIgnoreCase(tag)) {
            return ratingTagRedisReader.mostEnrolled();
        }
        if (MOST_TRENDING.equalsIgnoreCase(tag)) {
            return ratingTagRedisReader.mostTrending();
        }
        throw new ApiException(ADDITIONAL_TAG_API_ID, HttpStatus.BAD_REQUEST, "Please provide a valid Tag");
    }

    @SuppressWarnings("unchecked")
    private boolean applyTag(String contentId, String tag, boolean remove) {
        Map<String, Object> content = contentClient.readContent(contentId, CONTENT_FIELDS);
        if (content == null || content.get("identifier") == null) {
            return false;
        }
        List<String> tags = content.get("additionalTags") == null
                ? new ArrayList<>()
                : new ArrayList<>((List<String>) content.get("additionalTags"));
        if (remove) {
            if (tags.isEmpty()) {
                return false;
            }
            tags.remove(tag);
        } else {
            if (tags.contains(tag)) {
                return true;
            }
            tags.add(tag);
        }
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("versionKey", content.get("versionKey"));
        values.put("additionalTags", tags);
        return contentClient.updateContentMeta((String) content.get("identifier"), values);
    }

    private static int intValue(Float value) {
        return value == null ? 0 : value.intValue();
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
}
