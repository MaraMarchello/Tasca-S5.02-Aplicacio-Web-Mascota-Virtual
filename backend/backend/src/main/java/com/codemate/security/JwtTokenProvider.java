package com.codemate.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SecurityException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.stream.Collectors;

@Slf4j
@Component
public class JwtTokenProvider {

    private final String jwtSecret;
    private final int jwtExpirationInMs;
    private SecretKey key;
    
    // Minimum recommended key size for HMAC SHA-256 is 32 bytes (256 bits)
    private static final int MIN_SECRET_LENGTH = 32;

    public JwtTokenProvider(
            @Value("${app.jwt.secret}") String jwtSecret,
            @Value("${app.jwt.expiration}") int jwtExpirationInMs) {
        this.jwtSecret = jwtSecret;
        this.jwtExpirationInMs = jwtExpirationInMs;
    }

    @PostConstruct
    public void init() {
        validateSecretKey();
        this.key = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
        log.info("JWT token provider initialized with expiration time: {} ms", jwtExpirationInMs);
    }
    
    /**
     * Validates that the JWT secret key meets minimum security requirements
     * 
     * @throws IllegalStateException if the secret key is too short
     */
    private void validateSecretKey() {
        if (jwtSecret == null || jwtSecret.getBytes(StandardCharsets.UTF_8).length < MIN_SECRET_LENGTH) {
            String errorMsg = "JWT secret key is too short! It should be at least " + MIN_SECRET_LENGTH + 
                              " bytes (characters) long for adequate security.";
            log.error(errorMsg);
            throw new IllegalStateException(errorMsg);
        }
        
        if (jwtSecret.equals("your-256-bit-secret-key-here") || jwtSecret.startsWith("default") || 
            jwtSecret.contains("changeme") || jwtSecret.contains("secret")) {
            String warningMsg = "WARNING: You appear to be using a default or placeholder JWT secret key. " +
                               "This is insecure and should be changed in production!";
            log.warn(warningMsg);
        }
        
        log.info("JWT secret key validation passed");
    }

    public String generateToken(Authentication authentication) {
        UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + jwtExpirationInMs);

        String authorities = userPrincipal.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.joining(","));

        log.debug("Generating JWT token for user ID: {}", userPrincipal.getId());
        
        try {
            return Jwts.builder()
                    .subject(Long.toString(userPrincipal.getId()))
                    .claim("authorities", authorities)
                    .issuedAt(new Date())
                    .expiration(expiryDate)
                    .signWith(key)
                    .compact();
        } catch (Exception e) {
            log.error("Error generating JWT token: {}", e.getMessage(), e);
            throw e;
        }
    }

    public Long getUserIdFromJWT(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            return Long.parseLong(claims.getSubject());
        } catch (Exception e) {
            log.error("Error extracting user ID from JWT: {}", e.getMessage(), e);
            throw e;
        }
    }

    public boolean validateToken(String authToken) {
        try {
            Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(authToken);
            return true;
        } catch (SecurityException ex) {
            log.error("Invalid JWT signature: {}", ex.getMessage());
        } catch (MalformedJwtException ex) {
            log.error("Invalid JWT token: {}", ex.getMessage());
        } catch (ExpiredJwtException ex) {
            log.error("Expired JWT token: {}", ex.getMessage());
        } catch (UnsupportedJwtException ex) {
            log.error("Unsupported JWT token: {}", ex.getMessage());
        } catch (IllegalArgumentException ex) {
            log.error("JWT claims string is empty: {}", ex.getMessage());
        } catch (Exception ex) {
            log.error("JWT validation error: {}", ex.getMessage());
        }
        return false;
    }
} 