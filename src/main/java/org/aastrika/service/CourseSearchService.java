package org.aastrika.service;

import org.aastrika.dto.request.CourseSearchRequest;
import org.aastrika.dto.response.CourseSearchResponse;

/**
 * Public course search over the composite-search index, migrated from the recommendation service's
 * {@code /publicSearch/getcourse} ({@code Recommendation_impl.get_coursesearch}).
 */
public interface CourseSearchService {

    /** Live courses matching the request; empty content (not an error) when nothing matches. */
    CourseSearchResponse searchCourses(CourseSearchRequest request);
}
