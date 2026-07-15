package org.aastrika.repository;

import org.aastrika.entity.UserProgramCompletion;
import org.aastrika.entity.UserProgramCompletionId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserProgramCompletionRepository
        extends JpaRepository<UserProgramCompletion, UserProgramCompletionId> {
}
