package com.codemate.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * Validates JWT configuration at application startup
 */
@Slf4j
@Component
public class JwtConfigurationValidator {

    @Value("${app.jwt.secret}")
    private String jwtSecret;

    @Value("${app.jwt.expiration}")
    private int jwtExpirationInMs;
    
    @Value("${app.jwt.cookie.secure:true}")
    private boolean cookieSecure;
    
    private final Environment environment;
    
    // Common default/placeholder secrets that should be changed
    private static final String[] INSECURE_SECRETS = {
        "your-256-bit-secret-key-here",
        "default",
        "changeme",
        "secret",
        "password",
        "mysecret",
        "jwt-secret",
        "jwt_secret",
        "jwtsecret",
        "jwtkey"
    };
    
    public JwtConfigurationValidator(Environment environment) {
        this.environment = environment;
    }
    
    /**
     * Validates JWT configuration when the application has started
     */
    @EventListener(ApplicationStartedEvent.class)
    public void validateJwtConfiguration() {
        log.info("Validating JWT configuration...");
        
        validateJwtSecret();
        validateJwtExpiration();
        validateCookieSettings();
        
        log.info("JWT configuration validation completed");
    }
    
    /**
     * Validates the JWT secret key
     */
    private void validateJwtSecret() {
        // Check if secret is null or empty
        if (jwtSecret == null || jwtSecret.trim().isEmpty()) {
            log.error("JWT secret key is not set! Application may not function correctly.");
            return;
        }
        
        // Check if secret is Base64 encoded
        try {
            Base64.getDecoder().decode(jwtSecret);
            log.debug("JWT secret appears to be Base64 encoded");
        } catch (IllegalArgumentException e) {
            // Not Base64 encoded, check raw byte length
            byte[] secretBytes = jwtSecret.getBytes(StandardCharsets.UTF_8);
            if (secretBytes.length < 32) {
                log.warn("JWT secret key is too short! It should be at least 32 bytes (256 bits) long.");
            }
        }
        
        // Check for common insecure secrets
        for (String insecureSecret : INSECURE_SECRETS) {
            if (jwtSecret.contains(insecureSecret)) {
                log.error("JWT secret key contains an insecure default value: '{}'. This is a security risk!", insecureSecret);
                break;
            }
        }
        
        // Check if secret is set from environment variable
        if (environment.getProperty("JWT_SECRET") == null) {
            log.warn("JWT secret is not set from an environment variable. This is less secure for production environments.");
        }
    }
    
    /**
     * Validates the JWT expiration time
     */
    private void validateJwtExpiration() {
        if (jwtExpirationInMs <= 0) {
            log.error("JWT expiration time is invalid: {} ms. Must be greater than 0.", jwtExpirationInMs);
        } else if (jwtExpirationInMs > 604800000) { // 7 days
            log.warn("JWT expiration time is set to {} ms ({} days), which is longer than recommended (7 days max).",
                    jwtExpirationInMs, jwtExpirationInMs / 86400000.0);
        } else {
            log.info("JWT expiration time set to {} ms ({} hours).", 
                    jwtExpirationInMs, jwtExpirationInMs / 3600000.0);
        }
    }
    
    /**
     * Validates cookie settings
     */
    private void validateCookieSettings() {
        boolean isProduction = environment.acceptsProfiles(Profiles.of("prod", "production"));
        
        if (isProduction && !cookieSecure) {
            log.error("Cookie 'secure' flag is set to false in production environment! This is a security risk.");
        }
        
        if (!cookieSecure) {
            log.warn("Cookie 'secure' flag is set to false. JWT cookies should be secure (HTTPS only) in production.");
        }
    }
} 