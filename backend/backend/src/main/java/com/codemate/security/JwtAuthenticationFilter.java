package com.codemate.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Optional;

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
        // Skip JWT filter for non-API paths and authentication endpoints
        return !path.startsWith("/api/") || 
               path.startsWith("/api/auth/") || 
               path.startsWith("/api/oauth2/") ||
               path.startsWith("/api/public/");
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request, 
                                  @NonNull HttpServletResponse response, 
                                  @NonNull FilterChain filterChain)
            throws ServletException, IOException {
        try {
            // Try to get JWT from Authorization header first (API clients)
            String jwt = getJwtFromHeader(request);
            
            // If not found in header, try to get from cookie (browser clients)
            if (!StringUtils.hasText(jwt)) {
                jwt = getJwtFromCookie(request);
                if (StringUtils.hasText(jwt)) {
                    log.debug("JWT token found in cookie");
                }
            } else {
                log.debug("JWT token found in Authorization header");
            }

            if (StringUtils.hasText(jwt)) {
                if (tokenProvider.validateToken(jwt)) {
                    Long userId = tokenProvider.getUserIdFromJWT(jwt);
                    log.debug("Processing JWT token for user ID: {}", userId);

                    UserDetails userDetails = customUserDetailsService.loadUserById(userId);
                    
                    // Use the SecurityContextService to set the authentication
                    securityContextService.createAndSetAuthentication(userDetails, request);
                    log.debug("User authenticated successfully: {}", userDetails.getUsername());
                }
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
    
    /**
     * Extract JWT token from cookies
     * 
     * @param request the HTTP request
     * @return the JWT token or null if not found
     */
    private String getJwtFromCookie(HttpServletRequest request) {
        Optional<Cookie> jwtCookie = CookieUtils.getJwtCookie(request);
        return jwtCookie.map(Cookie::getValue).orElse(null);
    }
} 