package org.aastrika.entity;

import java.io.Serializable;

import org.springframework.data.cassandra.core.cql.Ordering;
import org.springframework.data.cassandra.core.cql.PrimaryKeyType;
import org.springframework.data.cassandra.core.mapping.PrimaryKeyClass;
import org.springframework.data.cassandra.core.mapping.PrimaryKeyColumn;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Primary key for {@link RatingSummary} ({@code sunbird.ratings_summary}): partition key
 * {@code activityid} + clustering column {@code activitytype}.
 */
@PrimaryKeyClass
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RatingSummaryKey implements Serializable {

    @PrimaryKeyColumn(name = "activityid", ordinal = 0, type = PrimaryKeyType.PARTITIONED)
    private String activityId;

    @PrimaryKeyColumn(name = "activitytype", ordinal = 1, type = PrimaryKeyType.CLUSTERED, ordering = Ordering.ASCENDING)
    private String activityType;
}
