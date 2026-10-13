package org.aastrika.service;

import java.util.Optional;
import java.util.Set;

/** Resolves an org's competency-certificate config (form/read {@code orgData.certificateConfig}). */
public interface CompetencyCertificateConfigProvider {

    /** An org's certificate config. An empty {@code competencyIds} means all competencies are allowed. */
    record CertificateConfig(Set<String> competencyIds, String templateId) { }

    /** @return empty if the org is not set up for competency certificates */
    Optional<CertificateConfig> getConfig(String rootOrgId);
}