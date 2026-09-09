package org.aastrika.service;

import java.util.Map;
import java.util.UUID;

import org.aastrika.dto.response.AppResponse;
import org.springframework.web.multipart.MultipartFile;

public interface UserBulkUploadService {

    /**
     * Accepts a user bulk-upload spreadsheet: parses it, runs the CPU-only validations, persists a
     * job row plus one row per spreadsheet line, and returns immediately. User creation happens
     * afterwards, driven off the persisted {@code PENDING} rows.
     *
     * @throws org.aastrika.exception.ApiException 400 if the file is not a readable {@code .xlsx},
     *         the header is wrong, there are no data rows, or the row cap is exceeded
     */
    AppResponse<Map<String, Object>> upload(MultipartFile file, String rootOrgId, String orgName, String userId);

    /** An org's upload history, newest first, each with counts derived from its rows. */
    AppResponse<Map<String, Object>> listJobs(String rootOrgId);

    /**
     * One job with its rows. {@code status} optionally filters the rows (e.g. only FAILED).
     *
     * @throws org.aastrika.exception.ApiException 404 if the job does not exist for that org
     */
    AppResponse<Map<String, Object>> getJob(String rootOrgId, UUID jobId, String status);

    /**
     * Returns FAILED rows to PENDING and re-queues the job. INVALID rows are left alone — their data
     * is wrong, so they need a corrected upload rather than a retry.
     *
     * @throws org.aastrika.exception.ApiException 404 if the job does not exist for that org
     */
    AppResponse<Map<String, Object>> retry(String rootOrgId, UUID jobId, String userId);
}
