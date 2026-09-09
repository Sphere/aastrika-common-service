package org.aastrika.service.impl;

import java.util.*;

import org.aastrika.client.UserMigrationClient;
import org.aastrika.common.Constants;
import org.aastrika.dao.CassandraDao;
import org.aastrika.dto.request.UserMigrateRequest;
import org.aastrika.dto.response.AppResponse;
import org.aastrika.exception.ApiException;
import org.aastrika.service.ProfileService;
import org.opensearch.action.search.SearchRequest;
import org.opensearch.action.search.SearchResponse;
import org.opensearch.client.RequestOptions;
import org.opensearch.client.RestHighLevelClient;
import org.opensearch.index.query.BoolQueryBuilder;
import org.opensearch.index.query.QueryBuilder;
import org.opensearch.index.query.QueryBuilders;
import org.opensearch.search.SearchHit;
import org.opensearch.search.builder.SearchSourceBuilder;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;

/**
 * Port of the source {@code ProfileServiceImpl.migrateUser}. The sequence is preserved:
 * <ol>
 *   <li>learner-service org migration (irreversible on success),</li>
 *   <li>read the user row from {@code sunbird.user},</li>
 *   <li>rewrite {@code profiledetails} to name the new org and write it back with {@code channel},</li>
 *   <li>assign the PUBLIC role,</li>
 *   <li>trigger a data-sync.</li>
 * </ol>
 *
 * <p>Two deliberate divergences from the source, both recorded in the release note:
 * <ul>
 *   <li>The PUBLIC role is assigned against {@code rootorgid} (the actual Cassandra column). The
 *       source read {@code "rootOrgId"} in camelCase from a row map whose keys are lower-cased by the
 *       driver, so it always passed {@code null} as the organisation — role assignment could never
 *       have worked. Fixing it means this step now genuinely runs.</li>
 *   <li>Failures map to meaningful status codes (404 for a missing user, 422 for unusable
 *       profile data, 502 for a downstream failure) instead of the source's blanket 500.</li>
 * </ul>
 *
 * <p>Field naming follows the source: the value written into {@code departmentName} and
 * {@code professionalDetails[].name} is the request's {@code channel}, not the organisation's
 * {@code orgname}. Those differ for some orgs; this preserves observed source behaviour.
 */
@Service
@Slf4j
public class ProfileServiceImpl implements ProfileService {

    private static final String API_ID = "api.user.migrate";
    private static final String API_ID_AUTOCOMPLETE = "api.user.autocomplete";
    private static final String TABLE_USER = "user";

    private static final String COL_ID = "id";
    private static final String COL_CHANNEL = "channel";
    private static final String COL_ROOT_ORG_ID = "rootorgid";
    private static final String COL_PROFILE_DETAILS = "profiledetails";

    private static final String EMPLOYMENT_DETAILS = "employmentDetails";
    private static final String PROFESSIONAL_DETAILS = "professionalDetails";

    private final UserMigrationClient userMigrationClient;
    private final CassandraDao cassandraDao;
    private final RestHighLevelClient openSearchClient;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final String keyspace;
    private final List<String> autoCompleteSearchFields;
    private final String[] autoCompleteIncludeFields;
    private final String userProfileIndex;
    private final String esProfileIndexType;

    public ProfileServiceImpl(
            @Value("${es.user.auto.complete.search.fields}") List<String> autoCompleteSearchFields,
            @Value("${es.user.auto.complete.include.fields}") String[] autoCompleteIncludeFields,
            @Value("${sb.lern.es.user.profile.index}") String userProfileIndex,
            @Value("${es.profile.index.type}") String esProfileIndexType,
            UserMigrationClient userMigrationClient,
            CassandraDao cassandraDao,
            @Qualifier("openSearchClient") RestHighLevelClient openSearchClient,
            @Value("${spring.cassandra.keyspace-name:sunbird}") String keyspace) {
        this.autoCompleteSearchFields = autoCompleteSearchFields;
        this.autoCompleteIncludeFields = autoCompleteIncludeFields;
        this.userProfileIndex = userProfileIndex;
        this.esProfileIndexType = esProfileIndexType;
        this.userMigrationClient = userMigrationClient;
        this.cassandraDao = cassandraDao;
        this.openSearchClient = openSearchClient;
        this.keyspace = keyspace;
    }

    @Override
    public AppResponse<Map<String, Object>> migrateUser(UserMigrateRequest request, String userToken,
                                                        String authToken) {
        String userId = request.getRequest().getUserId();
        String channel = request.getRequest().getChannel();

        // 1) Org migration. Irreversible once it succeeds (softDeleteOldOrg + forceMigration).
        String errMsg = userMigrationClient.migrateUser(userId, channel, userToken, authToken);
        if (errMsg != null) {
            // Surfaced verbatim: an invalid channel is reported by the learner service, not here.
            throw new ApiException(API_ID, HttpStatus.BAD_GATEWAY, errMsg);
        }
        log.info("User migrated to channel {}; continuing with profile update", channel);

        // 2) Read the user row.
        Map<String, Object> userData = readUser(userId);
        String rootOrgId = (String) userData.get(COL_ROOT_ORG_ID);

        // 3) Rewrite profiledetails, then write it back together with the new channel.
        String profileDetailsJson = (String) userData.get(COL_PROFILE_DETAILS);
        if (profileDetailsJson == null || profileDetailsJson.isBlank()) {
            throw new ApiException(API_ID, HttpStatus.UNPROCESSABLE_ENTITY,
                    "User is migrated but has no profileDetails to update");
        }

        Map<String, Object> values = new LinkedHashMap<>();
        values.put(COL_CHANNEL, channel);
        values.put(COL_PROFILE_DETAILS, rewriteProfileDetails(profileDetailsJson, channel, rootOrgId));
        cassandraDao.update(keyspace, TABLE_USER, values, Map.of(COL_ID, userId));

        // 4) PUBLIC role, against the real rootorgid (see class javadoc).
        errMsg = userMigrationClient.assignPublicRole(rootOrgId, userId);
        if (errMsg != null) {
            throw new ApiException(API_ID, HttpStatus.BAD_GATEWAY, errMsg);
        }

        // 5) Search reindex.
        errMsg = userMigrationClient.syncUser(userId);
        if (errMsg != null) {
            throw new ApiException(API_ID, HttpStatus.BAD_GATEWAY, errMsg);
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put(Constants.RESPONSE, "SUCCESS");
        return AppResponse.success(API_ID, result, HttpStatus.OK);
    }

    @Override
    public AppResponse<Map<String, Object>> userAutoComplete(String searchTerm) {
        Map<String, Object> result = new LinkedHashMap<>();
        Map<String, Object> resultResp = new HashMap<>();

        try {
            List<Map<String, Object>> userData = getUserSearchData(searchTerm);
            resultResp.put(Constants.CONTENT, userData);
            resultResp.put(Constants.COUNT, userData.size());
            result.put(Constants.RESPONSE, resultResp);
        } catch (Exception e) {
            log.error("Failed to get user details from ES for searchTerm={}", searchTerm, e);
            throw new ApiException(API_ID_AUTOCOMPLETE, HttpStatus.INTERNAL_SERVER_ERROR,
                    "Failed to get user details from ES: " + e.getMessage());
        }

        return AppResponse.success(API_ID_AUTOCOMPLETE, result, HttpStatus.OK);
    }

    public List<Map<String, Object>> getUserSearchData(String searchTerm) throws Exception {
        List<Map<String, Object>> resultArray = new ArrayList<>();
        Map<String, Object> result;
        final BoolQueryBuilder query = QueryBuilders.boolQuery();
        for (String field : autoCompleteSearchFields) {
            query.should(QueryBuilders.matchPhrasePrefixQuery(field, searchTerm));
        }
        final BoolQueryBuilder finalQuery = QueryBuilders.boolQuery();
        finalQuery.must(QueryBuilders.termQuery("status", 1)).must(query);
        SearchSourceBuilder sourceBuilder = new SearchSourceBuilder().query((QueryBuilder) finalQuery);
        sourceBuilder.fetchSource(autoCompleteIncludeFields, new String[] {});
        log.debug("user autocomplete: index={} query={}", userProfileIndex, sourceBuilder);

        SearchRequest searchRequest = new SearchRequest(userProfileIndex);
        searchRequest.source(sourceBuilder);
        searchRequest.indices(userProfileIndex);

        SearchResponse searchResponse = openSearchClient.search(searchRequest, RequestOptions.DEFAULT);
        log.debug("user autocomplete: {} hit(s)", searchResponse.getHits().getHits().length);

        for (SearchHit hit : searchResponse.getHits()) {
            result = hit.getSourceAsMap();
            resultArray.add(result);
        }
        return resultArray;
    }


    private Map<String, Object> readUser(String userId) {
        List<Map<String, Object>> rows = cassandraDao.findByProperties(
                keyspace, TABLE_USER, Map.of(COL_ID, userId),
                List.of(COL_ID, COL_CHANNEL, COL_ROOT_ORG_ID, COL_PROFILE_DETAILS));
        if (rows.isEmpty()) {
            // The source returned 500 here; a missing user is a 404.
            throw new ApiException(API_ID, HttpStatus.NOT_FOUND,
                    "User is migrated but no user record was found to update");
        }
        return rows.get(0);
    }

    /**
     * Sets the new org on {@code employmentDetails} (only if that key already exists, as in the
     * source) and replaces {@code professionalDetails} with a single entry naming the new org,
     * minting an {@code osid} when there was no entry to carry over.
     */
    @SuppressWarnings("unchecked")
    private String rewriteProfileDetails(String profileDetailsJson, String channel, String rootOrgId) {
        Map<String, Object> profileDetails;
        try {
            profileDetails = objectMapper.readValue(profileDetailsJson, new TypeReference<>() {
            });
        } catch (Exception e) {
            log.warn("profileDetails is not parseable JSON: {}", e.getMessage());
            throw new ApiException(API_ID, HttpStatus.UNPROCESSABLE_ENTITY,
                    "User is migrated but profileDetails could not be parsed");
        }

        if (profileDetails.get(EMPLOYMENT_DETAILS) instanceof Map<?, ?> existing) {
            Map<String, Object> employment = new LinkedHashMap<>((Map<String, Object>) existing);
            employment.put("departmentName", channel);
            employment.put("departmentId", rootOrgId);
            profileDetails.put(EMPLOYMENT_DETAILS, employment);
        }

        Map<String, Object> professional = new LinkedHashMap<>();
        Object current = profileDetails.get(PROFESSIONAL_DETAILS);
        if (current instanceof List<?> list && !list.isEmpty() && list.get(0) instanceof Map<?, ?> first) {
            professional.putAll((Map<String, Object>) first);
        } else {
            professional.put("osid", UUID.randomUUID().toString());
        }
        professional.put("name", channel);
        professional.put("id", rootOrgId);

        List<Map<String, Object>> professionalDetails = new ArrayList<>();
        professionalDetails.add(professional);
        profileDetails.put(PROFESSIONAL_DETAILS, professionalDetails);

        try {
            return objectMapper.writeValueAsString(profileDetails);
        } catch (Exception e) {
            throw new ApiException(API_ID, HttpStatus.INTERNAL_SERVER_ERROR,
                    "Failed to serialise updated profileDetails");
        }
    }
}
