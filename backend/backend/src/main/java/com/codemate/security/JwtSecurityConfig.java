package com.codemate.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;

import jakarta.annotation.PostConstruct;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * Configuration class for JWT security settings
 */
@Slf4j
@Configuration
public class JwtSecurityConfig {

    @Value("${app.jwt.secret:}")
    private String jwtSecret;

    @Value("${app.jwt.expiration:86400000}")
    private int jwtExpirationInMs;
    
    private final Environment environment;
    
    public JwtSecurityConfig(Environment environment) {
        this.environment = environment;
    }

    @PostConstruct
    public void init() {
        validateJwtConfiguration();
    }
    
    /**
     * Validates the JWT configuration at startup
     */
    private void validateJwtConfiguration() {
        // Check expiration time
        if (jwtExpirationInMs <= 0) {
            log.warn("JWT expiration time is set to {} ms, which is invalid. Using default: 86400000 ms (24 hours)",
                    jwtExpirationInMs);
        } else if (jwtExpirationInMs > 604800000) { // 7 days
            log.warn("JWT expiration time is set to {} ms, which is longer than recommended (7 days max)",
                    jwtExpirationInMs);
        } else {
            log.info("JWT expiration time set to {} ms ({} hours)", 
                    jwtExpirationInMs, jwtExpirationInMs / 3600000.0);
        }
        
        // Check if we're in a development environment
        boolean isDev = environment.acceptsProfiles(Profiles.of("dev", "development", "local"));
        if (isDev) {
            log.info("Running in development mode - JWT security validations are less strict");
        }
    }
    
    /**
     * Generates a secure random JWT secret key
     * Only used if the 'app.jwt.generate-random-secret' property is set to true
     * 
     * @return A Base64-encoded random secret key of sufficient length
     */
    @Bean
    @ConditionalOnProperty(name = "app.jwt.generate-random-secret", havingValue = "true")
    public String generateRandomJwtSecret() {
        byte[] keyBytes = new byte[64]; // 512 bits
        new SecureRandom().nextBytes(keyBytes);
        String generatedSecret = Base64.getEncoder().encodeToString(keyBytes);
        
        log.warn("Generated a random JWT secret key. This should only be used for development/testing!");
        log.warn("For production, set a fixed secret key in environment variables or a secure configuration store.");
        log.info("Generated secret key (save this for future use): {}", generatedSecret);
        
        return generatedSecret;
    }
} 