package org.aastrika.entity;

import java.time.OffsetDateTime;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Maps {@code public.user_bulk_upload_job} — one row per uploaded spreadsheet, holding the state of
 * the upload as a whole. Lives on the primary datasource.
 *
 * <p>{@code successfulrecordscount} / {@code failedrecordscount} are deliberately absent: they are
 * derived from the record rows on read, so there are no counters to drift if a worker dies mid-update.
 * {@code totalrecords} / {@code validrecords} / {@code invalidrecords} are set once at intake and
 * never change.
 */
@Entity
@Table(name = "user_bulk_upload_job")
@Getter
@Setter
@NoArgsConstructor
public class UserBulkUploadJob {

    /** Job lifecycle. There is no job-level FAILED — an unreadable sheet is rejected with 400 and no row is written. */
    public static final String INITIATED = "INITIATED";
    public static final String IN_PROGRESS = "IN_PROGRESS";
    public static final String COMPLETED = "COMPLETED";
    public static final String COMPLETED_WITH_ERRORS = "COMPLETED_WITH_ERRORS";
    public static final String INCOMPLETE = "INCOMPLETE";

    @Id
    @Column(name = "jobid", nullable = false)
    private UUID jobId;

    @Column(name = "rootorgid", nullable = false)
    private String rootOrgId;

    @Column(name = "orgname")
    private String orgName;

    @Column(name = "filename")
    private String fileName;

    @Column(name = "status", nullable = false)
    private String status;

    @Column(name = "totalrecords", nullable = false)
    private int totalRecords;

    @Column(name = "validrecords", nullable = false)
    private int validRecords;

    @Column(name = "invalidrecords", nullable = false)
    private int invalidRecords;

    @Column(name = "createdat", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "createdby")
    private String createdBy;

    @Column(name = "updatedat", nullable = false)
    private OffsetDateTime updatedAt;

    @Column(name = "updatedby")
    private String updatedBy;
}
