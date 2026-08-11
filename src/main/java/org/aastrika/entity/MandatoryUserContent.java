package org.aastrika.entity;

import org.springframework.data.cassandra.core.mapping.Column;
import org.springframework.data.cassandra.core.mapping.PrimaryKey;
import org.springframework.data.cassandra.core.mapping.Table;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * A mandatory course configured for an org, from {@code sunbird.mandatory_user_content}. The
 * snake_case Cassandra columns are mapped to camelCase fields explicitly via {@code @Column} — this
 * is the aastrika replacement for the source's global {@code cassandratablecolumn.properties} rename
 * layer, so {@code batchId}/{@code contentType}/{@code minProgressForCompletion} always populate.
 */
@Table("mandatory_user_content")
@Getter
@Setter
@NoArgsConstructor
public class MandatoryUserContent {

    @PrimaryKey
    private MandatoryUserContentKey key;

    @Column("batch_id")
    private String batchId;

    @Column("content_type")
    private String contentType;

    @Column("minprogressforcompletion")
    private Float minProgressForCompletion;
}
