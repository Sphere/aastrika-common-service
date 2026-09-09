package org.aastrika.entity;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Maps {@code public.user_bulk_upload_record} — one row per spreadsheet line. This table is the
 * control state for the upload: {@code PENDING} rows are exactly the work remaining, which is what
 * lets a restarted pod resume without a cursor.
 *
 * <p>{@code errors} is a PostgreSQL {@code text[]}, mapped with {@link SqlTypes#ARRAY}.
 */
@Entity
@Table(name = "user_bulk_upload_record")
@Getter
@Setter
@NoArgsConstructor
public class UserBulkUploadRecord {

    /** Per-row outcome. INVALID rows failed a CPU-only check and are never attempted. */
    public static final String PENDING = "PENDING";
    public static final String INVALID = "INVALID";
    public static final String SUCCESS = "SUCCESS";
    public static final String FAILED = "FAILED";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "jobid", nullable = false)
    private UUID jobId;

    @Column(name = "rootorgid", nullable = false)
    private String rootOrgId;

    /** The 1-based sheet row number the admin sees in Excel, so errors point at the right line. */
    @Column(name = "rownumber", nullable = false)
    private int rowNumber;

    @Column(name = "firstname")
    private String firstName;

    @Column(name = "lastname")
    private String lastName;

    @Column(name = "email")
    private String email;

    @Column(name = "phone")
    private String phone;

    @Column(name = "status", nullable = false)
    private String status;

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "errors")
    private List<String> errors;

    @Column(name = "createdat", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "createdby")
    private String createdBy;

    @Column(name = "updatedat", nullable = false)
    private OffsetDateTime updatedAt;

    @Column(name = "updatedby")
    private String updatedBy;
}
