package org.aastrika.dto.response;

import java.util.List;
import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response body of {@code POST /publicSearch/getcourse}:
 * {@code {"results": {"content": [ ...raw index documents... ], "totalCount": n}}}.
 *
 * <p>Deliberately NOT wrapped in {@link AppResponse}: this endpoint is a drop-in replacement for the
 * recommendation service's {@code /publicSearch/getcourse}, so existing callers keep working by
 * changing only the host. It is the one endpoint in this service that does not use the envelope.
 *
 * <p>{@code content} entries are the search index documents verbatim (no projection), matching the
 * source, which put {@code hit.getSourceAsMap()} straight into the array.
 *
 * <p>One deliberate difference from the source: {@code content} is ALWAYS present, as {@code []} when
 * nothing matches. The source built the array inside its hit loop in three of its four query
 * branches, so a zero-hit search there returned {@code {"results":{"totalCount":0}}} with no
 * {@code content} key at all — the key's presence depended on which branch ran. Always emitting the
 * array is a superset of both shapes: callers that guarded the missing key still work.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CourseSearchResponse {

    private Results results;

    public static CourseSearchResponse of(List<Map<String, Object>> content, int totalCount) {
        return new CourseSearchResponse(new Results(content, totalCount));
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Results {
        private List<Map<String, Object>> content;
        private int totalCount;
    }
}
