package org.aastrika.dao.impl;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.aastrika.dao.CassandraDao;
import org.springframework.stereotype.Repository;

import com.datastax.oss.driver.api.core.CqlSession;
import com.datastax.oss.driver.api.core.cql.ColumnDefinitions;
import com.datastax.oss.driver.api.core.cql.ResultSet;
import com.datastax.oss.driver.api.core.cql.Row;
import com.datastax.oss.driver.api.core.cql.SimpleStatement;

import lombok.extern.slf4j.Slf4j;

/**
 * {@link CassandraDao} backed by the Spring Boot auto-configured {@link CqlSession}
 * (from {@code spring.cassandra.*}). Values are always passed as bound parameters ({@code ?});
 * keyspace/table/column names are code-supplied identifiers, never end-user input.
 */
@Repository
@Slf4j
public class CassandraDaoImpl implements CassandraDao {

    private final CqlSession session;

    public CassandraDaoImpl(CqlSession session) {
        this.session = session;
    }

    @Override
    public void insert(String keyspace, String table, Map<String, Object> row) {
        if (row == null || row.isEmpty()) {
            throw new IllegalArgumentException("insert: row must not be null or empty");
        }
        List<String> columns = new ArrayList<>(row.keySet());
        String placeholders = String.join(", ", columns.stream().map(c -> "?").toList());
        String cql = "INSERT INTO " + qualified(keyspace, table)
                + " (" + String.join(", ", columns) + ") VALUES (" + placeholders + ")";
        session.execute(SimpleStatement.newInstance(cql, row.values().toArray()));
    }

    @Override
    public List<Map<String, Object>> findByProperties(String keyspace, String table,
                                                       Map<String, Object> conditions, List<String> columns) {
        String projection = (columns == null || columns.isEmpty()) ? "*" : String.join(", ", columns);
        StringBuilder cql = new StringBuilder("SELECT ").append(projection)
                .append(" FROM ").append(qualified(keyspace, table));
        List<Object> values = new ArrayList<>();
        if (conditions != null && !conditions.isEmpty()) {
            List<String> clauses = new ArrayList<>();
            for (Map.Entry<String, Object> entry : conditions.entrySet()) {
                clauses.add(entry.getKey() + " = ?");
                values.add(entry.getValue());
            }
            // ALLOW FILTERING keeps this generic helper working on non-key columns; for hot paths,
            // prefer a query restricted to the partition/clustering key instead.
            cql.append(" WHERE ").append(String.join(" AND ", clauses)).append(" ALLOW FILTERING");
        }
        ResultSet rs = session.execute(SimpleStatement.newInstance(cql.toString(), values.toArray()));
        List<Map<String, Object>> results = new ArrayList<>();
        for (Row row : rs) {
            results.add(toMap(row));
        }
        return results;
    }

    @Override
    public void deleteByKey(String keyspace, String table, Map<String, Object> keyConditions) {
        if (keyConditions == null || keyConditions.isEmpty()) {
            throw new IllegalArgumentException("deleteByKey: keyConditions must not be null or empty");
        }
        List<String> clauses = new ArrayList<>();
        List<Object> values = new ArrayList<>();
        for (Map.Entry<String, Object> entry : keyConditions.entrySet()) {
            clauses.add(entry.getKey() + " = ?");
            values.add(entry.getValue());
        }
        String cql = "DELETE FROM " + qualified(keyspace, table)
                + " WHERE " + String.join(" AND ", clauses);
        session.execute(SimpleStatement.newInstance(cql, values.toArray()));
    }

    @Override
    public long count(String keyspace, String table) {
        ResultSet rs = session.execute(SimpleStatement.newInstance(
                "SELECT COUNT(*) FROM " + qualified(keyspace, table)));
        Row row = rs.one();
        return row == null ? 0L : row.getLong(0);
    }

    @Override
    public boolean isReachable() {
        try {
            return session.execute("SELECT release_version FROM system.local").one() != null;
        } catch (Exception e) {
            log.error("Cassandra reachability check failed", e);
            return false;
        }
    }

    private static String qualified(String keyspace, String table) {
        return (keyspace == null || keyspace.isBlank()) ? table : keyspace + "." + table;
    }

    private static Map<String, Object> toMap(Row row) {
        Map<String, Object> map = new LinkedHashMap<>();
        ColumnDefinitions definitions = row.getColumnDefinitions();
        for (int i = 0; i < definitions.size(); i++) {
            map.put(definitions.get(i).getName().asInternal(), row.getObject(i));
        }
        return map;
    }
}
