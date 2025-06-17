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
import java.util.Arrays;
import java.util.List;

/**
 * Filter to prevent redirect loops by tracking the number of redirects in the session
 * This filter runs before the Spring Security filters
 */
@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RedirectLoopPreventionFilter extends OncePerRequestFilter {

    // List of paths that are likely to be involved in redirects
    private static final List<String> REDIRECT_PATHS = Arrays.asList(
            "/login", "/oauth2/", "/api/auth/", "/dashboard", "/signup"
    );
    
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        
        String path = request.getServletPath();
        
        // Only check for redirect loops on paths that are likely to be involved in redirects
        if (isRedirectPath(path)) {
            if (RedirectLoopPreventer.isRedirectLoop(request)) {
                log.warn("Detected potential redirect loop on path: {}", path);
                RedirectLoopPreventer.handleRedirectLoop(request, response);
                return;
            }
        } else {
            // Reset redirect count for non-redirect paths
            RedirectLoopPreventer.resetRedirectCount(request);
        }
        
        filterChain.doFilter(request, response);
    }
    
    /**
     * Checks if the given path is likely to be involved in redirects
     * 
     * @param path the path to check
     * @return true if the path is likely to be involved in redirects, false otherwise
     */
    private boolean isRedirectPath(String path) {
        return REDIRECT_PATHS.stream().anyMatch(path::startsWith);
    }
} 