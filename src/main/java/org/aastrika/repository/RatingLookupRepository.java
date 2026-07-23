package org.aastrika.repository;

import java.util.List;
import java.util.UUID;

/**
 * Paginated reads over {@code sunbird.ratings_lookup}. The query is built dynamically (optional
 * {@code rating} / cursor) so it can't be a Spring Data derived method — see the impl.
 */
public interface RatingLookupRepository {

    /**
     * A page of lookup rows for an activity, newest first.
     *
     * @param rating           optional star filter; when null the query spans all star partitions
     *                         (ALLOW FILTERING) and results are ordered client-side
     * @param updatedOnCursor  optional {@code updatedon} UUID; rows strictly older than it are returned
     * @param limit            max rows to return
     */
    List<Row> findPage(String activityId, String activityType, Float rating, String updatedOnCursor, int limit);

    /** A single {@code ratings_lookup} row. */
    record Row(String activityId, String activityType, Float rating, UUID updatedOn, String review, String userId) {
    }
}
