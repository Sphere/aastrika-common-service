package org.aastrika.client;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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
 * HTTP client for the content platform used by the ratings meta-update (#6) and additional-tag (#7)
 * jobs: reads content metadata, searches content by tag, and writes back via the system-update PATCH.
 * Content read targets the real content service; search + update are configurable (mocked for now).
 */
@Component
@Slf4j
public class ContentClient {

    private final RestTemplate restTemplate;
    private final String readUrl;
    private final String searchUrl;
    private final String updateUrl;
    private final int searchLimit;

    public ContentClient(
            RestTemplate contentRestTemplate,
            @Value("${content.read-url:http://localhost:9000/content/v4/read}") String readUrl,
            @Value("${content.search-url:http://localhost:8080/v1/search}") String searchUrl,
            @Value("${content.update-url:http://localhost:8080/system/v3/content/update/}") String updateUrl,
            @Value("${content.search-limit:200}") int searchLimit) {
        this.restTemplate = contentRestTemplate;
        this.readUrl = readUrl;
        this.searchUrl = searchUrl;
        this.updateUrl = updateUrl;
        this.searchLimit = searchLimit;
    }

    /** Reads a content's metadata; returns the {@code result.content} map, or null on miss/error. */
    @SuppressWarnings("unchecked")
    public Map<String, Object> readContent(String contentId, List<String> fields) {
        String url = readUrl + "/" + contentId;
        if (fields != null && !fields.isEmpty()) {
            url += "?fields=" + String.join(",", fields);
        }
        try {
            Map<String, Object> body = restTemplate.getForObject(url, Map.class);
            if (body != null && "OK".equalsIgnoreCase(String.valueOf(body.get("responseCode")))) {
                Map<String, Object> result = (Map<String, Object>) body.get("result");
                return result == null ? null : (Map<String, Object>) result.get("content");
            }
        } catch (RestClientException e) {
            log.warn("readContent failed for {}: {}", contentId, e.getMessage());
        }
        return null;
    }

    /** Returns the content currently tagged with {@code tag} (paged through {@code result.count}). */
    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> searchContent(String tag) {
        List<Map<String, Object>> all = new ArrayList<>();
        int count;
        int guard = 0;
        do {
            Map<String, Object> filters = new LinkedHashMap<>();
            filters.put("status", List.of("Live"));
            filters.put("additionalTags", List.of(tag));
            Map<String, Object> req = new LinkedHashMap<>();
            req.put("filters", filters);
            req.put("fields", List.of("identifier"));
            req.put("limit", searchLimit);
            req.put("offset", all.size());
            Map<String, Object> requestBody = Map.of("request", req);

            Map<String, Object> body;
            try {
                body = restTemplate.postForObject(searchUrl, new HttpEntity<>(requestBody, jsonHeaders()), Map.class);
            } catch (RestClientException e) {
                log.warn("searchContent failed for tag {}: {}", tag, e.getMessage());
                break;
            }
            Map<String, Object> result = body == null ? null : (Map<String, Object>) body.get("result");
            if (result == null) {
                break;
            }
            count = ((Number) result.getOrDefault("count", 0)).intValue();
            List<Map<String, Object>> content = (List<Map<String, Object>>) result.get("content");
            if (content != null) {
                all.addAll(content);
            }
            guard++;
        } while (count > all.size() && guard < 20);
        return all;
    }

    /** Writes content metadata via the system-update PATCH; returns true on {@code responseCode == OK}. */
    @SuppressWarnings("unchecked")
    public boolean updateContentMeta(String contentId, Map<String, Object> contentValues) {
        Map<String, Object> body = Map.of("request", Map.of("content", contentValues));
        try {
            Map<String, Object> response = restTemplate.exchange(
                    updateUrl + contentId, HttpMethod.PATCH,
                    new HttpEntity<>(body, jsonHeaders()), Map.class).getBody();
            return response != null && "OK".equalsIgnoreCase(String.valueOf(response.get("responseCode")));
        } catch (RestClientException e) {
            log.warn("updateContentMeta failed for {}: {}", contentId, e.getMessage());
            return false;
        }
    }

    private static HttpHeaders jsonHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }
}
