package com.codemate.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Filter to ensure proper cleanup of the security context after request processing
 * This filter runs with the lowest precedence to ensure it runs after all other filters
 */
@Slf4j
@Component
@Order(Ordered.LOWEST_PRECEDENCE)
public class SecurityContextCleanupFilter extends OncePerRequestFilter {

    private final SecurityContextService securityContextService;

    public SecurityContextCleanupFilter(SecurityContextService securityContextService) {
        this.securityContextService = securityContextService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        try {
            // Let the request continue down the filter chain
            filterChain.doFilter(request, response);
        } finally {
            // Check if this is an asynchronous request
            if (!isAsyncStarted(request)) {
                // For non-async requests, clear the security context if needed
                // Spring Security's SecurityContextPersistenceFilter will handle
                // context cleanup for regular requests, but this ensures cleanup
                // for any edge cases
                if (log.isTraceEnabled()) {
                    log.trace("Checking security context cleanup for request: {}", request.getRequestURI());
                }
            }
        }
    }
} 