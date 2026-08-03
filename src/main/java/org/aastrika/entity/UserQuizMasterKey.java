package org.aastrika.entity;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import org.springframework.data.cassandra.core.cql.PrimaryKeyType;
import org.springframework.data.cassandra.core.mapping.PrimaryKeyClass;
import org.springframework.data.cassandra.core.mapping.PrimaryKeyColumn;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Primary key for {@link UserQuizMaster} ({@code sunbird.user_quiz_master}):
 * partition {@code (root_org, ts_created)}, clustering {@code result_percent, id}.
 */
@PrimaryKeyClass
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserQuizMasterKey implements Serializable {

    @PrimaryKeyColumn(name = "root_org", ordinal = 0, type = PrimaryKeyType.PARTITIONED)
    private String rootOrg;

    @PrimaryKeyColumn(name = "ts_created", ordinal = 1, type = PrimaryKeyType.PARTITIONED)
    private Instant tsCreated;

    @PrimaryKeyColumn(name = "result_percent", ordinal = 2, type = PrimaryKeyType.CLUSTERED)
    private BigDecimal resultPercent;

    @PrimaryKeyColumn(name = "id", ordinal = 3, type = PrimaryKeyType.CLUSTERED)
    private UUID id;
}
