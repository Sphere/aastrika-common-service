package org.aastrika.service.impl;

import java.time.OffsetDateTime;
import java.util.concurrent.Executor;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.aastrika.client.UserProvisioningClient;
import org.aastrika.entity.UserBulkUploadJob;
import org.aastrika.entity.UserBulkUploadRecord;
import org.aastrika.repository.UserBulkUploadJobRepository;
import org.aastrika.repository.UserBulkUploadRecordRepository;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionalEventListener;

import lombok.extern.slf4j.Slf4j;

/**
 * Turns {@code PENDING} rows into {@code SUCCESS}/{@code FAILED} by provisioning each user, then
 * settles the job's terminal status.
 *
 * <p>Correctness rests on <b>row-level idempotency</b>, not on locking: every row is checked for an
 * existing user before creation, and its outcome is written to its own row. Two instances processing
 * the same job therefore duplicate some existence checks but cannot create duplicate users — which is
 * why there are no lease columns, no LWTs and no staleness timeout to tune.
 *
 * <p>Resume needs no cursor: {@code PENDING} rows <i>are</i> the remaining work.
 */
@Service
@Slf4j
public class UserBulkUploadProcessor {

    private final UserProvisioningClient provisioningClient;
    private final UserBulkUploadJobRepository jobRepository;
    private final UserBulkUploadRecordRepository recordRepository;
    private final Executor executor;

    public UserBulkUploadProcessor(
            UserProvisioningClient provisioningClient,
            UserBulkUploadJobRepository jobRepository,
            UserBulkUploadRecordRepository recordRepository,
            @Qualifier("bulkUploadExecutor") Executor executor) {
        this.provisioningClient = provisioningClient;
        this.jobRepository = jobRepository;
        this.recordRepository = recordRepository;
        this.executor = executor;
    }

    /**
     * Starts processing once the intake transaction has committed. Firing before commit would let
     * the worker query rows that are not yet visible.
     */
    @TransactionalEventListener
    public void onAccepted(BulkUploadAcceptedEvent event) {
        executor.execute(() -> process(event.jobId()));
    }

    /**
     * Picks up jobs left unfinished by a previous run. Safe to run repeatedly: it only ever acts on
     * PENDING rows.
     */
    @EventListener(ApplicationReadyEvent.class)
    public void resumeUnfinished() {
        List<UserBulkUploadJob> unfinished = jobRepository.findByStatusIn(
                List.of(UserBulkUploadJob.INITIATED, UserBulkUploadJob.IN_PROGRESS, UserBulkUploadJob.INCOMPLETE));
        if (unfinished.isEmpty()) {
            return;
        }
        log.info("Resuming {} unfinished bulk-upload job(s)", unfinished.size());
        // Submitted, never run inline: ApplicationReadyEvent is on the startup thread, and a job of
        // 2000 rows is thousands of learner-service calls.
        unfinished.forEach(job -> executor.execute(() -> process(job.getJobId())));
    }

    /** Provisions every PENDING row of the job, then settles the job status. */
    public void process(UUID jobId) {
        Optional<UserBulkUploadJob> maybeJob = jobRepository.findById(jobId);
        if (maybeJob.isEmpty()) {
            log.warn("Bulk-upload job {} no longer exists; nothing to process", jobId);
            return;
        }
        UserBulkUploadJob job = maybeJob.get();
        markInProgress(job);

        List<UserBulkUploadRecord> pending =
                recordRepository.findByJobIdAndStatusOrderByRowNumberAsc(jobId, UserBulkUploadRecord.PENDING);
        log.info("Processing bulk-upload job {}: {} pending row(s)", jobId, pending.size());

        for (UserBulkUploadRecord record : pending) {
            try {
                provisionRow(record, job.getOrgName());
            } catch (Exception e) {
                // One row must never take the job down — the source let an exception out of the row
                // loop and failed the whole upload with zeroed counts.
                log.error("Bulk-upload job {} row {} failed unexpectedly: {}", jobId, record.getRowNumber(),
                        e.getMessage());
                complete(record, UserBulkUploadRecord.FAILED, List.of("Unexpected error while creating the user"));
            }
        }
        settle(jobId);
    }

    private void provisionRow(UserBulkUploadRecord record, String orgName) {
        if (provisioningClient.exists("email", record.getEmail())) {
            complete(record, UserBulkUploadRecord.FAILED, List.of("A user with this email already exists"));
            return;
        }
        if (provisioningClient.exists("phone", record.getPhone())) {
            complete(record, UserBulkUploadRecord.FAILED, List.of("A user with this phone number already exists"));
            return;
        }
        String error = provisioningClient.provision(record.getFirstName(), record.getLastName(), record.getEmail(),
                record.getPhone(), orgName);
        if (error == null) {
            complete(record, UserBulkUploadRecord.SUCCESS, null);
        } else {
            complete(record, UserBulkUploadRecord.FAILED, List.of(error));
        }
    }

    private void complete(UserBulkUploadRecord record, String status, List<String> errors) {
        record.setStatus(status);
        record.setErrors(errors);
        record.setUpdatedAt(OffsetDateTime.now());
        record.setUpdatedBy("system");
        recordRepository.save(record);
    }

    private void markInProgress(UserBulkUploadJob job) {
        job.setStatus(UserBulkUploadJob.IN_PROGRESS);
        job.setUpdatedAt(OffsetDateTime.now());
        job.setUpdatedBy("system");
        jobRepository.save(job);
    }

    /**
     * Derives the terminal status from the rows themselves — no maintained counters to drift.
     *
     * <p>Note each save below commits on its own (Spring Data repositories are transactional per
     * call). That is deliberate: wrapping a whole job in one transaction would roll back every
     * provisioned row if the pod died late in the file, throwing away real work that already
     * happened in the learner service and cannot be undone.
     * PENDING rows remaining means the run stopped early, which is {@code INCOMPLETE} (resumable) and
     * deliberately distinct from {@code COMPLETED_WITH_ERRORS} (finished, but the data was bad).
     */
    private void settle(UUID jobId) {
        long pending = 0;
        long failedOrInvalid = 0;
        for (Object[] row : recordRepository.countByStatus(jobId)) {
            String status = (String) row[0];
            long count = ((Number) row[1]).longValue();
            if (UserBulkUploadRecord.PENDING.equals(status)) {
                pending += count;
            } else if (UserBulkUploadRecord.FAILED.equals(status) || UserBulkUploadRecord.INVALID.equals(status)) {
                failedOrInvalid += count;
            }
        }
        String status = pending > 0
                ? UserBulkUploadJob.INCOMPLETE
                : (failedOrInvalid == 0 ? UserBulkUploadJob.COMPLETED : UserBulkUploadJob.COMPLETED_WITH_ERRORS);

        jobRepository.findById(jobId).ifPresent(job -> {
            job.setStatus(status);
            job.setUpdatedAt(OffsetDateTime.now());
            job.setUpdatedBy("system");
            jobRepository.save(job);
        });
        log.info("Bulk-upload job {} settled as {}", jobId, status);
    }

    /** Published by the intake service; consumed only after its transaction commits. */
    public record BulkUploadAcceptedEvent(UUID jobId) {
    }
}
