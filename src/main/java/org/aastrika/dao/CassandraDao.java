package org.aastrika.dao;

import java.util.List;
import java.util.Map;

/**
 * Generic Cassandra data-access contract, backed by the Spring Boot auto-configured
 * {@code CqlSession}. Operations are expressed over (keyspace, table, column-map) so callers
 * don't hand-write CQL. Intended as a starting point — extend with typed methods as concrete
 * tables/entities are introduced.
 */
public interface CassandraDao {

    /**
     * Insert a single row.
     *
     * @param keyspace keyspace name (may be null/blank to use the session's default keyspace)
     * @param table    table name
     * @param row      column name to value; must not be empty
     */
    void insert(String keyspace, String table, Map<String, Object> row);

    /**
     * Select rows matching all of the given {@code column = value} conditions.
     *
     * @param conditions column to value conditions ANDed together; empty/null means full-table scan
     * @param columns    columns to project; null/empty selects all columns (*)
     * @return each matching row as an insertion-ordered column to value map
     */
    List<Map<String, Object>> findByProperties(String keyspace, String table,
                                               Map<String, Object> conditions, List<String> columns);

    /**
     * Delete rows matching all of the given primary-key {@code column = value} conditions.
     *
     * @param keyConditions primary-key column to value conditions; must not be empty
     */
    void deleteByKey(String keyspace, String table, Map<String, Object> keyConditions);

    /** Count all rows in the table (uses {@code COUNT(*)} — a coordinator-side full scan). */
    long count(String keyspace, String table);

    /** Lightweight connectivity check ({@code SELECT release_version FROM system.local}). */
    boolean isReachable();
}
