package org.aastrika.entity;

import java.math.BigDecimal;
import java.time.Instant;

import org.springframework.data.cassandra.core.mapping.Column;
import org.springframework.data.cassandra.core.mapping.PrimaryKey;
import org.springframework.data.cassandra.core.mapping.Table;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Maps {@code sunbird.user_quiz_master} — one row per quiz attempt (non-assessment submissions). */
@Table("user_quiz_master")
@Getter
@Setter
@NoArgsConstructor
public class UserQuizMaster {

    @PrimaryKey
    private UserQuizMasterKey key;

    @Column("correct_count")
    private Integer correctCount;

    @Column("date_created")
    private Instant dateCreated;

    @Column("incorrect_count")
    private Integer incorrectCount;

    @Column("not_answered_count")
    private Integer notAnsweredCount;

    @Column("pass_percent")
    private BigDecimal passPercent;

    @Column("source_id")
    private String sourceId;

    @Column("source_title")
    private String sourceTitle;

    @Column("user_id")
    private String userId;
}
