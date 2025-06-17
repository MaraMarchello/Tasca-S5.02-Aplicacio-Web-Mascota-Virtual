package com.codemate.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Utility class for security operations
 */
@Slf4j
public final class SecurityUtils {

    private SecurityUtils() {
        // Private constructor to prevent instantiation
    }

    /**
     * Get the current user ID from the security context
     *
     * @return the current user ID or null if not authenticated
     */
    public static Long getCurrentUserId() {
        UserPrincipal principal = getCurrentUserPrincipal();
        return principal != null ? principal.getId() : null;
    }

    /**
     * Get the current user principal from the security context
     *
     * @return the current user principal or null if not authenticated
     */
    public static UserPrincipal getCurrentUserPrincipal() {
        Authentication authentication = getCurrentAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof UserPrincipal) {
            return (UserPrincipal) authentication.getPrincipal();
        }
        return null;
    }

    /**
     * Get the current authentication from the security context
     *
     * @return the current authentication or null if not authenticated
     */
    public static Authentication getCurrentAuthentication() {
        SecurityContext context = SecurityContextHolder.getContext();
        return context != null ? context.getAuthentication() : null;
    }

    /**
     * Check if the current user is authenticated
     *
     * @return true if authenticated, false otherwise
     */
    public static boolean isAuthenticated() {
        Authentication authentication = getCurrentAuthentication();
        return authentication != null && authentication.isAuthenticated();
    }

    /**
     * Check if the current user has the given authority
     *
     * @param authority the authority to check
     * @return true if the user has the authority, false otherwise
     */
    public static boolean hasAuthority(String authority) {
        Authentication authentication = getCurrentAuthentication();
        if (authentication != null) {
            return authentication.getAuthorities().stream()
                    .anyMatch(grantedAuthority -> grantedAuthority.getAuthority().equals(authority));
        }
        return false;
    }

    /**
     * Check if the current user has any of the given authorities
     *
     * @param authorities the authorities to check
     * @return true if the user has any of the authorities, false otherwise
     */
    public static boolean hasAnyAuthority(String... authorities) {
        Authentication authentication = getCurrentAuthentication();
        if (authentication != null) {
            for (String authority : authorities) {
                if (hasAuthority(authority)) {
                    return true;
                }
            }
        }
        return false;
    }
} 