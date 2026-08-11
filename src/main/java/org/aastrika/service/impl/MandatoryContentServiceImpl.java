package org.aastrika.service.impl;


import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import lombok.extern.slf4j.Slf4j;
import org.aastrika.common.Constants;
import org.aastrika.config.ServerConfig;
import org.aastrika.dao.CassandraDao;
import org.aastrika.dto.BatchEnrolment;
import org.aastrika.dto.MandatoryContentInfo;
import org.aastrika.dto.SearchUserApiContent;
import org.aastrika.dto.SunbirdUserProfileDetail;
import org.aastrika.dto.request.SunbirdApiRequest;
import org.aastrika.dto.request.UserProgressRequest;
import org.aastrika.dto.response.MandatoryContentResponse;
import org.aastrika.service.MandatoryContentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

@Slf4j
@Service
public class MandatoryContentServiceImpl implements MandatoryContentService {

    @Autowired
    private OutboundRequestHandlerServiceImpl outboundReqService;

    @Autowired
    private ServerConfig serverConfig;

    @Autowired
    private CassandraDao cassandraDao;

    @Autowired
    private UserUtilityServiceImpl userUtilService;

    @Autowired
    private ContentServiceImpl contentService;

    private ObjectMapper mapper = new ObjectMapper();

    @Override
    public MandatoryContentResponse getMandatoryContentStatusForUser(String authUserToken, String rootOrg, String org,
                                                                     String userId) {
        MandatoryContentResponse response = new MandatoryContentResponse();

        Map<String, Object> propertyMap = new HashMap<>();
        propertyMap.put(Constants.ROOT_ORG, rootOrg);
        propertyMap.put(Constants.ORG, org);
        List<Map<String, Object>> contentList = cassandraDao.findByProperties(Constants.KEYSPACE_SUNBIRD,
                 Constants.TABLE_MANDATORY_USER_CONTENT, propertyMap, new ArrayList<>());

        if (CollectionUtils.isEmpty(contentList)) {
            log.info("getMandatoryContentStatusForUser: There are no mandatory Content set in DB.");
            return response;
        }

        for (Map<String, Object> content : contentList) {
            String contentId = (String) content.get(Constants.CONTENT_ID);
            content.remove(Constants.CONTENT_ID);
            MandatoryContentInfo info = mapper.convertValue(content, MandatoryContentInfo.class);
            response.addContentInfo(contentId, info);
        }

        try {
            log.info("getMandatoryContentStatusForUser: MandatoryCourse Details : "
                    + new ObjectMapper().writer().withDefaultPrettyPrinter().writeValueAsString(response));
        } catch (JsonProcessingException e) {
            log.error(String.valueOf(e));
        }
        enrichProgressDetails(authUserToken, response, userId);
        try {
            log.info("getMandatoryContentStatusForUser: Ret Value is: "
                    + new ObjectMapper().writer().withDefaultPrettyPrinter().writeValueAsString(response));
        } catch (JsonProcessingException e) {
            log.error(String.valueOf(e));
        }
        Iterator<MandatoryContentInfo> entries = response.getContentDetails().values().iterator();
        boolean isCompleted = false;
        while (entries.hasNext()) {
            MandatoryContentInfo entry = entries.next();
            if (entry.getUserProgress() < entry.getMinProgressForCompletion()) {
                response.setMandatoryCourseCompleted(false);
                isCompleted = false;
                break;
            } else {
                isCompleted = true;
            }
        }
        if (isCompleted) {
            response.setMandatoryCourseCompleted(true);
        }
        return response;
    }

    public void enrichProgressDetails(String authUserToken, MandatoryContentResponse mandatoryContentInfo,
                                      String userId) {
        HashMap<String, Object> req;
        HashMap<String, Object> reqObj;
        List<String> fields = Arrays.asList("progressdetails");
        HashMap<String, String> headersValues = new HashMap<>();
        headersValues.put("X-Authenticated-User-Token", authUserToken);
        headersValues.put("Authorization", serverConfig.getSbApiKey());
        for (Map.Entry<String, MandatoryContentInfo> infoMap : mandatoryContentInfo.getContentDetails().entrySet()) {
            try {
                req = new HashMap<>();
                reqObj = new HashMap<>();
                reqObj.put("userId", userId);
                reqObj.put("courseId", infoMap.getKey());
                reqObj.put("batchId", infoMap.getValue().getBatchId());
                reqObj.put("fields", fields);
                req.put("request", reqObj);
                Map<String, Object> response = outboundReqService.fetchResultUsingPost(
                        serverConfig.getCourseServiceHost() + serverConfig.getProgressReadEndPoint(),
                        req, headersValues);
                if (response.get("responseCode").equals("OK")) {
                    List<Object> result = (List<Object>) ((HashMap<String, Object>) response.get("result"))
                            .get("contentList");
                    if (!CollectionUtils.isEmpty(result)) {
                        Optional<Object> optionResult = result.stream().findFirst();
                        if (optionResult.isPresent()) {
                            Map<String, Object> content = (Map<String, Object>) optionResult.get();
                            BigDecimal progress = new BigDecimal(content.get("completionPercentage").toString());
                            mandatoryContentInfo.getContentDetails().get(infoMap.getKey())
                                    .setUserProgress(progress.floatValue());
                        }
                    }
                }
            } catch (Exception ex) {
                log.error(String.valueOf(ex));
            }
        }
    }

    public Map<String, Object> getUserProgress(SunbirdApiRequest requestBody, String authUserToken) {
        Map<String, Object> result = new HashMap<>();
        try {
            UserProgressRequest requestData = validateGetBatchEnrolment(requestBody);
            if (ObjectUtils.isEmpty(requestData)) {
                result.put(Constants.STATUS, Constants.FAILED);
                result.put(Constants.MESSAGE, "check your request params");
                return result;
            }

            // get all enrolled details
            List<Map<String, Object>> userEnrolmentList = new ArrayList<>();
            for (BatchEnrolment request : requestData.getBatchList()) {
                Map<String, Object> propertyMap = new HashMap<>();
                propertyMap.put(Constants.BATCH_ID, request.getBatchId());
                propertyMap.put(Constants.ACTIVE, Boolean.TRUE);
                if (request.getUserList() != null && !request.getUserList().isEmpty()) {
                    propertyMap.put(Constants.USER_ID_CONSTANT, request.getUserList());
                }
                userEnrolmentList.addAll(cassandraDao.findByProperties(Constants.KEYSPACE_SUNBIRD_COURSES,
                        Constants.TABLE_USER_ENROLMENT, propertyMap,
                        new ArrayList<>(Arrays.asList(Constants.USER_ID_CONSTANT, Constants.COURSE_ID,
                                Constants.BATCH_ID, Constants.COMPLETION_PERCENTAGE, Constants.PROGRESS,
                                Constants.STATUS, Constants.ISSUED_CERTIFICATES))));
            }
            // restricting with only 100 items in the response
            if (userEnrolmentList.size() > 100) {
                userEnrolmentList = userEnrolmentList.subList(0, 100);
            }

            // get all user details
            List<String> enrolledUserId = userEnrolmentList.stream()
                    .map(obj -> (String) obj.get(Constants.USER_ID_CONSTANT)).collect(Collectors.toList());
            List<String> userFields = new ArrayList<>(Arrays.asList(Constants.USER_ID_CONSTANT, Constants.FIRSTNAME,
                    Constants.LASTNAME, Constants.PROFILE_DETAILS_PRIMARY_EMAIL, Constants.CHANNEL,
                    Constants.PROFILE_DETAILS_DESIGNATION, Constants.PROFILE_DETAILS_DESIGNATION_OTHER));
            Map<String, Object> userMap = userUtilService.getUsersDataFromUserIds(enrolledUserId, userFields,
                    authUserToken);

            Map<String, Integer> courseLeafCount = new HashMap<>();
            for (Map<String, Object> responseObj : userEnrolmentList) {
                // set user details
                if (userMap.containsKey(responseObj.get(Constants.USER_ID_CONSTANT))) {
                    SearchUserApiContent userObj = mapper.convertValue(
                            userMap.get(responseObj.get(Constants.USER_ID_CONSTANT)), SearchUserApiContent.class);
                    appendUserDetails(responseObj, userObj);
                }
                // set completion percentage & status
                String courseId = (String) responseObj.get(Constants.COURSE_ID);
                if (!courseLeafCount.containsKey(courseId)) {
                    Map<String, Object> contentResponse = contentService.searchLiveContent(courseId);
                    if (!ObjectUtils.isEmpty(contentResponse)) {
                        Map<String, Object> contentResult = (Map<String, Object>) contentResponse.get(Constants.RESULT);
                        if (0 < (Integer) contentResult.get(Constants.COUNT)) {
                            List<Map<String, Object>> contentList = (List<Map<String, Object>>) contentResult
                                    .get(Constants.CONTENT);
                            Map<String, Object> content = contentList.get(0);
                            courseLeafCount.put(courseId, (Integer) content.get(Constants.LEAF_NODES_COUNT));
                        }
                    }
                }
                setCourseCompletiondetails(responseObj, courseLeafCount.get(courseId));
            }

            result.put(Constants.STATUS, Constants.SUCCESSFUL);
            result.put(Constants.RESULT, userEnrolmentList);
        } catch (Exception ex) {
            result.put(Constants.STATUS, Constants.FAILED);
            log.error(String.valueOf(ex));
        }
        return result;
    }

    private UserProgressRequest validateGetBatchEnrolment(SunbirdApiRequest requestBody) {
        try {
            UserProgressRequest userProgressRequest = new UserProgressRequest();
            if (!ObjectUtils.isEmpty(requestBody.getRequest())) {
                userProgressRequest = mapper.convertValue(requestBody.getRequest(), UserProgressRequest.class);
            }

            if (!userProgressRequest.getBatchList().isEmpty()) {
                for (BatchEnrolment batchEnrolment : userProgressRequest.getBatchList()) {
                    if (ObjectUtils.isEmpty(batchEnrolment.getBatchId())) {
                        return null;
                    }
                }
                return userProgressRequest;
            }
        } catch (Exception e) {
            log.error(String.valueOf(e));
        }
        return null;
    }


    private void appendUserDetails(Map<String, Object> responseObj, SearchUserApiContent userObj) throws Exception {
        if (!ObjectUtils.isEmpty(userObj)) {
            responseObj.put(Constants.FIRSTNAME, userObj.getFirstName());
            responseObj.put(Constants.LASTNAME, userObj.getLastName());
            responseObj.put(Constants.DEPARTMENT, userObj.getChannel());

            if (!ObjectUtils.isEmpty(userObj.getProfileDetails())) {
                SunbirdUserProfileDetail profileDetails = userObj.getProfileDetails();
                if (!ObjectUtils.isEmpty(profileDetails.getPersonalDetails())
                        && profileDetails.getPersonalDetails().containsKey(Constants.PRIMARY_EMAIL)) {
                    responseObj.put(Constants.EMAIL, profileDetails.getPersonalDetails().get(Constants.PRIMARY_EMAIL));
                }
                if (!ObjectUtils.isEmpty(profileDetails.getProfessionalDetails())
                        && !profileDetails.getProfessionalDetails().isEmpty()) {
                    if (!ObjectUtils.isEmpty(profileDetails.getProfessionalDetails().get(0).getDesignation())) {
                        responseObj.put(Constants.DESIGNATION,
                                profileDetails.getProfessionalDetails().get(0).getDesignation());
                    } else if (!ObjectUtils
                            .isEmpty(profileDetails.getProfessionalDetails().get(0).getDesignationOther())) {
                        responseObj.put(Constants.DESIGNATION,
                                profileDetails.getProfessionalDetails().get(0).getDesignationOther());
                    } else {
                        responseObj.put(Constants.DESIGNATION, "");
                    }
                } else {
                    responseObj.put(Constants.DESIGNATION, "");
                }
            }
        }
    }


    private void setCourseCompletiondetails(Map<String, Object> responseObj, int leafNodeCount) {
        int progress = (int) responseObj.get(Constants.PROGRESS);
        if (progress == 0) {
            responseObj.put(Constants.COMPLETION_PERCENTAGE, 0);
            responseObj.put(Constants.STATUS, 0);
        } else if (progress >= 1 && progress < leafNodeCount) {
            responseObj.put(Constants.COMPLETION_PERCENTAGE, (progress * 100) / leafNodeCount);
            responseObj.put(Constants.STATUS, 1);
        } else {
            responseObj.put(Constants.COMPLETION_PERCENTAGE, 100);
            responseObj.put(Constants.STATUS, 2);
        }
    }
}