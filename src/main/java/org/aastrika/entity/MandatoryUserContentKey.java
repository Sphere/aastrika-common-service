package org.aastrika.entity;

import java.io.Serializable;

import org.springframework.data.cassandra.core.cql.PrimaryKeyType;
import org.springframework.data.cassandra.core.mapping.PrimaryKeyClass;
import org.springframework.data.cassandra.core.mapping.PrimaryKeyColumn;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Primary key of {@code sunbird.mandatory_user_content}: {@code PRIMARY KEY (root_org, org, content_id)}
 * — {@code root_org} is the partition key, {@code org} and {@code content_id} are clustering columns.
 */
@PrimaryKeyClass
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MandatoryUserContentKey implements Serializable {

    @PrimaryKeyColumn(name = "root_org", ordinal = 0, type = PrimaryKeyType.PARTITIONED)
    private String rootOrg;

    @PrimaryKeyColumn(name = "org", ordinal = 1, type = PrimaryKeyType.CLUSTERED)
    private String org;

    @PrimaryKeyColumn(name = "content_id", ordinal = 2, type = PrimaryKeyType.CLUSTERED)
    private String contentId;
}
