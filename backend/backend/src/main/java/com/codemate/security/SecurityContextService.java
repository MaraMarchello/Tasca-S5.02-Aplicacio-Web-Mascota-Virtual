package com.codemate.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Service;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Service for managing security context operations
 */
@Slf4j
@Service
public class SecurityContextService {

    /**
     * Sets the authentication in the security context
     *
     * @param authentication the authentication to set
     */
    public void setAuthentication(Authentication authentication) {
        SecurityContextHolder.getContext().setAuthentication(authentication);
        log.debug("Set authentication in security context for user: {}", 
                authentication.getName());
    }

    /**
     * Creates and sets authentication in the security context from user details
     *
     * @param userDetails the user details
     * @param request the HTTP request
     */
    public void createAndSetAuthentication(UserDetails userDetails, HttpServletRequest request) {
        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                userDetails, null, userDetails.getAuthorities());
        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
        
        setAuthentication(authentication);
        log.debug("Created and set authentication in security context for user: {}", 
                userDetails.getUsername());
    }

    /**
     * Gets the current authentication from the security context
     *
     * @return the current authentication or null if not authenticated
     */
    public Authentication getCurrentAuthentication() {
        SecurityContext context = SecurityContextHolder.getContext();
        return context.getAuthentication();
    }

    /**
     * Gets the current user principal from the security context
     *
     * @return the current user principal or null if not authenticated
     */
    public UserPrincipal getCurrentUserPrincipal() {
        Authentication authentication = getCurrentAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof UserPrincipal) {
            return (UserPrincipal) authentication.getPrincipal();
        }
        return null;
    }

    /**
     * Gets the current user ID from the security context
     *
     * @return the current user ID or null if not authenticated
     */
    public Long getCurrentUserId() {
        UserPrincipal userPrincipal = getCurrentUserPrincipal();
        return userPrincipal != null ? userPrincipal.getId() : null;
    }

    /**
     * Checks if the current user is authenticated
     *
     * @return true if authenticated, false otherwise
     */
    public boolean isAuthenticated() {
        Authentication authentication = getCurrentAuthentication();
        return authentication != null && authentication.isAuthenticated();
    }

    /**
     * Clears the security context
     */
    public void clearContext() {
        SecurityContextHolder.clearContext();
        log.debug("Cleared security context");
    }
} 