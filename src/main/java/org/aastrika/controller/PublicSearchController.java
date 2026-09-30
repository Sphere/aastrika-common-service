package org.aastrika.controller;

import org.aastrika.dto.request.CourseSearchRequest;
import org.aastrika.dto.response.CourseSearchResponse;
import org.aastrika.service.CourseSearchService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;

/**
 * Public (unauthenticated) course search, migrated from the recommendation service's
 * {@code Controller.get_getCourses}. The path and the request/response bodies match the source
 * verbatim so callers switch over by changing the host only — which is why this is the one
 * controller that does not return the {@code AppResponse} envelope.
 */
@RestController
public class PublicSearchController {

    private final CourseSearchService courseSearchService;

    public PublicSearchController(CourseSearchService courseSearchService) {
        this.courseSearchService = courseSearchService;
    }

    /** Search live courses in the composite-search index. */
    @PostMapping("/publicSearch/getcourse")
    public ResponseEntity<CourseSearchResponse> getCourses(@Valid @RequestBody CourseSearchRequest request) {
        return ResponseEntity.ok(courseSearchService.searchCourses(request));
    }
}
