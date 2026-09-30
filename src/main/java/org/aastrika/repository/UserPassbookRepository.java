package org.aastrika.repository;

import java.util.List;

import org.aastrika.entity.UserPassbook;
import org.aastrika.entity.UserPassbookKey;
import org.springframework.data.cassandra.repository.CassandraRepository;
import org.springframework.stereotype.Repository;

/**
 * Typed Spring Data Cassandra repository for {@code user_passbook_v2}. Mirrors the Postgres
 * {@code UserProgramCompletionRepository} style. Routed to the Cassandra module (not JPA) because
 * it extends {@link CassandraRepository} and its entity is a Cassandra {@code @Table}.
 */
@Repository
public interface UserPassbookRepository extends CassandraRepository<UserPassbook, UserPassbookKey> {

    /** All passbook entries for a user — a single-partition query (efficient, no ALLOW FILTERING). */
    List<UserPassbook> findByKeyUserId(String userId);

    /**
     * A user's entries of one type — restricts the partition key ({@code userid}) plus the first
     * clustering column ({@code typename}), so no ALLOW FILTERING is needed.
     */
    List<UserPassbook> findByKeyUserIdAndKeyTypeName(String userId, String typeName);

    /**
     * Entries of one type for several users (admin read). {@code userid IN (...)} on the partition
     * key plus equality on {@code typename} — still a keyed, filtering-free query.
     */
    List<UserPassbook> findByKeyUserIdInAndKeyTypeName(List<String> userIds, String typeName);
}
