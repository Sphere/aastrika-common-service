package org.aastrika.entity;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Maps the {@code user_competency_certificate} table — one row per (user, competency) recording
 * certificate generation state. Schema: {@code db/migrations/user_competency_certificate.sql}.
 */
@Entity
@Table(name = "user_competency_certificate")
@IdClass(UserCompetencyCertificateId.class)
@Getter
@Setter
@NoArgsConstructor
public class UserCompetencyCertificate {

    public static final String PENDING = "PENDING";
    public static final String GENERATED = "GENERATED";
    public static final String FAILED = "FAILED";

    @Id
    @Column(name = "user_id")
    private String userId;

    @Id
    @Column(name = "competency_id")
    private String competencyId;

    @Column(name = "root_org_id")
    private String rootOrgId;

    @Column(name = "template_id")
    private String templateId;

    /** Minted once when the row is created and reused on every retry. */
    @Column(name = "event_id")
    private UUID eventId;

    /** {@code PENDING}, {@code GENERATED} or {@code FAILED}. */
    @Column(name = "status")
    private String status;

    @Column(name = "certificate_url")
    private String certificateUrl;

    /** Failure reason as {@code CODE: text} when {@code FAILED}; null otherwise. Safe to return to API callers. */
    @Column(name = "message")
    private String message;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(name = "updated_at")
    private Instant updatedAt;
}