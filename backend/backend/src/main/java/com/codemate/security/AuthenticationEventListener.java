package com.codemate.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.security.authentication.event.*;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

/**
 * Listener for Spring Security authentication events
 */
@Slf4j
@Component
public class AuthenticationEventListener {

    /**
     * Handles successful authentication events
     *
     * @param event the authentication success event
     */
    @EventListener
    public void onSuccess(AuthenticationSuccessEvent event) {
        Authentication authentication = event.getAuthentication();
        log.info("User '{}' authenticated successfully", authentication.getName());
    }

    /**
     * Handles failed authentication events
     *
     * @param event the authentication failure event
     */
    @EventListener
    public void onFailure(AbstractAuthenticationFailureEvent event) {
        String username = event.getAuthentication().getName();
        String errorMessage = event.getException().getMessage();
        log.warn("Authentication failed for user '{}': {}", username, errorMessage);
    }

    /**
     * Handles interactive login attempts
     *
     * @param event the interactive authentication event
     */
    @EventListener
    public void onInteractiveLogin(InteractiveAuthenticationSuccessEvent event) {
        Authentication authentication = event.getAuthentication();
        log.info("Interactive login successful for user '{}'", authentication.getName());
    }

    /**
     * Handles logout events
     *
     * @param event the logout success event
     */
    @EventListener
    public void onLogout(LogoutSuccessEvent event) {
        Authentication authentication = event.getAuthentication();
        if (authentication != null) {
            log.info("User '{}' logged out successfully", authentication.getName());
        } else {
            log.info("Anonymous user logged out");
        }
    }
} 