package com.codemate.controller;

import com.codemate.payload.ApiResponse;
import com.codemate.security.SecurityContextService;
import com.codemate.security.SecurityUtils;
import com.codemate.security.UserPrincipal;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

/**
 * Controller for testing security context management
 * This controller is for testing purposes only and should be removed in production
 */
@Slf4j
@RestController
@RequestMapping("/api/security-test")
public class SecurityTestController {

    private final SecurityContextService securityContextService;

    public SecurityTestController(SecurityContextService securityContextService) {
        this.securityContextService = securityContextService;
    }

    /**
     * Test endpoint to check the current security context
     */
    @GetMapping("/context")
    public ResponseEntity<?> checkSecurityContext() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        
        Map<String, Object> response = new HashMap<>();
        response.put("authenticated", authentication != null && authentication.isAuthenticated());
        
        if (authentication != null && authentication.isAuthenticated()) {
            response.put("username", authentication.getName());
            response.put("authorities", authentication.getAuthorities());
            
            if (authentication.getPrincipal() instanceof UserPrincipal) {
                UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();
                response.put("userId", userPrincipal.getId());
            }
        }
        
        log.info("Security context check: {}", response);
        return ResponseEntity.ok(response);
    }
    
    /**
     * Test endpoint to check the current security context using SecurityContextService
     */
    @GetMapping("/service-context")
    public ResponseEntity<?> checkSecurityContextService() {
        Map<String, Object> response = new HashMap<>();
        response.put("authenticated", securityContextService.isAuthenticated());
        
        if (securityContextService.isAuthenticated()) {
            UserPrincipal userPrincipal = securityContextService.getCurrentUserPrincipal();
            if (userPrincipal != null) {
                response.put("username", userPrincipal.getUsername());
                response.put("userId", userPrincipal.getId());
                response.put("authorities", userPrincipal.getAuthorities());
            }
        }
        
        log.info("Security context service check: {}", response);
        return ResponseEntity.ok(response);
    }
    
    /**
     * Test endpoint to check the current security context using SecurityUtils
     */
    @GetMapping("/utils-context")
    public ResponseEntity<?> checkSecurityUtils() {
        Map<String, Object> response = new HashMap<>();
        response.put("authenticated", SecurityUtils.isAuthenticated());
        
        if (SecurityUtils.isAuthenticated()) {
            UserPrincipal userPrincipal = SecurityUtils.getCurrentUserPrincipal();
            if (userPrincipal != null) {
                response.put("username", userPrincipal.getUsername());
                response.put("userId", userPrincipal.getId());
                response.put("authorities", userPrincipal.getAuthorities());
            }
        }
        
        log.info("Security utils check: {}", response);
        return ResponseEntity.ok(response);
    }
    
    /**
     * Test endpoint to check if the security context is propagated to async tasks
     */
    @GetMapping("/async-context")
    public ResponseEntity<?> checkAsyncSecurityContext() throws ExecutionException, InterruptedException {
        // Get current authentication
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        
        // Create a response map
        Map<String, Object> response = new HashMap<>();
        response.put("mainThreadAuthenticated", authentication != null && authentication.isAuthenticated());
        
        if (authentication != null && authentication.isAuthenticated()) {
            response.put("mainThreadUsername", authentication.getName());
        }
        
        // Test async context propagation
        CompletableFuture<Boolean> asyncResult = CompletableFuture.supplyAsync(() -> {
            Authentication asyncAuth = SecurityContextHolder.getContext().getAuthentication();
            boolean asyncAuthenticated = asyncAuth != null && asyncAuth.isAuthenticated();
            log.info("Async thread authentication: {}", asyncAuthenticated);
            return asyncAuthenticated;
        });
        
        response.put("asyncThreadAuthenticated", asyncResult.get());
        
        log.info("Async security context check: {}", response);
        return ResponseEntity.ok(response);
    }
    
    /**
     * Test endpoint that requires ADMIN role
     */
    @GetMapping("/admin-only")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> adminOnlyEndpoint() {
        return ResponseEntity.ok(new ApiResponse(true, "You have ADMIN access"));
    }
    
    /**
     * Test endpoint that requires USER role
     */
    @GetMapping("/user-only")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<?> userOnlyEndpoint() {
        return ResponseEntity.ok(new ApiResponse(true, "You have USER access"));
    }
    
    /**
     * Test endpoint that uses @AuthenticationPrincipal
     */
    @GetMapping("/principal")
    public ResponseEntity<?> principalEndpoint(@AuthenticationPrincipal UserPrincipal userPrincipal) {
        if (userPrincipal == null) {
            return ResponseEntity.ok(new ApiResponse(false, "Not authenticated"));
        }
        
        Map<String, Object> response = new HashMap<>();
        response.put("userId", userPrincipal.getId());
        response.put("username", userPrincipal.getUsername());
        response.put("authorities", userPrincipal.getAuthorities());
        
        return ResponseEntity.ok(response);
    }
} 