package com.codemate.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

/**
 * Configuration component for Git execution settings
 * Provides startup information about Git execution mode
 */
@Component
@Slf4j
public class GitExecutionConfig implements CommandLineRunner {

    @Value("${app.git.execution.enabled:false}")
    private boolean gitExecutionEnabled;

    @Value("${app.git.execution.timeout:30}")
    private int commandTimeout;

    @Value("${app.git.workspace.base-path:${java.io.tmpdir}/codemate-git-workspaces}")
    private String workspacePath;

    @Override
    public void run(String... args) {
        log.info("=== Git Execution Configuration ===");
        log.info("Real Git Execution: {}", gitExecutionEnabled ? "ENABLED" : "DISABLED (Simulation Mode)");
        log.info("Command Timeout: {} seconds", commandTimeout);
        log.info("Workspace Path: {}", workspacePath);
        
        if (gitExecutionEnabled) {
            log.warn("Real Git execution is ENABLED. Commands will be executed in isolated environments.");
            log.warn("To disable real Git execution, set app.git.execution.enabled=false");
        } else {
            log.info("Real Git execution is DISABLED. Using simulation mode for all commands.");
            log.info("To enable real Git execution, set app.git.execution.enabled=true");
        }
        log.info("====================================");
    }
}
