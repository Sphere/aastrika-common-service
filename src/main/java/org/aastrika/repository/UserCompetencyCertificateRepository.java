package org.aastrika.repository;

import org.aastrika.entity.UserCompetencyCertificate;
import org.aastrika.entity.UserCompetencyCertificateId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserCompetencyCertificateRepository
        extends JpaRepository<UserCompetencyCertificate, UserCompetencyCertificateId> {
}