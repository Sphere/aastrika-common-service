package org.aastrika.client;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.aastrika.common.Constants;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

/**
 * Generates a certificate through the RC-proxy service
 * ({@code /v1/certificate/generateUserCertificatesFromRc}). Faithful port of program-certification's
 * {@code issue_certificate}: same request body, the service api key as the raw {@code Authorization}
 * header, and the date as today in UTC ({@code dd-MM-yyyy}). The URL is read from
 * {@code result.certificateUrl}, falling back to a top-level {@code certificateUrl}.
 *
 * <p>HTTP and I/O failures propagate as {@code RestClientException} so the caller can record the
 * attempt as failed. Nothing is logged here: the request and response carry user identity
 * (SECURITY.md 2.5).
 */
@Component
public class RcCertificateClient {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd-MM-yyyy");

    private final RestTemplate restTemplate;
    private final String generateUrl;
    private final String apiKey;

    public RcCertificateClient(
            RestTemplate contentRestTemplate,
            @Value("${rc.certificate.generate-url:http://localhost:3011/v1/certificate/generateUserCertificatesFromRc}") String generateUrl,
            @Value("${sb.api-key:apiKey}") String apiKey) {
        this.restTemplate = contentRestTemplate;
        this.generateUrl = generateUrl;
        this.apiKey = apiKey;
    }

    /**
     * @param title printed on the certificate (sent as both {@code courseName} and {@code workshop-name})
     * @return the certificate URL, or empty if RC answered without one
     */
    @SuppressWarnings("unchecked")
    public Optional<String> generate(String templateId, String userId, String userName, UUID eventId,
                                     String title) {
        Map<String, Object> certBody = new LinkedHashMap<>();
        certBody.put("date", LocalDate.now(ZoneOffset.UTC).format(DATE_FORMAT));
        certBody.put("name", userName);
        certBody.put("courseName", title);
        certBody.put("workshop-name", title);

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("templateId", templateId);
        body.put("userId", userId);
        body.put("userName", userName);
        body.put("eventId", eventId.toString());
        body.put("rcCertificateGenerationBody", certBody);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set(Constants.AUTH_TOKEN, apiKey);

        Map<String, Object> response = restTemplate.exchange(generateUrl, HttpMethod.POST,
                new HttpEntity<>(body, headers), Map.class).getBody();
        if (response == null) {
            return Optional.empty();
        }
        Object url = response.get("result") instanceof Map<?, ?> result
                ? ((Map<String, Object>) result).get("certificateUrl")
                : null;
        if (!(url instanceof String s) || s.isBlank()) {
            url = response.get("certificateUrl");
        }
        return url instanceof String s && !s.isBlank() ? Optional.of(s) : Optional.empty();
    }
}