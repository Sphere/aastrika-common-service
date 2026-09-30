package org.aastrika.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Maps the {@code user_program_completion} table — one row per (rootOrg, program, user)
 * recording completion state and the generated certificate URL.
 */
@Entity
@Table(name = "user_program_completion")
@IdClass(UserProgramCompletionId.class)
@Getter
@Setter
@NoArgsConstructor
public class UserProgramCompletion {

    @Id
    @Column(name = "root_org_id")
    private String rootOrgId;

    @Id
    @Column(name = "program_id")
    private String programId;

    @Id
    @Column(name = "user_id")
    private String userId;

    @Column(name = "is_completed")
    private Boolean completed;

    @Column(name = "is_certificate_generated")
    private Boolean certificateGenerated;

    @Column(name = "certificate_url")
    private String certificateUrl;
}
