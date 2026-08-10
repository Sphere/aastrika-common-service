package org.aastrika.service;

import java.util.Map;

import org.aastrika.dto.response.SBApiResponse;
import org.springframework.web.multipart.MultipartFile;

public interface ProfileService {

    SBApiResponse userAutoComplete(String searchTerm);

    SBApiResponse migrateUser(Map<String, Object> request, String userToken, String authToken);

    SBApiResponse bulkUpload(MultipartFile mFile, String orgId, String orgName, String userId);

    SBApiResponse getBulkUploadDetails(String orgId);
}

