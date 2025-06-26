package com.codemate.controller;

import com.codemate.security.CurrentUser;
import com.codemate.security.UserPrincipal;
import com.codemate.service.AIAnalyticsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/v1/analytics")
@RequiredArgsConstructor
public class AnalyticsController {

    private final AIAnalyticsService analyticsService;

    @GetMapping("/user")
    public ResponseEntity<Map<String, Object>> getUserAnalytics(
            @RequestParam(defaultValue = "7") int days,
            @CurrentUser UserPrincipal currentUser) {
        log.info("Getting user analytics for user: {} over {} days", currentUser.getId(), days);
        
        try {
            Map<String, Object> analytics = analyticsService.getUserAnalytics(currentUser.getId(), days);
            return ResponseEntity.ok(analytics);
        } catch (Exception e) {
            log.error("Error getting user analytics for user: {}", currentUser.getId(), e);
            throw new RuntimeException("Failed to get user analytics: " + e.getMessage());
        }
    }

    @GetMapping("/system")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> getSystemAnalytics(
            @RequestParam(defaultValue = "30") int days) {
        log.info("Getting system analytics over {} days", days);
        
        try {
            Map<String, Object> analytics = analyticsService.getSystemAnalytics(days);
            return ResponseEntity.ok(analytics);
        } catch (Exception e) {
            log.error("Error getting system analytics", e);
            throw new RuntimeException("Failed to get system analytics: " + e.getMessage());
        }
    }

    @GetMapping("/dashboard")
    public ResponseEntity<Map<String, Object>> getDashboardData(
            @RequestParam(defaultValue = "7") int userDays,
            @CurrentUser UserPrincipal currentUser) {
        log.info("Getting dashboard data for user: {}", currentUser.getId());
        
        try {
            Map<String, Object> dashboardData = analyticsService.getUserAnalytics(currentUser.getId(), userDays);
            
            // Add some quick stats
            dashboardData.put("userId", currentUser.getId());
            dashboardData.put("username", currentUser.getUsername());
            dashboardData.put("reportPeriod", userDays + " days");
            
            return ResponseEntity.ok(dashboardData);
        } catch (Exception e) {
            log.error("Error getting dashboard data for user: {}", currentUser.getId(), e);
            throw new RuntimeException("Failed to get dashboard data: " + e.getMessage());
        }
    }
} 