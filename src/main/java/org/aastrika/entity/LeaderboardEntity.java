package org.aastrika.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Maps {@code public.leaderboard_table} (PostgreSQL). Ported from the source repo's entity, updated
 * from {@code javax.persistence} to {@code jakarta.persistence} for Spring Boot 3, with the
 * {@code etl_updated_date} column added. {@link #rank} is computed at read time and not persisted.
 */
@Entity
@Table(name = "leaderboard_table", schema = "public")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LeaderboardEntity {

    @Id
    @Column(name = "userid", nullable = false)
    private String userId;

    @Column(name = "firstname")
    private String firstname;

    @Column(name = "lastname")
    private String lastname;

    @Column(name = "points")
    private Long points;

    @Column(name = "date")
    private LocalDateTime date;

    @Column(name = "professional_institute_name")
    private String professionalInstituteName;

    @Column(name = "rootorgid")
    private String rootOrgId;

    @Column(name = "district")
    private String district;

    @Column(name = "state")
    private String state;

    @Column(name = "profession")
    private String profession;

    @Column(name = "background")
    private String background;

    @Column(name = "etl_updated_date")
    private LocalDateTime etlUpdatedDate;

    @Transient
    private Integer rank;
}
