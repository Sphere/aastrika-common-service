package org.aastrika.entity;

import java.io.Serializable;

import org.springframework.data.cassandra.core.cql.PrimaryKeyType;
import org.springframework.data.cassandra.core.mapping.PrimaryKeyClass;
import org.springframework.data.cassandra.core.mapping.PrimaryKeyColumn;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Composite <b>partition</b> key for {@link Rating} ({@code sunbird.ratings}):
 * {@code (activityid, activitytype, userid)} — all three form the partition key (no clustering),
 * so a rating is addressed by the full triple.
 */
@PrimaryKeyClass
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RatingKey implements Serializable {

    @PrimaryKeyColumn(name = "activityid", ordinal = 0, type = PrimaryKeyType.PARTITIONED)
    private String activityId;

    @PrimaryKeyColumn(name = "activitytype", ordinal = 1, type = PrimaryKeyType.PARTITIONED)
    private String activityType;

    @PrimaryKeyColumn(name = "userid", ordinal = 2, type = PrimaryKeyType.PARTITIONED)
    private String userId;
}
