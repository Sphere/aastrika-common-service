package org.aastrika.entity;

import java.util.UUID;

import org.springframework.data.cassandra.core.mapping.Column;
import org.springframework.data.cassandra.core.mapping.PrimaryKey;
import org.springframework.data.cassandra.core.mapping.Table;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Maps {@code sunbird.ratings} — one row per (activity, user) rating/review. The composite
 * partition key lives in {@link RatingKey}. The {@code *On} columns are Cassandra {@code timeuuid}s
 * (time-based UUIDs), so the embedded timestamp is recovered with
 * {@code com.datastax.oss.driver.api.core.uuid.Uuids#unixTimestamp}.
 */
@Table("ratings")
@Getter
@Setter
@NoArgsConstructor
public class Rating {

    @PrimaryKey
    private RatingKey key;

    @Column("rating")
    private Float rating;

    @Column("review")
    private String review;

    @Column("comment")
    private String comment;

    @Column("commentby")
    private String commentBy;

    @Column("recommended")
    private String recommended;

    @Column("createdon")
    private UUID createdOn;

    @Column("updatedon")
    private UUID updatedOn;

    @Column("commentupdatedon")
    private UUID commentUpdatedOn;
}
