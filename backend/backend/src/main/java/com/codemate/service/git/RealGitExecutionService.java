package com.codemate.service.git;

import com.codemate.model.GitRepository;
import com.codemate.service.git.GitRepositoryManagementService.GitCommandResult;
import com.codemate.service.security.*;
import com.codemate.util.PerformanceMonitor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Service responsible for executing real Git commands in isolated environments
 * This service provides actual Git command execution as an alternative to simulation
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RealGitExecutionService {

    private final GitCommandValidator gitCommandValidator;
    private final GitSessionService gitSessionService;
    private final PerformanceMonitor performanceMonitor;
    private final GitRateLimitingService rateLimitingService;
    private final ResourceMonitoringService resourceMonitoringService;
    private final SecurityAuditService securityAuditService;

    @Value("${app.git.execution.timeout:30}")
    private long commandTimeoutSeconds;

    @Value("${app.git.execution.enabled:false}")
    private boolean realGitExecutionEnabled;

    /**
     * Executes a real Git command in an isolated workspace
     */
    public GitCommandResult executeRealGitCommand(GitRepository repository, String command, Long userId) {
        if (!realGitExecutionEnabled) {
            return GitCommandResult.builder()
                .successful(false)
                .exitCode(1)
                .output("")
                .errorOutput("Real Git execution is disabled. Using simulation mode.")
                .build();
        }

        performanceMonitor.startOperation("real_git_command_execution", userId, repository.getId(), null);
        long startTime = System.currentTimeMillis();
        
        log.info("Executing real Git command: {} in repository: {} for user: {}", command, repository.getId(), userId);
        
        try {
            // Check rate limits
            rateLimitingService.checkRateLimit(userId, command);
            
            // Check system resources
            resourceMonitoringService.checkSystemResources();
            
            // Validate command for security
            gitCommandValidator.validateCommand(command);
            
            // Get or create isolated workspace for this repository
            Path workspacePath = gitSessionService.getOrCreateWorkspace(repository, userId);
            
            // Execute the command
            GitCommandResult result = executeCommandInWorkspace(workspacePath, command);
            
            // Update repository state from real Git workspace
            if (result.isSuccessful()) {
                gitSessionService.syncRepositoryStateFromWorkspace(repository, workspacePath);
            }
            
            long duration = System.currentTimeMillis() - startTime;
            performanceMonitor.recordGitCommandExecution(command, duration, result.isSuccessful(), userId, repository.getId(), null);
            
            // Record rate limiting completion
            rateLimitingService.recordCommandCompletion(userId, command, result.isSuccessful(), duration);
            
            // Audit log successful command
            securityAuditService.logGitCommandEvent(userId, "user", command, 
                repository.getId().toString(), result.isSuccessful(), 
                result.getOutput(), result.getErrorOutput(), duration);
            
            log.info("Real Git command executed in {}ms: {} (success: {})", duration, command, result.isSuccessful());
            return result;
            
        } catch (SecurityException e) {
            log.warn("Security violation in Git command: {} for user: {}", command, userId, e);
            
            // Audit log security violation
            securityAuditService.logSecurityViolation(userId, "user", "COMMAND_SECURITY_VIOLATION", 
                command, e.getMessage(), "system");
            
            return GitCommandResult.builder()
                .successful(false)
                .exitCode(1)
                .output("")
                .errorOutput("Command not allowed: " + e.getMessage())
                .build();
                
        } catch (Exception e) {
            log.error("Error executing real Git command: {} for user: {}", command, userId, e);
            return GitCommandResult.builder()
                .successful(false)
                .exitCode(1)
                .output("")
                .errorOutput("Command execution failed: " + e.getMessage())
                .build();
                
        } finally {
            performanceMonitor.endOperation("real_git_command_execution", userId, repository.getId());
        }
    }

    /**
     * Executes a Git command in the specified workspace directory
     */
    private GitCommandResult executeCommandInWorkspace(Path workspacePath, String command) throws IOException, InterruptedException {
        log.debug("Executing command in workspace: {} -> {}", workspacePath, command);
        
        // Prepare command for execution
        List<String> commandList = new ArrayList<>();
        
        // Use bash/cmd to handle complex commands properly
        if (System.getProperty("os.name").toLowerCase().contains("windows")) {
            commandList.add("cmd");
            commandList.add("/c");
        } else {
            commandList.add("/bin/bash");
            commandList.add("-c");
        }
        commandList.add(command);
        
        ProcessBuilder processBuilder = new ProcessBuilder(commandList);
        processBuilder.directory(workspacePath.toFile());
        
        // Set environment variables for Git
        processBuilder.environment().put("GIT_TERMINAL_PROMPT", "0"); // Disable interactive prompts
        processBuilder.environment().put("GIT_MERGE_AUTOEDIT", "no"); // Disable auto-edit for merge commits
        
        // Redirect stderr to stdout to capture all output
        processBuilder.redirectErrorStream(false);
        
        Process process = processBuilder.start();
        
        // Read output streams
        StringBuilder output = new StringBuilder();
        StringBuilder errorOutput = new StringBuilder();
        
        // Start threads to read output and error streams
        Thread outputReader = new Thread(() -> {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append("\n");
                }
            } catch (IOException e) {
                log.warn("Error reading command output", e);
            }
        });
        
        Thread errorReader = new Thread(() -> {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getErrorStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    errorOutput.append(line).append("\n");
                }
            } catch (IOException e) {
                log.warn("Error reading command error output", e);
            }
        });
        
        outputReader.start();
        errorReader.start();
        
        // Wait for process to complete with timeout
        boolean finished = process.waitFor(commandTimeoutSeconds, TimeUnit.SECONDS);
        
        if (!finished) {
            process.destroyForcibly();
            throw new RuntimeException("Command execution timed out after " + commandTimeoutSeconds + " seconds");
        }
        
        // Wait for output readers to finish
        outputReader.join(1000);
        errorReader.join(1000);
        
        int exitCode = process.exitValue();
        String outputStr = output.toString().trim();
        String errorOutputStr = errorOutput.toString().trim();
        
        log.debug("Command completed with exit code: {} output length: {} error length: {}", 
                 exitCode, outputStr.length(), errorOutputStr.length());
        
        return GitCommandResult.builder()
            .successful(exitCode == 0)
            .exitCode(exitCode)
            .output(outputStr)
            .errorOutput(errorOutputStr)
            .build();
    }

    /**
     * Checks if real Git execution is enabled
     */
    public boolean isRealGitExecutionEnabled() {
        return realGitExecutionEnabled;
    }

    /**
     * Gets the timeout for Git command execution
     */
    public long getCommandTimeoutSeconds() {
        return commandTimeoutSeconds;
    }
}
