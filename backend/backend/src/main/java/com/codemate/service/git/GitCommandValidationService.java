package com.codemate.service.git;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

/**
 * Service responsible for Git command validation and security
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class GitCommandValidationService {

    /**
     * Sanitizes git command by removing potentially dangerous characters
     */
    public String sanitizeGitCommand(String command) {
        if (command == null || command.isEmpty()) {
            return "";
        }
        
        // Remove potentially dangerous characters
        String sanitized = command.replaceAll("[;&|`$(){}\\[\\]<>]", "")
                                 .replaceAll("\\.\\.+", ".")  // Remove directory traversal attempts
                                 .trim();
        
        // Limit command length to prevent DoS
        if (sanitized.length() > 500) {
            log.warn("Command too long, truncating: {}", sanitized.substring(0, 50) + "...");
            sanitized = sanitized.substring(0, 500);
        }
        
        log.debug("Sanitized command '{}' to '{}'", command, sanitized);
        return sanitized;
    }

    /**
     * Validates if a git command is allowed in the simulation environment
     */
    public boolean isCommandAllowed(String command) {
        if (command == null || command.isEmpty()) {
            return false;
        }
        
        String[] parts = command.trim().toLowerCase().split("\\s+");
        if (parts.length == 0) {
            return false;
        }
        
        // Must start with 'git'
        if (!"git".equals(parts[0])) {
            log.warn("Command must start with 'git': {}", command);
            return false;
        }
        
        if (parts.length < 2) {
            return true; // Just 'git' command is allowed
        }
        
        String subCommand = parts[1];
        
        // Allowed git subcommands
        Set<String> allowedCommands = Set.of(
            "init", "add", "commit", "status", "log", "diff", "show",
            "branch", "checkout", "switch", "merge", "rebase", "reset",
            "stash", "remote", "fetch", "pull", "push", "clone",
            "config", "help", "version", "tag", "blame", "ls-files",
            "cherry-pick", "restore", "clean",
            "fs" // Our custom file system simulation command
        );
        
        boolean isAllowed = allowedCommands.contains(subCommand);
        if (!isAllowed) {
            log.warn("Subcommand '{}' not allowed in simulation environment", subCommand);
        }
        
        return isAllowed;
    }

    /**
     * Parses a Git command into its components
     */
    public GitCommandInfo parseGitCommand(String command) {
        if (command == null || command.trim().isEmpty()) {
            throw new IllegalArgumentException("Command cannot be null or empty");
        }
        
        String[] parts = command.trim().split("\\s+");
        if (parts.length == 0) {
            throw new IllegalArgumentException("Invalid git command: " + command);
        }
        
        if (!parts[0].equalsIgnoreCase("git")) {
            throw new IllegalArgumentException("Command must start with 'git': " + command);
        }

        String subCommand = parts.length > 1 ? parts[1] : "";
        List<String> args = parts.length > 2 ? 
            new ArrayList<>(Arrays.asList(parts).subList(2, parts.length)) : 
            new ArrayList<>();

        log.debug("Parsed git command: subCommand='{}', args={}", subCommand, args);
        return new GitCommandInfo(command, subCommand, args);
    }

    /**
     * Validates command parameters for security
     */
    public ValidationResult validateCommandParameters(String command, Long repositoryId, Long userId) {
        // Validate input parameters
        if (repositoryId == null || command == null || userId == null) {
            log.warn("Invalid parameters for command execution: repositoryId={}, command={}, userId={}", 
                    repositoryId, command, userId);
            return ValidationResult.invalid("Invalid command parameters");
        }

        // Sanitize and validate command
        String sanitizedCommand = sanitizeGitCommand(command);
        if (!isCommandAllowed(sanitizedCommand)) {
            log.warn("Disallowed command attempted by user {}: {}", userId, sanitizedCommand);
            return ValidationResult.invalid("Command not allowed or invalid syntax");
        }

        return ValidationResult.valid(sanitizedCommand);
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

    public static class ValidationResult {
        private final boolean valid;
        private final String sanitizedCommand;
        private final String errorMessage;

        private ValidationResult(boolean valid, String sanitizedCommand, String errorMessage) {
            this.valid = valid;
            this.sanitizedCommand = sanitizedCommand;
            this.errorMessage = errorMessage;
        }

        public static ValidationResult valid(String sanitizedCommand) {
            return new ValidationResult(true, sanitizedCommand, null);
        }

        public static ValidationResult invalid(String errorMessage) {
            return new ValidationResult(false, null, errorMessage);
        }

        public boolean isValid() { return valid; }
        public String getSanitizedCommand() { return sanitizedCommand; }
        public String getErrorMessage() { return errorMessage; }
    }
}
