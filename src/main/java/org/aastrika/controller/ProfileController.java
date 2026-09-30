package org.aastrika.controller;

import java.util.Map;
import java.util.UUID;

import jakarta.validation.Valid;
import org.aastrika.common.Constants;
import org.aastrika.dto.request.UserMigrateRequest;
import org.aastrika.dto.response.AppResponse;
import org.aastrika.service.ProfileService;
import org.aastrika.service.UserBulkUploadService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * Profile endpoints, keeping the source repo's controller name. Currently only the admin org
 * migration is migrated; the source class also held profile/org read-update, signup, autocomplete
 * and bulk-upload handlers that have not been brought over.
 *
 * <p>The source declared its migrate handler {@code private} (Spring invokes it reflectively) — it
 * is public here.
 */
@RestController
public class ProfileController {

    private final ProfileService profileService;
    private final UserBulkUploadService userBulkUploadService;

    public ProfileController(ProfileService profileService, UserBulkUploadService userBulkUploadService) {
        this.profileService = profileService;
        this.userBulkUploadService = userBulkUploadService;
    }

    @GetMapping("/user/v1/autocomplete/{searchTerm}")
    public ResponseEntity<?> userAutoComplete(@PathVariable("searchTerm") String searchTerm) {
        return ResponseEntity.ok(profileService.userAutoComplete(searchTerm));
    }

    /** Migrates a user to another organisation and refreshes their profile, role and search index. */
    @PatchMapping("/user/v1/migrate")
    public ResponseEntity<AppResponse<Map<String, Object>>> migrateUser(
            @RequestHeader(Constants.X_AUTH_TOKEN) String userToken,
            @RequestHeader(Constants.AUTH_TOKEN) String authToken,
            @Valid @RequestBody UserMigrateRequest request) {
        return ResponseEntity.ok(profileService.migrateUser(request, userToken, authToken));
    }

    /**
     * Accepts a user bulk-upload spreadsheet and returns immediately with a job id. Rows are
     * persisted as PENDING; user creation happens afterwards. Unlike the source, the file is parsed
     * in memory and not retained — per-row state in Cassandra is the record of the upload.
     */
    @PostMapping("/user/v1/bulkupload")
    public ResponseEntity<AppResponse<Map<String, Object>>> bulkUpload(
            @RequestParam("file") MultipartFile file,
            @RequestHeader(Constants.X_AUTH_USER_ORG_ID) String rootOrgId,
            @RequestHeader(Constants.X_AUTH_USER_ORG_NAME) String orgName,
            @RequestHeader(Constants.X_AUTH_USER_ID) String userId) {
        return ResponseEntity.ok(userBulkUploadService.upload(file, rootOrgId, orgName, userId));
    }

    /** An org's bulk-upload history, newest first, with counts derived from the stored rows. */
    @GetMapping("/user/v1/bulkupload/{orgId}")
    public ResponseEntity<AppResponse<Map<String, Object>>> bulkUploadHistory(
            @PathVariable("orgId") String orgId) {
        return ResponseEntity.ok(userBulkUploadService.listJobs(orgId));
    }

    /**
     * One upload with its rows. Pass {@code ?status=FAILED} (or INVALID/PENDING/SUCCESS) to see only
     * those rows — this is what replaces the source's re-uploaded annotated spreadsheet.
     */
    @GetMapping("/user/v1/bulkupload/{orgId}/{jobId}")
    public ResponseEntity<AppResponse<Map<String, Object>>> bulkUploadDetail(
            @PathVariable("orgId") String orgId,
            @PathVariable("jobId") UUID jobId,
            @RequestParam(value = "status", required = false) String status) {
        return ResponseEntity.ok(userBulkUploadService.getJob(orgId, jobId, status));
    }

    /** Re-queues a job's FAILED rows. INVALID rows are left alone — those need a corrected sheet. */
    @PostMapping("/user/v1/bulkupload/{orgId}/{jobId}/retry")
    public ResponseEntity<AppResponse<Map<String, Object>>> bulkUploadRetry(
            @PathVariable("orgId") String orgId,
            @PathVariable("jobId") UUID jobId,
            @RequestHeader(Constants.X_AUTH_USER_ID) String userId) {
        return ResponseEntity.ok(userBulkUploadService.retry(orgId, jobId, userId));
    }
}
