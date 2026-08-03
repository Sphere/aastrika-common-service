package org.aastrika.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.datastax.oss.driver.api.core.CqlSession;
import com.datastax.oss.driver.api.core.cql.SimpleStatement;

/**
 * Enrolment gate for the rating aggregator — a keyspace-qualified read of
 * {@code sunbird_courses.user_enrolments} (a different keyspace than the session default, so it's a
 * direct {@link CqlSession} query rather than a typed repository). Mirrors the Flink job's check:
 * {@code WHERE userid = ? AND courseid = ?} (partition key + first clustering column).
 */
@Component
public class UserEnrolmentDao {

    private final CqlSession session;
    private final String selectCql;

    public UserEnrolmentDao(
            CqlSession session,
            @Value("${ratings.enrolment.keyspace:sunbird_courses}") String keyspace,
            @Value("${ratings.enrolment.table:user_enrolments}") String table) {
        this.session = session;
        // keyspace/table are code/config-supplied identifiers, never end-user input.
        this.selectCql = "SELECT userid FROM " + keyspace + "." + table
                + " WHERE userid = ? AND courseid = ? LIMIT 1";
    }

    /** True if the user has any enrolment (any batch) in the given course/activity. */
    public boolean isEnrolled(String userId, String courseId) {
        return session.execute(SimpleStatement.newInstance(selectCql, userId, courseId)).one() != null;
    }
}
