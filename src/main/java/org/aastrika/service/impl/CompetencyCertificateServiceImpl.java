package org.aastrika.service.impl;

import java.time.Instant;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.aastrika.client.UserProvisioningClient;
import org.aastrika.config.CompetencyCertificateAsyncConfig;
import org.aastrika.entity.UserCompetencyCertificate;
import org.aastrika.entity.UserCompetencyCertificateId;
import org.aastrika.entity.UserPassbook;
import org.aastrika.exception.CompetencyCertificateException;
import org.aastrika.repository.UserCompetencyCertificateRepository;
import org.aastrika.repository.UserPassbookRepository;
import org.aastrika.service.CompetencyCertificateConfigProvider;
import org.aastrika.service.CompetencyCertificateConfigProvider.CertificateConfig;
import org.aastrika.service.CompetencyCertificateGenerator;
import org.aastrika.service.CompetencyCertificateGenerator.CertificateSubject;
import org.aastrika.service.CompetencyCertificateService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;

/**
 * Competency certificate task (PRD: COMPETENCY_CERTIFICATE_REVISED_REQUIREMENT §3). Started after a
 * passbook update; only the competencies in that update's payload are evaluated.
 *
 * <p>No user identifiers are logged (SECURITY.md 2.5).
 */
@Service
@Slf4j
public class CompetencyCertificateServiceImpl implements CompetencyCertificateService {

    private static final String TYPE_NAME = "competency";
    /** Width of {@code user_competency_certificate.message}. */
    private static final int MESSAGE_MAX_LENGTH = 512;

    private final UserProvisioningClient userClient;
    private final CompetencyCertificateConfigProvider configProvider;
    private final CompetencyCertificateGenerator generator;
    private final UserPassbookRepository userPassbookRepository;
    private final UserCompetencyCertificateRepository certificateRepository;
    private final Set<String> requiredLevels;

    public CompetencyCertificateServiceImpl(
            UserProvisioningClient userClient,
            CompetencyCertificateConfigProvider configProvider,
            CompetencyCertificateGenerator generator,
            UserPassbookRepository userPassbookRepository,
            UserCompetencyCertificateRepository certificateRepository,
            @Value("${competency-certificate.required-levels:1,2,3,4,5}") Set<String> requiredLevels) {
        this.userClient = userClient;
        this.configProvider = configProvider;
        this.generator = generator;
        this.userPassbookRepository = userPassbookRepository;
        this.certificateRepository = certificateRepository;
        this.requiredLevels = requiredLevels;
    }

    @Async(CompetencyCertificateAsyncConfig.EXECUTOR)
    @Override
    public void processAsync(String userId, Collection<String> competencyIds) {
        try {
            process(userId, new HashSet<>(competencyIds));
        } catch (RuntimeException e) {
            log.error("Competency certificate task failed: {}", e.getMessage(), e);
        }
    }

    private void process(String userId, Set<String> candidates) {
        Map<String, Object> user = userClient.readUser(userId);
        String rootOrgId = user == null ? null : (String) user.get("rootOrgId");
        if (rootOrgId == null || rootOrgId.isBlank()) {
            log.warn("Competency certificate skipped: user read returned no rootOrgId");
            return;
        }

        Optional<CertificateConfig> config = configProvider.getConfig(rootOrgId);
        if (config.isEmpty()) {
            log.debug("Competency certificates not set up for org {}", rootOrgId);
            return;
        }
        // An empty competencyIds list means every competency is allowed.
        if (!config.get().competencyIds().isEmpty()) {
            candidates.retainAll(config.get().competencyIds());
        }
        if (candidates.isEmpty()) {
            return;
        }

        String userName = fullName(user);

        // Single-partition read of only the payload's competencies; group acquired levels (contextid)
        // per typeid, and keep the competency name printed on the certificate.
        Map<String, Set<String>> levelsByCompetency = new HashMap<>();
        Map<String, String> nameByCompetency = new HashMap<>();

        for (UserPassbook row : userPassbookRepository
                .findByKeyUserIdAndKeyTypeNameAndKeyTypeIdIn(userId, TYPE_NAME, candidates)) {
            String typeId = row.getKey().getTypeId();
            levelsByCompetency.computeIfAbsent(typeId, k -> new HashSet<>()).add(row.getKey().getContextId());
            String name = row.getAdditionalParams() == null ? null : row.getAdditionalParams().get("competencyName");
            if (name != null && !name.isBlank()) {
                nameByCompetency.putIfAbsent(typeId, name);
            }
        }

        for (String competencyId : candidates) {
            if (levelsByCompetency.getOrDefault(competencyId, Set.of()).containsAll(requiredLevels)) {
                String competencyName = nameByCompetency.get(competencyId);
                // The certificate falls back to the id when the passbook has no name; the table keeps null.
                CertificateSubject subject = new CertificateSubject(userName,
                        competencyName != null ? competencyName : competencyId);
                issue(userId, competencyId, competencyName, rootOrgId, config.get().templateId(), subject);
//                issue(userId, competencyId, rootOrgId, "WaterBirth", subject);
            }
        }
    }

    private static String fullName(Map<String, Object> user) {
        String first = user.get("firstName") instanceof String s ? s : "";
        String last = user.get("lastName") instanceof String s ? s : "";
        return (first + " " + last).trim();
    }

    /** Generates the certificate unless already generated; a failure is recorded and retried on the next update. */
    private void issue(String userId, String competencyId, String competencyName, String rootOrgId,
                       String templateId, CertificateSubject subject) {
        try {
            UserCompetencyCertificate record = certificateRepository
                    .findById(new UserCompetencyCertificateId(userId, competencyId))
                    .orElseGet(() -> newRecord(userId, competencyId));
            if (UserCompetencyCertificate.GENERATED.equals(record.getStatus())) {
                return;
            }
            record.setCompetencyName(competencyName);
            record.setRootOrgId(rootOrgId);
            record.setTemplateId(templateId);
            try {
                Optional<String> url = generator.generate(record, subject);
                record.setStatus(url.isPresent() ? UserCompetencyCertificate.GENERATED : UserCompetencyCertificate.PENDING);
                record.setCertificateUrl(url.orElse(null));
                record.setMessage(null);
            } catch (CompetencyCertificateException e) {
                record.setStatus(UserCompetencyCertificate.FAILED);
                record.setMessage(truncate(e.getMessage()));
                log.error("Competency certificate generation failed for competency {}: {} ({})", competencyId,
                        e.getMessage(), e.getCause() == null ? "no cause" : e.getCause().getMessage());
            } catch (RuntimeException e) {
                // Raw text may carry internal detail, so only a fixed message is stored (SECURITY.md 2.6).
                record.setStatus(UserCompetencyCertificate.FAILED);
                record.setMessage(CompetencyCertificateException.UNEXPECTED_ERROR
                        + ": certificate generation failed unexpectedly");
                log.error("Competency certificate generation failed for competency {}: {}",
                        competencyId, e.getMessage());
            }
            record.setUpdatedAt(Instant.now());
            certificateRepository.save(record);
        } catch (RuntimeException e) {
            // Isolate each competency so one bad row does not stop the others.
            log.error("Competency certificate bookkeeping failed for competency {}: {}",
                    competencyId, e.getMessage());
        }
    }

    private static String truncate(String message) {
        return message.length() <= MESSAGE_MAX_LENGTH ? message : message.substring(0, MESSAGE_MAX_LENGTH);
    }

    private static UserCompetencyCertificate newRecord(String userId, String competencyId) {
        UserCompetencyCertificate record = new UserCompetencyCertificate();
        record.setUserId(userId);
        record.setCompetencyId(competencyId);
        record.setEventId(UUID.randomUUID());
        record.setCompletedAt(Instant.now());
        return record;
    }
}