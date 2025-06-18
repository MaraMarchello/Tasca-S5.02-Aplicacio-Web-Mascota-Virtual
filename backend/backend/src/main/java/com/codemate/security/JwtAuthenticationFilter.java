package com.codemate.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider tokenProvider;
    private final CustomUserDetailsService customUserDetailsService;
    private final SecurityContextService securityContextService;
    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    public JwtAuthenticationFilter(
            JwtTokenProvider tokenProvider, 
            CustomUserDetailsService customUserDetailsService,
            SecurityContextService securityContextService) {
        this.tokenProvider = tokenProvider;
        this.customUserDetailsService = customUserDetailsService;
        this.securityContextService = securityContextService;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getServletPath();
        // Skip JWT filter for static resources, auth, and OAuth2 endpoints
        return path.equals("/api/auth/login") ||
                path.equals("/api/auth/form-login") ||
                path.equals("/api/auth/signup") ||
                path.equals("/api/auth/logout") ||
                path.startsWith("/api/oauth2/") ||
                path.startsWith("/oauth2/") ||
                path.startsWith("/api/public/") ||
                path.equals("/login") ||
                path.equals("/signup") ||
                path.startsWith("/css/") ||
                path.startsWith("/js/") ||
                path.startsWith("/images/");
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request, 
                                  @NonNull HttpServletResponse response, 
                                  @NonNull FilterChain filterChain)
            throws ServletException, IOException {
        try {
            // Get JWT from Authorization header
            String jwt = getJwtFromHeader(request);
            
            if (StringUtils.hasText(jwt)) {
                log.debug("JWT token found in Authorization header for path: {}", request.getServletPath());
            } else {
                log.debug("No JWT token found in Authorization header for path: {}", request.getServletPath());
            }

            if (StringUtils.hasText(jwt) && tokenProvider.validateToken(jwt)) {
                Long userId = tokenProvider.getUserIdFromJWT(jwt);
                log.debug("Processing JWT token for user ID: {}", userId);

                UserDetails userDetails = customUserDetailsService.loadUserById(userId);
                
                // Use the SecurityContextService to set the authentication
                securityContextService.createAndSetAuthentication(userDetails, request);
                log.debug("User authenticated successfully: {}", userDetails.getUsername());
            }
        } catch (Exception ex) {
            log.error("Could not set user authentication in security context: {}", ex.getMessage(), ex);
            // Don't throw the exception to allow the request to proceed to other filters
            // The user will be treated as unauthenticated
        }

        filterChain.doFilter(request, response);
    }

    /**
     * Extract JWT token from Authorization header
     * 
     * @param request the HTTP request
     * @return the JWT token or null if not found
     */
    private String getJwtFromHeader(HttpServletRequest request) {
        String bearerToken = request.getHeader(AUTHORIZATION_HEADER);
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith(BEARER_PREFIX)) {
            return bearerToken.substring(7);
        }
        return null;
    }
    

} 