package org.aastrika.service.impl;

import java.io.IOException;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.aastrika.common.Constants;
import org.aastrika.dto.response.AppResponse;
import org.aastrika.entity.UserBulkUploadJob;
import org.aastrika.entity.UserBulkUploadRecord;
import org.aastrika.exception.ApiException;
import org.aastrika.repository.UserBulkUploadJobRepository;
import org.aastrika.repository.UserBulkUploadRecordRepository;
import org.aastrika.service.UserBulkUploadService;
import org.aastrika.service.impl.UserBulkUploadProcessor.BulkUploadAcceptedEvent;
import org.aastrika.util.UserRecordValidator;
import org.aastrika.util.UserSheetParser;
import org.aastrika.util.UserSheetParser.ParsedRow;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import lombok.extern.slf4j.Slf4j;

/**
 * Intake half of the user bulk upload. Replaces the source's object-storage + Kafka pipeline: the
 * spreadsheet is parsed in memory and discarded, and per-row state is persisted instead, which is
 * what makes the work resumable. Nothing here calls the learner service.
 *
 * <p>The job row and all record rows are written in one transaction, so a partially-recorded upload
 * cannot exist — the reason this moved from Cassandra to PostgreSQL.
 */
@Service
@Slf4j
public class UserBulkUploadServiceImpl implements UserBulkUploadService {

    private static final String API_ID = "api.user.bulk.upload";

    private final UserSheetParser parser;
    private final UserRecordValidator validator;
    private final UserBulkUploadJobRepository jobRepository;
    private final UserBulkUploadRecordRepository recordRepository;
    private final ApplicationEventPublisher eventPublisher;

    public UserBulkUploadServiceImpl(
            UserSheetParser parser,
            UserRecordValidator validator,
            UserBulkUploadJobRepository jobRepository,
            UserBulkUploadRecordRepository recordRepository,
            ApplicationEventPublisher eventPublisher) {
        this.parser = parser;
        this.validator = validator;
        this.jobRepository = jobRepository;
        this.recordRepository = recordRepository;
        this.eventPublisher = eventPublisher;
    }

    @Override
    @Transactional
    public AppResponse<Map<String, Object>> upload(MultipartFile file, String rootOrgId, String orgName,
                                                    String userId) {
        byte[] bytes = readBytes(file);

        // Both of these throw 400 before anything is persisted, so a bad sheet leaves no job behind.
        List<ParsedRow> rows = parser.parse(bytes);
        Map<Integer, List<String>> errorsByRow = validator.validate(rows);

        UUID jobId = UUID.randomUUID();
        OffsetDateTime now = OffsetDateTime.now();
        int invalid = errorsByRow.size();
        int valid = rows.size() - invalid;

        jobRepository.save(newJob(jobId, rootOrgId, orgName, file.getOriginalFilename(), userId, now,
                rows.size(), valid, invalid));
        recordRepository.saveAll(newRecords(jobId, rootOrgId, userId, now, rows, errorsByRow));

        // Handed to the worker only after this transaction commits, so it cannot query rows that
        // are not yet visible.
        eventPublisher.publishEvent(new BulkUploadAcceptedEvent(jobId));

        // No user identifiers in the log line (SECURITY.md 2.5) — the source printed the whole
        // registration object, email and phone included, to stdout.
        log.info("Bulk upload accepted: job {} for org {} — {} rows ({} valid, {} invalid)",
                jobId, rootOrgId, rows.size(), valid, invalid);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("jobId", jobId.toString());
        result.put("fileName", file.getOriginalFilename());
        result.put("status", UserBulkUploadJob.INITIATED);
        result.put("totalRecords", rows.size());
        result.put("validRecords", valid);
        result.put("invalidRecords", invalid);
        // Returned up front: these rows will never be attempted, so there is nothing to poll for.
        result.put("invalidRows", invalidRowDetail(errorsByRow));
        return AppResponse.success(API_ID, result, HttpStatus.OK);
    }

    private byte[] readBytes(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ApiException(API_ID, HttpStatus.BAD_REQUEST, "The uploaded file is empty");
        }
        try {
            return file.getBytes();
        } catch (IOException e) {
            throw new ApiException(API_ID, HttpStatus.BAD_REQUEST, "The uploaded file could not be read");
        }
    }

    private static UserBulkUploadJob newJob(UUID jobId, String rootOrgId, String orgName, String fileName,
                                            String userId, OffsetDateTime now, int total, int valid, int invalid) {
        UserBulkUploadJob job = new UserBulkUploadJob();
        job.setJobId(jobId);
        job.setRootOrgId(rootOrgId);
        job.setOrgName(orgName);
        job.setFileName(fileName);
        job.setStatus(UserBulkUploadJob.INITIATED);
        job.setTotalRecords(total);
        job.setValidRecords(valid);
        job.setInvalidRecords(invalid);
        job.setCreatedAt(now);
        job.setCreatedBy(userId);
        job.setUpdatedAt(now);
        job.setUpdatedBy(userId);
        return job;
    }

    private static List<UserBulkUploadRecord> newRecords(UUID jobId, String rootOrgId, String userId,
                                                          OffsetDateTime now, List<ParsedRow> rows,
                                                          Map<Integer, List<String>> errorsByRow) {
        List<UserBulkUploadRecord> out = new ArrayList<>(rows.size());
        for (ParsedRow row : rows) {
            List<String> errors = errorsByRow.get(row.rowNumber());
            UserBulkUploadRecord record = new UserBulkUploadRecord();
            record.setJobId(jobId);
            record.setRootOrgId(rootOrgId);
            record.setRowNumber(row.rowNumber());
            record.setFirstName(row.firstName());
            record.setLastName(row.lastName());
            record.setEmail(row.email());
            record.setPhone(row.phone());
            record.setStatus(errors == null ? UserBulkUploadRecord.PENDING : UserBulkUploadRecord.INVALID);
            record.setErrors(errors);
            record.setCreatedAt(now);
            record.setCreatedBy(userId);
            record.setUpdatedAt(now);
            record.setUpdatedBy(userId);
            out.add(record);
        }
        return out;
    }

    private static List<Map<String, Object>> invalidRowDetail(Map<Integer, List<String>> errorsByRow) {
        List<Map<String, Object>> out = new ArrayList<>();
        errorsByRow.forEach((rowNumber, errors) -> {
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("rowNumber", rowNumber);
            entry.put("errors", errors);
            out.add(entry);
        });
        return out;
    }

    @Override
    @Transactional(readOnly = true)
    public AppResponse<Map<String, Object>> listJobs(String rootOrgId) {
        List<UserBulkUploadJob> jobs = jobRepository.findByRootOrgIdOrderByCreatedAtDesc(rootOrgId);
        Map<UUID, Map<String, Long>> countsByJob = countsFor(jobs.stream().map(UserBulkUploadJob::getJobId).toList());

        List<Map<String, Object>> content = jobs.stream()
                .map(job -> jobSummary(job, countsByJob.getOrDefault(job.getJobId(), Map.of())))
                .collect(Collectors.toList());

        Map<String, Object> result = new LinkedHashMap<>();
        result.put(Constants.COUNT, content.size());
        result.put(Constants.CONTENT, content);
        return AppResponse.success(API_ID, result, HttpStatus.OK);
    }

    @Override
    @Transactional(readOnly = true)
    public AppResponse<Map<String, Object>> getJob(String rootOrgId, UUID jobId, String status) {
        UserBulkUploadJob job = requireJob(rootOrgId, jobId);
        List<UserBulkUploadRecord> records = (status == null || status.isBlank())
                ? recordRepository.findByJobIdOrderByRowNumberAsc(jobId)
                : recordRepository.findByJobIdAndStatusOrderByRowNumberAsc(jobId, status.toUpperCase());

        Map<String, Object> result = new LinkedHashMap<>(jobSummary(job, countsFor(List.of(jobId))
                .getOrDefault(jobId, Map.of())));
        result.put("records", records.stream().map(UserBulkUploadServiceImpl::recordDetail).toList());
        return AppResponse.success(API_ID, result, HttpStatus.OK);
    }

    @Override
    @Transactional
    public AppResponse<Map<String, Object>> retry(String rootOrgId, UUID jobId, String userId) {
        UserBulkUploadJob job = requireJob(rootOrgId, jobId);
        List<UserBulkUploadRecord> failed =
                recordRepository.findByJobIdAndStatusOrderByRowNumberAsc(jobId, UserBulkUploadRecord.FAILED);

        OffsetDateTime now = OffsetDateTime.now();
        failed.forEach(record -> {
            record.setStatus(UserBulkUploadRecord.PENDING);
            record.setErrors(null);
            record.setUpdatedAt(now);
            record.setUpdatedBy(userId);
        });
        recordRepository.saveAll(failed);

        job.setStatus(UserBulkUploadJob.INITIATED);
        job.setUpdatedAt(now);
        job.setUpdatedBy(userId);
        jobRepository.save(job);

        eventPublisher.publishEvent(new BulkUploadAcceptedEvent(jobId));

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("jobId", jobId.toString());
        result.put("status", UserBulkUploadJob.INITIATED);
        result.put("requeuedRecords", failed.size());
        return AppResponse.success(API_ID, result, HttpStatus.OK);
    }

    private UserBulkUploadJob requireJob(String rootOrgId, UUID jobId) {
        return jobRepository.findById(jobId)
                .filter(job -> job.getRootOrgId().equals(rootOrgId))
                .orElseThrow(() -> new ApiException(API_ID, HttpStatus.NOT_FOUND,
                        "No bulk upload found with that id for this organisation"));
    }

    /** jobId -> (status -> count), from a single grouped query. */
    private Map<UUID, Map<String, Long>> countsFor(List<UUID> jobIds) {
        if (jobIds.isEmpty()) {
            return Map.of();
        }
        Map<UUID, Map<String, Long>> out = new HashMap<>();
        for (Object[] row : recordRepository.countByJobIdAndStatus(jobIds)) {
            out.computeIfAbsent((UUID) row[0], k -> new LinkedHashMap<>())
                    .put((String) row[1], ((Number) row[2]).longValue());
        }
        return out;
    }

    private static Map<String, Object> jobSummary(UserBulkUploadJob job, Map<String, Long> counts) {
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("jobId", job.getJobId().toString());
        out.put("fileName", job.getFileName());
        out.put("status", job.getStatus());
        out.put("totalRecords", job.getTotalRecords());
        out.put("validRecords", job.getValidRecords());
        out.put("invalidRecords", job.getInvalidRecords());
        // Derived from the rows, so these cannot drift from reality.
        out.put("pendingRecords", counts.getOrDefault(UserBulkUploadRecord.PENDING, 0L));
        out.put("successfulRecords", counts.getOrDefault(UserBulkUploadRecord.SUCCESS, 0L));
        out.put("failedRecords", counts.getOrDefault(UserBulkUploadRecord.FAILED, 0L));
        out.put("createdAt", job.getCreatedAt());
        out.put("createdBy", job.getCreatedBy());
        out.put("updatedAt", job.getUpdatedAt());
        return out;
    }

    private static Map<String, Object> recordDetail(UserBulkUploadRecord record) {
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("rowNumber", record.getRowNumber());
        out.put("firstName", record.getFirstName());
        out.put("lastName", record.getLastName());
        out.put("email", record.getEmail());
        out.put("phone", record.getPhone());
        out.put("status", record.getStatus());
        out.put("errors", record.getErrors());
        out.put("updatedAt", record.getUpdatedAt());
        return out;
    }
}
