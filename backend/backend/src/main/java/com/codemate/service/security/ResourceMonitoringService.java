package com.codemate.service.security;

import com.codemate.util.PerformanceMonitor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;
import java.nio.file.FileStore;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Service responsible for monitoring system resources and enforcing quotas
 * Protects against resource exhaustion attacks and ensures fair usage
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ResourceMonitoringService {

    private final PerformanceMonitor performanceMonitor;

    @Value("${app.git.resources.max-cpu-percent:80}")
    private double maxCpuPercent;

    @Value("${app.git.resources.max-memory-percent:70}")
    private double maxMemoryPercent;

    @Value("${app.git.resources.max-disk-percent:85}")
    private double maxDiskPercent;

    @Value("${app.git.resources.user-disk-quota-mb:500}")
    private long userDiskQuotaMb;

    @Value("${app.git.resources.user-memory-quota-mb:100}")
    private long userMemoryQuotaMb;

    @Value("${app.git.resources.max-file-count-per-user:1000}")
    private int maxFileCountPerUser;

    @Value("${app.git.workspace.base-path:${java.io.tmpdir}/codemate-git-workspaces}")
    private String baseWorkspacePath;

    @Value("${app.git.resources.monitoring-enabled:true}")
    private boolean monitoringEnabled;

    // Resource usage tracking
    private final ConcurrentHashMap<Long, UserResourceUsage> userResourceUsage = new ConcurrentHashMap<>();
    
    // System resource tracking
    private volatile SystemResourceStatus systemStatus = new SystemResourceStatus();
    
    // Resource usage limits
    private final AtomicLong totalSystemDiskUsage = new AtomicLong(0);
    private final AtomicLong totalSystemMemoryUsage = new AtomicLong(0);

    /**
     * Checks if system resources allow command execution
     */
    public void checkSystemResources() {
        if (!monitoringEnabled) {
            return;
        }

        log.debug("Checking system resource availability");

        // Check CPU usage
        if (systemStatus.getCpuUsagePercent() > maxCpuPercent) {
            throw new SecurityException(
                String.format("System CPU usage too high: %.1f%% (max: %.1f%%)", 
                    systemStatus.getCpuUsagePercent(), maxCpuPercent)
            );
        }

        // Check memory usage
        if (systemStatus.getMemoryUsagePercent() > maxMemoryPercent) {
            throw new SecurityException(
                String.format("System memory usage too high: %.1f%% (max: %.1f%%)", 
                    systemStatus.getMemoryUsagePercent(), maxMemoryPercent)
            );
        }

        // Check disk usage
        if (systemStatus.getDiskUsagePercent() > maxDiskPercent) {
            throw new SecurityException(
                String.format("System disk usage too high: %.1f%% (max: %.1f%%)", 
                    systemStatus.getDiskUsagePercent(), maxDiskPercent)
            );
        }

        log.debug("System resource check passed");
    }

    /**
     * Checks user-specific resource quotas
     */
    public void checkUserResourceQuota(Long userId, Path workspacePath) {
        if (!monitoringEnabled) {
            return;
        }

        log.debug("Checking resource quota for user: {}", userId);

        UserResourceUsage usage = getUserResourceUsage(userId, workspacePath);
        
        // Check disk quota
        if (usage.getDiskUsageMb() > userDiskQuotaMb) {
            throw new SecurityException(
                String.format("User disk quota exceeded: %dMB (max: %dMB)", 
                    usage.getDiskUsageMb(), userDiskQuotaMb)
            );
        }

        // Check file count
        if (usage.getFileCount() > maxFileCountPerUser) {
            throw new SecurityException(
                String.format("User file count exceeded: %d (max: %d)", 
                    usage.getFileCount(), maxFileCountPerUser)
            );
        }

        // Check memory usage (if tracking process-level memory)
        if (usage.getMemoryUsageMb() > userMemoryQuotaMb) {
            log.warn("User memory usage high: {}MB (max: {}MB) for user: {}", 
                usage.getMemoryUsageMb(), userMemoryQuotaMb, userId);
            // Don't throw exception for memory - just warn for now
        }

        log.debug("User resource quota check passed for user: {}", userId);
    }

    /**
     * Records resource usage for a user operation
     */
    public void recordResourceUsage(Long userId, String operation, long durationMs, long memoryUsedBytes) {
        if (!monitoringEnabled) {
            return;
        }

        UserResourceUsage usage = userResourceUsage.computeIfAbsent(userId, k -> new UserResourceUsage(userId));
        usage.recordOperation(operation, durationMs, memoryUsedBytes);
        
        // Update performance monitoring
        // performanceMonitor.recordResourceUsage(userId, operation, durationMs, memoryUsedBytes);
        
        log.debug("Recorded resource usage for user {} operation {}: {}ms, {}bytes", 
            userId, operation, durationMs, memoryUsedBytes);
    }

    /**
     * Gets current resource usage for a user
     */
    public UserResourceUsage getUserResourceUsage(Long userId, Path workspacePath) {
        UserResourceUsage usage = userResourceUsage.computeIfAbsent(userId, k -> new UserResourceUsage(userId));
        
        // Update disk usage from workspace
        if (workspacePath != null && Files.exists(workspacePath)) {
            try {
                long diskUsage = calculateDirectorySize(workspacePath);
                int fileCount = countFilesInDirectory(workspacePath);
                
                usage.setDiskUsageMb(diskUsage / (1024 * 1024));
                usage.setFileCount(fileCount);
                usage.setLastUpdated(LocalDateTime.now());
                
            } catch (IOException e) {
                log.warn("Failed to calculate disk usage for user {}: {}", userId, e.getMessage());
            }
        }
        
        return usage;
    }

    /**
     * Gets current system resource status
     */
    public SystemResourceStatus getSystemResourceStatus() {
        return systemStatus;
    }

    /**
     * Resets resource usage tracking for a user
     */
    public void resetUserResourceUsage(Long userId) {
        userResourceUsage.remove(userId);
        log.info("Reset resource usage tracking for user: {}", userId);
    }

    /**
     * Updates system resource status (scheduled task)
     */
    @Scheduled(fixedDelayString = "${app.git.resources.monitoring-interval-ms:30000}")
    public void updateSystemResourceStatus() {
        if (!monitoringEnabled) {
            return;
        }

        try {
            SystemResourceStatus newStatus = new SystemResourceStatus();
            
            // Update CPU usage
            OperatingSystemMXBean osBean = ManagementFactory.getOperatingSystemMXBean();
            if (osBean instanceof com.sun.management.OperatingSystemMXBean) {
                com.sun.management.OperatingSystemMXBean sunBean = (com.sun.management.OperatingSystemMXBean) osBean;
                newStatus.setCpuUsagePercent(sunBean.getProcessCpuLoad() * 100);
            }
            
            // Update memory usage
            Runtime runtime = Runtime.getRuntime();
            long totalMemory = runtime.totalMemory();
            long freeMemory = runtime.freeMemory();
            long usedMemory = totalMemory - freeMemory;
            long maxMemory = runtime.maxMemory();
            
            newStatus.setMemoryUsagePercent((double) usedMemory / maxMemory * 100);
            newStatus.setMemoryUsedMb(usedMemory / (1024 * 1024));
            newStatus.setMemoryTotalMb(maxMemory / (1024 * 1024));
            
            // Update disk usage
            updateDiskUsage(newStatus);
            
            newStatus.setLastUpdated(LocalDateTime.now());
            systemStatus = newStatus;
            
            // Log warning if resources are high
            if (newStatus.getCpuUsagePercent() > maxCpuPercent * 0.9 ||
                newStatus.getMemoryUsagePercent() > maxMemoryPercent * 0.9 ||
                newStatus.getDiskUsagePercent() > maxDiskPercent * 0.9) {
                
                log.warn("High system resource usage detected - CPU: {:.1f}%, Memory: {:.1f}%, Disk: {:.1f}%",
                    newStatus.getCpuUsagePercent(), newStatus.getMemoryUsagePercent(), newStatus.getDiskUsagePercent());
            }
            
        } catch (Exception e) {
            log.error("Error updating system resource status: {}", e.getMessage(), e);
        }
    }

    /**
     * Cleans up old resource usage data (scheduled task)
     */
    @Scheduled(fixedDelayString = "${app.git.resources.cleanup-interval-ms:3600000}") // 1 hour
    public void cleanupResourceUsageData() {
        LocalDateTime cutoff = LocalDateTime.now().minusHours(24);
        
        userResourceUsage.entrySet().removeIf(entry -> {
            UserResourceUsage usage = entry.getValue();
            return usage.getLastUpdated().isBefore(cutoff);
        });
        
        log.debug("Cleaned up old resource usage data");
    }

    /**
     * Updates disk usage information
     */
    private void updateDiskUsage(SystemResourceStatus status) {
        try {
            Path workspacePath = Paths.get(baseWorkspacePath);
            if (!Files.exists(workspacePath)) {
                Files.createDirectories(workspacePath);
            }
            
            FileStore store = Files.getFileStore(workspacePath);
            long totalSpace = store.getTotalSpace();
            long usableSpace = store.getUsableSpace();
            long usedSpace = totalSpace - usableSpace;
            
            status.setDiskUsagePercent((double) usedSpace / totalSpace * 100);
            status.setDiskUsedMb(usedSpace / (1024 * 1024));
            status.setDiskTotalMb(totalSpace / (1024 * 1024));
            
        } catch (IOException e) {
            log.warn("Failed to get disk usage information: {}", e.getMessage());
        }
    }

    /**
     * Calculates directory size in bytes
     */
    private long calculateDirectorySize(Path dirPath) throws IOException {
        return Files.walk(dirPath)
            .filter(Files::isRegularFile)
            .mapToLong(path -> {
                try {
                    return Files.size(path);
                } catch (IOException e) {
                    return 0;
                }
            })
            .sum();
    }

    /**
     * Counts files in directory
     */
    private int countFilesInDirectory(Path dirPath) throws IOException {
        return (int) Files.walk(dirPath)
            .filter(Files::isRegularFile)
            .count();
    }

    /**
     * User resource usage tracking
     */
    public static class UserResourceUsage {
        private final Long userId;
        private long diskUsageMb;
        private long memoryUsageMb;
        private int fileCount;
        private int totalOperations;
        private long totalExecutionTime;
        private LocalDateTime lastUpdated;

        public UserResourceUsage(Long userId) {
            this.userId = userId;
            this.lastUpdated = LocalDateTime.now();
        }

        public void recordOperation(String operation, long durationMs, long memoryUsedBytes) {
            this.totalOperations++;
            this.totalExecutionTime += durationMs;
            this.memoryUsageMb = Math.max(this.memoryUsageMb, memoryUsedBytes / (1024 * 1024));
            this.lastUpdated = LocalDateTime.now();
        }

        // Getters and setters
        public Long getUserId() { return userId; }
        public long getDiskUsageMb() { return diskUsageMb; }
        public void setDiskUsageMb(long diskUsageMb) { this.diskUsageMb = diskUsageMb; }
        public long getMemoryUsageMb() { return memoryUsageMb; }
        public void setMemoryUsageMb(long memoryUsageMb) { this.memoryUsageMb = memoryUsageMb; }
        public int getFileCount() { return fileCount; }
        public void setFileCount(int fileCount) { this.fileCount = fileCount; }
        public int getTotalOperations() { return totalOperations; }
        public long getTotalExecutionTime() { return totalExecutionTime; }
        public LocalDateTime getLastUpdated() { return lastUpdated; }
        public void setLastUpdated(LocalDateTime lastUpdated) { this.lastUpdated = lastUpdated; }
    }

    /**
     * System resource status
     */
    public static class SystemResourceStatus {
        private double cpuUsagePercent;
        private double memoryUsagePercent;
        private double diskUsagePercent;
        private long memoryUsedMb;
        private long memoryTotalMb;
        private long diskUsedMb;
        private long diskTotalMb;
        private LocalDateTime lastUpdated;

        public SystemResourceStatus() {
            this.lastUpdated = LocalDateTime.now();
        }

        // Getters and setters
        public double getCpuUsagePercent() { return cpuUsagePercent; }
        public void setCpuUsagePercent(double cpuUsagePercent) { this.cpuUsagePercent = cpuUsagePercent; }
        public double getMemoryUsagePercent() { return memoryUsagePercent; }
        public void setMemoryUsagePercent(double memoryUsagePercent) { this.memoryUsagePercent = memoryUsagePercent; }
        public double getDiskUsagePercent() { return diskUsagePercent; }
        public void setDiskUsagePercent(double diskUsagePercent) { this.diskUsagePercent = diskUsagePercent; }
        public long getMemoryUsedMb() { return memoryUsedMb; }
        public void setMemoryUsedMb(long memoryUsedMb) { this.memoryUsedMb = memoryUsedMb; }
        public long getMemoryTotalMb() { return memoryTotalMb; }
        public void setMemoryTotalMb(long memoryTotalMb) { this.memoryTotalMb = memoryTotalMb; }
        public long getDiskUsedMb() { return diskUsedMb; }
        public void setDiskUsedMb(long diskUsedMb) { this.diskUsedMb = diskUsedMb; }
        public long getDiskTotalMb() { return diskTotalMb; }
        public void setDiskTotalMb(long diskTotalMb) { this.diskTotalMb = diskTotalMb; }
        public LocalDateTime getLastUpdated() { return lastUpdated; }
        public void setLastUpdated(LocalDateTime lastUpdated) { this.lastUpdated = lastUpdated; }

        public boolean isHealthy(double maxCpu, double maxMemory, double maxDisk) {
            return cpuUsagePercent <= maxCpu && 
                   memoryUsagePercent <= maxMemory && 
                   diskUsagePercent <= maxDisk;
        }
    }
}
