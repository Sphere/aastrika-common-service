package org.aastrika.service;


import org.aastrika.dto.UserRegistration;

import java.util.List;
import java.util.Map;

public interface UserUtilityService {

    boolean assignRole(String sbOrgId, String userId, String objectDetails);

    boolean isUserExist(String key, String value) throws Exception;

    Boolean isDomainAccepted(String email);

    boolean createUser(UserRegistration userRegistration);

}