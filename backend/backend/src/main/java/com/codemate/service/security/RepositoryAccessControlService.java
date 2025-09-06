package com.codemate.service.security;

import com.codemate.model.GitRepository;
import com.codemate.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service responsible for repository access control and permissions
 * Ensures users can only access repositories they own or have permission to use
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RepositoryAccessControlService {

    private final SecurityAuditService securityAuditService;

    @Value("${app.git.permissions.enable-strict-access:true}")
    private boolean enableStrictAccess;

    @Value("${app.git.permissions.demo-access-timeout-hours:24}")
    private int demoAccessTimeoutHours;

    @Value("${app.git.permissions.shared-repository-enabled:false}")
    private boolean sharedRepositoryEnabled;

    // Repository access permissions cache
    private final ConcurrentHashMap<String, RepositoryPermission> repositoryPermissions = new ConcurrentHashMap<>();
    
    // Demo repository access tracking
    private final ConcurrentHashMap<Long, LocalDateTime> demoRepositoryAccess = new ConcurrentHashMap<>();

    /**
     * Checks if user has permission to access a repository
     */
    public void validateRepositoryAccess(GitRepository repository, UserPrincipal userPrincipal, String operation) {
        if (!enableStrictAccess) {
            log.debug("Strict access control disabled, allowing access");
            return;
        }

        Long userId = userPrincipal.getId();
        String username = userPrincipal.getUsername();

        log.debug("Validating repository access for user {} on repository {} for operation {}", 
                 username, repository.getId(), operation);

        // Check repository ownership
        if (isRepositoryOwner(repository, userId)) {
            log.debug("User {} is owner of repository {}", username, repository.getId());
            return;
        }

        // Check demo repository access
        if (isDemoRepository(repository) && validateDemoAccess(userId, username, repository)) {
            log.debug("User {} has valid demo access to repository {}", username, repository.getId());
            return;
        }

        // Check shared repository permissions
        if (sharedRepositoryEnabled && hasSharedPermission(repository, userId, operation)) {
            log.debug("User {} has shared permission for repository {} operation {}", 
                     username, repository.getId(), operation);
            return;
        }

        // Access denied
        String reason = String.format("User %s does not have permission to %s repository %d", 
                                    username, operation, repository.getId());
        
        securityAuditService.logSecurityViolation(userId, username, "ACCESS_DENIED", 
            operation, reason, getCurrentIpAddress());
        
        throw new SecurityException("Access denied: " + reason);
    }

    /**
     * Grants temporary demo repository access to a user
     */
    public void grantDemoRepositoryAccess(GitRepository repository, UserPrincipal userPrincipal) {
        if (!isDemoRepository(repository)) {
            throw new SecurityException("Repository is not a demo repository");
        }

        Long userId = userPrincipal.getId();
        String username = userPrincipal.getUsername();
        
        demoRepositoryAccess.put(userId, LocalDateTime.now());
        
        log.info("Granted demo repository access to user: {} for repository: {}", 
                username, repository.getId());
        
        securityAuditService.logWorkspaceEvent(userId, username, "DEMO_ACCESS_GRANTED", 
            repository.getId().toString(), true, "Demo repository access granted");
    }

    /**
     * Creates a repository permission entry
     */
    public void createRepositoryPermission(Long repositoryId, Long userId, String permission, 
                                         LocalDateTime expiresAt, String grantedBy) {
        String permissionKey = repositoryId + ":" + userId;
        
        RepositoryPermission repoPermission = new RepositoryPermission(
            repositoryId, userId, permission, expiresAt, grantedBy, LocalDateTime.now()
        );
        
        repositoryPermissions.put(permissionKey, repoPermission);
        
        log.info("Created repository permission: repository={}, user={}, permission={}, grantedBy={}", 
                repositoryId, userId, permission, grantedBy);
        
        securityAuditService.logAdminEvent(null, grantedBy, "PERMISSION_GRANTED", 
            "Repository:" + repositoryId + " User:" + userId, true, "Permission: " + permission);
    }

    /**
     * Revokes repository permission
     */
    public void revokeRepositoryPermission(Long repositoryId, Long userId, String revokedBy) {
        String permissionKey = repositoryId + ":" + userId;
        RepositoryPermission removed = repositoryPermissions.remove(permissionKey);
        
        if (removed != null) {
            log.info("Revoked repository permission: repository={}, user={}, revokedBy={}", 
                    repositoryId, userId, revokedBy);
            
            securityAuditService.logAdminEvent(null, revokedBy, "PERMISSION_REVOKED", 
                "Repository:" + repositoryId + " User:" + userId, true, "Permission revoked");
        }
    }

    /**
     * Gets all permissions for a repository
     */
    public Set<RepositoryPermission> getRepositoryPermissions(Long repositoryId) {
        return repositoryPermissions.values().stream()
            .filter(permission -> permission.getRepositoryId().equals(repositoryId))
            .filter(permission -> !permission.isExpired())
            .collect(java.util.stream.Collectors.toSet());
    }

    /**
     * Gets all permissions for a user
     */
    public Set<RepositoryPermission> getUserPermissions(Long userId) {
        return repositoryPermissions.values().stream()
            .filter(permission -> permission.getUserId().equals(userId))
            .filter(permission -> !permission.isExpired())
            .collect(java.util.stream.Collectors.toSet());
    }

    /**
     * Validates repository ownership
     */
    private boolean isRepositoryOwner(GitRepository repository, Long userId) {
        return repository.getUserId() != null && repository.getUserId().equals(userId);
    }

    /**
     * Checks if repository is a demo repository
     */
    private boolean isDemoRepository(GitRepository repository) {
        return "DEMO".equals(repository.getScenarioId()) || 
               (repository.getName() != null && repository.getName().startsWith("demo-repository-"));
    }

    /**
     * Validates demo repository access
     */
    private boolean validateDemoAccess(Long userId, String username, GitRepository repository) {
        LocalDateTime lastAccess = demoRepositoryAccess.get(userId);
        
        if (lastAccess == null) {
            log.debug("No demo access recorded for user: {}", username);
            return false;
        }
        
        LocalDateTime expiryTime = lastAccess.plusHours(demoAccessTimeoutHours);
        
        if (LocalDateTime.now().isAfter(expiryTime)) {
            log.debug("Demo access expired for user: {}", username);
            demoRepositoryAccess.remove(userId);
            return false;
        }
        
        return true;
    }

    /**
     * Checks shared repository permissions
     */
    private boolean hasSharedPermission(GitRepository repository, Long userId, String operation) {
        String permissionKey = repository.getId() + ":" + userId;
        RepositoryPermission permission = repositoryPermissions.get(permissionKey);
        
        if (permission == null || permission.isExpired()) {
            return false;
        }
        
        return permission.allowsOperation(operation);
    }

    /**
     * Gets current IP address (simplified for this example)
     */
    private String getCurrentIpAddress() {
        // In a real implementation, this would extract IP from request context
        return "unknown";
    }

    /**
     * Validates that user can perform specific operation on repository
     */
    public void validateRepositoryOperation(GitRepository repository, UserPrincipal userPrincipal, 
                                          String operation, String command) {
        validateRepositoryAccess(repository, userPrincipal, operation);
        
        // Additional operation-specific validations
        validateOperationPermissions(repository, userPrincipal, operation, command);
    }

    /**
     * Validates operation-specific permissions
     */
    private void validateOperationPermissions(GitRepository repository, UserPrincipal userPrincipal, 
                                            String operation, String command) {
        String username = userPrincipal.getUsername();
        Long userId = userPrincipal.getId();
        
        // Demo repositories have restricted operations
        if (isDemoRepository(repository)) {
            validateDemoOperations(command, userId, username);
        }
        
        // Additional operation validations can be added here
        log.debug("Operation permission validation passed for user {} operation {} command {}", 
                 username, operation, command);
    }

    /**
     * Validates operations allowed on demo repositories
     */
    private void validateDemoOperations(String command, Long userId, String username) {
        // Define allowed demo operations
        Set<String> allowedDemoOperations = Set.of(
            "git status", "git log", "git diff", "git show", "git branch",
            "git add", "git commit", "git checkout", "git init", "git fs"
        );
        
        String baseCommand = command.toLowerCase().trim();
        if (baseCommand.startsWith("git ")) {
            String[] parts = baseCommand.split("\\s+", 2);
            if (parts.length >= 2) {
                String gitSubcommand = "git " + parts[1].split("\\s+")[0];
                
                if (!allowedDemoOperations.stream().anyMatch(allowed -> baseCommand.startsWith(allowed))) {
                    securityAuditService.logSecurityViolation(userId, username, "DEMO_OPERATION_DENIED", 
                        command, "Operation not allowed in demo mode", getCurrentIpAddress());
                    
                    throw new SecurityException("Operation not allowed in demo mode: " + command);
                }
            }
        }
    }

    /**
     * Cleans up expired permissions and demo access
     */
    public void cleanupExpiredPermissions() {
        // Remove expired repository permissions
        repositoryPermissions.entrySet().removeIf(entry -> entry.getValue().isExpired());
        
        // Remove expired demo access
        LocalDateTime cutoff = LocalDateTime.now().minusHours(demoAccessTimeoutHours);
        demoRepositoryAccess.entrySet().removeIf(entry -> entry.getValue().isBefore(cutoff));
        
        log.debug("Cleaned up expired permissions and demo access");
    }

    /**
     * Repository permission data structure
     */
    public static class RepositoryPermission {
        private final Long repositoryId;
        private final Long userId;
        private final String permission; // READ, WRITE, ADMIN
        private final LocalDateTime expiresAt;
        private final String grantedBy;
        private final LocalDateTime createdAt;

        public RepositoryPermission(Long repositoryId, Long userId, String permission, 
                                  LocalDateTime expiresAt, String grantedBy, LocalDateTime createdAt) {
            this.repositoryId = repositoryId;
            this.userId = userId;
            this.permission = permission;
            this.expiresAt = expiresAt;
            this.grantedBy = grantedBy;
            this.createdAt = createdAt;
        }

        public boolean isExpired() {
            return expiresAt != null && LocalDateTime.now().isAfter(expiresAt);
        }

        public boolean allowsOperation(String operation) {
            switch (permission.toUpperCase()) {
                case "ADMIN":
                    return true; // Admin can do everything
                case "WRITE":
                    return !operation.equals("DELETE") && !operation.equals("ADMIN");
                case "READ":
                    return operation.equals("READ") || operation.equals("view");
                default:
                    return false;
            }
        }

        // Getters
        public Long getRepositoryId() { return repositoryId; }
        public Long getUserId() { return userId; }
        public String getPermission() { return permission; }
        public LocalDateTime getExpiresAt() { return expiresAt; }
        public String getGrantedBy() { return grantedBy; }
        public LocalDateTime getCreatedAt() { return createdAt; }
    }
}
