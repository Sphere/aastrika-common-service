package org.aastrika.service.impl;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.aastrika.common.Constants;
import org.aastrika.client.CompositeSearchClient;
import org.aastrika.dto.request.CourseSearchRequest;
import org.aastrika.dto.response.CourseSearchResponse;
import org.aastrika.service.CourseSearchService;
import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;

/**
 * Faithful port of the source {@code Recommendation_impl.get_coursesearch}.
 *
 * <p>The source branched into four near-identical blocks on whether {@code search_text} and
 * {@code language} were blank. They collapse to one query with two conditional clauses — the filters
 * ({@code status}, {@code primaryCategory}), the {@code competency} exclusion, the
 * {@code lastUpdatedOn.raw} sort and the pagination are identical in all four; only the
 * {@code multi_match} (added when {@code search_text} is present) and the {@code lang} match (added
 * when {@code language} is present) vary. Same request to the index, one code path.
 */
@Service
@Slf4j
public class CourseSearchServiceImpl implements CourseSearchService {

    /** The source's fallback page size when {@code limit} is null or <= 0. */
    private static final int DEFAULT_LIMIT = 25;
    private static final String SORT_FIELD = "lastUpdatedOn.raw";

    private final CompositeSearchClient compositeSearchClient;

    public CourseSearchServiceImpl(CompositeSearchClient compositeSearchClient) {
        this.compositeSearchClient = compositeSearchClient;
    }

    @Override
    @SuppressWarnings("unchecked")
    public CourseSearchResponse searchCourses(CourseSearchRequest request) {
        CompositeSearchClient.SearchHits hits = compositeSearchClient.search(buildQuery(request));
        log.info("publicSearch/getcourse: searchText='{}', language='{}', hits={}, totalCount={}",
                request.getSearchText(), request.getLanguage(), hits.content().size(), hits.totalCount());

        List<Map<String, Object>> content = new ArrayList<>(hits.content().size());
        for (Map<String, Object> document : hits.content()) {
            content.add((Map<String, Object>) stripNullValues(document));
        }
        return CourseSearchResponse.of(content, hits.totalCount());
    }

    /**
     * Drops null-valued object members at every level, which is what callers already receive: the
     * source serialised each hit through {@code org.json.JSONObject}, whose {@code Map} constructor
     * skips null values (recursively, via {@code wrap}). So a batch with no {@code endDate} arrived
     * with the key absent, not as {@code "endDate": null} — Jackson would emit the latter.
     *
     * <p>Nulls held directly in an array are kept: {@code org.json} mapped those to
     * {@code JSONObject.NULL}, which serialises as {@code null}.
     */
    private static Object stripNullValues(Object value) {
        if (value instanceof Map<?, ?> map) {
            Map<String, Object> cleaned = new LinkedHashMap<>();
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                if (entry.getValue() != null) {
                    cleaned.put(String.valueOf(entry.getKey()), stripNullValues(entry.getValue()));
                }
            }
            return cleaned;
        }
        if (value instanceof List<?> list) {
            List<Object> cleaned = new ArrayList<>(list.size());
            for (Object item : list) {
                cleaned.add(item == null ? null : stripNullValues(item));
            }
            return cleaned;
        }
        return value;
    }

    private Map<String, Object> buildQuery(CourseSearchRequest request) {
        List<Map<String, Object>> must = new ArrayList<>();

        if (isPresent(request.getSearchText())) {
            List<String> fields = request.getSearchFieldNames();
            if (fields == null || fields.isEmpty()) {
                // The source passed the null array straight to the client and died with an NPE;
                // reject it up front instead (handled as 400 by ApplicationExceptionHandler).
                throw new IllegalArgumentException("search_fieldnames is required when search_text is provided");
            }
            must.add(Map.of("multi_match", Map.of(
                    "query", request.getSearchText(),
                    Constants.FIELDS, fields,
                    "type", "phrase")));
        }
        must.add(match("status", request.getCourseStatus()));
        must.add(match("primaryCategory", request.getContentType()));
        if (isPresent(request.getLanguage())) {
            must.add(match("lang", request.getLanguage()));
        }

        Map<String, Object> bool = new LinkedHashMap<>();
        bool.put("must", must);
        bool.put("must_not", List.of(match("competency", true)));

        Map<String, Object> query = new LinkedHashMap<>();
        query.put("query", Map.of("bool", bool));
        query.put("sort", List.of(Map.of(SORT_FIELD, Map.of("order", "desc"))));

        // Pagination, kept bug-compatible with the source: a positive offset is sent as offset - 1,
        // so offset=1 and offset=0 both start at the first hit. Existing callers page against this
        // behaviour — do not "fix" it here without coordinating a contract change.
        Integer offset = request.getOffset();
        if (offset != null && offset > 0) {
            query.put("from", offset - 1);
        }
        Integer limit = request.getLimit();
        query.put("size", limit != null && limit > 0 ? limit : DEFAULT_LIMIT);

        return query;
    }

    private static Map<String, Object> match(String field, Object value) {
        return Map.of("match", Map.of(field, value));
    }

    /** Null/empty check matching the source exactly — a whitespace-only value counts as present. */
    private static boolean isPresent(String value) {
        return value != null && !value.isEmpty();
    }
}
