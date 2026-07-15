package org.aastrika.entity;

import java.util.Map;

import org.springframework.data.cassandra.core.mapping.Column;
import org.springframework.data.cassandra.core.mapping.PrimaryKey;
import org.springframework.data.cassandra.core.mapping.Table;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Maps {@code user_passbook_v2} in the session's keyspace ({@code sunbird}). The composite primary
 * key lives in {@link UserPassbookKey}. For tables in other keyspaces, use the generic
 * {@code CassandraDao} (which qualifies {@code keyspace.table}).
 */
@Table("user_passbook_v2")
@Getter
@Setter
@NoArgsConstructor
public class UserPassbook {

    @PrimaryKey
    private UserPassbookKey key;

    @Column("acquireddetails")
    private Map<String, String> acquiredDetails;

    @Column("additionalparams")
    private Map<String, String> additionalParams;
}
