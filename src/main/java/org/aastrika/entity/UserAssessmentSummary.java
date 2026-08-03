package org.aastrika.entity;

import java.time.Instant;

import org.springframework.data.cassandra.core.mapping.Column;
import org.springframework.data.cassandra.core.mapping.PrimaryKey;
import org.springframework.data.cassandra.core.mapping.Table;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Maps {@code sunbird.user_assessment_summary} — per (user, content) roll-up tracking the first
 * max score and the first passing score for course assessments.
 */
@Table("user_assessment_summary")
@Getter
@Setter
@NoArgsConstructor
public class UserAssessmentSummary {

    @PrimaryKey
    private UserAssessmentSummaryKey key;

    @Column("max_score")
    private Float firstMaxScore;

    @Column("max_score_date")
    private Instant firstMaxScoreDate;

    @Column("first_passed_score")
    private Float firstPassedScore;

    @Column("first_passed_score_date")
    private Instant firstPassedScoreDate;
}
