package org.aastrika.entity;

import org.springframework.data.cassandra.core.mapping.Column;
import org.springframework.data.cassandra.core.mapping.PrimaryKey;
import org.springframework.data.cassandra.core.mapping.Table;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Maps {@code sunbird.ratings_summary} — pre-aggregated per-activity rating stats maintained by the
 * external aggregation pipeline. {@code latest50reviews} is a JSON-array string (may be null).
 */
@Table("ratings_summary")
@Getter
@Setter
@NoArgsConstructor
public class RatingSummary {

    @PrimaryKey
    private RatingSummaryKey key;

    @Column("latest50reviews")
    private String latest50Reviews;

    @Column("sum_of_total_ratings")
    private Float sumOfTotalRatings;

    @Column("total_number_of_ratings")
    private Float totalNumberOfRatings;

    @Column("totalcount1stars")
    private Float totalCount1Stars;

    @Column("totalcount2stars")
    private Float totalCount2Stars;

    @Column("totalcount3stars")
    private Float totalCount3Stars;

    @Column("totalcount4stars")
    private Float totalCount4Stars;

    @Column("totalcount5stars")
    private Float totalCount5Stars;
}
