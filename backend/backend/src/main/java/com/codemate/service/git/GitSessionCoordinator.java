package com.codemate.service.git;

import com.codemate.model.GitRepository;
import com.codemate.model.GitScenario;
import com.codemate.repository.GitRepositoryRepository;
import com.codemate.repository.GitScenarioRepository;
import com.codemate.security.UserPrincipal;
import com.codemate.service.security.SecurityAuditService;
import com.codemate.service.security.SessionSecurityService;
import com.codemate.util.PerformanceMonitor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Coordinator service that integrates session-based repository management
 * with existing Git services, providing unified session and repository lifecycle management
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class GitSessionCoordinator {

    private final SessionBasedRepositoryService sessionBasedRepositoryService;
    private final GitRepositoryManagementService gitRepositoryManagementService;
    private final ScenarioStateInitializer scenarioStateInitializer;
    private final SessionSecurityService sessionSecurityService;
    private final SecurityAuditService securityAuditService;
    private final GitRepositoryRepository gitRepositoryRepository;
    private final GitScenarioRepository gitScenarioRepository;
    private final PerformanceMonitor performanceMonitor;

    /**
     * Creates or gets a Git repository for a user session with comprehensive setup
     */
    public GitSessionContext createOrGetGitSession(UserPrincipal userPrincipal, String scenarioId) throws IOException {
        Long userId = userPrincipal.getId();
        String username = userPrincipal.getUsername();
        
        log.info("Creating/getting Git session for user: {} scenario: {}", username, scenarioId);
        
        performanceMonitor.startOperation("git_session_coordination", userId, null, scenarioId);
        
        try {
            // Initialize user session
            SessionSecurityService.UserSession userSession = sessionSecurityService.initializeUserSession(
                userId, username, getCurrentIpAddress());
            
            // Create or get session repository
            SessionBasedRepositoryService.SessionRepository sessionRepo = 
                sessionBasedRepositoryService.getOrCreateSessionRepository(userId, username, scenarioId);
            
            // Create or get database repository
            GitRepository dbRepository = createOrGetDatabaseRepository(userId, scenarioId, sessionRepo);
            
            // Initialize scenario state if needed
            initializeSessionScenario(sessionRepo, dbRepository, scenarioId, username);
            
            // Create session context
            GitSessionContext sessionContext = new GitSessionContext(
                userSession, sessionRepo, dbRepository, scenarioId
            );
            
            // Audit log session creation
            securityAuditService.logWorkspaceEvent(userId, username, "GIT_SESSION_CREATED",
                sessionRepo.getWorkspacePath().toString(), true, 
                "Scenario: " + scenarioId + ", Repository: " + dbRepository.getId());
            
            performanceMonitor.endOperation("git_session_coordination", userId, null);
            
            log.info("Git session ready for user: {} scenario: {} at: {}", 
                    username, scenarioId, sessionRepo.getWorkspacePath());
            
            return sessionContext;
            
        } catch (Exception e) {
            performanceMonitor.endOperation("git_session_coordination", userId, null);
            log.error("Failed to create Git session for user: {} scenario: {}", username, scenarioId, e);
            throw new IOException("Git session creation failed", e);
        }
    }

    /**
     * Gets an existing Git session context
     */
    public Optional<GitSessionContext> getGitSession(String sessionId) {
        try {
            SessionBasedRepositoryService.SessionRepository sessionRepo = 
                sessionBasedRepositoryService.getSessionRepository(sessionId);
            
            if (sessionRepo == null) {
                return Optional.empty();
            }
            
            // Find corresponding database repository
            Optional<GitRepository> dbRepo = gitRepositoryRepository
                .findByUserIdAndScenarioIdAndIsActive(
                    sessionRepo.getUserId(), 
                    sessionRepo.getScenarioId(), 
                    true
                );
            
            if (dbRepo.isEmpty()) {
                log.warn("Database repository not found for session: {}", sessionId);
                return Optional.empty();
            }
            
            // Get user session
            SessionSecurityService.UserSession userSession = 
                sessionSecurityService.getUserSession(sessionRepo.getUserId());
            
            if (userSession == null) {
                log.warn("User session not found for session: {}", sessionId);
                return Optional.empty();
            }
            
            GitSessionContext context = new GitSessionContext(
                userSession, sessionRepo, dbRepo.get(), sessionRepo.getScenarioId()
            );
            
            return Optional.of(context);
            
        } catch (Exception e) {
            log.error("Error getting Git session: {}", sessionId, e);
            return Optional.empty();
        }
    }

    /**
     * Terminates a Git session and cleans up all resources
     */
    public void terminateGitSession(String sessionId, UserPrincipal userPrincipal, String reason) {
        Long userId = userPrincipal.getId();
        String username = userPrincipal.getUsername();
        
        log.info("Terminating Git session: {} for user: {} reason: {}", sessionId, username, reason);
        
        try {
            // Get session context
            Optional<GitSessionContext> contextOpt = getGitSession(sessionId);
            
            if (contextOpt.isPresent()) {
                GitSessionContext context = contextOpt.get();
                
                // Persist any final state
                persistSessionState(context);
                
                // Mark database repository as inactive
                GitRepository dbRepo = context.getDatabaseRepository();
                dbRepo.setIsActive(false);
                dbRepo.setUpdatedAt(LocalDateTime.now());
                gitRepositoryRepository.save(dbRepo);
                
                // Destroy session repository
                sessionBasedRepositoryService.destroySessionRepository(sessionId, userId, username);
                
                // Terminate user session if no other active repositories
                List<SessionBasedRepositoryService.SessionRepository> userRepos = 
                    sessionBasedRepositoryService.getUserSessionRepositories(userId);
                
                if (userRepos.isEmpty()) {
                    sessionSecurityService.terminateUserSession(userId, "No active repositories");
                }
                
                // Audit log termination
                securityAuditService.logWorkspaceEvent(userId, username, "GIT_SESSION_TERMINATED",
                    context.getSessionRepository().getWorkspacePath().toString(), true, reason);
                
                log.info("Git session terminated successfully: {}", sessionId);
            } else {
                log.warn("Git session not found for termination: {}", sessionId);
            }
            
        } catch (Exception e) {
            log.error("Error terminating Git session: {}", sessionId, e);
            
            securityAuditService.logWorkspaceEvent(userId, username, "GIT_SESSION_TERMINATION_FAILED",
                sessionId, false, e.getMessage());
        }
    }

    /**
     * Lists all active Git sessions for a user
     */
    public List<GitSessionInfo> getUserGitSessions(UserPrincipal userPrincipal) {
        Long userId = userPrincipal.getId();
        
        return sessionBasedRepositoryService.getUserSessionRepositories(userId).stream()
            .map(sessionRepo -> {
                Optional<GitRepository> dbRepo = gitRepositoryRepository
                    .findByUserIdAndScenarioIdAndIsActive(userId, sessionRepo.getScenarioId(), true);
                
                return new GitSessionInfo(
                    sessionRepo.getSessionId(),
                    sessionRepo.getScenarioId(),
                    sessionRepo.getCreatedAt(),
                    sessionRepo.getLastAccess(),
                    dbRepo.map(GitRepository::getId).orElse(null),
                    sessionRepo.getWorkspacePath().toString()
                );
            })
            .toList();
    }

    /**
     * Gets session statistics for monitoring
     */
    public SessionCoordinatorStats getStats() {
        SessionBasedRepositoryService.SessionRepositoryStats repoStats = 
            sessionBasedRepositoryService.getStats();
        
        SessionSecurityService.SessionStatistics sessionStats = 
            sessionSecurityService.getSessionStatistics();
        
        return new SessionCoordinatorStats(repoStats, sessionStats);
    }

    /**
     * Creates or gets database repository for session
     */
    private GitRepository createOrGetDatabaseRepository(Long userId, String scenarioId, 
                                                      SessionBasedRepositoryService.SessionRepository sessionRepo) {
        // Check for existing active repository
        Optional<GitRepository> existingRepo = gitRepositoryRepository
            .findByUserIdAndScenarioIdAndIsActive(userId, scenarioId, true);
        
        if (existingRepo.isPresent()) {
            log.debug("Using existing database repository: {}", existingRepo.get().getId());
            return existingRepo.get();
        }
        
        // Create new database repository
        String repositoryName = "session-repo-" + sessionRepo.getSessionId();
        GitRepository newRepo = gitRepositoryManagementService.createVirtualRepository(userId, scenarioId, repositoryName);
        
        log.debug("Created new database repository: {}", newRepo.getId());
        return newRepo;
    }

    /**
     * Initializes scenario state for session if needed
     */
    private void initializeSessionScenario(SessionBasedRepositoryService.SessionRepository sessionRepo,
                                         GitRepository dbRepository, String scenarioId, String username) throws IOException {
        // Check if already initialized
        Path workspacePath = sessionRepo.getWorkspacePath();
        
        if (!scenarioId.equals("DEMO") && scenarioId != null) {
            GitScenario scenario = gitScenarioRepository.findByScenarioId(scenarioId).orElse(null);
            
            if (scenario != null) {
                log.debug("Initializing scenario state: {} for workspace: {}", scenarioId, workspacePath);
                scenarioStateInitializer.initializeScenarioState(workspacePath, scenario, username);
            }
        }
    }

    /**
     * Persists session state back to database
     */
    private void persistSessionState(GitSessionContext context) {
        try {
            // This would sync the workspace state back to the database repository
            // For now, we'll just update the last access time
            GitRepository dbRepo = context.getDatabaseRepository();
            dbRepo.setUpdatedAt(LocalDateTime.now());
            gitRepositoryRepository.save(dbRepo);
            
            log.debug("Persisted session state for repository: {}", dbRepo.getId());
            
        } catch (Exception e) {
            log.warn("Failed to persist session state: {}", e.getMessage());
        }
    }

    /**
     * Gets current IP address (simplified)
     */
    private String getCurrentIpAddress() {
        // In a real implementation, this would extract IP from request context
        return "127.0.0.1";
    }

    /**
     * Git session context containing all session-related information
     */
    public static class GitSessionContext {
        private final SessionSecurityService.UserSession userSession;
        private final SessionBasedRepositoryService.SessionRepository sessionRepository;
        private final GitRepository databaseRepository;
        private final String scenarioId;

        public GitSessionContext(SessionSecurityService.UserSession userSession,
                               SessionBasedRepositoryService.SessionRepository sessionRepository,
                               GitRepository databaseRepository,
                               String scenarioId) {
            this.userSession = userSession;
            this.sessionRepository = sessionRepository;
            this.databaseRepository = databaseRepository;
            this.scenarioId = scenarioId;
        }

        // Getters
        public SessionSecurityService.UserSession getUserSession() { return userSession; }
        public SessionBasedRepositoryService.SessionRepository getSessionRepository() { return sessionRepository; }
        public GitRepository getDatabaseRepository() { return databaseRepository; }
        public String getScenarioId() { return scenarioId; }
        
        public Path getWorkspacePath() { return sessionRepository.getWorkspacePath(); }
        public Long getUserId() { return userSession.getUserId(); }
        public String getUsername() { return userSession.getUsername(); }
        public String getSessionId() { return sessionRepository.getSessionId(); }
    }

    /**
     * Information about a user's Git session
     */
    public static class GitSessionInfo {
        private final String sessionId;
        private final String scenarioId;
        private final LocalDateTime createdAt;
        private final LocalDateTime lastAccess;
        private final Long databaseRepositoryId;
        private final String workspacePath;

        public GitSessionInfo(String sessionId, String scenarioId, LocalDateTime createdAt,
                            LocalDateTime lastAccess, Long databaseRepositoryId, String workspacePath) {
            this.sessionId = sessionId;
            this.scenarioId = scenarioId;
            this.createdAt = createdAt;
            this.lastAccess = lastAccess;
            this.databaseRepositoryId = databaseRepositoryId;
            this.workspacePath = workspacePath;
        }

        // Getters
        public String getSessionId() { return sessionId; }
        public String getScenarioId() { return scenarioId; }
        public LocalDateTime getCreatedAt() { return createdAt; }
        public LocalDateTime getLastAccess() { return lastAccess; }
        public Long getDatabaseRepositoryId() { return databaseRepositoryId; }
        public String getWorkspacePath() { return workspacePath; }
    }

    /**
     * Combined statistics from all session services
     */
    public static class SessionCoordinatorStats {
        private final SessionBasedRepositoryService.SessionRepositoryStats repositoryStats;
        private final SessionSecurityService.SessionStatistics sessionStats;

        public SessionCoordinatorStats(SessionBasedRepositoryService.SessionRepositoryStats repositoryStats,
                                     SessionSecurityService.SessionStatistics sessionStats) {
            this.repositoryStats = repositoryStats;
            this.sessionStats = sessionStats;
        }

        // Getters
        public SessionBasedRepositoryService.SessionRepositoryStats getRepositoryStats() { return repositoryStats; }
        public SessionSecurityService.SessionStatistics getSessionStats() { return sessionStats; }
        
        public long getTotalActiveSessions() { 
            return Math.min(repositoryStats.getActiveRepositories(), sessionStats.getTotalSessions()); 
        }
    }
}
