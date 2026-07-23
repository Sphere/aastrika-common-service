package org.aastrika.service.impl;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.aastrika.dto.request.AcquiredDetail;
import org.aastrika.dto.request.AdminPassbookReadRequest;
import org.aastrika.dto.request.CompetencyDetail;
import org.aastrika.dto.request.PassbookReadRequest;
import org.aastrika.dto.request.PassbookUpdateRequest;
import org.aastrika.dto.response.AppResponse;
import org.aastrika.dto.response.CompetencyInfo;
import org.aastrika.dto.response.CompetencyPassbookInfo;
import org.aastrika.entity.UserPassbook;
import org.aastrika.entity.UserPassbookKey;
import org.aastrika.exception.ApiException;
import org.aastrika.repository.UserPassbookRepository;
import org.aastrika.service.PassbookService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class PassbookServiceImpl implements PassbookService {

    private static final String READ_API_ID = "user.passbook.read";
    private static final String ADMIN_READ_API_ID = "user.passbook.admin.read";
    private static final String ADD_API_ID = "user.passbook.add";

    private static final ZoneId IST = ZoneId.of("Asia/Kolkata");
    private static final DateTimeFormatter SQL_TIMESTAMP = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    // Sorts a competency's acquired entries newest-first by effectiveDate; nulls sort last.
    private static final Comparator<Map<String, Object>> BY_EFFECTIVE_DATE_DESC =
            Comparator.comparing(m -> asInstant(m.get("effectiveDate")),
                    Comparator.nullsLast(Comparator.reverseOrder()));

    private final UserPassbookRepository userPassbookRepository;
    private final List<String> supportedTypeNames;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public PassbookServiceImpl(
            UserPassbookRepository userPassbookRepository,
            @Value("${passbook.supported-type-names:competency}") List<String> supportedTypeNames) {
        this.userPassbookRepository = userPassbookRepository;
        this.supportedTypeNames = supportedTypeNames;
    }

    @Override
    public AppResponse<Map<String, Object>> getPassbook(String userId, PassbookReadRequest request) {
        validateTypeName(READ_API_ID, request.getTypeName());
        List<UserPassbook> rows =
                userPassbookRepository.findByKeyUserIdAndKeyTypeName(userId, request.getTypeName());
        return AppResponse.success(READ_API_ID, buildContent(rows), HttpStatus.OK);
    }

    @Override
    public AppResponse<Map<String, Object>> getPassbookByAdmin(AdminPassbookReadRequest request) {
        validateTypeName(ADMIN_READ_API_ID, request.getTypeName());
        List<UserPassbook> rows =
                userPassbookRepository.findByKeyUserIdInAndKeyTypeName(request.getUserIds(), request.getTypeName());
        return AppResponse.success(ADMIN_READ_API_ID, buildContent(rows), HttpStatus.OK);
    }

    @Override
    public AppResponse<Map<String, Object>> updatePassbook(String actingUserId, PassbookUpdateRequest request) {
        validateTypeName(ADD_API_ID, request.getTypeName());

        // Reject the same competency listed twice in one request (matches the source behaviour).
        Set<String> seenCompetencyIds = new HashSet<>();
        for (CompetencyDetail detail : request.getCompetencyDetails()) {
            if (!seenCompetencyIds.add(detail.getCompetencyId())) {
                throw new ApiException(ADD_API_ID, HttpStatus.BAD_REQUEST,
                        "Invalid request. Competency " + detail.getCompetencyId() + " is provided twice.");
            }
        }

        String createdDate = OffsetDateTime.now(IST).toString();
        List<UserPassbook> entities = new ArrayList<>();
        for (CompetencyDetail detail : request.getCompetencyDetails()) {
            entities.add(toEntity(request.getUserId(), request.getTypeName(), actingUserId, createdDate, detail));
        }

        rejectExistingDuplicates(request.getUserId(), request.getTypeName(), entities);

        userPassbookRepository.saveAll(entities);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("insertedCount", entities.size());
        return AppResponse.success(ADD_API_ID, result, HttpStatus.OK);
    }

    /** Maps one request competency to a {@code user_passbook_v2} row for the target user. */
    private UserPassbook toEntity(String userId, String typeName, String actingUserId,
                                  String createdDate, CompetencyDetail detail) {
        AcquiredDetail acquired = detail.getAcquiredDetails();

        Map<String, String> acquiredDetails = new HashMap<>();
        if (acquired.getAdditionalParams() != null) {
            acquired.getAdditionalParams()
                    .forEach((k, v) -> acquiredDetails.put(k, stringify(v)));
        }
        acquiredDetails.put("createdBy", actingUserId);
        acquiredDetails.put("createdDate", createdDate);

        UserPassbookKey key = new UserPassbookKey(
                userId, typeName, acquired.getAcquiredChannel(), detail.getCompetencyId(),
                acquired.getCompetencyLevelId(), parseEffectiveDate(acquired.getEffectiveDate()));

        UserPassbook entity = new UserPassbook();
        entity.setKey(key);
        entity.setAcquiredDetails(acquiredDetails);
        entity.setAdditionalParams(detail.getAdditionalParams());
        return entity;
    }

    /** Rejects a candidate that duplicates an existing entry with the same competency, level and course. */
    private void rejectExistingDuplicates(String userId, String typeName, List<UserPassbook> candidates) {
        List<UserPassbook> existing = userPassbookRepository.findByKeyUserIdAndKeyTypeName(userId, typeName);
        if (existing.isEmpty()) {
            return;
        }
        Set<String> existingKeys = new HashSet<>();
        for (UserPassbook row : existing) {
            String key = dedupeKey(row);
            if (key != null) {
                existingKeys.add(key);
            }
        }
        for (UserPassbook candidate : candidates) {
            String key = dedupeKey(candidate);
            // A missing courseId is never treated as a duplicate (matches the source).
            if (key != null && existingKeys.contains(key)) {
                throw new ApiException(ADD_API_ID, HttpStatus.BAD_REQUEST, "Passbook entry already exist");
            }
        }
    }

    /** Duplicate identity: competencyId + competencyLevelId + courseId (case-insensitive); null if no courseId. */
    private static String dedupeKey(UserPassbook row) {
        String courseId = row.getAcquiredDetails() == null ? null : row.getAcquiredDetails().get("courseId");
        if (courseId == null || courseId.isBlank()) {
            return null;
        }
        return row.getKey().getTypeId() + "|" + row.getKey().getContextId() + "|"
                + courseId.toLowerCase();
    }

    private String stringify(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof String string) {
            return string;
        }
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new ApiException(ADD_API_ID, HttpStatus.BAD_REQUEST,
                    "Failed to parse acquiredDetails value: " + value);
        }
    }

    private Instant parseEffectiveDate(String value) {
        if (value == null || value.isBlank()) {
            return Instant.now();
        }
        try {
            return Instant.parse(value);
        } catch (DateTimeParseException ignored) {
            // fall through to the local date-time formats below
        }
        for (DateTimeFormatter formatter : List.of(DateTimeFormatter.ISO_LOCAL_DATE_TIME, SQL_TIMESTAMP)) {
            try {
                return LocalDateTime.parse(value, formatter).atZone(IST).toInstant();
            } catch (DateTimeParseException ignored) {
                // try the next format
            }
        }
        throw new ApiException(ADD_API_ID, HttpStatus.BAD_REQUEST,
                "Invalid effectiveDate. Use ISO-8601 or 'yyyy-MM-dd HH:mm:ss'. Got: " + value);
    }

    private void validateTypeName(String apiId, String typeName) {
        if (!supportedTypeNames.contains(typeName)) {
            throw new ApiException(apiId, HttpStatus.BAD_REQUEST,
                    "Invalid typeName. Supported typeNames are " + supportedTypeNames);
        }
    }

    /**
     * Groups the raw passbook rows into per-user, per-competency structures. Mirrors the source
     * repo's {@code CompetencyPassbookParser}: one {@link CompetencyPassbookInfo} per user, each
     * competency carrying its acquired entries (newest first, duplicates by course+level dropped).
     */
    private Map<String, Object> buildContent(List<UserPassbook> rows) {
        Map<String, CompetencyPassbookInfo> byUser = new LinkedHashMap<>();

        for (UserPassbook row : rows) {
            UserPassbookKey key = row.getKey();
            CompetencyPassbookInfo passbook =
                    byUser.computeIfAbsent(key.getUserId(), CompetencyPassbookInfo::new);

            CompetencyInfo competency = passbook.getCompetencies()
                    .computeIfAbsent(key.getTypeId(), CompetencyInfo::new);
            if (competency.getAdditionalParams() == null || competency.getAdditionalParams().isEmpty()) {
                competency.setAdditionalParams(row.getAdditionalParams());
            }

            Map<String, Object> acquired = new LinkedHashMap<>();
            acquired.put("acquiredChannel", key.getAcquiredChannel());
            acquired.put("competencyLevelId", key.getContextId());
            acquired.put("effectiveDate", key.getEffectiveDate());
            Map<String, String> acquiredDetails = row.getAcquiredDetails();
            acquired.put("additionalParams", acquiredDetails);
            if (acquiredDetails != null) {
                // Also flatten the stored details (courseId, createdBy, ...) to the top level,
                // preserving the source API's response shape.
                acquired.putAll(acquiredDetails);
            }

            if (!isDuplicate(competency, acquired)) {
                competency.getAcquiredDetails().add(acquired);
                competency.getAcquiredDetails().sort(BY_EFFECTIVE_DATE_DESC);
            }
        }

        Map<String, Object> content = new LinkedHashMap<>();
        content.put("count", byUser.size());
        content.put("content", new ArrayList<>(byUser.values()));
        return content;
    }

    /** True if this competency already has an entry with the same courseId and competencyLevelId. */
    private boolean isDuplicate(CompetencyInfo competency, Map<String, Object> candidate) {
        Object courseId = candidate.get("courseId");
        Object levelId = candidate.get("competencyLevelId");
        if (courseId == null || levelId == null) {
            return false;
        }
        for (Map<String, Object> existing : competency.getAcquiredDetails()) {
            if (courseId.toString().equalsIgnoreCase(String.valueOf(existing.get("courseId")))
                    && levelId.toString().equalsIgnoreCase(String.valueOf(existing.get("competencyLevelId")))) {
                return true;
            }
        }
        return false;
    }

    private static Instant asInstant(Object value) {
        return value instanceof Instant instant ? instant : null;
    }
}
