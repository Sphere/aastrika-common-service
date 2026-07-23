package org.aastrika.repository;

import org.aastrika.entity.User;
import org.springframework.data.cassandra.repository.CassandraRepository;
import org.springframework.stereotype.Repository;

/**
 * Typed Spring Data Cassandra repository for {@code sunbird.user}. {@code findAllById(ids)} reads by
 * {@code id IN (...)} — a keyed multi-partition read used to resolve reviewer first names.
 */
@Repository
public interface UserRepository extends CassandraRepository<User, String> {
}
