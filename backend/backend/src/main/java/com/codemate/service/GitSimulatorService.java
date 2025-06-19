package com.codemate.service;

import com.codemate.model.*;
import com.codemate.repository.*;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class GitSimulatorService {

    private final GitRepositoryRepository gitRepositoryRepository;
    private final GitCommitRepository gitCommitRepository;
    private final GitBranchRepository gitBranchRepository;
    private final GitCommandRepository gitCommandRepository;
    private final GitScenarioRepository gitScenarioRepository;
    private final ObjectMapper objectMapper;

    /**
     * Creates a virtual git repository for a user and scenario
     */
    public GitRepository createVirtualRepository(Long userId, String scenarioId, String repositoryName) {
        log.info("Creating virtual repository for user: {} and scenario: {}", userId, scenarioId);
        
        // Check if repository already exists for this user and scenario
        Optional<GitRepository> existingRepo = gitRepositoryRepository
            .findByUserIdAndScenarioIdAndIsActive(userId, scenarioId, true);
        
        if (existingRepo.isPresent()) {
            log.info("Repository already exists for user: {} and scenario: {}", userId, scenarioId);
            return existingRepo.get();
        }

        // Get scenario details
        GitScenario scenario = gitScenarioRepository.findByScenarioId(scenarioId)
            .orElseThrow(() -> new RuntimeException("Scenario not found: " + scenarioId));

        // Create repository
        GitRepository repository = GitRepository.builder()
            .name(repositoryName)
            .userId(userId)
            .scenarioId(scenarioId)
            .currentBranch("main")
            .currentState(scenario.getInitialState())
            .isActive(true)
            .build();

        repository = gitRepositoryRepository.save(repository);

        // Initialize with scenario's initial state
        initializeRepositoryFromScenario(repository, scenario);

        log.info("Virtual repository created successfully with id: {}", repository.getId());
        return repository;
    }

    /**
     * Executes a git command in the virtual repository
     */
    public GitCommandResult executeGitCommand(Long repositoryId, String command, Long userId) {
        log.info("Executing git command: {} in repository: {}", command, repositoryId);
        
        GitRepository repository = gitRepositoryRepository.findById(repositoryId)
            .orElseThrow(() -> new RuntimeException("Repository not found: " + repositoryId));

        // Parse and validate command
        GitCommandInfo commandInfo = parseGitCommand(command);
        
        // Execute command simulation
        GitCommandResult result = simulateCommand(repository, commandInfo, userId);
        
        // Save command execution record
        saveCommandExecution(repository, command, result, userId);
        
        // Update repository state if command was successful
        if (result.isSuccessful()) {
            updateRepositoryState(repository, commandInfo, result);
        }

        log.info("Git command executed. Success: {}, Exit code: {}", result.isSuccessful(), result.getExitCode());
        return result;
    }

    /**
     * Gets the current state of a virtual repository
     */
    public GitRepositoryState getRepositoryState(Long repositoryId) {
        log.info("Getting repository state for repository: {}", repositoryId);
        
        GitRepository repository = gitRepositoryRepository.findById(repositoryId)
            .orElseThrow(() -> new RuntimeException("Repository not found: " + repositoryId));

        return buildRepositoryState(repository);
    }

    /**
     * Generates a conflict scenario in the repository
     */
    public void generateConflictScenario(Long repositoryId, String conflictType) {
        log.info("Generating conflict scenario of type: {} in repository: {}", conflictType, repositoryId);
        
        GitRepository repository = gitRepositoryRepository.findById(repositoryId)
            .orElseThrow(() -> new RuntimeException("Repository not found: " + repositoryId));

        switch (conflictType.toLowerCase()) {
            case "merge":
                generateMergeConflict(repository);
                break;
            case "rebase":
                generateRebaseConflict(repository);
                break;
            default:
                throw new IllegalArgumentException("Unknown conflict type: " + conflictType);
        }
    }

    /**
     * Validates if a command execution is correct for the current scenario step
     */
    public boolean validateCommandExecution(Long repositoryId, String command, String scenarioId, int stepNumber) {
        log.info("Validating command: {} for scenario: {} step: {}", command, scenarioId, stepNumber);
        
        GitScenario scenario = gitScenarioRepository.findByScenarioId(scenarioId)
            .orElseThrow(() -> new RuntimeException("Scenario not found: " + scenarioId));

        return isCommandValid(scenario, command, stepNumber);
    }

    // Private helper methods

    private void initializeRepositoryFromScenario(GitRepository repository, GitScenario scenario) {
        try {
            if (scenario.getInitialState() != null) {
                JsonNode initialState = objectMapper.readTree(scenario.getInitialState());
                
                // Create initial commit
                String initialCommitHash = generateCommitHash("Initial commit", "system@codemate.com");
                GitCommit initialCommit = GitCommit.builder()
                    .hash(initialCommitHash)
                    .message("Initial commit")
                    .author("System")
                    .email("system@codemate.com")
                    .branchName("main")
                    .repository(repository)
                    .parentHashes(new ArrayList<>())
                    .modifiedFiles(new ArrayList<>())
                    .build();
                
                gitCommitRepository.save(initialCommit);

                // Create main branch
                GitBranch mainBranch = GitBranch.builder()
                    .name("main")
                    .headCommitHash(initialCommitHash)
                    .isActive(true)
                    .repository(repository)
                    .build();
                
                gitBranchRepository.save(mainBranch);
            }
        } catch (JsonProcessingException e) {
            log.error("Error parsing initial state for scenario: {}", scenario.getScenarioId(), e);
            throw new RuntimeException("Failed to initialize repository from scenario", e);
        }
    }

    private GitCommandInfo parseGitCommand(String command) {
        String[] parts = command.trim().split("\\s+");
        if (parts.length == 0 || !parts[0].equals("git")) {
            throw new IllegalArgumentException("Invalid git command: " + command);
        }

        String subCommand = parts.length > 1 ? parts[1] : "";
        List<String> args = new ArrayList<>(Arrays.asList(parts).subList(2, parts.length));

        return new GitCommandInfo(command, subCommand, args);
    }

    private GitCommandResult simulateCommand(GitRepository repository, GitCommandInfo commandInfo, Long userId) {
        switch (commandInfo.getSubCommand().toLowerCase()) {
            case "status":
                return simulateStatusCommand(repository);
            case "add":
                return simulateAddCommand(repository, commandInfo.getArgs());
            case "commit":
                return simulateCommitCommand(repository, commandInfo.getArgs(), userId);
            case "branch":
                return simulateBranchCommand(repository, commandInfo.getArgs());
            case "checkout":
                return simulateCheckoutCommand(repository, commandInfo.getArgs());
            case "merge":
                return simulateMergeCommand(repository, commandInfo.getArgs());
            case "log":
                return simulateLogCommand(repository, commandInfo.getArgs());
            case "diff":
                return simulateDiffCommand(repository, commandInfo.getArgs());
            default:
                return GitCommandResult.builder()
                    .successful(false)
                    .exitCode(1)
                    .output("")
                    .errorOutput("Unknown git command: " + commandInfo.getSubCommand())
                    .build();
        }
    }

    private GitCommandResult simulateStatusCommand(GitRepository repository) {
        // Simulate git status output based on current repository state
        StringBuilder output = new StringBuilder();
        output.append("On branch ").append(repository.getCurrentBranch()).append("\n");
        output.append("nothing to commit, working tree clean\n");

        return GitCommandResult.builder()
            .successful(true)
            .exitCode(0)
            .output(output.toString())
            .errorOutput("")
            .build();
    }

    private GitCommandResult simulateAddCommand(GitRepository repository, List<String> args) {
        if (args.isEmpty()) {
            return GitCommandResult.builder()
                .successful(false)
                .exitCode(1)
                .output("")
                .errorOutput("Nothing specified, nothing added.\n")
                .build();
        }

        return GitCommandResult.builder()
            .successful(true)
            .exitCode(0)
            .output("")
            .errorOutput("")
            .build();
    }

    private GitCommandResult simulateCommitCommand(GitRepository repository, List<String> args, Long userId) {
        String message = extractCommitMessage(args);
        if (message.isEmpty()) {
            return GitCommandResult.builder()
                .successful(false)
                .exitCode(1)
                .output("")
                .errorOutput("Aborting commit due to empty commit message.\n")
                .build();
        }

        // Create new commit
        String commitHash = generateCommitHash(message, "user@codemate.com");
        GitCommit commit = GitCommit.builder()
            .hash(commitHash)
            .message(message)
            .author("User")
            .email("user@codemate.com")
            .branchName(repository.getCurrentBranch())
            .repository(repository)
            .parentHashes(getLastCommitHashes(repository))
            .modifiedFiles(new ArrayList<>())
            .build();

        gitCommitRepository.save(commit);

        String output = String.format("[%s %s] %s\n", 
            repository.getCurrentBranch(), 
            commitHash.substring(0, 7), 
            message);

        return GitCommandResult.builder()
            .successful(true)
            .exitCode(0)
            .output(output)
            .errorOutput("")
            .commitHash(commitHash)
            .build();
    }

    private GitCommandResult simulateBranchCommand(GitRepository repository, List<String> args) {
        if (args.isEmpty()) {
            // List branches
            List<GitBranch> branches = gitBranchRepository.findByRepositoryOrderByName(repository);
            StringBuilder output = new StringBuilder();
            for (GitBranch branch : branches) {
                if (branch.getName().equals(repository.getCurrentBranch())) {
                    output.append("* ").append(branch.getName()).append("\n");
                } else {
                    output.append("  ").append(branch.getName()).append("\n");
                }
            }

            return GitCommandResult.builder()
                .successful(true)
                .exitCode(0)
                .output(output.toString())
                .errorOutput("")
                .build();
        }

        // Create new branch
        String branchName = args.get(0);
        String currentCommitHash = getCurrentCommitHash(repository);
        
        GitBranch newBranch = GitBranch.builder()
            .name(branchName)
            .headCommitHash(currentCommitHash)
            .isActive(false)
            .parentBranch(repository.getCurrentBranch())
            .repository(repository)
            .build();

        gitBranchRepository.save(newBranch);

        return GitCommandResult.builder()
            .successful(true)
            .exitCode(0)
            .output("")
            .errorOutput("")
            .build();
    }

    private GitCommandResult simulateCheckoutCommand(GitRepository repository, List<String> args) {
        if (args.isEmpty()) {
            return GitCommandResult.builder()
                .successful(false)
                .exitCode(1)
                .output("")
                .errorOutput("error: pathspec '' did not match any file(s) known to git\n")
                .build();
        }

        String branchName = args.get(0);
        Optional<GitBranch> branch = gitBranchRepository.findByRepositoryAndName(repository, branchName);
        
        if (branch.isEmpty()) {
            return GitCommandResult.builder()
                .successful(false)
                .exitCode(1)
                .output("")
                .errorOutput(String.format("error: pathspec '%s' did not match any file(s) known to git\n", branchName))
                .build();
        }

        // Update current branch
        repository.setCurrentBranch(branchName);
        gitRepositoryRepository.save(repository);

        String output = String.format("Switched to branch '%s'\n", branchName);

        return GitCommandResult.builder()
            .successful(true)
            .exitCode(0)
            .output(output)
            .errorOutput("")
            .build();
    }

    private GitCommandResult simulateMergeCommand(GitRepository repository, List<String> args) {
        if (args.isEmpty()) {
            return GitCommandResult.builder()
                .successful(false)
                .exitCode(1)
                .output("")
                .errorOutput("fatal: No commit specified and merge.defaultToUpstream not set.\n")
                .build();
        }

        String branchToMerge = args.get(0);
        String output = String.format("Merge made by the 'recursive' strategy.\n");

        return GitCommandResult.builder()
            .successful(true)
            .exitCode(0)
            .output(output)
            .errorOutput("")
            .build();
    }

    private GitCommandResult simulateLogCommand(GitRepository repository, List<String> args) {
        List<GitCommit> commits = gitCommitRepository.findByRepositoryAndBranchNameOrderByCommitTimeDesc(
            repository, repository.getCurrentBranch());

        StringBuilder output = new StringBuilder();
        for (GitCommit commit : commits) {
            output.append(String.format("commit %s\n", commit.getHash()));
            output.append(String.format("Author: %s <%s>\n", commit.getAuthor(), commit.getEmail()));
            output.append(String.format("Date: %s\n\n", commit.getCommitTime()));
            output.append(String.format("    %s\n\n", commit.getMessage()));
        }

        return GitCommandResult.builder()
            .successful(true)
            .exitCode(0)
            .output(output.toString())
            .errorOutput("")
            .build();
    }

    private GitCommandResult simulateDiffCommand(GitRepository repository, List<String> args) {
        String output = "diff --git a/file.txt b/file.txt\nindex abc123..def456 100644\n--- a/file.txt\n+++ b/file.txt\n@@ -1,3 +1,3 @@\n line1\n-line2\n+modified line2\n line3\n";

        return GitCommandResult.builder()
            .successful(true)
            .exitCode(0)
            .output(output)
            .errorOutput("")
            .build();
    }

    // Additional helper methods would be implemented here...
    
    private void saveCommandExecution(GitRepository repository, String command, GitCommandResult result, Long userId) {
        GitCommand gitCommand = GitCommand.builder()
            .command(command)
            .output(result.getOutput())
            .errorOutput(result.getErrorOutput())
            .successful(result.isSuccessful())
            .exitCode(result.getExitCode())
            .userId(userId)
            .scenarioId(repository.getScenarioId())
            .repository(repository)
            .build();

        gitCommandRepository.save(gitCommand);
    }

    private void updateRepositoryState(GitRepository repository, GitCommandInfo commandInfo, GitCommandResult result) {
        // Update repository state based on successful command execution
        // This would include updating branches, commits, etc.
        repository.setUpdatedAt(LocalDateTime.now());
        gitRepositoryRepository.save(repository);
    }

    private GitRepositoryState buildRepositoryState(GitRepository repository) {
        List<GitCommit> commits = gitCommitRepository.findByRepositoryOrderByCommitTimeDesc(repository);
        List<GitBranch> branches = gitBranchRepository.findByRepositoryOrderByName(repository);
        
        return GitRepositoryState.builder()
            .repositoryId(repository.getId())
            .currentBranch(repository.getCurrentBranch())
            .commits(commits)
            .branches(branches)
            .build();
    }

    private void generateMergeConflict(GitRepository repository) {
        // Implementation for generating merge conflicts
        log.info("Generating merge conflict for repository: {}", repository.getId());
    }

    private void generateRebaseConflict(GitRepository repository) {
        // Implementation for generating rebase conflicts
        log.info("Generating rebase conflict for repository: {}", repository.getId());
    }

    private boolean isCommandValid(GitScenario scenario, String command, int stepNumber) {
        // Validate command against scenario expectations
        return true; // Simplified for now
    }

    private String generateCommitHash(String message, String email) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-1");
            String input = message + email + System.currentTimeMillis();
            byte[] hash = md.digest(input.getBytes());
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            return UUID.randomUUID().toString().replace("-", "");
        }
    }

    private String extractCommitMessage(List<String> args) {
        for (int i = 0; i < args.size(); i++) {
            if (args.get(i).equals("-m") && i + 1 < args.size()) {
                return args.get(i + 1);
            }
        }
        return "";
    }

    private List<String> getLastCommitHashes(GitRepository repository) {
        List<GitCommit> lastCommits = gitCommitRepository
            .findByRepositoryAndBranchNameOrderByCommitTimeDesc(repository, repository.getCurrentBranch());
        
        if (!lastCommits.isEmpty()) {
            return Arrays.asList(lastCommits.get(0).getHash());
        }
        return new ArrayList<>();
    }

    private String getCurrentCommitHash(GitRepository repository) {
        List<GitCommit> commits = gitCommitRepository
            .findByRepositoryAndBranchNameOrderByCommitTimeDesc(repository, repository.getCurrentBranch());
        
        return commits.isEmpty() ? generateCommitHash("initial", "system") : commits.get(0).getHash();
    }

    // Inner classes for data transfer
    
    public static class GitCommandInfo {
        private final String fullCommand;
        private final String subCommand;
        private final List<String> args;

        public GitCommandInfo(String fullCommand, String subCommand, List<String> args) {
            this.fullCommand = fullCommand;
            this.subCommand = subCommand;
            this.args = args;
        }

        public String getFullCommand() { return fullCommand; }
        public String getSubCommand() { return subCommand; }
        public List<String> getArgs() { return args; }
    }

    @lombok.Builder
    @lombok.Data
    public static class GitCommandResult {
        private boolean successful;
        private int exitCode;
        private String output;
        private String errorOutput;
        private String commitHash;
    }

    @lombok.Builder
    @lombok.Data
    public static class GitRepositoryState {
        private Long repositoryId;
        private String currentBranch;
        private List<GitCommit> commits;
        private List<GitBranch> branches;
    }
} 