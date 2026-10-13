package org.aastrika.service.impl;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.aastrika.client.FormClient;
import org.aastrika.service.CompetencyCertificateConfigProvider;
import org.springframework.stereotype.Component;

/**
 * Reads {@code orgData.certificateConfig.competency} ({@code competencyIds}, {@code templateUrl}) from
 * the org's form/read data. The org counts as not set up when the form cannot be read, or
 * {@code certificateConfig.competency} / {@code competencyIds} is missing. {@code templateUrl} holds
 * the RC template id. Ids are compared as strings so a numeric id matches the passbook {@code typeid}.
 */
@Component
public class FormReadCertificateConfigProvider implements CompetencyCertificateConfigProvider {

    private final FormClient formClient;

    public FormReadCertificateConfigProvider(FormClient formClient) {
        this.formClient = formClient;
    }

    @Override
    @SuppressWarnings("unchecked")
    public Optional<CertificateConfig> getConfig(String rootOrgId) {
        Map<String, Object> data = formClient.readFormData(rootOrgId);
        if (data == null
                || !(data.get("orgData") instanceof Map<?, ?> orgData)
                || !(((Map<String, Object>) orgData).get("certificateConfig") instanceof Map<?, ?> certificateConfig)
                || !(((Map<String, Object>) certificateConfig).get("competency") instanceof Map<?, ?> config)
                || !(((Map<String, Object>) config).get("competencyIds") instanceof Collection<?> ids)) {
            return Optional.empty();
        }
        Set<String> competencyIds = new HashSet<>();
        for (Object id : ids) {
            if (id != null) {
                competencyIds.add(String.valueOf(id));
            }
        }
        Object template = ((Map<String, Object>) config).get("templateUrl");
        String templateId = template instanceof String s && !s.isBlank() ? s : null;
        return Optional.of(new CertificateConfig(competencyIds, templateId));
    }
}