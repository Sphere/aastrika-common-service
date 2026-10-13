package org.aastrika.service;

import java.util.Collection;

public interface CompetencyCertificateService {

    /**
     * Fire-and-forget: for the given competencies, issues a certificate to the user for each one
     * that is allowed by the user's root-org config and has all required levels in the passbook.
     * Runs on its own executor and never throws to the caller.
     */
    void processAsync(String userId, Collection<String> competencyIds);
}