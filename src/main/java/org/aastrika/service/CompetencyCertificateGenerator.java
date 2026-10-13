package org.aastrika.service;

import java.util.Optional;

import org.aastrika.entity.UserCompetencyCertificate;

/** Issues one competency certificate. */
public interface CompetencyCertificateGenerator {

    /** Who and what the certificate is for — printed on it. */
    record CertificateSubject(String userName, String competencyName) { }

    /**
     * @return the certificate URL if it was issued synchronously; empty if generation is pending
     * @throws RuntimeException if generation failed — the record is saved FAILED and retried on the
     *         next passbook update
     */
    Optional<String> generate(UserCompetencyCertificate record, CertificateSubject subject);
}