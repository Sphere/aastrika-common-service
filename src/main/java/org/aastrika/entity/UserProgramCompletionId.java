package org.aastrika.entity;

import java.io.Serializable;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Composite primary key for {@link UserProgramCompletion}. Field names and types must match the
 * {@code @Id}-annotated fields on the entity. Serializable with equals/hashCode (via {@code @Data})
 * as required by JPA for an {@code @IdClass}.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserProgramCompletionId implements Serializable {

    private String rootOrgId;
    private String programId;
    private String userId;
}
