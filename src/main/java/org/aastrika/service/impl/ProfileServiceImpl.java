package org.aastrika.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.aastrika.client.Producer;
import org.aastrika.common.Constants;
import org.aastrika.config.ServerConfig;
import org.aastrika.dao.CassandraDao;
import org.aastrika.dto.response.SBApiResponse;
import org.aastrika.service.IndexerService;
import org.aastrika.service.ProfileService;
import org.aastrika.service.UserUtilityService;
import org.aastrika.util.ProjectUtil;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.opensearch.action.search.SearchResponse;
import org.opensearch.index.query.BoolQueryBuilder;
import org.opensearch.index.query.QueryBuilder;
import org.opensearch.index.query.QueryBuilders;
import org.opensearch.search.SearchHit;
import org.opensearch.search.builder.SearchSourceBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.sql.Timestamp;
import java.util.*;

@Slf4j
@Service
public class ProfileServiceImpl implements ProfileService {

    @Autowired
    ServerConfig serverConfig;

    @Autowired
    IndexerService indexerService;

    @Autowired
    ObjectMapper mapper;

    @Autowired
    OutboundRequestHandlerServiceImpl outboundRequestHandlerService;

    @Autowired
    UserUtilityService userUtilityService;

    @Autowired
    Producer kafkaProducer;

    @Autowired
    StorageServiceImpl storageService;

    @Autowired
    CassandraDao cassandraDao;


    @Override
    public SBApiResponse userAutoComplete(String searchTerm) {
        SBApiResponse response = new SBApiResponse();
        response.setResponseCode(HttpStatus.BAD_REQUEST);
        response.getParams().setStatus(Constants.FAILED);
        if (StringUtils.isEmpty(searchTerm)) {
            response.getParams().setErrmsg("Invalid Search Term");
            return response;
        }

        response.setResponseCode(HttpStatus.INTERNAL_SERVER_ERROR);
        Map<String, Object> resultResp = new HashMap<>();
        System.out.println(response);
        try {
            List<Map<String, Object>> userData = getUserSearchData(searchTerm);
            System.out.println("userData" + userData);
            resultResp.put(Constants.CONTENT, userData);
            resultResp.put(Constants.COUNT, userData.size());
            response.setResponseCode(HttpStatus.OK);
            response.getParams().setStatus(Constants.SUCCESS);
            response.put(Constants.RESPONSE, resultResp);
        } catch (Exception e) {
            response.getParams().setErrmsg("Failed to get user details from ES. Exception: " + e.getStackTrace());
        }
        System.out.println("Came at last");
        return response;
    }

    public List<Map<String, Object>> getUserSearchData(String searchTerm) throws Exception {
        List<Map<String, Object>> resultArray = new ArrayList<>();
        Map<String, Object> result;
        final BoolQueryBuilder query = QueryBuilders.boolQuery();
        for (String field : serverConfig.getEsAutoCompleteSearchFields()) {
            query.should(QueryBuilders.matchPhrasePrefixQuery(field, searchTerm));
        }
        System.out.println(query);
        final BoolQueryBuilder finalQuery = QueryBuilders.boolQuery();
        finalQuery.must(QueryBuilders.termQuery(Constants.STATUS, 1)).must(query);
        SearchSourceBuilder sourceBuilder = new SearchSourceBuilder().query((QueryBuilder) finalQuery);
        sourceBuilder.fetchSource(serverConfig.getEsAutoCompleteIncludeFields(), new String[] {});
        System.out.println("indexname" + serverConfig.getSbEsUserProfileIndex());
        System.out.println("final query" + finalQuery);
        SearchResponse searchResponse = indexerService.getEsResult(serverConfig.getSbEsUserProfileIndex(),
                serverConfig.getEsProfileIndexType(), sourceBuilder, true);
        System.out.println("source builder" + sourceBuilder);
        System.out.println("search response" + searchResponse);

        for (SearchHit hit : searchResponse.getHits()) {
            result = hit.getSourceAsMap();
            resultArray.add(result);
        }
        return resultArray;
    }


    @Override
    public SBApiResponse migrateUser(Map<String, Object> request, String userToken, String authToken) {
        SBApiResponse response = new SBApiResponse(Constants.ORG_PROFILE_UPDATE);
        // Initializing default error
        response.setResponseCode(HttpStatus.INTERNAL_SERVER_ERROR);
        response.getParams().setStatus(Constants.FAILED);

        String errMsg = validateMigrateRequest(request);
        if (StringUtils.isNotEmpty(errMsg)) {
            response.getParams().setErrmsg(errMsg);
            response.setResponseCode(HttpStatus.BAD_REQUEST);
            return response;
        }

        HashMap<String, String> headerValues = new HashMap<>();
        headerValues.put(Constants.AUTH_TOKEN, authToken);
        headerValues.put(Constants.X_AUTH_TOKEN, userToken);
        headerValues.put(Constants.CONTENT_TYPE, Constants.APPLICATION_JSON);

        Map<String, Object> requestBody = (Map<String, Object>) request.get(Constants.REQUEST);
        String userId = (String) requestBody.get(Constants.USER_ID);
        String orgName = (String) requestBody.get(Constants.CHANNEL);
        errMsg = executeMigrateUser(getUserMigrateRequest(userId, orgName, false), headerValues);
        if (StringUtils.isNotEmpty(errMsg)) {
            setErrorData(response, errMsg);
            return response;
        }
        log.info(String.format("Successfully migrated user. UserId: %s, Channel: %s", userId, orgName));

        Map<String, Object> userData = getUserDetailsForId(userId);
        if (ObjectUtils.isEmpty(userData)) {
            response.getParams().setErrmsg(String.format("Failed to get User record from DB. UserId: %s", userId));
            return response;
        }

        Map<String, Object> updateDBRequest = new HashMap<>();
        updateDBRequest.put(Constants.CHANNEL, orgName);

        String profileDetailsStr = (String) userData.get(Constants.PROFILE_DETAILS_LOWER);
        if (StringUtils.isEmpty(profileDetailsStr)) {
            response.getParams().setErrmsg("ProfileDetails is null for User.");
            return response;
        }
        try {
            Map<String, Object> profileDetails = mapper.readValue(profileDetailsStr,
                    new TypeReference<Map<String, Object>>() {
                    });
            if (profileDetails.containsKey(Constants.EMPLOYMENT_DETAILS)) {
                Map<String, Object> empDetails = (Map<String, Object>) profileDetails.get(Constants.EMPLOYMENT_DETAILS);
                empDetails.put(Constants.DEPARTMENTNAME, orgName);
                empDetails.put(Constants.DEPARTMENT_ID, (String) userData.get(Constants.ROOT_ORG_ID_LOWER));
                profileDetails.put(Constants.EMPLOYMENT_DETAILS, empDetails);
            }

            Map<String, Object> professionalDetail = null;
            if (profileDetails.containsKey(Constants.PROFESSIONAL_DETAILS)
                    && !ObjectUtils.isEmpty(profileDetails.get(Constants.PROFESSIONAL_DETAILS))) {
                professionalDetail = ((List<Map<String, Object>>) profileDetails.get(Constants.PROFESSIONAL_DETAILS))
                        .get(0);
            } else {
                professionalDetail = new HashMap<>();
                professionalDetail.put(Constants.OSID, UUID.randomUUID().toString());
            }

            professionalDetail.put(Constants.NAME, orgName);
            professionalDetail.put(Constants.ID, (String) userData.get(Constants.ROOT_ORG_ID_LOWER));
            profileDetails.put(Constants.PROFESSIONAL_DETAILS, Arrays.asList(professionalDetail));

            updateDBRequest.put(Constants.PROFILE_DETAILS_LOWER, mapper.writeValueAsString(profileDetails));
        } catch (Exception e) {
            errMsg = String.format("Failed to parse profileDetails object for userId: %s. Exception: ",
                    (String) requestBody.get(Constants.USER_ID));
            log.error(errMsg, e);
            response.getParams().setErrmsg(errMsg);
            return response;
        }

        Map<String, Object> compositeKey = new HashMap<String, Object>() {
            private static final long serialVersionUID = 1L;
            {
                put(Constants.ID, userId);
            }
        };

        // Single, non-batched insert (YCQL/Yugabyte compatibility - see
        // sunbird-cb-ext@dcb1547): Cassandra INSERT on an existing primary key only
        // sets the supplied columns and leaves the rest of the row untouched.
        Map<String, Object> updateRow = new HashMap<>(updateDBRequest);
        updateRow.putAll(compositeKey);
        try {
            cassandraDao.insert(Constants.KEYSPACE_SUNBIRD, Constants.TABLE_USER, updateRow);
        } catch (Exception e) {
            errMsg = String.format("Failed to update profileDetails for UserId : %s", userId);
            log.error(errMsg, e);
            response.getParams().setErrmsg(errMsg);
            return response;
        }

        boolean assignValue = userUtilityService.assignRole((String) userData.get(Constants.ROOT_ORG_ID), userId,
                StringUtils.EMPTY);

        if (assignValue) {
            errMsg = syncUserData(userId);
        } else {
            response.getParams().setErrmsg("Failed to assign PUBLIC role to user. UserId: " + userId);
            return response;
        }

        if (StringUtils.isNotEmpty(errMsg)) {
            response.getParams().setErrmsg(errMsg);
            return response;
        }

        response.setResponseCode(HttpStatus.OK);
        response.getResult().put(Constants.RESPONSE, Constants.SUCCESS);
        response.getParams().setStatus(Constants.SUCCESS);
        return response;
    }

    private String validateMigrateRequest(Map<String, Object> requestBody) {
        StringBuffer str = new StringBuffer();
        List<String> errObjList = new ArrayList<String>();

        Map<String, Object> request = (Map<String, Object>) requestBody.get(Constants.REQUEST);
        if (ObjectUtils.isEmpty(request)) {
            str.append("Request object is empty.");
            return str.toString();
        }
        if (StringUtils.isEmpty((String) request.get(Constants.USER_ID))) {
            errObjList.add(Constants.USER_ID);
        }
        if (StringUtils.isEmpty((String) request.get(Constants.CHANNEL))) {
            errObjList.add(Constants.CHANNEL);
        }

        if (!errObjList.isEmpty()) {
            str.append("Failed to Register User Details. Missing Params - [").append(errObjList.toString()).append("]");
        }
        return str.toString();
    }

    private String executeMigrateUser(Map<String, Object> request, Map<String, String> headers) {
        String errMsg = StringUtils.EMPTY;
        Map<String, Object> migrateResponse = (Map<String, Object>) outboundRequestHandlerService.fetchResultUsingPatch(
                serverConfig.getSbUrl() + serverConfig.getLmsUserMigratePath(), request, headers);
        if (migrateResponse == null
                || !Constants.OK.equalsIgnoreCase((String) migrateResponse.get(Constants.RESPONSE_CODE))) {
            errMsg = migrateResponse == null ? "Failed to migrate User."
                    : (String) ((Map<String, Object>) migrateResponse.get(Constants.PARAMS))
                    .get(Constants.ERROR_MESSAGE);
        }
        return errMsg;
    }

    private Map<String, Object> getUserMigrateRequest(String userId, String channel, boolean isSelfMigrate) {
        Map<String, Object> requestBody = new HashMap<String, Object>() {
            {
                put(Constants.USER_ID, userId);
                put(Constants.CHANNEL, channel);
                put(Constants.SOFT_DELETE_OLD_ORG, true);
                put(Constants.NOTIFY_MIGRATION, false);
                if (!isSelfMigrate) {
                    put(Constants.FORCE_MIGRATION, true);
                }
            }
        };
        Map<String, Object> request = new HashMap<String, Object>() {
            {
                put(Constants.REQUEST, requestBody);
            }
        };
        return request;
    }

    private void setErrorData(SBApiResponse response, String errMsg) {
        response.getParams().setStatus(Constants.FAILED);
        response.getParams().setErrmsg(errMsg);
        response.setResponseCode(HttpStatus.INTERNAL_SERVER_ERROR);
    }

    private String syncUserData(String userId) {
        String errMsg = null;
        Map<String, Object> requestBody = new HashMap<String, Object>();
        Map<String, Object> request = new HashMap<String, Object>();
        request.put(Constants.OPERATION_TYPE, Constants.SYNC);
        request.put(Constants.OBJECT_IDS, Arrays.asList(userId));
        request.put(Constants.OBJECT_TYPE, Constants.USER);
        requestBody.put(Constants.REQUEST, request);

        Map<String, Object> syncDataResp = (Map<String, Object>) outboundRequestHandlerService.fetchResultUsingPost(
                serverConfig.getSbUrl() + serverConfig.getLmsDataSyncPath(), requestBody, Map.of());
        if (syncDataResp == null
                || !Constants.OK.equalsIgnoreCase((String) syncDataResp.get(Constants.RESPONSE_CODE))) {
            errMsg = "Failed to call Data Sync after updating Profile for User: " + userId;
        }
        return errMsg;
    }

    private Map<String, Object> getUserDetailsForId(String userId) {
        Map<String, Object> request = new HashMap<>();
        request.put(Constants.ID, userId);
        List<Map<String, Object>> userList = cassandraDao.findByProperties(Constants.KEYSPACE_SUNBIRD,
                Constants.TABLE_USER, request, null);
        if (CollectionUtils.isNotEmpty(userList)) {
            return userList.get(0);
        } else {
            return Map.of();
        }
    }

    @Override
    public SBApiResponse bulkUpload(MultipartFile mFile, String orgId, String orgName, String userId) {
        SBApiResponse response = ProjectUtil.createDefaultResponse(Constants.API_USER_BULK_UPLOAD);
        try {
            SBApiResponse uploadResponse = storageService.uploadFile(mFile, serverConfig.getBulkUploadContainerName());
            if (!HttpStatus.OK.equals(uploadResponse.getResponseCode())) {
                setErrorData(response, String.format("Failed to upload file. Error: %s",
                        (String) uploadResponse.getParams().getErrmsg()));
                return response;
            }

            Map<String, Object> uploadedFile = new HashMap<>();
            uploadedFile.put(Constants.ROOT_ORG_ID, orgId);
            uploadedFile.put(Constants.IDENTIFIER, UUID.randomUUID().toString());
            uploadedFile.put(Constants.FILE_NAME, uploadResponse.getResult().get(Constants.NAME));
            uploadedFile.put(Constants.FILE_PATH, uploadResponse.getResult().get(Constants.URL));
            uploadedFile.put(Constants.DATE_CREATED_ON, new Timestamp(System.currentTimeMillis()));
            uploadedFile.put(Constants.STATUS, Constants.INITIATED_CAPITAL);
            uploadedFile.put(Constants.COMMENT, StringUtils.EMPTY);
            uploadedFile.put(Constants.CREATED_BY, userId);

            // Single, non-batched insert (YCQL/Yugabyte compatibility - see
            // sunbird-cb-ext@dcb1547).
            try {
                cassandraDao.insert(Constants.DATABASE, Constants.TABLE_USER_BULK_UPLOAD, uploadedFile);
            } catch (Exception e) {
                log.error("Failed to insert user bulk upload record for orgId: " + orgId, e);
                setErrorData(response, "Failed to update database with user bulk upload file details.");
                return response;
            }

            response.getParams().setStatus(Constants.SUCCESSFUL);
            response.setResponseCode(HttpStatus.OK);
            response.getResult().putAll(uploadedFile);
            uploadedFile.put(Constants.ORG_NAME, orgName);
            kafkaProducer.push(serverConfig.getUserBulkUploadTopic(), uploadedFile);
            sendBulkUploadNotification(orgId, orgName, (String) uploadResponse.getResult().get(Constants.URL));
        } catch (Exception e) {
            setErrorData(response,
                    String.format("Failed to process user bulk upload request. Error: ", e.getMessage()));
        }
        return response;
    }

    private void sendBulkUploadNotification(String orgId, String orgName, String fileUrl) {
        for (String email : serverConfig.getBulkUploadEmailNotificationList()) {
            if (StringUtils.isBlank(email)) {
                return;
            }
        }
        Map<String, Object> request = new HashMap<>();
        Map<String, Object> requestBody = new HashMap<String, Object>();
        requestBody.put(Constants.BODY, Constants.HELLO);
        requestBody.put(Constants.EMAIL_TEMPLATE_TYPE, serverConfig.getBulkUploadEmailTemplate());
        requestBody.put(Constants.LINK, fileUrl);
        requestBody.put(Constants.MODE, Constants.EMAIL);
        requestBody.put(Constants.ORG_NAME, orgName);
        requestBody.put(Constants.ORG_ID, orgId);
        requestBody.put(Constants.RECIPIENT_EMAILS, serverConfig.getBulkUploadEmailNotificationList());
        requestBody.put(Constants.SET_PASSWORD_LINK, true);
        requestBody.put(Constants.SUBJECT, serverConfig.getBulkUploadEmailNotificationSubject());

        request.put(Constants.REQUEST, requestBody);

        outboundRequestHandlerService.fetchResultUsingPost(
                serverConfig.getSbUrl() + serverConfig.getSbSendNotificationEmailPath(), request,
                ProjectUtil.getDefaultHeaders());
    }

    @Override
    public SBApiResponse getBulkUploadDetails(String orgId) {
        SBApiResponse response = ProjectUtil.createDefaultResponse(Constants.API_USER_BULK_UPLOAD_STATUS);
        try {
            Map<String, Object> propertyMap = new HashMap<>();
            if (StringUtils.isNotBlank(orgId)) {
                propertyMap.put(Constants.ROOT_ORG_ID, orgId);
            }
            List<Map<String, Object>> bulkUploadList = cassandraDao.findByProperties(Constants.DATABASE,
                    Constants.TABLE_USER_BULK_UPLOAD, propertyMap, serverConfig.getBulkUploadStatusFields());
            response.getParams().setStatus(Constants.SUCCESSFUL);
            response.setResponseCode(HttpStatus.OK);
            response.getResult().put(Constants.CONTENT, bulkUploadList);
            response.getResult().put(Constants.COUNT, bulkUploadList != null ? bulkUploadList.size() : 0);
        } catch (Exception e) {
            setErrorData(response,
                    String.format("Failed to get user bulk upload request status. Error: ", e.getMessage()));
        }
        return response;
    }
}
