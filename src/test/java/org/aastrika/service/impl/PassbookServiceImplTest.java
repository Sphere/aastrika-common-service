package org.aastrika.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import org.aastrika.dto.request.AcquiredDetail;
import org.aastrika.dto.request.CompetencyDetail;
import org.aastrika.dto.request.PassbookUpdateRequest;
import org.aastrika.entity.UserPassbook;
import org.aastrika.repository.UserPassbookRepository;
import org.aastrika.service.CompetencyCertificateService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/**
 * Date handling on passbook update must match sb-cb-ext (JVM in UTC), so rows written by either
 * service line up: zone-less effectiveDate is UTC, createdDate is UTC with milliseconds.
 */
class PassbookServiceImplTest {

    private static final String TARGET_USER = "target-user";
    private static final String ACTING_USER = "acting-user";

    private UserPassbookRepository repository;
    private PassbookServiceImpl service;

    @BeforeEach
    void setUp() {
        repository = mock(UserPassbookRepository.class);
        when(repository.findByKeyUserIdAndKeyTypeName(anyString(), anyString())).thenReturn(List.of());
        service = new PassbookServiceImpl(repository, mock(CompetencyCertificateService.class), List.of("competency"));
    }

    @Test
    void zoneLessEffectiveDateIsReadAsUtc() {
        UserPassbook saved = update("2026-11-11 10:00:00");
        assertThat(saved.getKey().getEffectiveDate()).isEqualTo(Instant.parse("2026-11-11T10:00:00Z"));
    }

    @Test
    void isoLocalEffectiveDateIsReadAsUtc() {
        UserPassbook saved = update("2026-11-11T10:00:00");
        assertThat(saved.getKey().getEffectiveDate()).isEqualTo(Instant.parse("2026-11-11T10:00:00Z"));
    }

    @Test
    void effectiveDateWithOffsetKeepsItsInstant() {
        UserPassbook saved = update("2026-11-11T04:30:00Z");
        assertThat(saved.getKey().getEffectiveDate()).isEqualTo(Instant.parse("2026-11-11T04:30:00Z"));
    }

    @Test
    void createdDateIsUtcWithMillis() {
        UserPassbook saved = update(null);
        assertThat(saved.getAcquiredDetails().get("createdDate"))
                .matches("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}\\.\\d{3}Z");
        assertThat(saved.getAcquiredDetails().get("createdBy")).isEqualTo(ACTING_USER);
    }

    @SuppressWarnings("unchecked")
    private UserPassbook update(String effectiveDate) {
        AcquiredDetail acquired = new AcquiredDetail("course", "5", effectiveDate, Map.of("courseId", "do_1"));
        CompetencyDetail detail = new CompetencyDetail("100", acquired, Map.of("competencyName", "X"));
        PassbookUpdateRequest request = new PassbookUpdateRequest(
                new PassbookUpdateRequest.Payload(TARGET_USER, "competency", List.of(detail)));

        service.updatePassbook(ACTING_USER, request);

        ArgumentCaptor<List<UserPassbook>> captor = ArgumentCaptor.forClass(List.class);
        verify(repository).saveAll(captor.capture());
        return captor.getValue().get(0);
    }
}
