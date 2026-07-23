package org.aastrika.repository;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Repository;

import com.datastax.oss.driver.api.core.CqlSession;
import com.datastax.oss.driver.api.core.cql.ResultSet;
import com.datastax.oss.driver.api.core.cql.SimpleStatement;

/**
 * {@link RatingLookupRepository} backed by the auto-configured {@link CqlSession}. Values are always
 * bound as {@code ?} parameters; only the fixed column/table identifiers are code-supplied.
 *
 * <p>When {@code rating} is given the full partition key {@code (activityid, activitytype, rating)}
 * is restricted, so the query is single-partition and needs no ALLOW FILTERING. When it's omitted
 * (browse all stars) the partition key is incomplete, so ALLOW FILTERING is required and the caller
 * re-sorts the merged rows.
 */
@Repository
public class RatingLookupRepositoryImpl implements RatingLookupRepository {

    private final CqlSession session;

    public RatingLookupRepositoryImpl(CqlSession session) {
        this.session = session;
    }

    @Override
    public List<Row> findPage(String activityId, String activityType, Float rating,
                              String updatedOnCursor, int limit) {
        StringBuilder cql = new StringBuilder(
                "SELECT activityid, activitytype, rating, updatedon, review, userid"
                        + " FROM ratings_lookup WHERE activityid = ? AND activitytype = ?");
        List<Object> values = new ArrayList<>();
        values.add(activityId);
        values.add(activityType);

        if (rating != null) {
            cql.append(" AND rating = ?");
            values.add(rating);
        }
        if (updatedOnCursor != null && !updatedOnCursor.isBlank()) {
            cql.append(" AND updatedon < ?");
            values.add(UUID.fromString(updatedOnCursor));
        }
        cql.append(" LIMIT ?");
        values.add(limit);
        if (rating == null) {
            cql.append(" ALLOW FILTERING");
        }

        ResultSet rs = session.execute(SimpleStatement.newInstance(cql.toString(), values.toArray()));
        List<Row> rows = new ArrayList<>();
        for (com.datastax.oss.driver.api.core.cql.Row r : rs) {
            rows.add(new Row(
                    r.getString("activityid"),
                    r.getString("activitytype"),
                    r.get("rating", Float.class),
                    r.getUuid("updatedon"),
                    r.getString("review"),
                    r.getString("userid")));
        }
        return rows;
    }
}
