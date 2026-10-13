package org.aastrika.entity;

import java.io.Serializable;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Composite primary key for {@link UserCompetencyCertificate}. Field names and types must match the
 * {@code @Id}-annotated fields on the entity.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserCompetencyCertificateId implements Serializable {

    private String userId;
    private String competencyId;
}