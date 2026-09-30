package org.aastrika.entity;

import java.io.Serializable;
import java.time.Instant;

import org.springframework.data.cassandra.core.cql.Ordering;
import org.springframework.data.cassandra.core.cql.PrimaryKeyType;
import org.springframework.data.cassandra.core.mapping.PrimaryKeyClass;
import org.springframework.data.cassandra.core.mapping.PrimaryKeyColumn;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Composite primary key for {@link UserPassbook} ({@code sunbird.user_passbook_v2}):
 * partition key {@code userid} + clustering columns {@code typename, acquiredchannel, typeid,
 * contextid, effectivedate} (all ascending).
 */
@PrimaryKeyClass
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserPassbookKey implements Serializable {

    @PrimaryKeyColumn(name = "userid", ordinal = 0, type = PrimaryKeyType.PARTITIONED)
    private String userId;

    @PrimaryKeyColumn(name = "typename", ordinal = 1, type = PrimaryKeyType.CLUSTERED, ordering = Ordering.ASCENDING)
    private String typeName;

    @PrimaryKeyColumn(name = "acquiredchannel", ordinal = 2, type = PrimaryKeyType.CLUSTERED, ordering = Ordering.ASCENDING)
    private String acquiredChannel;

    @PrimaryKeyColumn(name = "typeid", ordinal = 3, type = PrimaryKeyType.CLUSTERED, ordering = Ordering.ASCENDING)
    private String typeId;

    @PrimaryKeyColumn(name = "contextid", ordinal = 4, type = PrimaryKeyType.CLUSTERED, ordering = Ordering.ASCENDING)
    private String contextId;

    @PrimaryKeyColumn(name = "effectivedate", ordinal = 5, type = PrimaryKeyType.CLUSTERED, ordering = Ordering.ASCENDING)
    private Instant effectiveDate;
}
