package org.aastrika.entity;

import java.io.Serializable;

import org.springframework.data.cassandra.core.cql.PrimaryKeyType;
import org.springframework.data.cassandra.core.mapping.PrimaryKeyClass;
import org.springframework.data.cassandra.core.mapping.PrimaryKeyColumn;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Primary key for {@link UserQuizSummary} ({@code sunbird.user_quiz_summary}):
 * partition {@code (root_org, user_id)}, clustering {@code content_id}.
 */
@PrimaryKeyClass
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserQuizSummaryKey implements Serializable {

    @PrimaryKeyColumn(name = "root_org", ordinal = 0, type = PrimaryKeyType.PARTITIONED)
    private String rootOrg;

    @PrimaryKeyColumn(name = "user_id", ordinal = 1, type = PrimaryKeyType.PARTITIONED)
    private String userId;

    @PrimaryKeyColumn(name = "content_id", ordinal = 2, type = PrimaryKeyType.CLUSTERED)
    private String contentId;
}
