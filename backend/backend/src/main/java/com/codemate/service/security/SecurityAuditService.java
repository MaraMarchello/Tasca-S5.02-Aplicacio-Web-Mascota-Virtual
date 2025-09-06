package com.codemate.service.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

/**
 * Service responsible for comprehensive security audit logging
 * Tracks all security-relevant events for compliance and forensic analysis
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SecurityAuditService {

    private final ObjectMapper objectMapper;

    @Value("${app.git.audit.enabled:true}")
    private boolean auditEnabled;

    @Value("${app.git.audit.log-path:${java.io.tmpdir}/codemate-audit}")
    private String auditLogPath;

    @Value("${app.git.audit.log-level:INFO}")
    private String auditLogLevel;

    @Value("${app.git.audit.include-command-output:false}")
    private boolean includeCommandOutput;

    @Value("${app.git.audit.max-log-file-size-mb:100}")
    private long maxLogFileSizeMb;

    private static final DateTimeFormatter TIMESTAMP_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");

    /**
     * Logs user authentication events
     */
    public void logAuthenticationEvent(Long userId, String username, String event, 
                                     String ipAddress, boolean successful, String details) {
        if (!auditEnabled) return;

        AuditEvent auditEvent = AuditEvent.builder()
            .eventType("AUTHENTICATION")
            .userId(userId)
            .username(username)
            .event(event)
            .successful(successful)
            .ipAddress(ipAddress)
            .details(details)
            .timestamp(LocalDateTime.now())
            .severity(successful ? "INFO" : "WARN")
            .build();

        logAuditEvent(auditEvent);
    }

    /**
     * Logs Git command execution events
     */
    public void logGitCommandEvent(Long userId, String username, String command, 
                                 String repositoryId, boolean successful, 
                                 String output, String errorOutput, long durationMs) {
        if (!auditEnabled) return;

        Map<String, Object> commandDetails = new HashMap<>();
        commandDetails.put("command", command);
        commandDetails.put("repositoryId", repositoryId);
        commandDetails.put("durationMs", durationMs);
        
        if (includeCommandOutput) {
            commandDetails.put("output", output != null ? output.substring(0, Math.min(output.length(), 1000)) : "");
            commandDetails.put("errorOutput", errorOutput != null ? errorOutput.substring(0, Math.min(errorOutput.length(), 500)) : "");
        }

        AuditEvent auditEvent = AuditEvent.builder()
            .eventType("GIT_COMMAND")
            .userId(userId)
            .username(username)
            .event("EXECUTE_COMMAND")
            .successful(successful)
            .details(mapToJson(commandDetails))
            .timestamp(LocalDateTime.now())
            .severity(successful ? "INFO" : "WARN")
            .build();

        logAuditEvent(auditEvent);
    }

    /**
     * Logs security violations
     */
    public void logSecurityViolation(Long userId, String username, String violationType, 
                                   String command, String reason, String ipAddress) {
        if (!auditEnabled) return;

        Map<String, Object> violationDetails = new HashMap<>();
        violationDetails.put("violationType", violationType);
        violationDetails.put("command", command);
        violationDetails.put("reason", reason);
        violationDetails.put("ipAddress", ipAddress);

        AuditEvent auditEvent = AuditEvent.builder()
            .eventType("SECURITY_VIOLATION")
            .userId(userId)
            .username(username)
            .event(violationType)
            .successful(false)
            .details(mapToJson(violationDetails))
            .timestamp(LocalDateTime.now())
            .severity("ERROR")
            .build();

        logAuditEvent(auditEvent);
        
        // Also log to application logs for immediate visibility
        log.error("SECURITY VIOLATION - User: {} ({}), Type: {}, Command: {}, Reason: {}, IP: {}", 
                 username, userId, violationType, command, reason, ipAddress);
    }

    /**
     * Logs workspace operations
     */
    public void logWorkspaceEvent(Long userId, String username, String operation, 
                                String workspacePath, boolean successful, String details) {
        if (!auditEnabled) return;

        Map<String, Object> workspaceDetails = new HashMap<>();
        workspaceDetails.put("operation", operation);
        workspaceDetails.put("workspacePath", workspacePath);
        workspaceDetails.put("additionalDetails", details);

        AuditEvent auditEvent = AuditEvent.builder()
            .eventType("WORKSPACE")
            .userId(userId)
            .username(username)
            .event(operation)
            .successful(successful)
            .details(mapToJson(workspaceDetails))
            .timestamp(LocalDateTime.now())
            .severity("INFO")
            .build();

        logAuditEvent(auditEvent);
    }

    /**
     * Logs rate limiting events
     */
    public void logRateLimitEvent(Long userId, String username, String limitType, 
                                String command, int currentCount, int maxLimit) {
        if (!auditEnabled) return;

        Map<String, Object> rateLimitDetails = new HashMap<>();
        rateLimitDetails.put("limitType", limitType);
        rateLimitDetails.put("command", command);
        rateLimitDetails.put("currentCount", currentCount);
        rateLimitDetails.put("maxLimit", maxLimit);

        AuditEvent auditEvent = AuditEvent.builder()
            .eventType("RATE_LIMIT")
            .userId(userId)
            .username(username)
            .event("LIMIT_EXCEEDED")
            .successful(false)
            .details(mapToJson(rateLimitDetails))
            .timestamp(LocalDateTime.now())
            .severity("WARN")
            .build();

        logAuditEvent(auditEvent);
    }

    /**
     * Logs resource quota events
     */
    public void logResourceQuotaEvent(Long userId, String username, String resourceType, 
                                    long currentUsage, long maxQuota, String unit) {
        if (!auditEnabled) return;

        Map<String, Object> quotaDetails = new HashMap<>();
        quotaDetails.put("resourceType", resourceType);
        quotaDetails.put("currentUsage", currentUsage);
        quotaDetails.put("maxQuota", maxQuota);
        quotaDetails.put("unit", unit);

        AuditEvent auditEvent = AuditEvent.builder()
            .eventType("RESOURCE_QUOTA")
            .userId(userId)
            .username(username)
            .event("QUOTA_EXCEEDED")
            .successful(false)
            .details(mapToJson(quotaDetails))
            .timestamp(LocalDateTime.now())
            .severity("WARN")
            .build();

        logAuditEvent(auditEvent);
    }

    /**
     * Logs system administration events
     */
    public void logAdminEvent(Long userId, String username, String action, 
                            String target, boolean successful, String details) {
        if (!auditEnabled) return;

        Map<String, Object> adminDetails = new HashMap<>();
        adminDetails.put("action", action);
        adminDetails.put("target", target);
        adminDetails.put("additionalDetails", details);

        AuditEvent auditEvent = AuditEvent.builder()
            .eventType("ADMIN_ACTION")
            .userId(userId)
            .username(username)
            .event(action)
            .successful(successful)
            .details(mapToJson(adminDetails))
            .timestamp(LocalDateTime.now())
            .severity("INFO")
            .build();

        logAuditEvent(auditEvent);
    }

    /**
     * Logs configuration changes
     */
    public void logConfigurationChange(Long userId, String username, String setting, 
                                     String oldValue, String newValue) {
        if (!auditEnabled) return;

        Map<String, Object> configDetails = new HashMap<>();
        configDetails.put("setting", setting);
        configDetails.put("oldValue", oldValue);
        configDetails.put("newValue", newValue);

        AuditEvent auditEvent = AuditEvent.builder()
            .eventType("CONFIGURATION")
            .userId(userId)
            .username(username)
            .event("SETTING_CHANGED")
            .successful(true)
            .details(mapToJson(configDetails))
            .timestamp(LocalDateTime.now())
            .severity("INFO")
            .build();

        logAuditEvent(auditEvent);
    }

    /**
     * Core method to log audit events
     */
    private void logAuditEvent(AuditEvent event) {
        try {
            // Write to audit log file
            writeToAuditLog(event);
            
            // Also log to application logger based on severity
            String logMessage = formatAuditLogMessage(event);
            switch (event.getSeverity()) {
                case "ERROR":
                    log.error("AUDIT: {}", logMessage);
                    break;
                case "WARN":
                    log.warn("AUDIT: {}", logMessage);
                    break;
                case "INFO":
                default:
                    log.info("AUDIT: {}", logMessage);
                    break;
            }
            
        } catch (Exception e) {
            log.error("Failed to write audit log: {}", e.getMessage(), e);
        }
    }

    /**
     * Writes audit event to dedicated log file
     */
    private void writeToAuditLog(AuditEvent event) throws IOException {
        Path auditDir = Paths.get(auditLogPath);
        Files.createDirectories(auditDir);
        
        String fileName = "security-audit-" + LocalDateTime.now().toLocalDate() + ".log";
        Path auditFile = auditDir.resolve(fileName);
        
        // Check file size and rotate if necessary
        if (Files.exists(auditFile) && Files.size(auditFile) > maxLogFileSizeMb * 1024 * 1024) {
            rotateAuditLog(auditFile);
        }
        
        String logLine = formatAuditLogLine(event) + System.lineSeparator();
        
        Files.write(auditFile, logLine.getBytes(), 
                   StandardOpenOption.CREATE, StandardOpenOption.APPEND);
    }

    /**
     * Rotates audit log file when it gets too large
     */
    private void rotateAuditLog(Path auditFile) throws IOException {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"));
        Path rotatedFile = auditFile.getParent().resolve(
            auditFile.getFileName() + "." + timestamp
        );
        
        Files.move(auditFile, rotatedFile);
        log.info("Rotated audit log file: {} to {}", auditFile, rotatedFile);
    }

    /**
     * Formats audit event for log file
     */
    private String formatAuditLogLine(AuditEvent event) {
        return String.format("%s|%s|%s|%s|%s|%s|%s|%s|%s|%s",
            event.getTimestamp().format(TIMESTAMP_FORMAT),
            event.getEventType(),
            event.getUserId() != null ? event.getUserId() : "SYSTEM",
            event.getUsername() != null ? event.getUsername() : "SYSTEM",
            event.getEvent(),
            event.isSuccessful() ? "SUCCESS" : "FAILURE",
            event.getSeverity(),
            event.getIpAddress() != null ? event.getIpAddress() : "N/A",
            event.getDetails() != null ? event.getDetails().replace("|", "\\|") : "N/A",
            Thread.currentThread().getName()
        );
    }

    /**
     * Formats audit event for application log
     */
    private String formatAuditLogMessage(AuditEvent event) {
        return String.format("[%s] User: %s (%s), Event: %s, Success: %s, Details: %s",
            event.getEventType(),
            event.getUsername() != null ? event.getUsername() : "SYSTEM",
            event.getUserId() != null ? event.getUserId() : "SYSTEM",
            event.getEvent(),
            event.isSuccessful(),
            event.getDetails() != null ? event.getDetails() : "N/A"
        );
    }

    /**
     * Converts map to JSON string
     */
    private String mapToJson(Map<String, Object> map) {
        try {
            return objectMapper.writeValueAsString(map);
        } catch (Exception e) {
            log.warn("Failed to serialize audit details to JSON: {}", e.getMessage());
            return map.toString();
        }
    }

    /**
     * Audit event data structure
     */
    public static class AuditEvent {
        private String eventType;
        private Long userId;
        private String username;
        private String event;
        private boolean successful;
        private String ipAddress;
        private String details;
        private LocalDateTime timestamp;
        private String severity;

        // Builder pattern
        public static AuditEventBuilder builder() {
            return new AuditEventBuilder();
        }

        public static class AuditEventBuilder {
            private final AuditEvent event = new AuditEvent();

            public AuditEventBuilder eventType(String eventType) { event.eventType = eventType; return this; }
            public AuditEventBuilder userId(Long userId) { event.userId = userId; return this; }
            public AuditEventBuilder username(String username) { event.username = username; return this; }
            public AuditEventBuilder event(String eventName) { event.event = eventName; return this; }
            public AuditEventBuilder successful(boolean successful) { event.successful = successful; return this; }
            public AuditEventBuilder ipAddress(String ipAddress) { event.ipAddress = ipAddress; return this; }
            public AuditEventBuilder details(String details) { event.details = details; return this; }
            public AuditEventBuilder timestamp(LocalDateTime timestamp) { event.timestamp = timestamp; return this; }
            public AuditEventBuilder severity(String severity) { event.severity = severity; return this; }

            public AuditEvent build() { return event; }
        }

        // Getters
        public String getEventType() { return eventType; }
        public Long getUserId() { return userId; }
        public String getUsername() { return username; }
        public String getEvent() { return event; }
        public boolean isSuccessful() { return successful; }
        public String getIpAddress() { return ipAddress; }
        public String getDetails() { return details; }
        public LocalDateTime getTimestamp() { return timestamp; }
        public String getSeverity() { return severity; }
    }
}
