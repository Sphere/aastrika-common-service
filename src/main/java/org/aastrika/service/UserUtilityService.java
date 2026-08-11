package org.aastrika.service;


import java.util.List;
import java.util.Map;

public interface UserUtilityService {

    public boolean assignRole(String sbOrgId, String userId, String objectDetails);

    Map<String, Object> getUsersDataFromUserIds(List<String> userIds, List<String> fields, String authToken);

}