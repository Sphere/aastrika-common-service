package org.aastrika.repository;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.aastrika.entity.LeaderboardEntity;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;

/**
 * {@link LeaderboardRepositoryCustom} backed by native SQL. Filter keys are appended as column names,
 * so they must already be whitelisted by the caller; values are always bound as named parameters.
 */
@Repository
public class LeaderboardRepositoryCustomImpl implements LeaderboardRepositoryCustom {

    private static final String BASE_SELECT = "SELECT * FROM public.leaderboard_table WHERE 1=1";

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public List<LeaderboardEntity> findAllUsersByDynamicFilters(
            Map<String, Object> filters, Integer limit, Integer offset) {
        StringBuilder sql = new StringBuilder(BASE_SELECT);
        appendFilters(sql, filters);
        sql.append(" ORDER BY points DESC, date ASC");

        Query query = entityManager.createNativeQuery(sql.toString(), LeaderboardEntity.class);
        bindFilters(query, filters);
        if (offset != null && offset >= 0) {
            query.setFirstResult(offset);
        }
        if (limit != null && limit > 0) {
            query.setMaxResults(limit);
        }
        @SuppressWarnings("unchecked")
        List<LeaderboardEntity> results = query.getResultList();
        return results;
    }

    /**
     * Runs inside a transaction and flushes/clears the persistence context first so the single-user
     * lookup returns fresh state rather than a first-level-cache hit from the list query above.
     */
    @Transactional
    @Override
    public Optional<LeaderboardEntity> findUserByDynamicFilter(String userId, Map<String, Object> filters) {
        StringBuilder sql = new StringBuilder(BASE_SELECT);
        appendFilters(sql, filters);
        sql.append(" AND userid = :userId");

        entityManager.flush();
        entityManager.clear();

        Query query = entityManager.createNativeQuery(sql.toString(), LeaderboardEntity.class);
        bindFilters(query, filters);
        query.setParameter("userId", userId);

        @SuppressWarnings("unchecked")
        List<LeaderboardEntity> results = query.getResultList();
        return results.size() == 1 ? Optional.of(results.get(0)) : Optional.empty();
    }

    @Override
    public Integer findUserRank(LeaderboardEntity leaderboardEntity, Map<String, Object> filters) {
        StringBuilder sql = new StringBuilder("SELECT COUNT(*) FROM public.leaderboard_table WHERE 1=1");
        appendFilters(sql, filters);
        sql.append(" AND ((points > :userPoints) OR (points = :userPoints AND date < :userDate))");

        Query query = entityManager.createNativeQuery(sql.toString());
        bindFilters(query, filters);
        query.setParameter("userPoints", leaderboardEntity.getPoints());
        query.setParameter("userDate", leaderboardEntity.getDate());

        return ((Number) query.getSingleResult()).intValue();
    }

    private static void appendFilters(StringBuilder sql, Map<String, Object> filters) {
        if (filters != null) {
            for (String key : filters.keySet()) {
                sql.append(" AND ").append(key).append(" = :").append(key);
            }
        }
    }

    private static void bindFilters(Query query, Map<String, Object> filters) {
        if (filters != null) {
            filters.forEach(query::setParameter);
        }
    }
}
