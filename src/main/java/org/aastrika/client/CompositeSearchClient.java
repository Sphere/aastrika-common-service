package org.aastrika.client;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.aastrika.exception.ApiException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import lombok.extern.slf4j.Slf4j;

/**
 * Read-only client for the composite-search index that backs the public course search.
 *
 * <p>The query is sent as raw JSON to {@code POST {url}/{index}/_search} over the shared
 * {@link RestTemplate} rather than through a high-level search client: only {@code _source} and a
 * hit count are needed here, so the extra client buys nothing.
 *
 * <p>The cluster is <b>OpenSearch</b> — the same one {@code OpenSearchConfig} points its
 * {@code RestHighLevelClient} at for user autocomplete — but nothing here depends on that:
 * {@link #search} reads either {@code hits.total} shape, so it works against Elasticsearch 6.x,
 * Elasticsearch 7+ and OpenSearch alike. Keep that if you change the parsing.
 */
@Component
@Slf4j
public class CompositeSearchClient {

    private static final String API_ID = "api.public.search.course";

    private final RestTemplate restTemplate;
    private final String searchUrl;

    public CompositeSearchClient(
            RestTemplate searchRestTemplate,
            @Value("${composite-search.url:http://localhost:9201}") String url,
            @Value("${composite-search.index:compositesearch}") String index) {
        this.restTemplate = searchRestTemplate;
        this.searchUrl = trimTrailingSlash(url) + "/" + index + "/_search";
    }

    /** Total hits plus the raw {@code _source} of each hit, in index order. */
    public record SearchHits(List<Map<String, Object>> content, int totalCount) {
    }

    /**
     * Runs a search-request body against the index. Any transport/cluster failure is surfaced as a
     * {@code 502} rather than an empty result set, so a search outage is never mistaken for
     * "no courses found".
     */
    @SuppressWarnings("unchecked")
    public SearchHits search(Map<String, Object> queryBody) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> body;
        try {
            ResponseEntity<Map> response =
                    restTemplate.postForEntity(searchUrl, new HttpEntity<>(queryBody, headers), Map.class);
            body = response.getBody();
        } catch (RestClientException e) {
            log.error("composite-search query failed against {}: {}", searchUrl, e.getMessage());
            throw new ApiException(API_ID, HttpStatus.BAD_GATEWAY,
                    "Course search is unavailable: " + e.getMessage());
        }

        Map<String, Object> hits = body == null ? null : (Map<String, Object>) body.get("hits");
        if (hits == null) {
            log.warn("composite-search returned no hits section: {}", body);
            return new SearchHits(List.of(), 0);
        }

        List<Map<String, Object>> content = new ArrayList<>();
        List<Map<String, Object>> hitList = (List<Map<String, Object>>) hits.get("hits");
        if (hitList != null) {
            for (Map<String, Object> hit : hitList) {
                Map<String, Object> source = (Map<String, Object>) hit.get("_source");
                if (source != null) {
                    content.add(source);
                }
            }
        }
        return new SearchHits(content, readTotal(hits.get("total")));
    }

    /** ES 6.x reports {@code hits.total} as a number; ES 7+/OpenSearch as {@code {value, relation}}. */
    @SuppressWarnings("unchecked")
    private static int readTotal(Object total) {
        if (total instanceof Number number) {
            return number.intValue();
        }
        if (total instanceof Map<?, ?> map) {
            Object value = ((Map<String, Object>) map).get("value");
            if (value instanceof Number number) {
                return number.intValue();
            }
        }
        return 0;
    }

    private static String trimTrailingSlash(String url) {
        return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }
}
