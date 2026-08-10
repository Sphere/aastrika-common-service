package org.aastrika.service;

import java.util.Map;

import org.aastrika.dto.request.SunbirdApiRequest;
import org.aastrika.dto.response.MandatoryContentResponse;

public interface MandatoryContentService {

    MandatoryContentResponse getMandatoryContentStatusForUser(String authUserToken, String rootOrg, String org,
                                                              String userId);
}

