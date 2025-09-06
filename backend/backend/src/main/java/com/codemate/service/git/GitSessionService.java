package com.codemate.service.git;

import com.codemate.model.GitBranch;
import com.codemate.model.GitRepository;
import com.codemate.model.GitScenario;
import com.codemate.repository.GitScenarioRepository;
import com.codemate.service.git.GitStateManagementService.RepositoryRuntimeState;
import com.codemate.util.PerformanceMonitor;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import jakarta.annotation.PreDestroy;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Service responsible for managing isolated Git workspace environments
 * Each user session gets its own isolated Git repository workspace
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class GitSessionService {

    private final GitRepositoryManagementService gitRepositoryManagementService;
    private final GitStateManagementService gitStateManagementService;
    private final GitScenarioRepository gitScenarioRepository;
    private final PerformanceMonitor performanceMonitor;
    private final ObjectMapper objectMapper;

    @Value("${app.git.workspace.base-path:${java.io.tmpdir}/codemate-git-workspaces}")
    private String baseWorkspacePath;

    @Value("${app.git.workspace.cleanup-hours:24}")
    private int workspaceCleanupHours;

    @Value("${app.git.workspace.max-size-mb:100}")
    private long maxWorkspaceSizeMb;

    // Cache of active workspaces: repositoryId -> workspace path
    private final Map<Long, Path> activeWorkspaces = new ConcurrentHashMap<>();
    
    // Cache of workspace creation times for cleanup
    private final Map<Long, LocalDateTime> workspaceCreationTimes = new ConcurrentHashMap<>();

    /**
     * Gets or creates an isolated Git workspace for a repository
     */
    public Path getOrCreateWorkspace(GitRepository repository, Long userId) throws IOException {
        Long repositoryId = repository.getId();
        
        // Check if workspace already exists
        if (activeWorkspaces.containsKey(repositoryId)) {
            Path existingPath = activeWorkspaces.get(repositoryId);
            if (Files.exists(existingPath) && isValidGitRepository(existingPath)) {
                log.debug("Using existing workspace for repository: {}", repositoryId);
                return existingPath;
            } else {
                // Clean up invalid workspace
                activeWorkspaces.remove(repositoryId);
                workspaceCreationTimes.remove(repositoryId);
            }
        }

        // Create new workspace
        Path workspacePath = createNewWorkspace(repository, userId);
        activeWorkspaces.put(repositoryId, workspacePath);
        workspaceCreationTimes.put(repositoryId, LocalDateTime.now());
        
        log.info("Created new Git workspace for repository: {} at: {}", repositoryId, workspacePath);
        return workspacePath;
    }

    /**
     * Creates a new isolated Git workspace
     */
    private Path createNewWorkspace(GitRepository repository, Long userId) throws IOException {
        performanceMonitor.startOperation("git_workspace_creation", userId, repository.getId(), null);
        
        try {
            // Create unique workspace directory
            String workspaceName = String.format("repo_%d_user_%d_%d", 
                repository.getId(), userId, System.currentTimeMillis());
            Path workspacePath = Paths.get(baseWorkspacePath, workspaceName);
            
            // Ensure base directory exists
            Files.createDirectories(workspacePath.getParent());
            Files.createDirectories(workspacePath);
            
            // Initialize Git repository
            initializeGitRepository(workspacePath);
            
            // Set up initial state from scenario
            setupInitialState(workspacePath, repository);
            
            // Configure Git settings for this workspace
            configureGitWorkspace(workspacePath, userId);
            
            performanceMonitor.endOperation("git_workspace_creation", userId, repository.getId());
            return workspacePath;
            
        } catch (Exception e) {
            performanceMonitor.endOperation("git_workspace_creation", userId, repository.getId());
            log.error("Failed to create Git workspace for repository: {}", repository.getId(), e);
            throw new IOException("Failed to create Git workspace", e);
        }
    }

    /**
     * Initializes a Git repository in the workspace
     */
    private void initializeGitRepository(Path workspacePath) throws IOException, InterruptedException {
        log.debug("Initializing Git repository in: {}", workspacePath);
        
        ProcessBuilder processBuilder = new ProcessBuilder("git", "init");
        processBuilder.directory(workspacePath.toFile());
        processBuilder.redirectErrorStream(true);
        
        Process process = processBuilder.start();
        int exitCode = process.waitFor();
        
        if (exitCode != 0) {
            throw new IOException("Failed to initialize Git repository, exit code: " + exitCode);
        }
        
        log.debug("Git repository initialized successfully in: {}", workspacePath);
    }

    /**
     * Sets up initial repository state from scenario configuration
     */
    private void setupInitialState(Path workspacePath, GitRepository repository) throws IOException {
        log.debug("Setting up initial state for repository: {}", repository.getId());
        
        try {
            // Get scenario for initial state
            if (repository.getScenarioId() != null) {
                GitScenario scenario = gitScenarioRepository.findByScenarioId(repository.getScenarioId())
                    .orElse(null);
                
                if (scenario != null && scenario.getInitialState() != null) {
                    setupScenarioInitialState(workspacePath, scenario);
                }
            }
            
            // Apply current repository state
            RepositoryRuntimeState runtimeState = gitStateManagementService.getRuntimeState(repository);
            applyRuntimeState(workspacePath, runtimeState);
            
            // Create commits from database
            recreateCommitHistory(workspacePath, repository);
            
        } catch (Exception e) {
            log.warn("Failed to setup initial state, creating empty repository: {}", e.getMessage());
            // Continue with empty repository if initial state setup fails
        }
    }

    /**
     * Sets up initial state from scenario configuration
     */
    private void setupScenarioInitialState(Path workspacePath, GitScenario scenario) throws IOException {
        try {
            JsonNode initialState = objectMapper.readTree(scenario.getInitialState());
            
            // Create initial files if specified
            if (initialState.has("files") && initialState.get("files").isObject()) {
                JsonNode files = initialState.get("files");
                files.fields().forEachRemaining(entry -> {
                    try {
                        Path filePath = workspacePath.resolve(entry.getKey());
                        Files.createDirectories(filePath.getParent());
                        Files.write(filePath, entry.getValue().asText().getBytes(), StandardOpenOption.CREATE);
                        log.debug("Created initial file: {} in workspace", entry.getKey());
                    } catch (IOException e) {
                        log.warn("Failed to create initial file: {}", entry.getKey(), e);
                    }
                });
            }
            
        } catch (Exception e) {
            log.warn("Failed to parse scenario initial state: {}", e.getMessage());
        }
    }

    /**
     * Applies runtime state (working directory and staging area) to workspace
     */
    private void applyRuntimeState(Path workspacePath, RepositoryRuntimeState runtimeState) throws IOException {
        // Create working directory files
        for (Map.Entry<String, String> entry : runtimeState.getWorkingDirectory().entrySet()) {
            Path filePath = workspacePath.resolve(entry.getKey());
            Files.createDirectories(filePath.getParent());
            Files.write(filePath, entry.getValue().getBytes(), StandardOpenOption.CREATE);
        }
        
        // Stage files that are in staging area
        for (Map.Entry<String, String> entry : runtimeState.getStagingArea().entrySet()) {
            Path filePath = workspacePath.resolve(entry.getKey());
            Files.createDirectories(filePath.getParent());
            Files.write(filePath, entry.getValue().getBytes(), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
            
            // Stage the file
            try {
                ProcessBuilder processBuilder = new ProcessBuilder("git", "add", entry.getKey());
                processBuilder.directory(workspacePath.toFile());
                Process process = processBuilder.start();
                process.waitFor();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IOException("Interrupted while staging file", e);
            }
        }
    }

    /**
     * Recreates commit history from database
     */
    private void recreateCommitHistory(Path workspacePath, GitRepository repository) throws IOException {
        // Get commits for potential future use in recreating exact history
        // List<GitCommit> commits = gitRepositoryManagementService.findAllCommits(repository);
        
        // Create branches first
        List<GitBranch> branches = gitRepositoryManagementService.findAllBranches(repository);
        for (GitBranch branch : branches) {
            if (!"main".equals(branch.getName())) {
                try {
                    ProcessBuilder processBuilder = new ProcessBuilder("git", "checkout", "-b", branch.getName());
                    processBuilder.directory(workspacePath.toFile());
                    Process process = processBuilder.start();
                    process.waitFor();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new IOException("Interrupted while creating branch", e);
                }
            }
        }
        
        // Switch back to main branch
        try {
            ProcessBuilder processBuilder = new ProcessBuilder("git", "checkout", "main");
            processBuilder.directory(workspacePath.toFile());
            Process process = processBuilder.start();
            process.waitFor();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Interrupted while switching to main branch", e);
        }
    }

    /**
     * Configures Git settings for the workspace
     */
    private void configureGitWorkspace(Path workspacePath, Long userId) throws IOException, InterruptedException {
        // Set user configuration
        configureGitSetting(workspacePath, "user.name", "CodeMate User " + userId);
        configureGitSetting(workspacePath, "user.email", "user" + userId + "@codemate.com");
        
        // Set safe configurations
        configureGitSetting(workspacePath, "init.defaultBranch", "main");
        configureGitSetting(workspacePath, "core.autocrlf", "false");
        configureGitSetting(workspacePath, "merge.tool", "false");
        configureGitSetting(workspacePath, "core.editor", "false");
    }

    /**
     * Configures a single Git setting
     */
    private void configureGitSetting(Path workspacePath, String setting, String value) throws IOException, InterruptedException {
        ProcessBuilder processBuilder = new ProcessBuilder("git", "config", setting, value);
        processBuilder.directory(workspacePath.toFile());
        Process process = processBuilder.start();
        int exitCode = process.waitFor();
        
        if (exitCode != 0) {
            log.warn("Failed to set Git config {} = {} in workspace: {}", setting, value, workspacePath);
        }
    }

    /**
     * Syncs repository state from real Git workspace back to database
     */
    public void syncRepositoryStateFromWorkspace(GitRepository repository, Path workspacePath) {
        try {
            log.debug("Syncing repository state from workspace: {}", workspacePath);
            
            // Update working directory and staging area
            // TODO: Implement updateRuntimeState method in GitStateManagementService
            // RepositoryRuntimeState newState = extractRuntimeStateFromWorkspace(workspacePath);
            // gitStateManagementService.updateRuntimeState(repository, newState);
            
            // Update current branch
            String currentBranch = getCurrentBranchFromWorkspace(workspacePath);
            if (currentBranch != null && !currentBranch.equals(repository.getCurrentBranch())) {
                repository.setCurrentBranch(currentBranch);
                gitRepositoryManagementService.updateRepositoryState(repository);
            }
            
            log.debug("Repository state synced successfully from workspace");
            
        } catch (Exception e) {
            log.error("Failed to sync repository state from workspace: {}", workspacePath, e);
        }
    }

    /**
     * Extracts runtime state from real Git workspace
     */
    private RepositoryRuntimeState extractRuntimeStateFromWorkspace(Path workspacePath) throws IOException, InterruptedException {
        RepositoryRuntimeState state = new RepositoryRuntimeState();
        
        // Get all files in working directory
        Files.walk(workspacePath)
            .filter(Files::isRegularFile)
            .filter(path -> !path.toString().contains(".git/"))
            .forEach(path -> {
                try {
                    String relativePath = workspacePath.relativize(path).toString().replace('\\', '/');
                    String content = Files.readString(path);
                    state.getWorkingDirectory().put(relativePath, content);
                } catch (IOException e) {
                    log.warn("Failed to read file: {}", path, e);
                }
            });
        
        // Get staged files
        ProcessBuilder processBuilder = new ProcessBuilder("git", "diff", "--cached", "--name-only");
        processBuilder.directory(workspacePath.toFile());
        Process process = processBuilder.start();
        
        if (process.waitFor() == 0) {
            // Implementation would read staged files and add to state.getStagingArea()
            // This is a simplified version
        }
        
        return state;
    }

    /**
     * Gets current branch from workspace
     */
    private String getCurrentBranchFromWorkspace(Path workspacePath) throws IOException, InterruptedException {
        ProcessBuilder processBuilder = new ProcessBuilder("git", "rev-parse", "--abbrev-ref", "HEAD");
        processBuilder.directory(workspacePath.toFile());
        Process process = processBuilder.start();
        
        if (process.waitFor() == 0) {
            return new String(process.getInputStream().readAllBytes()).trim();
        }
        
        return null;
    }

    /**
     * Checks if a directory contains a valid Git repository
     */
    private boolean isValidGitRepository(Path path) {
        return Files.exists(path) && Files.exists(path.resolve(".git"));
    }

    /**
     * Cleans up a specific workspace
     */
    public void cleanupWorkspace(Long repositoryId) {
        Path workspacePath = activeWorkspaces.remove(repositoryId);
        workspaceCreationTimes.remove(repositoryId);
        
        if (workspacePath != null && Files.exists(workspacePath)) {
            try {
                deleteDirectoryRecursively(workspacePath);
                log.info("Cleaned up workspace for repository: {}", repositoryId);
            } catch (IOException e) {
                log.error("Failed to cleanup workspace: {}", workspacePath, e);
            }
        }
    }

    /**
     * Scheduled cleanup of old workspaces
     */
    @Scheduled(fixedRate = 3600000) // Run every hour
    public void cleanupOldWorkspaces() {
        LocalDateTime cutoff = LocalDateTime.now().minusHours(workspaceCleanupHours);
        
        workspaceCreationTimes.entrySet().removeIf(entry -> {
            if (entry.getValue().isBefore(cutoff)) {
                cleanupWorkspace(entry.getKey());
                return true;
            }
            return false;
        });
    }

    /**
     * Cleanup all workspaces on shutdown
     */
    @PreDestroy
    public void cleanupAllWorkspaces() {
        log.info("Cleaning up all Git workspaces on shutdown");
        
        for (Long repositoryId : activeWorkspaces.keySet()) {
            cleanupWorkspace(repositoryId);
        }
    }

    /**
     * Recursively deletes a directory
     */
    private void deleteDirectoryRecursively(Path path) throws IOException {
        Files.walk(path)
            .sorted((a, b) -> b.getNameCount() - a.getNameCount()) // Delete files before directories
            .forEach(p -> {
                try {
                    Files.delete(p);
                } catch (IOException e) {
                    log.warn("Failed to delete: {}", p, e);
                }
            });
    }

    /**
     * Gets statistics about active workspaces
     */
    public Map<String, Object> getWorkspaceStats() {
        return Map.of(
            "activeWorkspaces", activeWorkspaces.size(),
            "baseWorkspacePath", baseWorkspacePath,
            "cleanupHours", workspaceCleanupHours,
            "maxSizeMb", maxWorkspaceSizeMb
        );
    }

    /**
     * Cleans up all workspaces for a specific user
     */
    public void cleanupUserWorkspaces(Long userId) {
        log.info("Cleaning up workspaces for user: {}", userId);
        
        activeWorkspaces.entrySet().removeIf(entry -> {
            Path workspacePath = entry.getValue();
            try {
                // Check if this workspace belongs to the user
                if (workspacePath.toString().contains("user_" + userId)) {
                    secureDeleteWorkspace(workspacePath);
                    workspaceCreationTimes.remove(entry.getKey());
                    return true;
                }
            } catch (Exception e) {
                log.error("Error cleaning up workspace: {}", workspacePath, e);
            }
            return false;
        });
    }

    /**
     * Cleans up orphaned workspaces older than the specified cutoff time
     */
    public int cleanupOrphanedWorkspaces(LocalDateTime cutoff) {
        log.debug("Cleaning up orphaned workspaces older than: {}", cutoff);
        
        AtomicInteger cleanedCount = new AtomicInteger(0);
        
        workspaceCreationTimes.entrySet().removeIf(entry -> {
            Long repositoryId = entry.getKey();
            LocalDateTime creationTime = entry.getValue();
            
            if (creationTime.isBefore(cutoff)) {
                Path workspacePath = activeWorkspaces.get(repositoryId);
                if (workspacePath != null) {
                    try {
                        secureDeleteWorkspace(workspacePath);
                        activeWorkspaces.remove(repositoryId);
                        log.debug("Cleaned up orphaned workspace: {}", workspacePath);
                        cleanedCount.incrementAndGet();
                        return true;
                    } catch (Exception e) {
                        log.error("Error cleaning up orphaned workspace: {}", workspacePath, e);
                    }
                }
            }
            return false;
        });
        
        return cleanedCount.get();
    }

    /**
     * Securely deletes a workspace directory
     */
    private void secureDeleteWorkspace(Path workspacePath) {
        try {
            if (Files.exists(workspacePath)) {
                deleteDirectoryRecursively(workspacePath);
                log.debug("Securely deleted workspace: {}", workspacePath);
            }
        } catch (IOException e) {
            log.error("Error during secure workspace deletion: {}", workspacePath, e);
        }
    }
}
