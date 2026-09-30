package org.aastrika.repository;

import java.util.List;

import org.aastrika.entity.RatingSummary;
import org.aastrika.entity.RatingSummaryKey;
import org.springframework.data.cassandra.repository.CassandraRepository;
import org.springframework.stereotype.Repository;

/** Typed Spring Data Cassandra repository for {@code sunbird.ratings_summary}. */
@Repository
public interface RatingSummaryRepository extends CassandraRepository<RatingSummary, RatingSummaryKey> {

    /** IN on the partition key ({@code activityid}) + equality on the clustering column ({@code activitytype}). */
    List<RatingSummary> findByKeyActivityIdInAndKeyActivityType(List<String> activityIds, String activityType);
}
