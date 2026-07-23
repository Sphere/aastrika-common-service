package org.aastrika.repository;

import org.aastrika.entity.LeaderboardEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * JPA repository for {@code leaderboard_table}, extended with {@link LeaderboardRepositoryCustom}
 * for the dynamic-filter queries the leaderboard read needs. Unused derived/native helpers from the
 * source repo were dropped — only the custom methods are called.
 */
@Repository
public interface LeaderboardRepository
        extends JpaRepository<LeaderboardEntity, String>, LeaderboardRepositoryCustom {
}
