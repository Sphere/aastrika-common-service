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

import org.aastrika.dto.response.CohortBatch;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;

/**
 * Read-only HTTP client for the content platform. Resolves an assessment's parent and content type
 * from the content hierarchy (used by assessment submit), and looks up a course's batches via the km
 * content-search (used by the cohort endpoints).
 */
@Component
@Slf4j
public class ContentClient {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final RestTemplate restTemplate;
    private final String searchUrl;
    private final String hierarchyUrl;

    public ContentClient(
            RestTemplate contentRestTemplate,
            @Value("${content.search-url:http://localhost:8080/v1/search}") String searchUrl,
            @Value("${content.hierarchy-url:http://localhost:9000/content/v3/hierarchy}") String hierarchyUrl) {
        this.restTemplate = contentRestTemplate;
        this.searchUrl = searchUrl;
        this.hierarchyUrl = hierarchyUrl;
    }

    /** Reads a content's hierarchy ({@code /content/v3/hierarchy/{id}?hierarchyType=detail}); returns
     * the {@code result.content} map or null. Used to resolve an assessment's parent + content type. */
    @SuppressWarnings("unchecked")
    public Map<String, Object> getHierarchyContent(String contentId) {
        String url = hierarchyUrl + "/" + contentId + "?hierarchyType=detail";
        try {
            Map<String, Object> body = restTemplate.getForObject(url, Map.class);
            if (body != null && "OK".equalsIgnoreCase(String.valueOf(body.get("responseCode")))) {
                Map<String, Object> result = (Map<String, Object>) body.get("result");
                return result == null ? null : (Map<String, Object>) result.get("content");
            }
        } catch (RestClientException e) {
            log.warn("getHierarchyContent failed for {}: {}", contentId, e.getMessage());
        }
        return null;
    }

    /** Parent identifier of a content (empty string if none), via the hierarchy read. */
    public String getParentIdentifier(String contentId) {
        Map<String, Object> content = getHierarchyContent(contentId);
        Object parent = content == null ? null : content.get("parent");
        return parent == null ? "" : parent.toString();
    }

    /** Content type of a content (empty string if unresolved), via the hierarchy read. */
    public String getContentType(String contentId) {
        Map<String, Object> content = getHierarchyContent(contentId);
        Object type = content == null ? null : content.get("contentType");
        return type == null ? "" : type.toString();
    }

    /**
     * Batches of a live course/program via the km content-search ({@code result.content[0].batches}).
     * Faithful port of the source {@code searchLiveContent} + {@code fetchBatchDetails}: same filters
     * ({@code primaryCategory in [Course, Program]}, {@code status = Live}, {@code identifier}) and
     * fields. Returns an empty list on miss/error (treated as "no batches" by callers).
     */
    @SuppressWarnings("unchecked")
    public List<CohortBatch> getCourseBatches(String contentId) {
        Map<String, Object> filters = new LinkedHashMap<>();
        filters.put("primaryCategory", List.of("Course", "Program"));
        filters.put("status", List.of("Live"));
        filters.put("identifier", contentId);
        Map<String, Object> req = new LinkedHashMap<>();
        req.put("filters", filters);
        req.put("fields", List.of("identifier", "name", "primaryCategory", "batches", "leafNodesCount", "contentType"));
        Map<String, Object> requestBody = Map.of("request", req);
        try {
            Map<String, Object> body = restTemplate.postForObject(
                    searchUrl, new HttpEntity<>(requestBody, jsonHeaders()), Map.class);
            if (body == null || !"OK".equalsIgnoreCase(String.valueOf(body.get("responseCode")))) {
                return List.of();
            }
            Map<String, Object> result = (Map<String, Object>) body.get("result");
            List<Map<String, Object>> content = result == null ? null : (List<Map<String, Object>>) result.get("content");
            if (content == null || content.isEmpty()) {
                return List.of();
            }
            Object batches = content.get(0).get("batches");
            if (batches == null) {
                return List.of();
            }
            return MAPPER.convertValue(batches, new TypeReference<List<CohortBatch>>() {
            });
        } catch (RestClientException | IllegalArgumentException e) {
            log.warn("getCourseBatches failed for {}: {}", contentId, e.getMessage());
            return List.of();
        }
    }

    private static HttpHeaders jsonHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }
}
