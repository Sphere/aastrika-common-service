package org.aastrika.client;

import java.util.LinkedHashMap;
import java.util.Map;

import org.aastrika.common.Constants;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import lombok.extern.slf4j.Slf4j;

/**
 * Reads an org's form config from the portal form service ({@code /v1/form/read}, served by the
 * UI proxy). Unauthenticated. Returns {@code result.form.data}, or {@code null} if the call fails
 * or the org has no form — an unknown rootOrgId comes back as ERR_READ_FORM_DATA, not an empty form.
 */
@Component
@Slf4j
public class FormClient {

    private final RestTemplate restTemplate;
    private final String readUrl;
    private final Map<String, String> requestTemplate;

    public FormClient(
            RestTemplate contentRestTemplate,
            @Value("${form.read-url:https://ekshamata-staging.aastrika.org/apis/v1/form/read}") String readUrl,
//            @Value("${form.read-url:http://localhost:3000/v1/form/read}") String readUrl,
            @Value("${form.read.type:web_layout}") String type,
            @Value("${form.read.subtype:v1}") String subtype,
            @Value("${form.read.action:get}") String action,
            @Value("${form.read.component:ekshamata}") String component,
            @Value("${form.read.framework:v2}") String framework) {
        this.restTemplate = contentRestTemplate;
        this.readUrl = readUrl;
        this.requestTemplate = Map.of("type", type, "subtype", subtype, "action", action,
                "component", component, "framework", framework);
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> readFormData(String rootOrgId) {
        Map<String, Object> request = new LinkedHashMap<>(requestTemplate);
        request.put("rootOrgId", rootOrgId);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        try {
            Map<String, Object> response = restTemplate.exchange(readUrl, HttpMethod.POST,
                    new HttpEntity<>(Map.of(Constants.REQUEST, request), headers), Map.class).getBody();
            if (response == null
                    || !(response.get("result") instanceof Map<?, ?> result)
                    || !(((Map<String, Object>) result).get("form") instanceof Map<?, ?> form)) {
                return null;
            }
            Object data = ((Map<String, Object>) form).get("data");
            return data instanceof Map<?, ?> map ? (Map<String, Object>) map : null;
        } catch (RestClientException e) {
            log.warn("Form read failed for org {}: {}", rootOrgId, e.getMessage());
            return null;
        }
    }
}