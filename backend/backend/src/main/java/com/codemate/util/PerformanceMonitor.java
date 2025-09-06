package com.codemate.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Component
public class PerformanceMonitor {
    
    private static final Logger performanceLogger = LoggerFactory.getLogger("performance");
    private static final Logger log = LoggerFactory.getLogger(PerformanceMonitor.class);
    
    private final ConcurrentHashMap<String, AtomicLong> metrics = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Long> startTimes = new ConcurrentHashMap<>();
    
    /**
     * Records the start time of an operation
     */
    public void startOperation(String operationName, Long userId, Long repositoryId, String scenarioId) {
        String operationId = generateOperationId(operationName, userId, repositoryId);
        startTimes.put(operationId, System.currentTimeMillis());
        
        // Set MDC context for logging
        if (userId != null) MDC.put("userId", userId.toString());
        if (repositoryId != null) MDC.put("repositoryId", repositoryId.toString());
        if (scenarioId != null) MDC.put("scenarioId", scenarioId);
        
        log.debug("Starting operation: {} for user: {} repository: {} scenario: {}", 
                 operationName, userId, repositoryId, scenarioId);
    }
    
    /**
     * Records the end time of an operation and logs performance metrics
     */
    public long endOperation(String operationName, Long userId, Long repositoryId) {
        String operationId = generateOperationId(operationName, userId, repositoryId);
        Long startTime = startTimes.remove(operationId);
        
        if (startTime == null) {
            log.warn("No start time found for operation: {}", operationId);
            return -1;
        }
        
        long duration = System.currentTimeMillis() - startTime;
        
        // Update metrics
        String metricKey = operationName + "_total_time";
        metrics.computeIfAbsent(metricKey, k -> new AtomicLong(0)).addAndGet(duration);
        
        String countKey = operationName + "_count";
        metrics.computeIfAbsent(countKey, k -> new AtomicLong(0)).incrementAndGet();
        
        // Log performance data
        performanceLogger.info("OPERATION:{} USER:{} REPO:{} DURATION:{}ms", 
                              operationName, userId, repositoryId, duration);
        
        // Log slow operations
        if (duration > getSlowOperationThreshold(operationName)) {
            log.warn("SLOW OPERATION detected: {} took {}ms for user: {} repository: {}", 
                    operationName, duration, userId, repositoryId);
        }
        
        log.debug("Completed operation: {} in {}ms for user: {} repository: {}", 
                 operationName, duration, userId, repositoryId);
        
        // Clear MDC context
        MDC.clear();
        
        return duration;
    }
    
    /**
     * Records a simple metric value
     */
    public void recordMetric(String metricName, long value) {
        metrics.computeIfAbsent(metricName, k -> new AtomicLong(0)).addAndGet(value);
        performanceLogger.info("METRIC:{} VALUE:{}", metricName, value);
    }
    
    /**
     * Gets the current value of a metric
     */
    public long getMetric(String metricName) {
        AtomicLong metric = metrics.get(metricName);
        return metric != null ? metric.get() : 0;
    }
    
    /**
     * Gets the average execution time for an operation
     */
    public double getAverageExecutionTime(String operationName) {
        long totalTime = getMetric(operationName + "_total_time");
        long count = getMetric(operationName + "_count");
        
        return count > 0 ? (double) totalTime / count : 0.0;
    }
    
    /**
     * Logs current performance statistics
     */
    public void logPerformanceStats() {
        performanceLogger.info("=== PERFORMANCE STATISTICS ===");
        
        String[] operations = {"git_command_execution", "repository_state_fetch", 
                              "scenario_validation", "user_progress_update"};
        
        for (String operation : operations) {
            long count = getMetric(operation + "_count");
            double avgTime = getAverageExecutionTime(operation);
            
            if (count > 0) {
                performanceLogger.info("OPERATION:{} COUNT:{} AVG_TIME:{:.2f}ms", 
                                      operation, count, avgTime);
            }
        }
        
        performanceLogger.info("=== END PERFORMANCE STATISTICS ===");
    }
    
    /**
     * Records a Git command execution with detailed context
     */
    public void recordGitCommandExecution(String command, long duration, boolean successful, 
                                        Long userId, Long repositoryId, String scenarioId) {
        
        // Set MDC context
        if (userId != null) MDC.put("userId", userId.toString());
        if (repositoryId != null) MDC.put("repositoryId", repositoryId.toString());
        if (scenarioId != null) MDC.put("scenarioId", scenarioId);
        
        performanceLogger.info("GIT_COMMAND:{} DURATION:{}ms SUCCESS:{} USER:{} REPO:{} SCENARIO:{}", 
                              command, duration, successful, userId, repositoryId, scenarioId);
        
        // Track command-specific metrics
        recordMetric("git_command_total", 1);
        recordMetric("git_command_duration_total", duration);
        
        if (successful) {
            recordMetric("git_command_success", 1);
        } else {
            recordMetric("git_command_failure", 1);
        }
        
        // Track slow commands
        if (duration > 1000) { // Commands taking more than 1 second
            recordMetric("git_command_slow", 1);
            log.warn("SLOW GIT COMMAND: '{}' took {}ms for user: {} repository: {}", 
                    command, duration, userId, repositoryId);
        }
        
        MDC.clear();
    }
    
    /**
     * Records scenario completion metrics
     */
    public void recordScenarioCompletion(String scenarioId, Long userId, int steps, 
                                       int hintsUsed, long totalTime, boolean completed) {
        
        MDC.put("userId", userId.toString());
        MDC.put("scenarioId", scenarioId);
        
        performanceLogger.info("SCENARIO_COMPLETION:{} USER:{} STEPS:{} HINTS:{} TIME:{}ms COMPLETED:{}", 
                              scenarioId, userId, steps, hintsUsed, totalTime, completed);
        
        if (completed) {
            recordMetric("scenario_completed", 1);
            recordMetric("scenario_completion_time_total", totalTime);
            recordMetric("scenario_steps_total", steps);
            recordMetric("scenario_hints_total", hintsUsed);
        } else {
            recordMetric("scenario_abandoned", 1);
        }
        
        MDC.clear();
    }
    
    private String generateOperationId(String operationName, Long userId, Long repositoryId) {
        return String.format("%s_%s_%s_%s", 
                           operationName, 
                           userId != null ? userId : "anonymous",
                           repositoryId != null ? repositoryId : "no-repo",
                           Thread.currentThread().getName());
    }
    
    private long getSlowOperationThreshold(String operationName) {
        switch (operationName) {
            case "git_command_execution":
                return 500; // 500ms
            case "repository_state_fetch":
                return 200; // 200ms
            case "scenario_validation":
                return 100; // 100ms
            case "user_progress_update":
                return 300; // 300ms
            default:
                return 1000; // 1 second default
        }
    }
}
