package com.codemate.security;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Configuration for the SecurityContextHolder strategy
 */
@Slf4j
@Configuration
public class SecurityContextHolderConfig {

    /**
     * Configure the SecurityContextHolder strategy
     * MODE_THREADLOCAL - Each thread has its own SecurityContext (default)
     * MODE_INHERITABLETHREADLOCAL - Child threads inherit the SecurityContext from the parent thread
     * MODE_GLOBAL - All threads share the same SecurityContext (not recommended for web applications)
     */
    @PostConstruct
    public void configureSecurityContextHolderStrategy() {
        // Use MODE_INHERITABLETHREADLOCAL to ensure child threads (e.g., async tasks)
        // inherit the SecurityContext from the parent thread
        SecurityContextHolder.setStrategyName(SecurityContextHolder.MODE_INHERITABLETHREADLOCAL);
        log.info("SecurityContextHolder strategy set to: {}", SecurityContextHolder.getContextHolderStrategy().getClass().getSimpleName());
    }
} 