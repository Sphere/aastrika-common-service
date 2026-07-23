package org.aastrika.repository;

import org.aastrika.entity.RatingSummary;
import org.aastrika.entity.RatingSummaryKey;
import org.springframework.data.cassandra.repository.CassandraRepository;
import org.springframework.stereotype.Repository;

/** Typed Spring Data Cassandra repository for {@code sunbird.ratings_summary}. */
@Repository
public interface RatingSummaryRepository extends CassandraRepository<RatingSummary, RatingSummaryKey> {
}
