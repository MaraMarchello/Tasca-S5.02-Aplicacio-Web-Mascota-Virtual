package com.codemate.service.security;

import com.codemate.service.git.GitSessionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;


import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Service responsible for managing user sessions and workspace security lifecycle
 * Handles session timeouts, workspace cleanup, and prevents resource leaks
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SessionSecurityService {

    private final GitSessionService gitSessionService;
    private final WorkspaceSecurityService workspaceSecurityService;
    private final SecurityAuditService securityAuditService;

    @Value("${app.git.session.max-idle-minutes:30}")
    private int maxIdleMinutes;

    @Value("${app.git.session.max-active-hours:8}")
    private int maxActiveHours;

    @Value("${app.git.session.cleanup-interval-minutes:15}")
    private int cleanupIntervalMinutes;

    @Value("${app.git.session.max-concurrent-sessions:5}")
    private int maxConcurrentSessions;

    @Value("${app.git.session.workspace-retention-hours:48}")
    private int workspaceRetentionHours;

    @Value("${app.git.session.emergency-cleanup-enabled:true}")
    private boolean emergencyCleanupEnabled;

    // Active user sessions tracking
    private final ConcurrentHashMap<Long, UserSession> activeSessions = new ConcurrentHashMap<>();
    
    // Session activity tracking
    private final ConcurrentHashMap<Long, LocalDateTime> lastUserActivity = new ConcurrentHashMap<>();
    
    // Resource usage tracking per session
    private final ConcurrentHashMap<Long, SessionResourceUsage> sessionResources = new ConcurrentHashMap<>();

    /**
     * Initializes or validates a user session
     */
    public UserSession initializeUserSession(Long userId, String username, String ipAddress) {
        log.debug("Initializing session for user: {}", userId);

        // Check if user has too many active sessions
        checkConcurrentSessionLimit(userId);
        
        // Get or create session
        UserSession session = activeSessions.computeIfAbsent(userId, id -> {
            UserSession newSession = new UserSession(id, username, ipAddress);
            log.info("Created new session for user: {} from IP: {}", username, ipAddress);
            
            // Audit log session creation
            securityAuditService.logAuthenticationEvent(userId, username, "SESSION_CREATED", 
                ipAddress, true, "User session initialized");
            
            return newSession;
        });

        // Update activity
        updateSessionActivity(userId);
        
        // Validate session is not expired
        validateSessionTimeout(session);
        
        return session;
    }

    /**
     * Updates last activity time for a user session
     */
    public void updateSessionActivity(Long userId) {
        lastUserActivity.put(userId, LocalDateTime.now());
        
        UserSession session = activeSessions.get(userId);
        if (session != null) {
            session.updateLastActivity();
        }
        
        log.debug("Updated activity for user: {}", userId);
    }

    /**
     * Records resource usage for a session
     */
    public void recordSessionResourceUsage(Long userId, String operation, long executionTime, long memoryUsed) {
        SessionResourceUsage usage = sessionResources.computeIfAbsent(userId, k -> new SessionResourceUsage());
        usage.recordOperation(operation, executionTime, memoryUsed);
        
        log.debug("Recorded resource usage for user {}: {}ms, {}bytes", userId, executionTime, memoryUsed);
    }

    /**
     * Terminates a user session and cleans up resources
     */
    public void terminateUserSession(Long userId, String reason) {
        UserSession session = activeSessions.remove(userId);
        if (session == null) {
            log.debug("No active session found for user: {}", userId);
            return;
        }

        log.info("Terminating session for user: {} ({}), reason: {}", 
                session.getUsername(), userId, reason);

        // Clean up session resources
        cleanupSessionResources(userId, session);
        
        // Remove activity tracking
        lastUserActivity.remove(userId);
        sessionResources.remove(userId);
        
        // Audit log session termination
        securityAuditService.logAuthenticationEvent(userId, session.getUsername(), 
            "SESSION_TERMINATED", session.getIpAddress(), true, "Reason: " + reason);
    }

    /**
     * Gets current session information for a user
     */
    public UserSession getUserSession(Long userId) {
        UserSession session = activeSessions.get(userId);
        if (session != null) {
            validateSessionTimeout(session);
        }
        return session;
    }

    /**
     * Gets session resource usage
     */
    public SessionResourceUsage getSessionResourceUsage(Long userId) {
        return sessionResources.get(userId);
    }

    /**
     * Checks if user has exceeded concurrent session limit
     */
    private void checkConcurrentSessionLimit(Long userId) {
        long userSessionCount = activeSessions.values().stream()
            .filter(session -> session.getUserId().equals(userId))
            .count();
            
        if (userSessionCount >= maxConcurrentSessions) {
            throw new SecurityException(
                String.format("Maximum concurrent sessions exceeded for user: %d (limit: %d)", 
                    userId, maxConcurrentSessions)
            );
        }
    }

    /**
     * Validates session has not timed out
     */
    private void validateSessionTimeout(UserSession session) {
        LocalDateTime now = LocalDateTime.now();
        
        // Check idle timeout
        long idleMinutes = ChronoUnit.MINUTES.between(session.getLastActivity(), now);
        if (idleMinutes > maxIdleMinutes) {
            terminateUserSession(session.getUserId(), 
                String.format("Session idle timeout: %d minutes", idleMinutes));
            throw new SecurityException("Session expired due to inactivity");
        }
        
        // Check maximum active time
        long activeHours = ChronoUnit.HOURS.between(session.getCreatedAt(), now);
        if (activeHours > maxActiveHours) {
            terminateUserSession(session.getUserId(), 
                String.format("Session maximum duration exceeded: %d hours", activeHours));
            throw new SecurityException("Session expired due to maximum duration");
        }
    }

    /**
     * Cleans up resources associated with a session
     */
    private void cleanupSessionResources(Long userId, UserSession session) {
        try {
            // Clean up Git workspaces for this user
            gitSessionService.cleanupUserWorkspaces(userId);
            
            log.info("Cleaned up session resources for user: {}", userId);
            
        } catch (Exception e) {
            log.error("Error cleaning up session resources for user: {}", userId, e);
            
            // Audit log cleanup failure
            securityAuditService.logWorkspaceEvent(userId, session.getUsername(), 
                "CLEANUP_FAILED", "session_resources", false, e.getMessage());
        }
    }

    /**
     * Scheduled task to clean up expired sessions and workspaces
     */
    @Scheduled(fixedDelayString = "${app.git.session.cleanup-interval-minutes:15}*60*1000")
    public void cleanupExpiredSessions() {
        log.debug("Starting scheduled session cleanup");
        
        LocalDateTime now = LocalDateTime.now();
        int expiredCount = 0;
        
        // Find and remove expired sessions
        activeSessions.entrySet().removeIf(entry -> {
            UserSession session = entry.getValue();
            Long userId = entry.getKey();
            
            // Check idle timeout
            long idleMinutes = ChronoUnit.MINUTES.between(session.getLastActivity(), now);
            if (idleMinutes > maxIdleMinutes) {
                log.info("Removing idle session for user: {} (idle: {} minutes)", 
                        session.getUsername(), idleMinutes);
                cleanupSessionResources(userId, session);
                return true;
            }
            
            // Check maximum duration
            long activeHours = ChronoUnit.HOURS.between(session.getCreatedAt(), now);
            if (activeHours > maxActiveHours) {
                log.info("Removing long-running session for user: {} (active: {} hours)", 
                        session.getUsername(), activeHours);
                cleanupSessionResources(userId, session);
                return true;
            }
            
            return false;
        });
        
        // Clean up orphaned workspaces
        cleanupOrphanedWorkspaces();
        
        if (expiredCount > 0) {
            log.info("Cleaned up {} expired sessions", expiredCount);
        }
    }

    /**
     * Cleans up workspaces that don't have active sessions
     */
    private void cleanupOrphanedWorkspaces() {
        try {
            LocalDateTime cutoff = LocalDateTime.now().minusHours(workspaceRetentionHours);
            
            // This would integrate with GitSessionService to find and clean orphaned workspaces
            int cleanedCount = gitSessionService.cleanupOrphanedWorkspaces(cutoff);
            
            if (cleanedCount > 0) {
                log.info("Cleaned up {} orphaned workspaces", cleanedCount);
            }
            
        } catch (Exception e) {
            log.error("Error during orphaned workspace cleanup: {}", e.getMessage(), e);
        }
    }

    /**
     * Emergency cleanup when system resources are low
     */
    public void performEmergencyCleanup() {
        if (!emergencyCleanupEnabled) {
            log.warn("Emergency cleanup requested but disabled by configuration");
            return;
        }
        
        log.warn("Performing emergency session and workspace cleanup");
        
        int cleaned = 0;
        LocalDateTime emergencyCutoff = LocalDateTime.now().minusMinutes(maxIdleMinutes / 2);
        
        // Aggressively clean up sessions that have been idle for half the normal timeout
        activeSessions.entrySet().removeIf(entry -> {
            UserSession session = entry.getValue();
            if (session.getLastActivity().isBefore(emergencyCutoff)) {
                cleanupSessionResources(entry.getKey(), session);
                
                // Audit log emergency cleanup
                securityAuditService.logWorkspaceEvent(entry.getKey(), session.getUsername(), 
                    "EMERGENCY_CLEANUP", "session", true, "System resource emergency");
                
                return true;
            }
            return false;
        });
        
        // Force garbage collection
        System.gc();
        
        log.warn("Emergency cleanup completed, cleaned {} sessions", cleaned);
    }

    /**
     * Gets current session statistics
     */
    public SessionStatistics getSessionStatistics() {
        LocalDateTime now = LocalDateTime.now();
        
        long totalSessions = activeSessions.size();
        long idleSessions = activeSessions.values().stream()
            .filter(session -> ChronoUnit.MINUTES.between(session.getLastActivity(), now) > 5)
            .count();
        long longRunningSessions = activeSessions.values().stream()
            .filter(session -> ChronoUnit.HOURS.between(session.getCreatedAt(), now) > 4)
            .count();
        
        return new SessionStatistics(totalSessions, idleSessions, longRunningSessions);
    }

    /**
     * User session data structure
     */
    public static class UserSession {
        private final Long userId;
        private final String username;
        private final String ipAddress;
        private final LocalDateTime createdAt;
        private volatile LocalDateTime lastActivity;
        private final AtomicInteger commandCount = new AtomicInteger(0);

        public UserSession(Long userId, String username, String ipAddress) {
            this.userId = userId;
            this.username = username;
            this.ipAddress = ipAddress;
            this.createdAt = LocalDateTime.now();
            this.lastActivity = LocalDateTime.now();
        }

        public void updateLastActivity() {
            this.lastActivity = LocalDateTime.now();
        }

        public void incrementCommandCount() {
            commandCount.incrementAndGet();
        }

        // Getters
        public Long getUserId() { return userId; }
        public String getUsername() { return username; }
        public String getIpAddress() { return ipAddress; }
        public LocalDateTime getCreatedAt() { return createdAt; }
        public LocalDateTime getLastActivity() { return lastActivity; }
        public int getCommandCount() { return commandCount.get(); }
    }

    /**
     * Session resource usage tracking
     */
    public static class SessionResourceUsage {
        private long totalOperations = 0;
        private long totalExecutionTime = 0;
        private long totalMemoryUsed = 0;
        private LocalDateTime lastUpdated = LocalDateTime.now();

        public void recordOperation(String operation, long executionTime, long memoryUsed) {
            this.totalOperations++;
            this.totalExecutionTime += executionTime;
            this.totalMemoryUsed = Math.max(this.totalMemoryUsed, memoryUsed);
            this.lastUpdated = LocalDateTime.now();
        }

        // Getters
        public long getTotalOperations() { return totalOperations; }
        public long getTotalExecutionTime() { return totalExecutionTime; }
        public long getTotalMemoryUsed() { return totalMemoryUsed; }
        public LocalDateTime getLastUpdated() { return lastUpdated; }
        public double getAverageExecutionTime() { 
            return totalOperations > 0 ? (double) totalExecutionTime / totalOperations : 0; 
        }
    }

    /**
     * Session statistics
     */
    public static class SessionStatistics {
        private final long totalSessions;
        private final long idleSessions;
        private final long longRunningSessions;

        public SessionStatistics(long totalSessions, long idleSessions, long longRunningSessions) {
            this.totalSessions = totalSessions;
            this.idleSessions = idleSessions;
            this.longRunningSessions = longRunningSessions;
        }

        public long getTotalSessions() { return totalSessions; }
        public long getIdleSessions() { return idleSessions; }
        public long getLongRunningSessions() { return longRunningSessions; }
        public long getActiveSessions() { return totalSessions - idleSessions; }
    }
}
