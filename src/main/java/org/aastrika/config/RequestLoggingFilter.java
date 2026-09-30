package org.aastrika.config;

import java.io.IOException;

import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.servlet.HandlerMapping;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;

/**
 * Logs one line per HTTP request: method, route, response status, and duration.
 *
 * <p>Logs the resolved route <b>pattern</b> (e.g. {@code /ratings/v2/summary/{activityId}/
 * {activityType}}), never the resolved path with real path-variable values, and never
 * headers/body/query string — several endpoints carry a user id as a path variable, and
 * SECURITY.md 2.5 forbids logging PII (user ids that map to individuals) at any level.
 */
@Component
@Slf4j
public class RequestLoggingFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        long start = System.currentTimeMillis();
        try {
            filterChain.doFilter(request, response);
        } finally {
            Object pattern = request.getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE);
            String route = pattern != null ? pattern.toString() : request.getRequestURI();
            long duration = System.currentTimeMillis() - start;
            log.info("{} {} -> {} ({}ms)", request.getMethod(), route, response.getStatus(), duration);
        }
    }
}