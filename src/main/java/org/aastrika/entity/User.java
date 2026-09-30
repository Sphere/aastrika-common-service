package org.aastrika.entity;

import org.springframework.data.cassandra.core.mapping.Column;
import org.springframework.data.cassandra.core.mapping.PrimaryKey;
import org.springframework.data.cassandra.core.mapping.Table;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Minimal view of {@code sunbird.user} — only the fields the ratings read APIs need for reviewer
 * name enrichment. Other columns on the table are simply not mapped (and ignored on read).
 */
@Table("user")
@Getter
@Setter
@NoArgsConstructor
public class User {

    @PrimaryKey
    private String id;

    @Column("firstname")
    private String firstName;
}
