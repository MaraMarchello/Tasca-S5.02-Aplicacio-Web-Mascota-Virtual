package com.codemate.service.security;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Central service for security configuration and monitoring
 * Provides unified security status, configuration management, and threat detection
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SecurityConfigurationService implements HealthIndicator {

    private final GitRateLimitingService rateLimitingService;
    private final ResourceMonitoringService resourceMonitoringService;
    private final SessionSecurityService sessionSecurityService;
    private final RepositoryAccessControlService accessControlService;
    private final SecurityAuditService auditService;

    @Value("${app.git.security.monitoring-enabled:true}")
    private boolean securityMonitoringEnabled;

    @Value("${app.git.security.threat-detection-enabled:true}")
    private boolean threatDetectionEnabled;

    @Value("${app.git.security.auto-response-enabled:true}")
    private boolean autoResponseEnabled;

    @Value("${app.git.security.max-failed-attempts:5}")
    private int maxFailedAttempts;

    @Value("${app.git.security.lockout-duration-minutes:15}")
    private int lockoutDurationMinutes;

    // Security metrics
    private final AtomicLong totalSecurityEvents = new AtomicLong(0);
    private final AtomicLong securityViolations = new AtomicLong(0);
    private final AtomicLong rateLimitViolations = new AtomicLong(0);
    private final AtomicLong accessDeniedEvents = new AtomicLong(0);
    private final AtomicLong suspiciousActivities = new AtomicLong(0);

    // Threat detection
    private final Map<Long, UserThreatProfile> userThreatProfiles = new HashMap<>();
    private volatile SecurityStatus currentSecurityStatus = SecurityStatus.NORMAL;
    private volatile LocalDateTime lastSecurityAssessment = LocalDateTime.now();

    /**
     * Gets current comprehensive security status
     */
    public SecurityStatusReport getSecurityStatus() {
        SecurityStatusReport report = new SecurityStatusReport();
        report.setOverallStatus(currentSecurityStatus);
        report.setLastAssessment(lastSecurityAssessment);
        
        // System resource status
        ResourceMonitoringService.SystemResourceStatus resourceStatus = 
            resourceMonitoringService.getSystemResourceStatus();
        report.setResourceStatus(resourceStatus);
        
        // Session statistics
        SessionSecurityService.SessionStatistics sessionStats = 
            sessionSecurityService.getSessionStatistics();
        report.setSessionStatistics(sessionStats);
        
        // Security metrics
        report.setSecurityMetrics(buildSecurityMetrics());
        
        // Active threats
        report.setActiveThreatCount(countActiveThreats());
        
        return report;
    }

    /**
     * Records a security event for monitoring and analysis
     */
    public void recordSecurityEvent(SecurityEvent event) {
        if (!securityMonitoringEnabled) {
            return;
        }

        totalSecurityEvents.incrementAndGet();
        
        switch (event.getEventType()) {
            case SECURITY_VIOLATION:
                securityViolations.incrementAndGet();
                break;
            case RATE_LIMIT_EXCEEDED:
                rateLimitViolations.incrementAndGet();
                break;
            case ACCESS_DENIED:
                accessDeniedEvents.incrementAndGet();
                break;
            case SUSPICIOUS_ACTIVITY:
                suspiciousActivities.incrementAndGet();
                break;

        }

        // Update user threat profile
        if (event.getUserId() != null) {
            updateUserThreatProfile(event);
        }

        // Perform threat analysis
        if (threatDetectionEnabled) {
            analyzeThreatLevel(event);
        }

        log.debug("Recorded security event: {} for user: {}", 
                 event.getEventType(), event.getUserId());
    }

    /**
     * Checks if user is currently locked out due to security violations
     */
    public boolean isUserLockedOut(Long userId) {
        UserThreatProfile profile = userThreatProfiles.get(userId);
        if (profile == null) {
            return false;
        }

        return profile.isLockedOut(lockoutDurationMinutes);
    }

    /**
     * Manually locks out a user for security reasons
     */
    public void lockoutUser(Long userId, String reason, String adminUser) {
        UserThreatProfile profile = userThreatProfiles.computeIfAbsent(userId, 
            k -> new UserThreatProfile(userId));
        
        profile.setLockedOut(true);
        profile.setLockoutReason(reason);
        profile.setLockoutTime(LocalDateTime.now());
        
        log.warn("User {} locked out by {}: {}", userId, adminUser, reason);
        
        auditService.logAdminEvent(null, adminUser, "USER_LOCKOUT", 
            "User:" + userId, true, reason);
    }

    /**
     * Unlocks a user
     */
    public void unlockUser(Long userId, String adminUser) {
        UserThreatProfile profile = userThreatProfiles.get(userId);
        if (profile != null) {
            profile.setLockedOut(false);
            profile.setLockoutReason(null);
            profile.setLockoutTime(null);
            
            log.info("User {} unlocked by {}", userId, adminUser);
            
            auditService.logAdminEvent(null, adminUser, "USER_UNLOCK", 
                "User:" + userId, true, "Manual unlock");
        }
    }

    /**
     * Gets threat profile for a user
     */
    public UserThreatProfile getUserThreatProfile(Long userId) {
        return userThreatProfiles.get(userId);
    }

    /**
     * Performs emergency security lockdown
     */
    public void performEmergencyLockdown(String reason, String adminUser) {
        log.error("EMERGENCY SECURITY LOCKDOWN initiated by {}: {}", adminUser, reason);
        
        currentSecurityStatus = SecurityStatus.EMERGENCY;
        
        // Terminate all user sessions
        // In a real implementation, this would terminate all active sessions
        log.warn("Emergency lockdown: terminating all user sessions");
        
        // Lock out all users temporarily
        userThreatProfiles.values().forEach(profile -> {
            profile.setLockedOut(true);
            profile.setLockoutReason("Emergency lockdown: " + reason);
            profile.setLockoutTime(LocalDateTime.now());
        });
        
        auditService.logAdminEvent(null, adminUser, "EMERGENCY_LOCKDOWN", 
            "SYSTEM", true, reason);
    }

    /**
     * Lifts emergency lockdown
     */
    public void liftEmergencyLockdown(String adminUser) {
        log.info("Emergency lockdown lifted by: {}", adminUser);
        
        currentSecurityStatus = SecurityStatus.NORMAL;
        
        // Unlock all users (except those manually locked)
        userThreatProfiles.values().forEach(profile -> {
            if (profile.getLockoutReason() != null && 
                profile.getLockoutReason().contains("Emergency lockdown")) {
                profile.setLockedOut(false);
                profile.setLockoutReason(null);
                profile.setLockoutTime(null);
            }
        });
        
        auditService.logAdminEvent(null, adminUser, "LOCKDOWN_LIFTED", 
            "SYSTEM", true, "Emergency lockdown lifted");
    }

    /**
     * Updates user threat profile based on security event
     */
    private void updateUserThreatProfile(SecurityEvent event) {
        UserThreatProfile profile = userThreatProfiles.computeIfAbsent(
            event.getUserId(), k -> new UserThreatProfile(event.getUserId()));
        
        profile.addSecurityEvent(event);
        
        // Auto-lockout if too many violations
        if (autoResponseEnabled && profile.getFailedAttempts() >= maxFailedAttempts) {
            profile.setLockedOut(true);
            profile.setLockoutReason("Too many security violations");
            profile.setLockoutTime(LocalDateTime.now());
            
            log.warn("Auto-locked user {} due to {} security violations", 
                    event.getUserId(), profile.getFailedAttempts());
                    
            auditService.logSecurityViolation(event.getUserId(), event.getUsername(), 
                "AUTO_LOCKOUT", "Multiple violations", 
                "Exceeded maximum failed attempts", "system");
        }
    }

    /**
     * Analyzes threat level from security events
     */
    private void analyzeThreatLevel(SecurityEvent event) {
        // Simple threat analysis - in production this would be more sophisticated
        long recentViolations = securityViolations.get();
        long recentSuspiciousActivity = suspiciousActivities.get();
        
        if (recentViolations > 100 || recentSuspiciousActivity > 50) {
            if (currentSecurityStatus == SecurityStatus.NORMAL) {
                currentSecurityStatus = SecurityStatus.ELEVATED;
                log.warn("Security status elevated due to increased threat activity");
            }
        } else if (recentViolations > 500 || recentSuspiciousActivity > 200) {
            if (currentSecurityStatus != SecurityStatus.CRITICAL) {
                currentSecurityStatus = SecurityStatus.CRITICAL;
                log.error("Security status CRITICAL due to high threat activity");
            }
        }
    }

    /**
     * Builds security metrics map
     */
    private Map<String, Object> buildSecurityMetrics() {
        Map<String, Object> metrics = new HashMap<>();
        metrics.put("totalSecurityEvents", totalSecurityEvents.get());
        metrics.put("securityViolations", securityViolations.get());
        metrics.put("rateLimitViolations", rateLimitViolations.get());
        metrics.put("accessDeniedEvents", accessDeniedEvents.get());
        metrics.put("suspiciousActivities", suspiciousActivities.get());
        metrics.put("lockedOutUsers", countLockedOutUsers());
        return metrics;
    }

    /**
     * Counts active threats
     */
    private int countActiveThreats() {
        return (int) userThreatProfiles.values().stream()
            .filter(profile -> profile.getThreatLevel() > 3)
            .count();
    }

    /**
     * Counts locked out users
     */
    private long countLockedOutUsers() {
        return userThreatProfiles.values().stream()
            .filter(profile -> profile.isLockedOut(lockoutDurationMinutes))
            .count();
    }

    /**
     * Scheduled security assessment
     */
    @Scheduled(fixedDelayString = "${app.git.security.assessment-interval-ms:300000}") // 5 minutes
    public void performSecurityAssessment() {
        if (!securityMonitoringEnabled) {
            return;
        }

        log.debug("Performing scheduled security assessment");
        
        // Update security status based on current metrics
        assessOverallSecurityStatus();
        
        // Clean up old threat profiles
        cleanupOldThreatProfiles();
        
        // Reset security status if things have calmed down
        resetSecurityStatusIfStable();
        
        lastSecurityAssessment = LocalDateTime.now();
    }

    /**
     * Assesses overall security status
     */
    private void assessOverallSecurityStatus() {
        // Reset metrics counters periodically
        if (lastSecurityAssessment.isBefore(LocalDateTime.now().minusHours(1))) {
            totalSecurityEvents.set(0);
            securityViolations.set(0);
            rateLimitViolations.set(0);
            accessDeniedEvents.set(0);
            suspiciousActivities.set(0);
        }
    }

    /**
     * Cleans up old threat profiles
     */
    private void cleanupOldThreatProfiles() {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(7);
        userThreatProfiles.entrySet().removeIf(entry -> 
            entry.getValue().getLastActivity().isBefore(cutoff));
    }

    /**
     * Resets security status if system is stable
     */
    private void resetSecurityStatusIfStable() {
        if (currentSecurityStatus != SecurityStatus.NORMAL) {
            if (securityViolations.get() < 10 && suspiciousActivities.get() < 5) {
                currentSecurityStatus = SecurityStatus.NORMAL;
                log.info("Security status reset to NORMAL - system appears stable");
            }
        }
    }

    /**
     * Health indicator implementation
     */
    @Override
    public Health health() {
        Health.Builder builder = new Health.Builder();
        
        if (currentSecurityStatus == SecurityStatus.CRITICAL || 
            currentSecurityStatus == SecurityStatus.EMERGENCY) {
            builder = builder.down();
        } else {
            builder = builder.up();
        }
        
        return builder
            .withDetail("securityStatus", currentSecurityStatus)
            .withDetail("lastAssessment", lastSecurityAssessment)
            .withDetail("totalSecurityEvents", totalSecurityEvents.get())
            .withDetail("activeThreats", countActiveThreats())
            .withDetail("lockedOutUsers", countLockedOutUsers())
            .build();
    }

    /**
     * Security status enumeration
     */
    public enum SecurityStatus {
        NORMAL, ELEVATED, CRITICAL, EMERGENCY
    }

    /**
     * Security event types
     */
    public enum SecurityEventType {
        SECURITY_VIOLATION, RATE_LIMIT_EXCEEDED, ACCESS_DENIED, 
        SUSPICIOUS_ACTIVITY, AUTHENTICATION_FAILURE, COMMAND_INJECTION_ATTEMPT
    }

    /**
     * Security event data structure
     */
    public static class SecurityEvent {
        private SecurityEventType eventType;
        private Long userId;
        private String username;
        private String description;
        private String ipAddress;
        private LocalDateTime timestamp;

        public SecurityEvent(SecurityEventType eventType, Long userId, String username, 
                           String description, String ipAddress) {
            this.eventType = eventType;
            this.userId = userId;
            this.username = username;
            this.description = description;
            this.ipAddress = ipAddress;
            this.timestamp = LocalDateTime.now();
        }

        // Getters
        public SecurityEventType getEventType() { return eventType; }
        public Long getUserId() { return userId; }
        public String getUsername() { return username; }
        public String getDescription() { return description; }
        public String getIpAddress() { return ipAddress; }
        public LocalDateTime getTimestamp() { return timestamp; }
    }

    /**
     * User threat profile
     */
    public static class UserThreatProfile {
        private final Long userId;
        private int failedAttempts = 0;
        private int threatLevel = 0;
        private boolean lockedOut = false;
        private String lockoutReason;
        private LocalDateTime lockoutTime;
        private LocalDateTime lastActivity = LocalDateTime.now();

        public UserThreatProfile(Long userId) {
            this.userId = userId;
        }

        public void addSecurityEvent(SecurityEvent event) {
            this.lastActivity = LocalDateTime.now();
            
            switch (event.getEventType()) {
                case SECURITY_VIOLATION:
                    failedAttempts++;
                    threatLevel += 2;
                    break;
                case RATE_LIMIT_EXCEEDED:
                    threatLevel += 1;
                    break;
                case ACCESS_DENIED:
                    failedAttempts++;
                    threatLevel += 1;
                    break;
                case SUSPICIOUS_ACTIVITY:
                    threatLevel += 3;
                    break;
                case COMMAND_INJECTION_ATTEMPT:
                    failedAttempts += 5;
                    threatLevel += 10;
                    break;
                case AUTHENTICATION_FAILURE:
                    failedAttempts++;
                    threatLevel += 1;
                    break;
            }
        }

        public boolean isLockedOut(int lockoutDurationMinutes) {
            if (!lockedOut) return false;
            
            if (lockoutTime == null) return true;
            
            return LocalDateTime.now().isBefore(
                lockoutTime.plusMinutes(lockoutDurationMinutes));
        }

        // Getters and setters
        public Long getUserId() { return userId; }
        public int getFailedAttempts() { return failedAttempts; }
        public int getThreatLevel() { return threatLevel; }
        public boolean isLockedOut() { return lockedOut; }
        public void setLockedOut(boolean lockedOut) { this.lockedOut = lockedOut; }
        public String getLockoutReason() { return lockoutReason; }
        public void setLockoutReason(String lockoutReason) { this.lockoutReason = lockoutReason; }
        public LocalDateTime getLockoutTime() { return lockoutTime; }
        public void setLockoutTime(LocalDateTime lockoutTime) { this.lockoutTime = lockoutTime; }
        public LocalDateTime getLastActivity() { return lastActivity; }
    }

    /**
     * Security status report
     */
    public static class SecurityStatusReport {
        private SecurityStatus overallStatus;
        private LocalDateTime lastAssessment;
        private ResourceMonitoringService.SystemResourceStatus resourceStatus;
        private SessionSecurityService.SessionStatistics sessionStatistics;
        private Map<String, Object> securityMetrics;
        private int activeThreatCount;

        // Getters and setters
        public SecurityStatus getOverallStatus() { return overallStatus; }
        public void setOverallStatus(SecurityStatus overallStatus) { this.overallStatus = overallStatus; }
        public LocalDateTime getLastAssessment() { return lastAssessment; }
        public void setLastAssessment(LocalDateTime lastAssessment) { this.lastAssessment = lastAssessment; }
        public ResourceMonitoringService.SystemResourceStatus getResourceStatus() { return resourceStatus; }
        public void setResourceStatus(ResourceMonitoringService.SystemResourceStatus resourceStatus) { this.resourceStatus = resourceStatus; }
        public SessionSecurityService.SessionStatistics getSessionStatistics() { return sessionStatistics; }
        public void setSessionStatistics(SessionSecurityService.SessionStatistics sessionStatistics) { this.sessionStatistics = sessionStatistics; }
        public Map<String, Object> getSecurityMetrics() { return securityMetrics; }
        public void setSecurityMetrics(Map<String, Object> securityMetrics) { this.securityMetrics = securityMetrics; }
        public int getActiveThreatCount() { return activeThreatCount; }
        public void setActiveThreatCount(int activeThreatCount) { this.activeThreatCount = activeThreatCount; }
    }
}
