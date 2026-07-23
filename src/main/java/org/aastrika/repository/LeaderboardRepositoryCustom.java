package org.aastrika.repository;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.aastrika.entity.LeaderboardEntity;

/**
 * Dynamic-filter queries for the leaderboard that Spring Data derivation can't express. Filter keys
 * are column names ANDed into the SQL, so callers MUST whitelist them (see
 * {@code LeaderboardServiceImpl}) before invoking these methods.
 */
public interface LeaderboardRepositoryCustom {

    /** Users matching all filters, ordered {@code points DESC, date ASC}, with optional paging. */
    List<LeaderboardEntity> findAllUsersByDynamicFilters(Map<String, Object> filters, Integer limit, Integer offset);

    /** The single user matching the filters and {@code userId}, or empty if not exactly one match. */
    Optional<LeaderboardEntity> findUserByDynamicFilter(String userId, Map<String, Object> filters);

    /** How many users rank above the given one (more points, or equal points but earlier date). */
    Integer findUserRank(LeaderboardEntity leaderboardEntity, Map<String, Object> filters);
}
