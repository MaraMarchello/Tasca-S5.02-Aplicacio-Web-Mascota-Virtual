package com.codemate.service.security;

import com.codemate.util.PerformanceMonitor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Service responsible for rate limiting Git command execution to prevent abuse
 * Implements multiple rate limiting strategies: per-user, per-command, and global
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class GitRateLimitingService {

    private final PerformanceMonitor performanceMonitor;

    @Value("${app.git.rate-limit.commands-per-minute:30}")
    private int commandsPerMinute;

    @Value("${app.git.rate-limit.commands-per-hour:500}")
    private int commandsPerHour;

    @Value("${app.git.rate-limit.commands-per-day:2000}")
    private int commandsPerDay;

    @Value("${app.git.rate-limit.global-commands-per-minute:200}")
    private int globalCommandsPerMinute;

    @Value("${app.git.rate-limit.max-concurrent-commands:10}")
    private int maxConcurrentCommands;

    @Value("${app.git.rate-limit.command-timeout-seconds:30}")
    private int commandTimeoutSeconds;

    // User-specific rate limiting
    private final ConcurrentHashMap<Long, UserRateLimit> userRateLimits = new ConcurrentHashMap<>();
    
    // Global rate limiting
    private final RateLimitBucket globalRateLimit = new RateLimitBucket();
    
    // Active command tracking
    private final ConcurrentHashMap<Long, AtomicInteger> activeConcurrentCommands = new ConcurrentHashMap<>();
    
    // Command execution tracking
    private final ConcurrentHashMap<String, LocalDateTime> activeCommandExecutions = new ConcurrentHashMap<>();

    /**
     * Checks if user can execute a Git command based on rate limits
     */
    public void checkRateLimit(Long userId, String command) {
        log.debug("Checking rate limit for user {} command: {}", userId, command);

        // Check global rate limits first
        checkGlobalRateLimit();
        
        // Check user-specific rate limits
        checkUserRateLimit(userId);
        
        // Check concurrent command limits
        checkConcurrentCommandLimit(userId);
        
        // Track command execution start
        trackCommandStart(userId, command);
        
        log.debug("Rate limit check passed for user {} command: {}", userId, command);
    }

    /**
     * Records command completion and updates rate limit counters
     */
    public void recordCommandCompletion(Long userId, String command, boolean successful, long durationMs) {
        try {
            // Update user rate limit
            UserRateLimit userLimit = userRateLimits.get(userId);
            if (userLimit != null) {
                userLimit.recordCommandCompletion(successful, durationMs);
            }
            
            // Update global rate limit
            globalRateLimit.recordCommandCompletion(successful, durationMs);
            
            // Decrease concurrent command count
            AtomicInteger concurrentCount = activeConcurrentCommands.get(userId);
            if (concurrentCount != null) {
                concurrentCount.decrementAndGet();
            }
            
            // Remove from active executions
            String executionKey = userId + ":" + command + ":" + System.currentTimeMillis();
            activeCommandExecutions.remove(executionKey);
            
            // Record performance metrics
            // performanceMonitor.recordGitCommandRateLimit(userId, command, successful, durationMs);
            
            log.debug("Recorded command completion for user {} command: {} ({}ms, success: {})", 
                     userId, command, durationMs, successful);
                     
        } catch (Exception e) {
            log.error("Error recording command completion for user {}: {}", userId, e.getMessage(), e);
        }
    }

    /**
     * Gets current rate limit status for a user
     */
    public RateLimitStatus getRateLimitStatus(Long userId) {
        UserRateLimit userLimit = userRateLimits.computeIfAbsent(userId, k -> new UserRateLimit());
        AtomicInteger concurrent = activeConcurrentCommands.computeIfAbsent(userId, k -> new AtomicInteger(0));
        
        return RateLimitStatus.builder()
            .userId(userId)
            .commandsThisMinute(userLimit.getCommandsThisMinute())
            .commandsThisHour(userLimit.getCommandsThisHour())
            .commandsThisDay(userLimit.getCommandsThisDay())
            .maxCommandsPerMinute(commandsPerMinute)
            .maxCommandsPerHour(commandsPerHour)
            .maxCommandsPerDay(commandsPerDay)
            .activeConcurrentCommands(concurrent.get())
            .maxConcurrentCommands(maxConcurrentCommands)
            .globalCommandsThisMinute(globalRateLimit.getCommandsThisMinute())
            .maxGlobalCommandsPerMinute(globalCommandsPerMinute)
            .build();
    }

    /**
     * Checks if user has exceeded any rate limits
     */
    public boolean isRateLimited(Long userId) {
        try {
            checkRateLimit(userId, "test");
            return false;
        } catch (SecurityException e) {
            return true;
        }
    }

    /**
     * Resets rate limits for a user (admin function)
     */
    public void resetUserRateLimit(Long userId) {
        userRateLimits.remove(userId);
        AtomicInteger concurrent = activeConcurrentCommands.get(userId);
        if (concurrent != null) {
            concurrent.set(0);
        }
        log.info("Reset rate limits for user: {}", userId);
    }

    /**
     * Checks global rate limits
     */
    private void checkGlobalRateLimit() {
        globalRateLimit.cleanup();
        
        if (globalRateLimit.getCommandsThisMinute() >= globalCommandsPerMinute) {
            throw new SecurityException("Global rate limit exceeded: too many commands system-wide");
        }
        
        globalRateLimit.incrementCommands();
    }

    /**
     * Checks user-specific rate limits
     */
    private void checkUserRateLimit(Long userId) {
        UserRateLimit userLimit = userRateLimits.computeIfAbsent(userId, k -> new UserRateLimit());
        userLimit.cleanup();
        
        if (userLimit.getCommandsThisMinute() >= commandsPerMinute) {
            throw new SecurityException("Rate limit exceeded: too many commands per minute");
        }
        
        if (userLimit.getCommandsThisHour() >= commandsPerHour) {
            throw new SecurityException("Rate limit exceeded: too many commands per hour");
        }
        
        if (userLimit.getCommandsThisDay() >= commandsPerDay) {
            throw new SecurityException("Rate limit exceeded: too many commands per day");
        }
        
        userLimit.incrementCommands();
    }

    /**
     * Checks concurrent command limits
     */
    private void checkConcurrentCommandLimit(Long userId) {
        AtomicInteger concurrentCount = activeConcurrentCommands.computeIfAbsent(userId, k -> new AtomicInteger(0));
        
        if (concurrentCount.get() >= maxConcurrentCommands) {
            throw new SecurityException("Concurrent command limit exceeded");
        }
        
        concurrentCount.incrementAndGet();
    }

    /**
     * Tracks command execution start
     */
    private void trackCommandStart(Long userId, String command) {
        String executionKey = userId + ":" + command + ":" + System.currentTimeMillis();
        activeCommandExecutions.put(executionKey, LocalDateTime.now());
        
        // Clean up old executions (potential timeouts)
        cleanupTimedOutCommands();
    }

    /**
     * Cleans up commands that have timed out
     */
    private void cleanupTimedOutCommands() {
        LocalDateTime timeout = LocalDateTime.now().minusSeconds(commandTimeoutSeconds);
        
        activeCommandExecutions.entrySet().removeIf(entry -> {
            if (entry.getValue().isBefore(timeout)) {
                log.warn("Command execution timed out: {}", entry.getKey());
                
                // Extract user ID and decrement concurrent count
                String[] parts = entry.getKey().split(":");
                if (parts.length > 0) {
                    try {
                        Long userId = Long.parseLong(parts[0]);
                        AtomicInteger concurrent = activeConcurrentCommands.get(userId);
                        if (concurrent != null) {
                            concurrent.decrementAndGet();
                        }
                    } catch (NumberFormatException e) {
                        log.warn("Invalid user ID in command key: {}", entry.getKey());
                    }
                }
                return true;
            }
            return false;
        });
    }

    /**
     * Rate limit bucket for tracking commands over time periods
     */
    private static class RateLimitBucket {
        private final AtomicInteger commandsThisMinute = new AtomicInteger(0);
        private final AtomicInteger commandsThisHour = new AtomicInteger(0);
        private final AtomicInteger commandsThisDay = new AtomicInteger(0);
        private final AtomicLong totalCommands = new AtomicLong(0);
        private final AtomicLong successfulCommands = new AtomicLong(0);
        private final AtomicLong totalExecutionTime = new AtomicLong(0);
        
        private LocalDateTime lastMinuteReset = LocalDateTime.now();
        private LocalDateTime lastHourReset = LocalDateTime.now();
        private LocalDateTime lastDayReset = LocalDateTime.now();

        public void incrementCommands() {
            commandsThisMinute.incrementAndGet();
            commandsThisHour.incrementAndGet();
            commandsThisDay.incrementAndGet();
        }

        public void recordCommandCompletion(boolean successful, long durationMs) {
            totalCommands.incrementAndGet();
            totalExecutionTime.addAndGet(durationMs);
            
            if (successful) {
                successfulCommands.incrementAndGet();
            }
        }

        public void cleanup() {
            LocalDateTime now = LocalDateTime.now();
            
            // Reset minute counter
            if (ChronoUnit.MINUTES.between(lastMinuteReset, now) >= 1) {
                commandsThisMinute.set(0);
                lastMinuteReset = now;
            }
            
            // Reset hour counter
            if (ChronoUnit.HOURS.between(lastHourReset, now) >= 1) {
                commandsThisHour.set(0);
                lastHourReset = now;
            }
            
            // Reset day counter
            if (ChronoUnit.DAYS.between(lastDayReset, now) >= 1) {
                commandsThisDay.set(0);
                lastDayReset = now;
            }
        }

        public int getCommandsThisMinute() { return commandsThisMinute.get(); }
        public int getCommandsThisHour() { return commandsThisHour.get(); }
        public int getCommandsThisDay() { return commandsThisDay.get(); }
        public long getTotalCommands() { return totalCommands.get(); }
        public long getSuccessfulCommands() { return successfulCommands.get(); }
        public long getTotalExecutionTime() { return totalExecutionTime.get(); }
    }

    /**
     * User-specific rate limit tracking
     */
    private static class UserRateLimit extends RateLimitBucket {
        // Inherits all functionality from RateLimitBucket
    }

    /**
     * Rate limit status information
     */
    public static class RateLimitStatus {
        private Long userId;
        private int commandsThisMinute;
        private int commandsThisHour;
        private int commandsThisDay;
        private int maxCommandsPerMinute;
        private int maxCommandsPerHour;
        private int maxCommandsPerDay;
        private int activeConcurrentCommands;
        private int maxConcurrentCommands;
        private int globalCommandsThisMinute;
        private int maxGlobalCommandsPerMinute;

        // Builder pattern
        public static RateLimitStatusBuilder builder() {
            return new RateLimitStatusBuilder();
        }

        public static class RateLimitStatusBuilder {
            private final RateLimitStatus status = new RateLimitStatus();

            public RateLimitStatusBuilder userId(Long userId) { status.userId = userId; return this; }
            public RateLimitStatusBuilder commandsThisMinute(int commands) { status.commandsThisMinute = commands; return this; }
            public RateLimitStatusBuilder commandsThisHour(int commands) { status.commandsThisHour = commands; return this; }
            public RateLimitStatusBuilder commandsThisDay(int commands) { status.commandsThisDay = commands; return this; }
            public RateLimitStatusBuilder maxCommandsPerMinute(int max) { status.maxCommandsPerMinute = max; return this; }
            public RateLimitStatusBuilder maxCommandsPerHour(int max) { status.maxCommandsPerHour = max; return this; }
            public RateLimitStatusBuilder maxCommandsPerDay(int max) { status.maxCommandsPerDay = max; return this; }
            public RateLimitStatusBuilder activeConcurrentCommands(int active) { status.activeConcurrentCommands = active; return this; }
            public RateLimitStatusBuilder maxConcurrentCommands(int max) { status.maxConcurrentCommands = max; return this; }
            public RateLimitStatusBuilder globalCommandsThisMinute(int global) { status.globalCommandsThisMinute = global; return this; }
            public RateLimitStatusBuilder maxGlobalCommandsPerMinute(int max) { status.maxGlobalCommandsPerMinute = max; return this; }

            public RateLimitStatus build() { return status; }
        }

        // Getters
        public Long getUserId() { return userId; }
        public int getCommandsThisMinute() { return commandsThisMinute; }
        public int getCommandsThisHour() { return commandsThisHour; }
        public int getCommandsThisDay() { return commandsThisDay; }
        public int getMaxCommandsPerMinute() { return maxCommandsPerMinute; }
        public int getMaxCommandsPerHour() { return maxCommandsPerHour; }
        public int getMaxCommandsPerDay() { return maxCommandsPerDay; }
        public int getActiveConcurrentCommands() { return activeConcurrentCommands; }
        public int getMaxConcurrentCommands() { return maxConcurrentCommands; }
        public int getGlobalCommandsThisMinute() { return globalCommandsThisMinute; }
        public int getMaxGlobalCommandsPerMinute() { return maxGlobalCommandsPerMinute; }

        public boolean isNearLimit() {
            return commandsThisMinute > maxCommandsPerMinute * 0.8 ||
                   commandsThisHour > maxCommandsPerHour * 0.8 ||
                   activeConcurrentCommands > maxConcurrentCommands * 0.8;
        }
    }
}
