package org.aastrika.service.impl;

import java.util.Optional;

import org.aastrika.client.RcCertificateClient;
import org.aastrika.entity.UserCompetencyCertificate;
import org.aastrika.exception.CompetencyCertificateException;
import org.aastrika.service.CompetencyCertificateGenerator;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientException;

/**
 * Issues a competency certificate through the RC-proxy mapper ({@link RcCertificateClient}).
 * The template is the org's form/read {@code certificateConfig.competency.templateUrl} (set on the
 * record before this is called); without one, generation fails (record FAILED, retried on the next
 * passbook update). Every failure is raised as a {@link CompetencyCertificateException} with a fixed
 * message: the RC client's own exception text carries URLs and response bodies, so it stays the cause
 * only. No user identifiers are logged (SECURITY.md 2.5).
 */
@Component
public class RcCompetencyCertificateGenerator implements CompetencyCertificateGenerator {

    private final RcCertificateClient rcCertificateClient;

    public RcCompetencyCertificateGenerator(RcCertificateClient rcCertificateClient) {
        this.rcCertificateClient = rcCertificateClient;
    }

    @Override
    public Optional<String> generate(UserCompetencyCertificate record, CertificateSubject subject) {
        String templateId = record.getTemplateId();
        if (templateId == null || templateId.isBlank()) {
            throw new CompetencyCertificateException(CompetencyCertificateException.TEMPLATE_NOT_CONFIGURED,
                    "no certificate template in org form config");
        }
        Optional<String> url;
        try {
            url = rcCertificateClient.generate(templateId, record.getUserId(),
                    subject.userName(), record.getEventId(), subject.competencyName());
        } catch (HttpStatusCodeException e) {
            throw new CompetencyCertificateException(CompetencyCertificateException.RC_GENERATION_FAILED,
                    "certificate service returned " + e.getStatusCode().value(), e);
        } catch (ResourceAccessException e) {
            throw new CompetencyCertificateException(CompetencyCertificateException.RC_UNAVAILABLE,
                    "certificate service not reachable", e);
        } catch (RestClientException e) {
            throw new CompetencyCertificateException(CompetencyCertificateException.RC_GENERATION_FAILED,
                    "certificate service call failed", e);
        }
        if (url.isEmpty()) {
            throw new CompetencyCertificateException(CompetencyCertificateException.RC_NO_CERTIFICATE_URL,
                    "certificate service returned no certificate link");
        }
        return url;
    }
}