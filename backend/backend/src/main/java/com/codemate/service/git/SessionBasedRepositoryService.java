package com.codemate.service.git;

import com.codemate.model.GitRepository;
import com.codemate.model.GitScenario;
import com.codemate.repository.GitScenarioRepository;
import com.codemate.service.security.SecurityAuditService;
import com.codemate.service.security.WorkspaceSecurityService;
import com.codemate.util.PerformanceMonitor;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service for managing session-based temporary Git repositories
 * Creates and manages isolated, temporary Git repositories for user sessions
 * with automatic cleanup and scenario-specific initialization
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SessionBasedRepositoryService {

    private final GitScenarioRepository gitScenarioRepository;
    private final WorkspaceSecurityService workspaceSecurityService;
    private final SecurityAuditService securityAuditService;
    private final PerformanceMonitor performanceMonitor;
    private final ObjectMapper objectMapper;

    @Value("${app.git.workspace.base-path:${java.io.tmpdir}/codemate-git-workspaces}")
    private String baseWorkspacePath;

    @Value("${app.git.session.repository-lifetime-hours:4}")
    private int repositoryLifetimeHours;

    @Value("${app.git.session.max-repositories-per-user:5}")
    private int maxRepositoriesPerUser;

    @Value("${app.git.session.cleanup-interval-minutes:30}")
    private int cleanupIntervalMinutes;

    // Session-based repository tracking
    private final ConcurrentHashMap<String, SessionRepository> sessionRepositories = new ConcurrentHashMap<>();
    
    // User repository count tracking
    private final ConcurrentHashMap<Long, Integer> userRepositoryCount = new ConcurrentHashMap<>();

    /**
     * Creates a new temporary Git repository for a user session
     */
    public SessionRepository createSessionRepository(Long userId, String username, String scenarioId) throws IOException {
        log.info("Creating session repository for user: {} scenario: {}", userId, scenarioId);

        // Check user repository limits
        checkUserRepositoryLimits(userId, username);

        // Generate unique session ID
        String sessionId = generateSessionId(userId, scenarioId);

        // Check if session repository already exists
        SessionRepository existingRepo = sessionRepositories.get(sessionId);
        if (existingRepo != null && !existingRepo.isExpired(repositoryLifetimeHours)) {
            log.debug("Reusing existing session repository: {}", sessionId);
            existingRepo.updateLastAccess();
            return existingRepo;
        }

        // Create new session repository
        SessionRepository sessionRepo = createNewSessionRepository(userId, username, scenarioId, sessionId);
        
        // Track repository
        sessionRepositories.put(sessionId, sessionRepo);
        userRepositoryCount.merge(userId, 1, Integer::sum);

        // Audit log creation
        securityAuditService.logWorkspaceEvent(userId, username, "SESSION_REPO_CREATED", 
            sessionRepo.getWorkspacePath().toString(), true, 
            "Scenario: " + scenarioId + ", Session: " + sessionId);

        log.info("Created session repository: {} at: {}", sessionId, sessionRepo.getWorkspacePath());
        return sessionRepo;
    }

    /**
     * Gets an existing session repository
     */
    public SessionRepository getSessionRepository(String sessionId) {
        SessionRepository repo = sessionRepositories.get(sessionId);
        if (repo != null && !repo.isExpired(repositoryLifetimeHours)) {
            repo.updateLastAccess();
            return repo;
        }
        return null;
    }

    /**
     * Gets or creates a session repository for a user and scenario
     */
    public SessionRepository getOrCreateSessionRepository(Long userId, String username, String scenarioId) throws IOException {
        String sessionId = generateSessionId(userId, scenarioId);
        SessionRepository existing = getSessionRepository(sessionId);
        
        if (existing != null) {
            return existing;
        }
        
        return createSessionRepository(userId, username, scenarioId);
    }

    /**
     * Destroys a session repository and cleans up all resources
     */
    public void destroySessionRepository(String sessionId, Long userId, String username) {
        SessionRepository sessionRepo = sessionRepositories.remove(sessionId);
        
        if (sessionRepo != null) {
            try {
                // Secure cleanup of workspace
                workspaceSecurityService.secureDeleteWorkspace(sessionRepo.getWorkspacePath(), userId);
                
                // Update user repository count
                userRepositoryCount.merge(userId, -1, (current, delta) -> Math.max(0, current + delta));
                
                // Audit log destruction
                securityAuditService.logWorkspaceEvent(userId, username, "SESSION_REPO_DESTROYED", 
                    sessionRepo.getWorkspacePath().toString(), true, "Manual destruction");
                
                log.info("Destroyed session repository: {} for user: {}", sessionId, userId);
                
            } catch (Exception e) {
                log.error("Error destroying session repository: {}", sessionId, e);
                
                securityAuditService.logWorkspaceEvent(userId, username, "SESSION_REPO_DESTROY_FAILED", 
                    sessionRepo.getWorkspacePath().toString(), false, e.getMessage());
            }
        }
    }

    /**
     * Lists all active session repositories for a user
     */
    public List<SessionRepository> getUserSessionRepositories(Long userId) {
        return sessionRepositories.values().stream()
            .filter(repo -> repo.getUserId().equals(userId))
            .filter(repo -> !repo.isExpired(repositoryLifetimeHours))
            .toList();
    }

    /**
     * Gets session repository statistics
     */
    public SessionRepositoryStats getStats() {
        long totalActive = sessionRepositories.values().stream()
            .filter(repo -> !repo.isExpired(repositoryLifetimeHours))
            .count();
        
        long totalExpired = sessionRepositories.values().stream()
            .filter(repo -> repo.isExpired(repositoryLifetimeHours))
            .count();
        
        return new SessionRepositoryStats(
            sessionRepositories.size(),
            totalActive,
            totalExpired,
            userRepositoryCount.size()
        );
    }

    /**
     * Creates a new session repository with full initialization
     */
    private SessionRepository createNewSessionRepository(Long userId, String username, 
                                                       String scenarioId, String sessionId) throws IOException {
        performanceMonitor.startOperation("session_repository_creation", userId, null, scenarioId);
        
        try {
            // Create secure workspace
            Path workspacePath = workspaceSecurityService.createSecureWorkspace(userId, null, sessionId);
            
            // Initialize Git repository
            initializeGitRepository(workspacePath);
            
            // Load scenario configuration
            GitScenario scenario = null;
            if (scenarioId != null && !scenarioId.equals("DEMO")) {
                scenario = gitScenarioRepository.findByScenarioId(scenarioId).orElse(null);
            }
            
            // Create session repository object
            SessionRepository sessionRepo = new SessionRepository(
                sessionId, userId, username, scenarioId, workspacePath, LocalDateTime.now()
            );
            
            // Initialize with scenario-specific state
            initializeScenarioState(sessionRepo, scenario);
            
            // Configure Git settings
            configureGitSettings(workspacePath, username);
            
            performanceMonitor.endOperation("session_repository_creation", userId, null);
            return sessionRepo;
            
        } catch (Exception e) {
            performanceMonitor.endOperation("session_repository_creation", userId, null);
            log.error("Failed to create session repository: {}", sessionId, e);
            throw new IOException("Failed to create session repository", e);
        }
    }

    /**
     * Initializes Git repository in workspace
     */
    private void initializeGitRepository(Path workspacePath) throws IOException {
        try {
            ProcessBuilder pb = new ProcessBuilder("git", "init");
            pb.directory(workspacePath.toFile());
            pb.redirectErrorStream(true);
            
            Process process = pb.start();
            int exitCode = process.waitFor();
            
            if (exitCode != 0) {
                throw new IOException("Git init failed with exit code: " + exitCode);
            }
            
            log.debug("Git repository initialized in: {}", workspacePath);
            
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Git init interrupted", e);
        }
    }

    /**
     * Initializes repository with scenario-specific state
     */
    private void initializeScenarioState(SessionRepository sessionRepo, GitScenario scenario) throws IOException {
        Path workspacePath = sessionRepo.getWorkspacePath();
        
        if (scenario != null && scenario.getInitialState() != null) {
            log.debug("Initializing scenario state for: {}", scenario.getScenarioId());
            
            try {
                JsonNode initialState = objectMapper.readTree(scenario.getInitialState());
                
                // Create initial files
                if (initialState.has("files")) {
                    createInitialFiles(workspacePath, initialState.get("files"));
                }
                
                // Create initial commits
                if (initialState.has("commits")) {
                    createInitialCommits(workspacePath, initialState.get("commits"), sessionRepo.getUsername());
                }
                
                // Set up branches
                if (initialState.has("branches")) {
                    createInitialBranches(workspacePath, initialState.get("branches"));
                }
                
                // Create scenario metadata
                createScenarioMetadata(workspacePath, scenario);
                
            } catch (Exception e) {
                log.warn("Failed to setup scenario state, using empty repository: {}", e.getMessage());
                // Continue with empty repository
            }
        } else {
            // Create empty repository with basic structure
            createBasicRepository(workspacePath, sessionRepo.getUsername());
        }
    }

    /**
     * Creates initial files from scenario configuration
     */
    private void createInitialFiles(Path workspacePath, JsonNode filesNode) throws IOException {
        if (filesNode.isObject()) {
            filesNode.fields().forEachRemaining(entry -> {
                String fileName = entry.getKey();
                String content = entry.getValue().asText();
                
                try {
                    Path filePath = workspacePath.resolve(fileName);
                    Files.createDirectories(filePath.getParent());
                    Files.write(filePath, content.getBytes(), StandardOpenOption.CREATE);
                    log.debug("Created initial file: {}", fileName);
                } catch (IOException e) {
                    log.warn("Failed to create initial file {}: {}", fileName, e.getMessage());
                }
            });
        }
    }

    /**
     * Creates initial commits from scenario configuration
     */
    private void createInitialCommits(Path workspacePath, JsonNode commitsNode, String username) throws IOException {
        if (commitsNode.isArray()) {
            for (JsonNode commitNode : commitsNode) {
                try {
                    String message = commitNode.get("message").asText();
                    
                    // Add all files to staging
                    executeGitCommand(workspacePath, "git", "add", ".");
                    
                    // Create commit
                    executeGitCommand(workspacePath, "git", "commit", "-m", message, 
                        "--author", username + " <" + username + "@codemate.local>");
                    
                    log.debug("Created initial commit: {}", message);
                    
                } catch (Exception e) {
                    log.warn("Failed to create initial commit: {}", e.getMessage());
                }
            }
        }
    }

    /**
     * Creates initial branches from scenario configuration
     */
    private void createInitialBranches(Path workspacePath, JsonNode branchesNode) throws IOException {
        if (branchesNode.isArray()) {
            for (JsonNode branchNode : branchesNode) {
                try {
                    String branchName = branchNode.asText();
                    executeGitCommand(workspacePath, "git", "branch", branchName);
                    log.debug("Created initial branch: {}", branchName);
                } catch (Exception e) {
                    log.warn("Failed to create initial branch {}: {}", branchNode.asText(), e.getMessage());
                }
            }
        }
    }

    /**
     * Creates scenario metadata file
     */
    private void createScenarioMetadata(Path workspacePath, GitScenario scenario) throws IOException {
        Path metadataFile = workspacePath.resolve(".codemate_scenario");
        
        String metadata = String.format(
            "# CodeMate Scenario Metadata\n" +
            "scenario_id=%s\n" +
            "scenario_title=%s\n" +
            "difficulty=%s\n" +
            "created_timestamp=%d\n",
            scenario.getScenarioId(),
            scenario.getTitle() != null ? scenario.getTitle() : "Unknown",
            scenario.getLevel() != null ? scenario.getLevel().toString() : "BEGINNER",
            System.currentTimeMillis()
        );
        
        Files.write(metadataFile, metadata.getBytes(), StandardOpenOption.CREATE);
    }

    /**
     * Creates basic empty repository structure
     */
    private void createBasicRepository(Path workspacePath, String username) throws IOException {
        // Create README file
        Path readmePath = workspacePath.resolve("README.md");
        String readmeContent = "# Git Learning Repository\n\nThis is your practice Git repository. Happy learning!\n";
        Files.write(readmePath, readmeContent.getBytes(), StandardOpenOption.CREATE);
        
        // Create initial commit
        executeGitCommand(workspacePath, "git", "add", "README.md");
        executeGitCommand(workspacePath, "git", "commit", "-m", "Initial commit", 
            "--author", username + " <" + username + "@codemate.local>");
    }

    /**
     * Configures Git settings for the workspace
     */
    private void configureGitSettings(Path workspacePath, String username) throws IOException {
        executeGitCommand(workspacePath, "git", "config", "user.name", username);
        executeGitCommand(workspacePath, "git", "config", "user.email", username + "@codemate.local");
        executeGitCommand(workspacePath, "git", "config", "init.defaultBranch", "main");
    }

    /**
     * Executes a Git command in the workspace
     */
    private void executeGitCommand(Path workspacePath, String... command) throws IOException {
        try {
            ProcessBuilder pb = new ProcessBuilder(command);
            pb.directory(workspacePath.toFile());
            pb.redirectErrorStream(true);
            
            Process process = pb.start();
            int exitCode = process.waitFor();
            
            if (exitCode != 0) {
                log.warn("Git command failed: {} (exit code: {})", String.join(" ", command), exitCode);
            }
            
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Git command interrupted", e);
        }
    }

    /**
     * Checks user repository limits
     */
    private void checkUserRepositoryLimits(Long userId, String username) {
        int currentCount = userRepositoryCount.getOrDefault(userId, 0);
        
        if (currentCount >= maxRepositoriesPerUser) {
            securityAuditService.logSecurityViolation(userId, username, "REPOSITORY_LIMIT_EXCEEDED", 
                "create_repository", "User has " + currentCount + " repositories (max: " + maxRepositoriesPerUser + ")", 
                "system");
            
            throw new SecurityException("Maximum repositories per user exceeded: " + currentCount + " >= " + maxRepositoriesPerUser);
        }
    }

    /**
     * Generates unique session ID
     */
    private String generateSessionId(Long userId, String scenarioId) {
        return String.format("session_%d_%s_%d", 
            userId, 
            scenarioId != null ? scenarioId : "demo",
            System.currentTimeMillis()
        );
    }

    /**
     * Scheduled cleanup of expired session repositories
     */
    @Scheduled(fixedDelayString = "${app.git.session.cleanup-interval-minutes:30}*60*1000")
    public void cleanupExpiredSessionRepositories() {
        log.debug("Starting cleanup of expired session repositories");
        
        int cleanedCount = 0;
        
        // Find and remove expired repositories
        sessionRepositories.entrySet().removeIf(entry -> {
            SessionRepository repo = entry.getValue();
            
            if (repo.isExpired(repositoryLifetimeHours)) {
                try {
                    // Secure cleanup
                    workspaceSecurityService.secureDeleteWorkspace(repo.getWorkspacePath(), repo.getUserId());
                    
                    // Update user count
                    userRepositoryCount.merge(repo.getUserId(), -1, 
                        (current, delta) -> Math.max(0, current + delta));
                    
                    // Audit log cleanup
                    securityAuditService.logWorkspaceEvent(repo.getUserId(), repo.getUsername(), 
                        "SESSION_REPO_EXPIRED", repo.getWorkspacePath().toString(), true, 
                        "Automatic cleanup after " + repositoryLifetimeHours + " hours");
                    
                    log.debug("Cleaned up expired session repository: {}", entry.getKey());
                    return true;
                    
                } catch (Exception e) {
                    log.error("Error cleaning up session repository: {}", entry.getKey(), e);
                    return false;
                }
            }
            
            return false;
        });
        
        if (cleanedCount > 0) {
            log.info("Cleaned up {} expired session repositories", cleanedCount);
        }
    }

    /**
     * Session repository data structure
     */
    public static class SessionRepository {
        private final String sessionId;
        private final Long userId;
        private final String username;
        private final String scenarioId;
        private final Path workspacePath;
        private final LocalDateTime createdAt;
        private volatile LocalDateTime lastAccess;

        public SessionRepository(String sessionId, Long userId, String username, 
                               String scenarioId, Path workspacePath, LocalDateTime createdAt) {
            this.sessionId = sessionId;
            this.userId = userId;
            this.username = username;
            this.scenarioId = scenarioId;
            this.workspacePath = workspacePath;
            this.createdAt = createdAt;
            this.lastAccess = createdAt;
        }

        public void updateLastAccess() {
            this.lastAccess = LocalDateTime.now();
        }

        public boolean isExpired(int lifetimeHours) {
            return LocalDateTime.now().isAfter(createdAt.plusHours(lifetimeHours));
        }

        // Getters
        public String getSessionId() { return sessionId; }
        public Long getUserId() { return userId; }
        public String getUsername() { return username; }
        public String getScenarioId() { return scenarioId; }
        public Path getWorkspacePath() { return workspacePath; }
        public LocalDateTime getCreatedAt() { return createdAt; }
        public LocalDateTime getLastAccess() { return lastAccess; }
    }

    /**
     * Session repository statistics
     */
    public static class SessionRepositoryStats {
        private final long totalRepositories;
        private final long activeRepositories;
        private final long expiredRepositories;
        private final long activeUsers;

        public SessionRepositoryStats(long totalRepositories, long activeRepositories, 
                                    long expiredRepositories, long activeUsers) {
            this.totalRepositories = totalRepositories;
            this.activeRepositories = activeRepositories;
            this.expiredRepositories = expiredRepositories;
            this.activeUsers = activeUsers;
        }

        // Getters
        public long getTotalRepositories() { return totalRepositories; }
        public long getActiveRepositories() { return activeRepositories; }
        public long getExpiredRepositories() { return expiredRepositories; }
        public long getActiveUsers() { return activeUsers; }
    }
}
