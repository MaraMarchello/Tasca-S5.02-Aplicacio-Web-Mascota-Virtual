package com.codemate.service.git;

import com.codemate.model.GitRepository;
import com.codemate.repository.GitRepositoryRepository;
import com.codemate.service.git.GitCommandValidationService.GitCommandInfo;
import com.codemate.service.git.GitCommandValidationService.ValidationResult;
import com.codemate.service.git.GitRepositoryManagementService.GitCommandResult;
import com.codemate.service.git.GitRepositoryManagementService.GitRepositoryState;
import com.codemate.util.PerformanceMonitor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Facade service that coordinates all Git simulation operations
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class GitSimulatorFacadeService {

    private final GitRepositoryManagementService gitRepositoryManagementService;
    private final GitCommandValidationService gitCommandValidationService;
    private final GitStateManagementService gitStateManagementService;
    private final GitBasicCommandService gitBasicCommandService;
    private final GitAdvancedCommandService gitAdvancedCommandService;
    private final GitFileSystemService gitFileSystemService;
    private final GitConflictService gitConflictService;
    private final GitRepositoryRepository gitRepositoryRepository;
    private final PerformanceMonitor performanceMonitor;
    private final RealGitExecutionService realGitExecutionService;

    /**
     * Creates a virtual git repository for a user and scenario
     */
    public GitRepository createVirtualRepository(Long userId, String scenarioId, String repositoryName) {
        return gitRepositoryManagementService.createVirtualRepository(userId, scenarioId, repositoryName);
    }

    /**
     * Executes a git command in the virtual repository
     * Supports both simulation mode and real Git execution
     */
    public GitCommandResult executeGitCommand(Long repositoryId, String command, Long userId) {
        // Start performance monitoring
        performanceMonitor.startOperation("git_command_execution", userId, repositoryId, null);
        long startTime = System.currentTimeMillis();
        
        log.info("Executing git command: {} in repository: {}", command, repositoryId);
        
        try {
            // Get repository
            GitRepository repository = getRepository(repositoryId);
            
            // Check if real Git execution is enabled and should be used
            if (realGitExecutionService.isRealGitExecutionEnabled() && shouldUseRealGit(repository, command)) {
                log.debug("Using real Git execution for command: {}", command);
                GitCommandResult result = realGitExecutionService.executeRealGitCommand(repository, command, userId);
                
                // Save command execution record
                gitRepositoryManagementService.saveCommandExecution(repository, command, result, userId);
                
                // Record performance metrics
                long duration = performanceMonitor.endOperation("git_command_execution", userId, repositoryId);
                performanceMonitor.recordGitCommandExecution(command, duration, result.isSuccessful(), 
                                                            userId, repositoryId, repository.getScenarioId());
                
                log.info("Real Git command executed. Success: {}, Exit code: {}, Duration: {}ms", 
                        result.isSuccessful(), result.getExitCode(), duration);
                return result;
            }
            
            // Fall back to simulation mode
            log.debug("Using simulation mode for command: {}", command);
            return executeSimulatedCommand(repositoryId, command, userId, repository, startTime);
            
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            performanceMonitor.recordGitCommandExecution(command, duration, false, userId, repositoryId, null);
            performanceMonitor.endOperation("git_command_execution", userId, repositoryId);
            log.error("Error executing git command: {} for user: {} in repository: {}", command, userId, repositoryId, e);
            throw e;
        }
    }
    
    /**
     * Executes a command using the existing simulation logic
     */
    private GitCommandResult executeSimulatedCommand(Long repositoryId, String command, Long userId, 
                                                   GitRepository repository, long startTime) {
        // Validate command parameters
        ValidationResult validation = gitCommandValidationService.validateCommandParameters(command, repositoryId, userId);
        if (!validation.isValid()) {
            GitCommandResult errorResult = GitCommandResult.builder()
                .successful(false)
                .exitCode(1)
                .output("")
                .errorOutput(validation.getErrorMessage())
                .build();
            
            long duration = System.currentTimeMillis() - startTime;
            performanceMonitor.recordGitCommandExecution(command, duration, false, userId, repositoryId, null);
            return errorResult;
        }

        // Parse command
        GitCommandInfo commandInfo = gitCommandValidationService.parseGitCommand(validation.getSanitizedCommand());
        
        // Execute command simulation
        GitCommandResult result = simulateCommand(repository, commandInfo, userId);
        
        // Save command execution record
        gitRepositoryManagementService.saveCommandExecution(repository, validation.getSanitizedCommand(), result, userId);
        
        // Update repository state if command was successful
        if (result.isSuccessful()) {
            gitRepositoryManagementService.updateRepositoryState(repository);
        }

        // Record performance metrics
        long duration = performanceMonitor.endOperation("git_command_execution", userId, repositoryId);
        performanceMonitor.recordGitCommandExecution(validation.getSanitizedCommand(), duration, result.isSuccessful(), 
                                                    userId, repositoryId, repository.getScenarioId());

        log.info("Simulated Git command executed. Success: {}, Exit code: {}, Duration: {}ms", 
                result.isSuccessful(), result.getExitCode(), duration);
        return result;
    }
    
    /**
     * Determines whether to use real Git execution or simulation for a command
     */
    private boolean shouldUseRealGit(GitRepository repository, String command) {
        // Use real Git for demo repositories or when specifically requested
        if (repository.getScenarioId() == null || "DEMO".equals(repository.getScenarioId())) {
            return true;
        }
        
        // Use simulation for learning scenarios by default to ensure predictable behavior
        // This can be overridden by configuration
        return false;
    }

    /**
     * Gets the current state of a virtual repository
     */
    public GitRepositoryState getRepositoryState(Long repositoryId) {
        return gitRepositoryManagementService.getRepositoryState(repositoryId);
    }

    /**
     * Generates a conflict scenario in the repository
     */
    public void generateConflictScenario(Long repositoryId, String conflictType) {
        GitRepository repository = getRepository(repositoryId);
        gitConflictService.generateConflictScenario(repository, conflictType);
    }

    /**
     * Validates if a command execution is correct for the current scenario step
     */
    public boolean validateCommandExecution(Long repositoryId, String command, String scenarioId, int stepNumber) {
        log.info("Validating command: {} for scenario: {} step: {}", command, scenarioId, stepNumber);
        
        // For now, return true - detailed validation would require scenario-specific logic
        return true;
    }

    // Private helper methods

    private GitRepository getRepository(Long repositoryId) {
        return gitRepositoryRepository.findById(repositoryId)
            .orElseThrow(() -> new RuntimeException("Repository not found: " + repositoryId));
    }

    private GitCommandResult simulateCommand(GitRepository repository, GitCommandInfo commandInfo, Long userId) {
        switch (commandInfo.getSubCommand().toLowerCase()) {
            // Basic commands
            case "status":
                return gitBasicCommandService.simulateStatusCommand(repository);
            case "add":
                return gitBasicCommandService.simulateAddCommand(repository, commandInfo.getArgs());
            case "commit":
                return gitBasicCommandService.simulateCommitCommand(repository, commandInfo.getArgs(), userId);
            case "branch":
                return gitBasicCommandService.simulateBranchCommand(repository, commandInfo.getArgs());
            case "checkout":
                return gitBasicCommandService.simulateCheckoutCommand(repository, commandInfo.getArgs());
            case "log":
                return gitBasicCommandService.simulateLogCommand(repository, commandInfo.getArgs());
            case "diff":
                return gitBasicCommandService.simulateDiffCommand(repository, commandInfo.getArgs());

            // Advanced commands
            case "merge":
                return gitAdvancedCommandService.simulateMergeCommand(repository, commandInfo.getArgs());
            case "rebase":
                return gitAdvancedCommandService.simulateRebaseCommand(repository, commandInfo.getArgs());
            case "cherry-pick":
                return gitAdvancedCommandService.simulateCherryPickCommand(repository, commandInfo.getArgs());
            case "stash":
                return gitAdvancedCommandService.simulateStashCommand(repository, commandInfo.getArgs());
            case "reset":
                return gitAdvancedCommandService.simulateResetCommand(repository, commandInfo.getArgs());

            // File system commands
            case "fs":
                return gitFileSystemService.simulateFsCommand(repository, commandInfo.getArgs());

            default:
                return GitCommandResult.builder()
                    .successful(false)
                    .exitCode(1)
                    .output("")
                    .errorOutput("Unknown git command: " + commandInfo.getSubCommand())
                    .build();
        }
    }

    // Delegate methods for accessing specific services

    /**
     * Gets the repository management service
     */
    public GitRepositoryManagementService getRepositoryManagementService() {
        return gitRepositoryManagementService;
    }

    /**
     * Gets the command validation service
     */
    public GitCommandValidationService getCommandValidationService() {
        return gitCommandValidationService;
    }

    /**
     * Gets the state management service
     */
    public GitStateManagementService getStateManagementService() {
        return gitStateManagementService;
    }

    /**
     * Gets the conflict service
     */
    public GitConflictService getConflictService() {
        return gitConflictService;
    }

    /**
     * Gets the basic command service
     */
    public GitBasicCommandService getBasicCommandService() {
        return gitBasicCommandService;
    }

    /**
     * Gets the advanced command service
     */
    public GitAdvancedCommandService getAdvancedCommandService() {
        return gitAdvancedCommandService;
    }

    /**
     * Gets the file system service
     */
    public GitFileSystemService getFileSystemService() {
        return gitFileSystemService;
    }
}
