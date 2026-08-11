package org.aastrika.repository;

import java.util.List;

import org.aastrika.entity.MandatoryUserContent;
import org.aastrika.entity.MandatoryUserContentKey;
import org.springframework.data.cassandra.repository.CassandraRepository;
import org.springframework.data.cassandra.repository.Query;
import org.springframework.stereotype.Repository;

/**
 * Reads the mandatory courses configured for an org from {@code sunbird.mandatory_user_content}.
 * The query filters by partition key ({@code root_org}) + first clustering column ({@code org}),
 * so no ALLOW FILTERING is needed.
 */
@Repository
public interface MandatoryUserContentRepository
        extends CassandraRepository<MandatoryUserContent, MandatoryUserContentKey> {

    @Query("SELECT * FROM mandatory_user_content WHERE root_org = ?0 AND org = ?1")
    List<MandatoryUserContent> findByRootOrgAndOrg(String rootOrg, String org);
}
