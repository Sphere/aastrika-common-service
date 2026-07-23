package org.aastrika.repository;

import java.util.List;

import org.aastrika.entity.Rating;
import org.aastrika.entity.RatingKey;
import org.springframework.data.cassandra.repository.CassandraRepository;
import org.springframework.stereotype.Repository;

/**
 * Typed Spring Data Cassandra repository for {@code sunbird.ratings}. Lookups by the full
 * {@link RatingKey} (partition key) are single-partition and need no ALLOW FILTERING.
 */
@Repository
public interface RatingRepository extends CassandraRepository<Rating, RatingKey> {

    /**
     * Ratings for one activity across several users — {@code IN} on the last partition-key column
     * ({@code userid}) with the rest of the partition key fixed, so no ALLOW FILTERING is needed.
     */
    List<Rating> findByKeyActivityIdAndKeyActivityTypeAndKeyUserIdIn(
            String activityId, String activityType, List<String> userIds);
}
