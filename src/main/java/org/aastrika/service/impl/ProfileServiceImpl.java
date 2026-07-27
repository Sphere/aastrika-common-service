package org.aastrika.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.aastrika.common.Constants;
import org.aastrika.common.PropertiesCache;
import org.aastrika.common.RedisCacheMgr;
import org.aastrika.common.ServerConfig;
import org.aastrika.dto.SBApiResponse;
import org.aastrika.service.IndexerService;
import org.aastrika.service.ProfileService;
import org.aastrika.service.UserUtilityService;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.elasticsearch.rest.RestStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.*;

@Slf4j
@Service
public class ProfileServiceImpl implements ProfileService {

    @Autowired
    private ServerConfig serverConfig;

    @Autowired
    private OutboundRequestHandlerServiceImpl outboundRequestHandlerService;

    @Autowired
    private UserUtilityService userUtilityService;

    @Autowired
    RedisCacheMgr redisCacheMgr;

    @Autowired
    ObjectMapper mapper;

    @Autowired
    IndexerService indexerService;

    @Override
    public SBApiResponse profileUpdate(Map<String, Object> request, String userToken, String authToken)
            throws Exception {
        SBApiResponse response = new SBApiResponse(Constants.API_PROFILE_UPDATE);
        try {
            Map<String, Object> requestData = (Map<String, Object>) request.get(Constants.REQUEST);
            if (!validateRequest(requestData)) {
                response.setResponseCode(HttpStatus.BAD_REQUEST);
                response.getParams().setStatus(Constants.FAILED);
                return response;
            }

            String userId = (String) requestData.get(Constants.USER_ID);
            Map<String, Object> profileDetailsMap = (Map<String, Object>) requestData.get(Constants.PROFILE_DETAILS);
            List<String> approvalFieldList = approvalFields();
            String newDeptName = checkDepartment(profileDetailsMap);
            Map<String, Object> transitionData = new HashMap<>();
            for (String approvalList : approvalFieldList) {
                if (profileDetailsMap.containsKey(approvalList)) {
                    transitionData.put(approvalList, profileDetailsMap.get(approvalList));
                    profileDetailsMap.remove(approvalList);
                }
            }
            Map<String, Object> responseMap = userUtilityService.getUsersReadData(userId, StringUtils.EMPTY,
                    StringUtils.EMPTY);
            String deptName = (String) responseMap.get(Constants.CHANNEL);
            Map<String, Object> existingProfileDetails = (Map<String, Object>) responseMap
                    .get(Constants.PROFILE_DETAILS);
            StringBuilder url = new StringBuilder();
            HashMap<String, String> headerValues = new HashMap<>();
            headerValues.put(Constants.AUTH_TOKEN, authToken);
            headerValues.put(Constants.CONTENT_TYPE, Constants.APPLICATION_JSON);
            Map<String, Object> workflowResponse = new HashMap<>();
            Map<String, Object> updateResponse = new HashMap<>();
            if (!profileDetailsMap.isEmpty()) {
                List<String> listOfChangedDetails = new ArrayList<>();
                for (String keys : profileDetailsMap.keySet()) {
                    listOfChangedDetails.add(keys);
                }
                if (listOfChangedDetails.contains(Constants.EMPLOYMENTDETAILS)) {
                    listOfChangedDetails.remove(Constants.EMPLOYMENTDETAILS);
                }
                for (String changedObj : listOfChangedDetails) {
                    if (profileDetailsMap.get(changedObj) instanceof ArrayList) {
                        existingProfileDetails.put(changedObj, profileDetailsMap.get(changedObj));
                    } else if (profileDetailsMap.get(changedObj) instanceof Boolean) {
                        existingProfileDetails.put(changedObj, profileDetailsMap.get(changedObj));
                    } else {
                        if (existingProfileDetails.containsKey(changedObj)) {
                            Map<String, Object> existingProfileChild = (Map<String, Object>) existingProfileDetails
                                    .get(changedObj);
                            Map<String, Object> requestedProfileChild = (Map<String, Object>) profileDetailsMap
                                    .get(changedObj);
                            for (String childKey : requestedProfileChild.keySet()) {
                                existingProfileChild.put(childKey, requestedProfileChild.get(childKey));
                            }
                        } else {
                            existingProfileDetails.put(changedObj, profileDetailsMap.get(changedObj));
                        }
                    }

                    // Additional Condition for updating personal Details directly to user object
                    if (Constants.PERSONAL_DETAILS.equalsIgnoreCase(changedObj)) {
                        getModifiedPersonalDetails(profileDetailsMap.get(changedObj), requestData);
                    }
                }
                Map<String, Object> updateRequestValue = requestData;
                updateRequestValue.put(Constants.PROFILE_DETAILS, existingProfileDetails);
                Map<String, Object> updateRequest = new HashMap<>();
                updateRequest.put(Constants.REQUEST, updateRequestValue);

                url.append(serverConfig.getSbUrl()).append(serverConfig.getLmsUserUpdatePath());
                updateResponse = outboundRequestHandlerService.fetchResultUsingPatch(
                        serverConfig.getSbUrl() + serverConfig.getLmsUserUpdatePath(), updateRequest, headerValues);
                if (Constants.OK.equalsIgnoreCase((String) updateResponse.get(Constants.RESPONSE_CODE))) {
                    response.setResponseCode(HttpStatus.OK);
                    response.getResult().put(Constants.RESPONSE, Constants.SUCCESS);
                    response.getParams().setStatus(Constants.SUCCESS);
                } else {
                    if (updateResponse != null && Constants.CLIENT_ERROR
                            .equalsIgnoreCase((String) updateResponse.get(Constants.RESPONSE_CODE))) {
                        response.setResponseCode(HttpStatus.BAD_REQUEST);
                    } else {
                        response.setResponseCode(HttpStatus.INTERNAL_SERVER_ERROR);
                    }
                    response.getParams().setStatus(Constants.FAILED);
                    String errMsg = (String) ((Map<String, Object>) updateResponse.get(Constants.PARAMS))
                            .get(Constants.ERROR_MESSAGE);
                    errMsg = PropertiesCache.getInstance().readCustomError(errMsg);
                    response.getParams().setErrmsg(errMsg);
                    return response;
                }
            }
            List<String> transitionList = new ArrayList<>();
            for (String key : transitionData.keySet()) {
                transitionList.add(key);
            }
            if (!transitionList.isEmpty()) {
                List<Map<String, Object>> finalTransitionList = new ArrayList<>();
                for (String listTransition : transitionList) {

                    if (transitionData.get(listTransition) instanceof ArrayList) {
                        List<Map<String, Object>> transList = (List<Map<String, Object>>) transitionData
                                .get(listTransition);
                        for (int j = 0; j < transList.size(); j++) {
                            Map<String, Object> transData = new HashMap<>();
                            transData = transList.get(j);
                            Set<String> list = transData.keySet();
                            String[] innerData = list.toArray(new String[list.size()]);

                            for (int k = 0; k < innerData.length; k++) {
                                Map<String, Object> dataRay = new HashMap<>();
                                Map<String, Object> fromValue = new HashMap<>();
                                Map<String, Object> toValue = new HashMap<>();
                                toValue.put(innerData[k], transData.get(innerData[k]));
                                if (existingProfileDetails.get(listTransition) instanceof ArrayList) {
                                    List<Map<String, Object>> readList = (List<Map<String, Object>>) existingProfileDetails
                                            .get(listTransition);
                                    Map<String, Object> readListData = readList.get(j);
                                    fromValue.put(innerData[k], readListData.get(innerData[k]));
                                    dataRay.put(Constants.OSID, readListData.get(Constants.OSID));
                                }
                                dataRay.put(Constants.FROM_VALUE, fromValue);
                                dataRay.put(Constants.TO_VALUE, toValue);
                                dataRay.put(Constants.FIELD_KEY, listTransition);
                                finalTransitionList.add(dataRay);
                            }
                        }
                    } else {
                        Map<String, Object> transListObject = new HashMap<>();
                        transListObject = (Map<String, Object>) transitionData.get(listTransition);
                        Set<String> listObject = transListObject.keySet();
                        String[] innerObjectData = listObject.toArray(new String[listObject.size()]);
                        for (int k = 0; k < innerObjectData.length; k++) {
                            Map<String, Object> updatedTransitionData = new HashMap<>();
                            Map<String, Object> fromValue = new HashMap<>();
                            Map<String, Object> toValue = new HashMap<>();
                            toValue.put(innerObjectData[k], transListObject.get(innerObjectData[k]));
                            Map<String, Object> readList = (Map<String, Object>) existingProfileDetails
                                    .get(listTransition);
                            fromValue.put(innerObjectData[k], readList.get(innerObjectData[k]));
                            updatedTransitionData.put(Constants.FROM_VALUE, fromValue);
                            updatedTransitionData.put(Constants.TO_VALUE, toValue);
                            updatedTransitionData.put(Constants.FIELD_KEY, listTransition);
                            updatedTransitionData.put(Constants.OSID, readList.get(Constants.OSID));
                            finalTransitionList.add(updatedTransitionData);
                        }
                    }
                }

                Map<String, Object> transitionRequests = new HashMap<>();
                transitionRequests.put(Constants.STATE, Constants.INITIATE);
                transitionRequests.put(Constants.ACTION, Constants.INITIATE);
                transitionRequests.put(Constants.USER_ID, userId);
                transitionRequests.put(Constants.APPLICATION_ID, userId);
                transitionRequests.put(Constants.ACTOR_USER_ID, userId);
                transitionRequests.put(Constants.SERVICE_NAME, Constants.PROFILE);
                transitionRequests.put(Constants.COMMENT, "");
                transitionRequests.put(Constants.WFID, "");
                if (null != newDeptName) {
                    transitionRequests.put(Constants.DEPT_NAME, newDeptName);
                } else {
                    transitionRequests.put(Constants.DEPT_NAME, deptName);
                }
                transitionRequests.put(Constants.UPDATE_FIELD_VALUES, finalTransitionList);
                url = new StringBuilder();
                url.append(serverConfig.getWfServiceHost()).append(serverConfig.getWfServiceTransitionPath());
                headerValues.put(Constants.ROOT_ORG_CONSTANT, Constants.IGOT);
                headerValues.put(Constants.ORG_CONSTANT, Constants.DOPT);
                workflowResponse = outboundRequestHandlerService.fetchResultUsingPost(
                        serverConfig.getWfServiceHost() + serverConfig.getWfServiceTransitionPath(), transitionRequests,
                        headerValues);

                Map<String, Object> resultValue = (Map<String, Object>) workflowResponse.get(Constants.RESULT);
                if (Constants.OK.equalsIgnoreCase((String) resultValue.get(Constants.STATUS))) {
                    response.getResult().put(Constants.RESPONSE, Constants.SUCCESS);
                    response.getParams().setStatus(Constants.SUCCESS);
                    response.setResponseCode(HttpStatus.OK);
                } else {
                    response.setResponseCode(HttpStatus.INTERNAL_SERVER_ERROR);
                    response.getParams().setStatus(Constants.FAILED);
                    response.getParams().setErrmsg("Failed to raise workflow transition request.");
                }
            }
        } catch (Exception e) {
            log.error("Failed to process profile update. Exception: ", e);
            response.getParams().setStatus(Constants.FAILED);
            response.getParams().setErr(e.getMessage());
            response.setResponseCode(HttpStatus.INTERNAL_SERVER_ERROR);
        }
        return response;
    }

    @Override
    public SBApiResponse orgProfileUpdate(Map<String, Object> request) throws Exception {
        SBApiResponse response = new SBApiResponse(Constants.ORG_PROFILE_UPDATE);
        Map<String, Object> requestData = (Map<String, Object>) request.get(Constants.REQUEST);
        String errMsg = validateOrgProfilePayload(requestData);
        if (StringUtils.isBlank(errMsg)) {
            try {
                String orgId = (String) requestData.get(Constants.ORG_ID);
                Map<String, Object> esOrgProfileMap = getOrgProfileForOrgId(orgId);
                boolean isOrgProfileExist = true;
                if (ObjectUtils.isEmpty(esOrgProfileMap)) {
                    isOrgProfileExist = false;
                    esOrgProfileMap = new HashMap<>();
                }

                Map<String, Object> orgProfileDetailsMap = (Map<String, Object>) requestData
                        .get(Constants.PROFILE_DETAILS);
                for (String keys : orgProfileDetailsMap.keySet()) {
                    esOrgProfileMap.put(keys, orgProfileDetailsMap.get(keys));
                }
                RestStatus status = null;
                if (isOrgProfileExist) {
                    status = indexerService.updateEntity(serverConfig.getOrgOnboardingIndex(),
                            serverConfig.getEsProfileIndexType(), orgId, esOrgProfileMap);
                } else {
                    status = indexerService.addEntity(serverConfig.getOrgOnboardingIndex(),
                            serverConfig.getEsProfileIndexType(), orgId, esOrgProfileMap);
                }
                if (status.equals(RestStatus.CREATED) || status.equals(RestStatus.OK)) {
                    response.setResponseCode(HttpStatus.ACCEPTED);
                    Map<String, Object> resultMap = new HashMap<String, Object>();
                    resultMap.put(Constants.ORG_ID, orgId);
                    resultMap.put(Constants.PROFILE_DETAILS, esOrgProfileMap);
                    response.getResult().put(Constants.RESULT, resultMap);
                } else {
                    response.setResponseCode(HttpStatus.INTERNAL_SERVER_ERROR);
                    response.getParams().setErrmsg("Failed to add details to ES Service");
                }
            } catch (Exception e) {
                log.error("Failed to process orgProfileUpdate. Exception: ", e);
                errMsg = String.format("Failed to process org profile update request. Exception: %s", e.getMessage());
                log.warn(errMsg);
            }
        }
        if (StringUtils.isNotBlank(errMsg)) {
            response.getParams().setStatus(Constants.FAILED);
            response.getParams().setErrmsg(errMsg);
            response.setResponseCode(HttpStatus.BAD_REQUEST);
        }

        return response;
    }

    private String validateOrgProfilePayload(Map<String, Object> orgProfileInfo) {
        StringBuffer str = new StringBuffer();
        List<String> errList = new ArrayList<String>();
        if (StringUtils.isBlank((String) orgProfileInfo.get(Constants.ORG_ID))) {
            errList.add(Constants.ORG_ID);
        }
        if (ObjectUtils.isEmpty(orgProfileInfo.get(Constants.PROFILE_DETAILS))) {
            errList.add(Constants.PROFILE_DETAILS);
        }
        if (!errList.isEmpty()) {
            str.append("Failed to Register User Details. Missing Params - [").append(errList.toString()).append("]");
        }
        return str.toString();
    }

    public Map<String, Object> getOrgProfileForOrgId(String registrationCode) {
        try {
            Map<String, Object> esObject = indexerService.readEntity(serverConfig.getOrgOnboardingIndex(),
                    serverConfig.getEsProfileIndexType(), registrationCode);
            return esObject;
        } catch (Exception e) {
            log.error("Failed to get Org Profile. Exception: ", e);
            log.warn(String.format("Exception in %s : %s", "getUserRegistrationDetails", e.getMessage()));
        }
        return null;
    }


    @Override
    public SBApiResponse orgProfileRead(String orgId) throws Exception {
        return null;
    }

    @Override
    public SBApiResponse userBasicInfo(String userId) {
        return null;
    }

    @Override
    public SBApiResponse userBasicProfileUpdate(Map<String, Object> request) {
        return null;
    }

    @Override
    public SBApiResponse userAutoComplete(String searchTerm) {
        return null;
    }

    @Override
    public SBApiResponse migrateUser(Map<String, Object> request, String userToken, String authToken) {
        return null;
    }

    @Override
    public SBApiResponse userSignup(Map<String, Object> request) {
        return null;
    }

    @Override
    public SBApiResponse bulkUpload(MultipartFile mFile, String orgId, String orgName, String userId) {
        return null;
    }

    @Override
    public SBApiResponse getBulkUploadDetails(String orgId) {
        return null;
    }

    @Override
    public SBApiResponse getUserEnrollmentReport() {
        return null;
    }

    @Override
    public SBApiResponse getUserReport() {
        return null;
    }

    @Override
    public ResponseEntity<Resource> downloadFile(String fileName) {
        return null;
    }

    public boolean validateRequest(Map<String, Object> requestBody) {
        if (!(ObjectUtils.isEmpty(requestBody.get(Constants.USER_ID)))
                && !(ObjectUtils.isEmpty(requestBody.get(Constants.PROFILE_DETAILS)))) {
            return true;
        } else {
            return false;
        }
    }

    private void getModifiedPersonalDetails(Object personalDetailsObj, Map<String, Object> updatedRequest) {
        try {
            Map<String, Object> personalDetailsMap = (Map<String, Object>) personalDetailsObj;
            if (!ObjectUtils.isEmpty(personalDetailsMap)) {
                for (String paramName : personalDetailsMap.keySet()) {
                    if (Constants.FIRST_NAME_LOWER_CASE.equalsIgnoreCase(paramName)) {
                        updatedRequest.put(Constants.FIRSTNAME, (String) personalDetailsMap.get(paramName));
                    } else if (Constants.SURNAME.equalsIgnoreCase(paramName)) {
                        updatedRequest.put(Constants.LASTNAME, (String) personalDetailsMap.get(paramName));
                    } else if (Constants.MOBILE.equalsIgnoreCase(paramName)) {
                        updatedRequest.put(Constants.PHONE, String.valueOf(personalDetailsMap.get(paramName)));
                    }
                }
            }
        } catch (Exception e) {
            log.error("Exception while verifying profile details. ", e);
        }
    }

    public List<String> approvalFields() {
        Map<String, Object> approvalFieldsCache =
        (Map<String, Object>) mapper
                .convertValue(redisCacheMgr.getCache(Constants.PROFILE_UPDATE_FIELDS), Map.class);

        if (!ObjectUtils.isEmpty(approvalFieldsCache)) {
            Map<String, Object> approvalResult = (Map<String, Object>) approvalFieldsCache.get(Constants.RESULT);
            Map<String, Object> approvalResponse = (Map<String, Object>) approvalResult.get(Constants.RESPONSE);
            String value = (String) approvalResponse.get(Constants.VALUE);
            List<String> approvalValues = new ArrayList<>();
            approvalValues.add(value);
            return approvalValues;
        } else {
            Map<String, String> header = new HashMap<>();
            Map<String, Object> approvalData = (Map<String, Object>) outboundRequestHandlerService
                    .fetchUsingGetWithHeadersProfile(serverConfig.getSbUrl() + serverConfig.getLmsSystemSettingsPath(),
                            header);
            Map<String, Object> approvalResult = (Map<String, Object>) approvalData.get(Constants.RESULT);
            Map<String, Object> approvalResponse = (Map<String, Object>) approvalResult.get(Constants.RESPONSE);
            String value = (String) approvalResponse.get(Constants.VALUE);
            String strArray[] = value.split(" ");
            List<String> approvalValues = Arrays.asList(strArray);
            return approvalValues;
        }
    }



    public String checkDepartment(Map<String, Object> requestProfile) throws Exception {
        String requestDeptName = null;
        if (requestProfile.containsKey(Constants.PROFESSIONAL_DETAILS)) {
            List<Map<String, Object>> profDetails = (List<Map<String, Object>>) requestProfile
                    .get(Constants.PROFESSIONAL_DETAILS);
            if (profDetails.get(0).containsKey(Constants.NAME)) {
                requestDeptName = (String) profDetails.get(0).get(Constants.NAME);
            }
        }
        return requestDeptName;
    }



}
