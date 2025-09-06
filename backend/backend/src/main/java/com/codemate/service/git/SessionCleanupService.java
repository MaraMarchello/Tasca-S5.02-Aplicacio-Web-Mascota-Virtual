package com.codemate.service.git;

import com.codemate.service.security.SecurityAuditService;
import com.codemate.service.security.WorkspaceSecurityService;
import com.codemate.util.PerformanceMonitor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import jakarta.annotation.PreDestroy;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Stream;

/**
 * Service responsible for comprehensive cleanup of abandoned Git sessions and repositories
 * Provides multiple cleanup strategies with configurable policies and health monitoring
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SessionCleanupService implements HealthIndicator {

    private final WorkspaceSecurityService workspaceSecurityService;
    private final SecurityAuditService securityAuditService;
    private final PerformanceMonitor performanceMonitor;

    @Value("${app.git.workspace.base-path:${java.io.tmpdir}/codemate-git-workspaces}")
    private String baseWorkspacePath;

    @Value("${app.git.cleanup.abandoned-session-hours:2}")
    private int abandonedSessionHours;

    @Value("${app.git.cleanup.orphaned-workspace-hours:24}")
    private int orphanedWorkspaceHours;

    @Value("${app.git.cleanup.temp-files-hours:1}")
    private int tempFilesHours;

    @Value("${app.git.cleanup.max-workspace-size-mb:1000}")
    private long maxWorkspaceSizeMb;

    @Value("${app.git.cleanup.max-total-size-gb:10}")
    private long maxTotalSizeGb;

    @Value("${app.git.cleanup.aggressive-cleanup-enabled:true}")
    private boolean aggressiveCleanupEnabled;

    @Value("${app.git.cleanup.parallel-cleanup:true}")
    private boolean parallelCleanupEnabled;

    // Cleanup statistics
    private final AtomicLong totalCleanupOperations = new AtomicLong(0);
    private final AtomicLong totalFilesDeleted = new AtomicLong(0);
    private final AtomicLong totalSpaceFreed = new AtomicLong(0);
    private final AtomicLong lastCleanupDuration = new AtomicLong(0);
    private volatile LocalDateTime lastCleanupTime = LocalDateTime.now();

    /**
     * Comprehensive cleanup of all abandoned sessions and workspaces
     */
    @Scheduled(fixedDelayString = "${app.git.cleanup.comprehensive-interval-minutes:60}*60*1000") // 1 hour
    public void performComprehensiveCleanup() {
        log.info("Starting comprehensive session cleanup");
        
        long startTime = System.currentTimeMillis();
        totalCleanupOperations.incrementAndGet();
        
        try {
            performanceMonitor.startOperation("comprehensive_cleanup", null, null, null);
            
            // Execute cleanup tasks
            ComprehensiveCleanupResult result = executeComprehensiveCleanup();
            
            // Update statistics
            updateCleanupStatistics(result, startTime);
            
            // Log results
            logCleanupResults(result);
            
            performanceMonitor.endOperation("comprehensive_cleanup", null, null);
            
        } catch (Exception e) {
            log.error("Error during comprehensive cleanup", e);
            performanceMonitor.endOperation("comprehensive_cleanup", null, null);
        } finally {
            lastCleanupTime = LocalDateTime.now();
            lastCleanupDuration.set(System.currentTimeMillis() - startTime);
        }
    }

    /**
     * Quick cleanup of obviously abandoned sessions
     */
    @Scheduled(fixedDelayString = "${app.git.cleanup.quick-interval-minutes:15}*60*1000") // 15 minutes
    public void performQuickCleanup() {
        log.debug("Starting quick session cleanup");
        
        try {
            QuickCleanupResult result = executeQuickCleanup();
            
            if (result.getCleanedSessions() > 0) {
                log.info("Quick cleanup completed: {} sessions, {} MB freed", 
                        result.getCleanedSessions(), result.getSpaceFreedMb());
            }
            
        } catch (Exception e) {
            log.error("Error during quick cleanup", e);
        }
    }

    /**
     * Emergency cleanup when storage is critically low
     */
    public void performEmergencyCleanup() {
        log.warn("Performing EMERGENCY session cleanup due to storage constraints");
        
        try {
            EmergencyCleanupResult result = executeEmergencyCleanup();
            
            log.warn("Emergency cleanup completed: {} workspaces removed, {} GB freed",
                    result.getWorkspacesRemoved(), result.getSpaceFreedGb());
            
            // Audit log emergency cleanup
            securityAuditService.logAdminEvent(null, "SYSTEM", "EMERGENCY_CLEANUP",
                "SESSION_STORAGE", true, 
                String.format("Freed %.2f GB by removing %d workspaces", 
                    result.getSpaceFreedGb(), result.getWorkspacesRemoved()));
            
        } catch (Exception e) {
            log.error("Error during emergency cleanup", e);
        }
    }

    /**
     * Cleanup specific to a user (when user is deleted or banned)
     */
    public void cleanupUserSessions(Long userId, String reason) {
        log.info("Cleaning up all sessions for user: {} (reason: {})", userId, reason);
        
        try {
            UserCleanupResult result = executeUserCleanup(userId);
            
            log.info("User cleanup completed for {}: {} workspaces removed", 
                    userId, result.getWorkspacesRemoved());
            
            // Audit log user cleanup
            securityAuditService.logWorkspaceEvent(userId, "SYSTEM", "USER_CLEANUP",
                "ALL_WORKSPACES", true, reason);
            
        } catch (Exception e) {
            log.error("Error during user cleanup for user: {}", userId, e);
        }
    }

    /**
     * Executes comprehensive cleanup with all strategies
     */
    private ComprehensiveCleanupResult executeComprehensiveCleanup() throws IOException {
        ComprehensiveCleanupResult result = new ComprehensiveCleanupResult();
        
        // 1. Clean up abandoned sessions
        result.addAbandonedSessions(cleanupAbandonedSessions());
        
        // 2. Clean up orphaned workspaces
        result.addOrphanedWorkspaces(cleanupOrphanedWorkspaces());
        
        // 3. Clean up temporary files
        result.addTempFiles(cleanupTemporaryFiles());
        
        // 4. Clean up oversized workspaces
        result.addOversizedWorkspaces(cleanupOversizedWorkspaces());
        
        // 5. Check total storage and clean if necessary
        if (getTotalStorageUsageGb() > maxTotalSizeGb) {
            result.addStorageCleanup(cleanupByStorageLimit());
        }
        
        return result;
    }

    /**
     * Executes quick cleanup for obviously abandoned sessions
     */
    private QuickCleanupResult executeQuickCleanup() throws IOException {
        QuickCleanupResult result = new QuickCleanupResult();
        
        Path basePath = Paths.get(baseWorkspacePath);
        if (!Files.exists(basePath)) {
            return result;
        }
        
        LocalDateTime cutoff = LocalDateTime.now().minusMinutes(abandonedSessionHours * 60 / 4); // Quarter of abandon time
        
        try (Stream<Path> workspaces = Files.list(basePath)) {
            List<Path> candidateWorkspaces = workspaces
                .filter(Files::isDirectory)
                .filter(this::isObviouslyAbandoned)
                .filter(path -> isOlderThan(path, cutoff))
                .limit(10) // Limit for quick cleanup
                .toList();
            
            for (Path workspace : candidateWorkspaces) {
                try {
                    long sizeMb = calculateDirectorySizeMb(workspace);
                    secureDeleteDirectory(workspace);
                    
                    result.incrementCleanedSessions();
                    result.addSpaceFreed(sizeMb);
                    
                } catch (Exception e) {
                    log.warn("Failed to quick clean workspace: {}", workspace, e);
                }
            }
        }
        
        return result;
    }

    /**
     * Executes emergency cleanup with aggressive policies
     */
    private EmergencyCleanupResult executeEmergencyCleanup() throws IOException {
        EmergencyCleanupResult result = new EmergencyCleanupResult();
        
        Path basePath = Paths.get(baseWorkspacePath);
        if (!Files.exists(basePath)) {
            return result;
        }
        
        // Emergency cleanup is more aggressive - 1 hour cutoff
        LocalDateTime emergencyCutoff = LocalDateTime.now().minusHours(1);
        
        try (Stream<Path> workspaces = Files.list(basePath)) {
            List<Path> emergencyWorkspaces = workspaces
                .filter(Files::isDirectory)
                .filter(path -> isOlderThan(path, emergencyCutoff))
                .sorted((a, b) -> compareBySize(b, a)) // Largest first
                .toList();
            
            if (parallelCleanupEnabled && emergencyWorkspaces.size() > 5) {
                // Parallel cleanup for large numbers
                List<CompletableFuture<EmergencyCleanupResult.WorkspaceCleanup>> futures = emergencyWorkspaces.stream()
                    .map(workspace -> CompletableFuture.supplyAsync(() -> cleanupWorkspaceEmergency(workspace)))
                    .toList();
                
                futures.stream()
                    .map(CompletableFuture::join)
                    .forEach(cleanup -> {
                        if (cleanup.isSuccess()) {
                            result.addWorkspace(cleanup.getSizeGb());
                        }
                    });
            } else {
                // Sequential cleanup
                for (Path workspace : emergencyWorkspaces) {
                    try {
                        double sizeGb = calculateDirectorySizeGb(workspace);
                        secureDeleteDirectory(workspace);
                        result.addWorkspace(sizeGb);
                        
                        // Stop if we've freed enough space
                        if (result.getSpaceFreedGb() > maxTotalSizeGb * 0.3) { // Free 30% of limit
                            break;
                        }
                        
                    } catch (Exception e) {
                        log.warn("Failed to emergency clean workspace: {}", workspace, e);
                    }
                }
            }
        }
        
        return result;
    }

    /**
     * Cleans up all workspaces for a specific user
     */
    private UserCleanupResult executeUserCleanup(Long userId) throws IOException {
        UserCleanupResult result = new UserCleanupResult();
        
        Path basePath = Paths.get(baseWorkspacePath);
        if (!Files.exists(basePath)) {
            return result;
        }
        
        String userPattern = "user_" + userId;
        
        try (Stream<Path> workspaces = Files.list(basePath)) {
            List<Path> userWorkspaces = workspaces
                .filter(Files::isDirectory)
                .filter(path -> path.getFileName().toString().contains(userPattern))
                .toList();
            
            for (Path workspace : userWorkspaces) {
                try {
                    secureDeleteDirectory(workspace);
                    result.incrementWorkspacesRemoved();
                } catch (Exception e) {
                    log.warn("Failed to clean user workspace: {}", workspace, e);
                }
            }
        }
        
        return result;
    }

    /**
     * Cleans up abandoned sessions based on inactivity
     */
    private int cleanupAbandonedSessions() throws IOException {
        Path basePath = Paths.get(baseWorkspacePath);
        if (!Files.exists(basePath)) {
            return 0;
        }
        
        LocalDateTime cutoff = LocalDateTime.now().minusHours(abandonedSessionHours);
        int cleaned = 0;
        
        try (Stream<Path> workspaces = Files.list(basePath)) {
            List<Path> abandonedWorkspaces = workspaces
                .filter(Files::isDirectory)
                .filter(path -> isOlderThan(path, cutoff))
                .filter(this::hasSessionIndicators)
                .toList();
            
            for (Path workspace : abandonedWorkspaces) {
                try {
                    secureDeleteDirectory(workspace);
                    cleaned++;
                } catch (Exception e) {
                    log.warn("Failed to clean abandoned session: {}", workspace, e);
                }
            }
        }
        
        return cleaned;
    }

    /**
     * Cleans up orphaned workspaces without session metadata
     */
    private int cleanupOrphanedWorkspaces() throws IOException {
        Path basePath = Paths.get(baseWorkspacePath);
        if (!Files.exists(basePath)) {
            return 0;
        }
        
        LocalDateTime cutoff = LocalDateTime.now().minusHours(orphanedWorkspaceHours);
        int cleaned = 0;
        
        try (Stream<Path> workspaces = Files.list(basePath)) {
            List<Path> orphanedWorkspaces = workspaces
                .filter(Files::isDirectory)
                .filter(path -> isOlderThan(path, cutoff))
                .filter(path -> !hasSessionIndicators(path))
                .toList();
            
            for (Path workspace : orphanedWorkspaces) {
                try {
                    secureDeleteDirectory(workspace);
                    cleaned++;
                } catch (Exception e) {
                    log.warn("Failed to clean orphaned workspace: {}", workspace, e);
                }
            }
        }
        
        return cleaned;
    }

    /**
     * Cleans up temporary files and incomplete workspaces
     */
    private int cleanupTemporaryFiles() throws IOException {
        Path basePath = Paths.get(baseWorkspacePath);
        if (!Files.exists(basePath)) {
            return 0;
        }
        
        LocalDateTime cutoff = LocalDateTime.now().minusHours(tempFilesHours);
        int cleaned = 0;
        
        try (Stream<Path> items = Files.list(basePath)) {
            List<Path> tempItems = items
                .filter(path -> isTemporaryItem(path))
                .filter(path -> isOlderThan(path, cutoff))
                .toList();
            
            for (Path item : tempItems) {
                try {
                    if (Files.isDirectory(item)) {
                        secureDeleteDirectory(item);
                    } else {
                        Files.delete(item);
                    }
                    cleaned++;
                } catch (Exception e) {
                    log.warn("Failed to clean temporary item: {}", item, e);
                }
            }
        }
        
        return cleaned;
    }

    /**
     * Cleans up workspaces that exceed size limits
     */
    private int cleanupOversizedWorkspaces() throws IOException {
        Path basePath = Paths.get(baseWorkspacePath);
        if (!Files.exists(basePath)) {
            return 0;
        }
        
        int cleaned = 0;
        
        try (Stream<Path> workspaces = Files.list(basePath)) {
            List<Path> oversizedWorkspaces = workspaces
                .filter(Files::isDirectory)
                .filter(this::isOversized)
                .toList();
            
            for (Path workspace : oversizedWorkspaces) {
                try {
                    log.warn("Removing oversized workspace: {} ({}MB)", 
                            workspace, calculateDirectorySizeMb(workspace));
                    secureDeleteDirectory(workspace);
                    cleaned++;
                } catch (Exception e) {
                    log.warn("Failed to clean oversized workspace: {}", workspace, e);
                }
            }
        }
        
        return cleaned;
    }

    /**
     * Cleans up based on total storage limit
     */
    private int cleanupByStorageLimit() throws IOException {
        Path basePath = Paths.get(baseWorkspacePath);
        if (!Files.exists(basePath)) {
            return 0;
        }
        
        int cleaned = 0;
        double targetFreeGb = maxTotalSizeGb * 0.2; // Free 20% of limit
        double currentFreeGb = 0;
        
        try (Stream<Path> workspaces = Files.list(basePath)) {
            List<Path> workspacesByAge = workspaces
                .filter(Files::isDirectory)
                .sorted(this::compareByLastModified) // Oldest first
                .toList();
            
            for (Path workspace : workspacesByAge) {
                if (currentFreeGb >= targetFreeGb) {
                    break;
                }
                
                try {
                    double sizeGb = calculateDirectorySizeGb(workspace);
                    secureDeleteDirectory(workspace);
                    currentFreeGb += sizeGb;
                    cleaned++;
                } catch (Exception e) {
                    log.warn("Failed to clean workspace for storage limit: {}", workspace, e);
                }
            }
        }
        
        return cleaned;
    }

    // Helper methods

    private boolean isObviouslyAbandoned(Path workspace) {
        try {
            // Check for lock files, incomplete states, etc.
            return !Files.exists(workspace.resolve(".git")) ||
                   Files.exists(workspace.resolve(".codemate_incomplete"));
        } catch (Exception e) {
            return false;
        }
    }

    private boolean isOlderThan(Path path, LocalDateTime cutoff) {
        try {
            LocalDateTime modified = LocalDateTime.ofInstant(
                Files.getLastModifiedTime(path).toInstant(),
                java.time.ZoneId.systemDefault()
            );
            return modified.isBefore(cutoff);
        } catch (Exception e) {
            return false;
        }
    }

    private boolean hasSessionIndicators(Path workspace) {
        return Files.exists(workspace.resolve(".codemate_security")) ||
               Files.exists(workspace.resolve(".codemate_scenario"));
    }

    private boolean isTemporaryItem(Path path) {
        String name = path.getFileName().toString();
        return name.startsWith("tmp_") || 
               name.startsWith("temp_") ||
               name.endsWith(".tmp") ||
               name.endsWith(".temp");
    }

    private boolean isOversized(Path workspace) {
        try {
            return calculateDirectorySizeMb(workspace) > maxWorkspaceSizeMb;
        } catch (Exception e) {
            return false;
        }
    }

    private long calculateDirectorySizeMb(Path directory) {
        try {
            return Files.walk(directory)
                .filter(Files::isRegularFile)
                .mapToLong(path -> {
                    try {
                        return Files.size(path);
                    } catch (IOException e) {
                        return 0;
                    }
                })
                .sum() / (1024 * 1024);
        } catch (IOException e) {
            return 0;
        }
    }

    private double calculateDirectorySizeGb(Path directory) {
        return calculateDirectorySizeMb(directory) / 1024.0;
    }

    private double getTotalStorageUsageGb() {
        try {
            Path basePath = Paths.get(baseWorkspacePath);
            if (!Files.exists(basePath)) {
                return 0;
            }
            return calculateDirectorySizeGb(basePath);
        } catch (Exception e) {
            return 0;
        }
    }

    private int compareBySize(Path a, Path b) {
        return Long.compare(calculateDirectorySizeMb(b), calculateDirectorySizeMb(a));
    }

    private int compareByLastModified(Path a, Path b) {
        try {
            return Files.getLastModifiedTime(a).compareTo(Files.getLastModifiedTime(b));
        } catch (IOException e) {
            return 0;
        }
    }

    private void secureDeleteDirectory(Path directory) throws IOException {
        Files.walk(directory)
            .sorted((a, b) -> b.compareTo(a)) // Delete files before directories
            .forEach(path -> {
                try {
                    Files.delete(path);
                } catch (IOException e) {
                    log.warn("Failed to delete: {}", path, e);
                }
            });
    }

    private EmergencyCleanupResult.WorkspaceCleanup cleanupWorkspaceEmergency(Path workspace) {
        try {
            double sizeGb = calculateDirectorySizeGb(workspace);
            secureDeleteDirectory(workspace);
            return new EmergencyCleanupResult.WorkspaceCleanup(true, sizeGb);
        } catch (Exception e) {
            log.warn("Failed to clean workspace in emergency: {}", workspace, e);
            return new EmergencyCleanupResult.WorkspaceCleanup(false, 0);
        }
    }

    private void updateCleanupStatistics(ComprehensiveCleanupResult result, long startTime) {
        totalFilesDeleted.addAndGet(result.getTotalItemsDeleted());
        totalSpaceFreed.addAndGet(result.getTotalSpaceFreedMb());
    }

    private void logCleanupResults(ComprehensiveCleanupResult result) {
        log.info("Comprehensive cleanup completed: {} items deleted, {} MB freed",
                result.getTotalItemsDeleted(), result.getTotalSpaceFreedMb());
    }

    /**
     * Application shutdown hook to clean up resources
     */
    @PreDestroy
    public void onShutdown() {
        log.info("Performing shutdown cleanup");
        try {
            performQuickCleanup();
        } catch (Exception e) {
            log.warn("Error during shutdown cleanup", e);
        }
    }

    /**
     * Health indicator implementation
     */
    @Override
    public Health health() {
        try {
            double storageUsageGb = getTotalStorageUsageGb();
            double storagePercentage = (storageUsageGb / maxTotalSizeGb) * 100;
            
            Health.Builder builder;
            if (storagePercentage > 90) {
                builder = Health.down();
            } else if (storagePercentage > 75) {
                builder = Health.up().withDetail("status", "WARNING");
            } else {
                builder = Health.up();
            }
            
            return builder
                   .withDetail("storageUsageGb", storageUsageGb)
                   .withDetail("storageUsagePercentage", String.format("%.1f%%", storagePercentage))
                   .withDetail("lastCleanupTime", lastCleanupTime)
                   .withDetail("totalCleanupOperations", totalCleanupOperations.get())
                   .withDetail("totalSpaceFreedMb", totalSpaceFreed.get())
                   .build();
                   
        } catch (Exception e) {
            return Health.down().withException(e).build();
        }
    }

    // Result classes
    public static class ComprehensiveCleanupResult {
        private int abandonedSessions = 0;
        private int orphanedWorkspaces = 0;
        private int tempFiles = 0;
        private int oversizedWorkspaces = 0;
        private int storageCleanup = 0;
        private long totalSpaceFreedMb = 0;

        public void addAbandonedSessions(int count) { this.abandonedSessions += count; }
        public void addOrphanedWorkspaces(int count) { this.orphanedWorkspaces += count; }
        public void addTempFiles(int count) { this.tempFiles += count; }
        public void addOversizedWorkspaces(int count) { this.oversizedWorkspaces += count; }
        public void addStorageCleanup(int count) { this.storageCleanup += count; }

        public int getTotalItemsDeleted() {
            return abandonedSessions + orphanedWorkspaces + tempFiles + oversizedWorkspaces + storageCleanup;
        }
        
        public long getTotalSpaceFreedMb() { return totalSpaceFreedMb; }
    }

    public static class QuickCleanupResult {
        private int cleanedSessions = 0;
        private long spaceFreedMb = 0;

        public void incrementCleanedSessions() { this.cleanedSessions++; }
        public void addSpaceFreed(long mb) { this.spaceFreedMb += mb; }
        
        public int getCleanedSessions() { return cleanedSessions; }
        public long getSpaceFreedMb() { return spaceFreedMb; }
    }

    public static class EmergencyCleanupResult {
        private int workspacesRemoved = 0;
        private double spaceFreedGb = 0;

        public void addWorkspace(double sizeGb) {
            this.workspacesRemoved++;
            this.spaceFreedGb += sizeGb;
        }
        
        public int getWorkspacesRemoved() { return workspacesRemoved; }
        public double getSpaceFreedGb() { return spaceFreedGb; }

        public static class WorkspaceCleanup {
            private final boolean success;
            private final double sizeGb;

            public WorkspaceCleanup(boolean success, double sizeGb) {
                this.success = success;
                this.sizeGb = sizeGb;
            }

            public boolean isSuccess() { return success; }
            public double getSizeGb() { return sizeGb; }
        }
    }

    public static class UserCleanupResult {
        private int workspacesRemoved = 0;

        public void incrementWorkspacesRemoved() { this.workspacesRemoved++; }
        public int getWorkspacesRemoved() { return workspacesRemoved; }
    }
}
