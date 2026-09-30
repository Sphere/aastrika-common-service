package org.aastrika.datalake.repository;

import org.aastrika.datalake.entity.LeaderboardEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * JPA repository for {@code leaderboard_table}, extended with {@link LeaderboardRepositoryCustom}
 * for the dynamic-filter queries the leaderboard read needs. Unused derived/native helpers from the
 * source repo were dropped — only the custom methods are called.
 *
 * <p>Lives in {@code org.aastrika.datalake.repository}, so it is bound to the data lake channel
 * rather than the primary database — see {@code DataLakeJpaConfig}. Package membership is what
 * routes it; there is no per-repository configuration to keep in sync.
 */
@Repository
public interface LeaderboardRepository
        extends JpaRepository<LeaderboardEntity, String>, LeaderboardRepositoryCustom {
}
