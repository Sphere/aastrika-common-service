package org.aastrika.entity;

import java.time.Instant;

import org.springframework.data.cassandra.core.mapping.Column;
import org.springframework.data.cassandra.core.mapping.PrimaryKey;
import org.springframework.data.cassandra.core.mapping.Table;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Maps {@code sunbird.user_quiz_summary} — per (user, content) marker of the last quiz submission. */
@Table("user_quiz_summary")
@Getter
@Setter
@NoArgsConstructor
public class UserQuizSummary {

    @PrimaryKey
    private UserQuizSummaryKey key;

    @Column("date_updated")
    private Instant dateUpdated;
}
