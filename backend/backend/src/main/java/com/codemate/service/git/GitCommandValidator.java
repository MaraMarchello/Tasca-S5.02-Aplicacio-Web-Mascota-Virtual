package com.codemate.service.git;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.regex.Pattern;

/**
 * Service responsible for validating Git commands for security and safety
 * Prevents execution of dangerous commands that could compromise the system
 */
@Component
@Slf4j
public class GitCommandValidator {

    // Allowed Git subcommands for real execution
    private static final Set<String> ALLOWED_GIT_COMMANDS = Set.of(
        "status", "log", "add", "commit", "checkout", "branch", "switch",
        "merge", "diff", "show", "tag", "remote", "fetch", "pull", "push",
        "init", "clone", "reset", "revert", "stash", "cherry-pick", "rebase",
        "config", "ls-files", "ls-remote", "describe", "shortlog", "blame",
        "grep", "bisect", "reflog", "archive", "bundle", "notes", "worktree",
        "fs" // Custom file system simulation command
    );

    // Dangerous patterns that should never be allowed
    private static final Set<String> BLOCKED_PATTERNS = Set.of(
        "rm -rf", "rmdir", "del", "format", "fsck", "gc --aggressive",
        "clean -f", "clean -d", "clean -x", "reset --hard HEAD~",
        "filter-branch", "replace", "update-ref", "symbolic-ref",
        "for-each-ref", "pack-refs", "verify-pack", "prune",
        "daemon", "serve", "upload-pack", "receive-pack",
        "--exec", "--upload-pack", "--receive-pack",
        "credential", "credential-", "askpass"
    );

    // Patterns for potentially dangerous options
    private static final Set<String> DANGEROUS_OPTIONS = Set.of(
        "--force", "-f", "--hard", "--aggressive", "--prune-packaged",
        "--delete-unprotected", "--unreachable", "--expire",
        "--allow-unrelated-histories", "--strategy-option"
    );

    // File system commands that should be blocked
    private static final Set<String> BLOCKED_FILESYSTEM_COMMANDS = Set.of(
        "cat", "rm", "mv", "cp", "chmod", "chown", "ln", "mkdir",
        "rmdir", "touch", "dd", "mount", "umount", "fdisk",
        "mkfs", "fsck", "df", "du", "find", "locate", "which"
    );

    // Network commands that should be blocked
    private static final Set<String> BLOCKED_NETWORK_COMMANDS = Set.of(
        "curl", "wget", "nc", "netcat", "telnet", "ssh", "scp",
        "rsync", "ftp", "sftp", "ping", "nslookup", "dig"
    );

    // System commands that should be blocked
    private static final Set<String> BLOCKED_SYSTEM_COMMANDS = Set.of(
        "sudo", "su", "passwd", "useradd", "userdel", "usermod",
        "groupadd", "groupdel", "ps", "kill", "killall", "pkill",
        "nohup", "screen", "tmux", "systemctl", "service",
        "crontab", "at", "batch", "jobs", "bg", "fg"
    );

    // Pattern for Git command structure
    private static final Pattern GIT_COMMAND_PATTERN = Pattern.compile(
        "^git\\s+([a-zA-Z0-9_-]+)(?:\\s+.*)?$"
    );

    // Pattern for potentially malicious characters
    private static final Pattern MALICIOUS_PATTERN = Pattern.compile(
        "[;&|`$(){}\\[\\]<>\\\\]"
    );

    /**
     * Validates a Git command for security and safety
     * @param command The command to validate
     * @throws SecurityException if the command is not allowed
     */
    public void validateCommand(String command) {
        if (command == null || command.trim().isEmpty()) {
            throw new SecurityException("Command cannot be empty");
        }

        String trimmedCommand = command.trim().toLowerCase();
        
        log.debug("Validating command: {}", command);

        // Check for malicious characters and patterns
        validateCommandStructure(command, trimmedCommand);
        
        // Validate it's a Git command
        validateGitCommand(command, trimmedCommand);
        
        // Check for blocked patterns
        validateAgainstBlockedPatterns(trimmedCommand);
        
        // Validate Git subcommand
        validateGitSubcommand(trimmedCommand);
        
        // Check for dangerous options
        validateCommandOptions(trimmedCommand);
        
        log.debug("Command validation passed: {}", command);
    }

    private void validateCommandStructure(String originalCommand, String trimmedCommand) {
        // Check for command injection patterns
        if (MALICIOUS_PATTERN.matcher(originalCommand).find()) {
            // Allow some characters that are valid in Git commands
            String allowedChars = originalCommand.replaceAll("[\"'\\-\\.@/:]", "");
            if (MALICIOUS_PATTERN.matcher(allowedChars).find()) {
                throw new SecurityException("Command contains potentially malicious characters");
            }
        }

        // Check for multiple commands (command chaining)
        if (trimmedCommand.contains("&&") || trimmedCommand.contains("||") || 
            trimmedCommand.contains(";") || trimmedCommand.contains("|")) {
            throw new SecurityException("Command chaining is not allowed");
        }

        // Check for redirection operators
        if (trimmedCommand.contains(">") || trimmedCommand.contains("<")) {
            throw new SecurityException("I/O redirection is not allowed");
        }

        // Check for background execution
        if (trimmedCommand.endsWith("&")) {
            throw new SecurityException("Background execution is not allowed");
        }
    }

    private void validateGitCommand(String originalCommand, String trimmedCommand) {
        // Must start with 'git' or be a git subcommand
        if (!trimmedCommand.startsWith("git ") && !trimmedCommand.equals("git")) {
            // Allow git fs commands for file simulation
            if (!trimmedCommand.startsWith("git fs ")) {
                throw new SecurityException("Only Git commands are allowed");
            }
        }

        // Validate Git command structure
        if (!GIT_COMMAND_PATTERN.matcher(originalCommand).matches() && 
            !originalCommand.trim().equals("git")) {
            throw new SecurityException("Invalid Git command structure");
        }
    }

    private void validateAgainstBlockedPatterns(String trimmedCommand) {
        // Check against blocked patterns
        for (String blockedPattern : BLOCKED_PATTERNS) {
            if (trimmedCommand.contains(blockedPattern)) {
                throw new SecurityException("Command contains blocked pattern: " + blockedPattern);
            }
        }

        // Check against blocked filesystem commands
        for (String blockedCmd : BLOCKED_FILESYSTEM_COMMANDS) {
            if (trimmedCommand.contains(blockedCmd)) {
                throw new SecurityException("Filesystem command not allowed: " + blockedCmd);
            }
        }

        // Check against blocked network commands
        for (String blockedCmd : BLOCKED_NETWORK_COMMANDS) {
            if (trimmedCommand.contains(blockedCmd)) {
                throw new SecurityException("Network command not allowed: " + blockedCmd);
            }
        }

        // Check against blocked system commands
        for (String blockedCmd : BLOCKED_SYSTEM_COMMANDS) {
            if (trimmedCommand.contains(blockedCmd)) {
                throw new SecurityException("System command not allowed: " + blockedCmd);
            }
        }
    }

    private void validateGitSubcommand(String trimmedCommand) {
        if (trimmedCommand.equals("git")) {
            return; // Just "git" is allowed (shows help)
        }

        // Extract subcommand
        String[] parts = trimmedCommand.split("\\s+");
        if (parts.length < 2) {
            throw new SecurityException("Git command requires a subcommand");
        }

        String subcommand = parts[1];
        
        // Validate subcommand is in allowed list
        if (!ALLOWED_GIT_COMMANDS.contains(subcommand)) {
            throw new SecurityException("Git subcommand not allowed: " + subcommand);
        }
    }

    private void validateCommandOptions(String trimmedCommand) {
        // Check for dangerous options, but allow some common safe ones
        for (String dangerousOption : DANGEROUS_OPTIONS) {
            if (trimmedCommand.contains(dangerousOption)) {
                // Allow some specific safe uses
                if (dangerousOption.equals("-f") && trimmedCommand.contains("git push -f")) {
                    continue; // Allow force push in learning environment
                }
                if (dangerousOption.equals("--force") && trimmedCommand.contains("git push --force")) {
                    continue; // Allow force push in learning environment
                }
                if (dangerousOption.equals("--hard") && trimmedCommand.contains("git reset --hard")) {
                    // Allow hard reset but warn it's destructive
                    log.warn("Allowing destructive command for learning: {}", trimmedCommand);
                    continue;
                }
                
                throw new SecurityException("Dangerous option not allowed: " + dangerousOption);
            }
        }
    }

    /**
     * Checks if a command is a safe Git command without throwing exceptions
     * @param command The command to check
     * @return true if the command is safe, false otherwise
     */
    public boolean isSafeCommand(String command) {
        try {
            validateCommand(command);
            return true;
        } catch (SecurityException e) {
            log.debug("Command failed validation: {} - {}", command, e.getMessage());
            return false;
        }
    }

    /**
     * Gets the list of allowed Git commands
     * @return Set of allowed Git subcommands
     */
    public Set<String> getAllowedCommands() {
        return ALLOWED_GIT_COMMANDS;
    }

    /**
     * Sanitizes a command by removing potentially dangerous parts
     * @param command The command to sanitize
     * @return Sanitized version of the command
     */
    public String sanitizeCommand(String command) {
        if (command == null) {
            return "";
        }
        
        // Remove multiple spaces and trim
        String sanitized = command.trim().replaceAll("\\s+", " ");
        
        // Remove any trailing semicolons or other command separators
        sanitized = sanitized.replaceAll("[;&|]+$", "");
        
        return sanitized;
    }
}
