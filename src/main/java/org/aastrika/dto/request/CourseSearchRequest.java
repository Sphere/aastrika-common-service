package org.aastrika.dto.request;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request body of {@code POST /publicSearch/getcourse}, migrated verbatim from the source
 * {@code GetCourse} model — snake_case field names are part of the published contract and are kept
 * as-is.
 *
 * <p>{@code resourceType}, {@code sorting_fieldname} and {@code primaryCategory} are accepted for
 * contract compatibility but are NOT used to build the query: the source ignored them too (results
 * are always sorted by {@code lastUpdatedOn.raw} descending, and {@code contentType} — not
 * {@code primaryCategory} — is what filters the index's {@code primaryCategory} field).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CourseSearchRequest {

    /** Free text; when blank the search degrades to a filter-only query (source behaviour). */
    @JsonProperty("search_text")
    private String searchText;

    /** Fields the free text is matched against. Required only when {@code search_text} is given. */
    @JsonProperty("search_fieldnames")
    private List<String> searchFieldNames;

    /** Matched against the index's {@code status} field, e.g. {@code Live}. */
    @JsonProperty("course_status")
    @NotBlank
    private String courseStatus;

    /** Accepted and ignored — kept so existing callers' payloads stay valid. */
    @JsonProperty("resourceType")
    private String resourceType;

    /** Matched against the index's {@code primaryCategory} field, e.g. {@code Course}. */
    @JsonProperty("contentType")
    @NotBlank
    private String contentType;

    /** Accepted and ignored — the sort is fixed to {@code lastUpdatedOn.raw} desc. */
    @JsonProperty("sorting_fieldname")
    private String sortingFieldName;

    /** Matched against the index's {@code lang} field; blank means "any language". */
    @JsonProperty("language")
    private String language;

    /** Accepted and ignored — see the class javadoc. */
    @JsonProperty("primaryCategory")
    private String primaryCategory;

    @JsonProperty("offset")
    private Integer offset = 0;

    @JsonProperty("limit")
    private Integer limit;
}
