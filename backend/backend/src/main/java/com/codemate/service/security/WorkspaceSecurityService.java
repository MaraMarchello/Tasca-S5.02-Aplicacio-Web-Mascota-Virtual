package com.codemate.service.security;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Service responsible for securing Git workspaces and ensuring proper isolation
 * Implements additional security measures beyond basic workspace creation
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class WorkspaceSecurityService {

    @Value("${app.git.workspace.base-path:${java.io.tmpdir}/codemate-git-workspaces}")
    private String baseWorkspacePath;

    @Value("${app.git.workspace.max-size-mb:100}")
    private long maxWorkspaceSizeMb;

    @Value("${app.git.security.enable-posix-permissions:true}")
    private boolean enablePosixPermissions;

    // Pattern to validate workspace names
    private static final Pattern SAFE_WORKSPACE_NAME = Pattern.compile("^[a-zA-Z0-9_-]+$");
    
    // Maximum allowed workspace name length
    private static final int MAX_WORKSPACE_NAME_LENGTH = 255;

    /**
     * Creates a secure isolated workspace for a user
     */
    public Path createSecureWorkspace(Long userId, Long repositoryId, String workspaceName) throws IOException {
        validateWorkspaceName(workspaceName);
        
        // Create user-specific base directory
        Path userBaseDir = createUserDirectory(userId);
        
        // Create workspace with secure permissions
        Path workspacePath = userBaseDir.resolve(sanitizeWorkspaceName(workspaceName));
        
        // Ensure workspace doesn't already exist
        if (Files.exists(workspacePath)) {
            throw new SecurityException("Workspace already exists: " + workspaceName);
        }
        
        // Create workspace directory with restricted permissions
        Files.createDirectories(workspacePath);
        secureWorkspacePermissions(workspacePath, userId);
        
        // Create security metadata file
        createSecurityMetadata(workspacePath, userId, repositoryId);
        
        log.info("Created secure workspace for user {} at: {}", userId, workspacePath);
        return workspacePath;
    }

    /**
     * Validates that a workspace belongs to the specified user
     */
    public void validateWorkspaceAccess(Path workspacePath, Long userId) {
        if (!isWithinUserDirectory(workspacePath, userId)) {
            throw new SecurityException("Workspace access denied: path outside user directory");
        }
        
        if (!Files.exists(workspacePath)) {
            throw new SecurityException("Workspace does not exist: " + workspacePath);
        }
        
        // Verify ownership through metadata
        validateWorkspaceOwnership(workspacePath, userId);
    }

    /**
     * Checks if workspace size is within allowed limits
     */
    public void validateWorkspaceSize(Path workspacePath) throws IOException {
        long sizeMb = calculateDirectorySize(workspacePath) / (1024 * 1024);
        
        if (sizeMb > maxWorkspaceSizeMb) {
            throw new SecurityException(
                String.format("Workspace size (%dMB) exceeds limit (%dMB)", sizeMb, maxWorkspaceSizeMb)
            );
        }
    }

    /**
     * Sanitizes file paths to prevent directory traversal attacks
     */
    public String sanitizeFilePath(String filePath) {
        if (filePath == null || filePath.trim().isEmpty()) {
            throw new SecurityException("File path cannot be empty");
        }
        
        // Remove dangerous path components
        String sanitized = filePath
            .replaceAll("\\.\\./", "")  // Remove parent directory references
            .replaceAll("\\.\\.", "")   // Remove any remaining dots
            .replaceAll("//+", "/")     // Replace multiple slashes with single
            .replaceAll("^/+", "")      // Remove leading slashes
            .trim();
        
        if (sanitized.isEmpty()) {
            throw new SecurityException("Invalid file path after sanitization");
        }
        
        // Ensure path doesn't contain absolute paths or dangerous patterns
        if (sanitized.startsWith("/") || sanitized.contains("\\") || 
            sanitized.contains(":") || sanitized.contains("*")) {
            throw new SecurityException("File path contains invalid characters: " + filePath);
        }
        
        return sanitized;
    }

    /**
     * Creates user-specific directory with proper isolation
     */
    private Path createUserDirectory(Long userId) throws IOException {
        Path baseDir = Paths.get(baseWorkspacePath);
        Path userDir = baseDir.resolve("user_" + userId);
        
        Files.createDirectories(userDir);
        secureDirectoryPermissions(userDir);
        
        return userDir;
    }

    /**
     * Validates workspace name for security
     */
    private void validateWorkspaceName(String workspaceName) {
        if (workspaceName == null || workspaceName.trim().isEmpty()) {
            throw new SecurityException("Workspace name cannot be empty");
        }
        
        if (workspaceName.length() > MAX_WORKSPACE_NAME_LENGTH) {
            throw new SecurityException("Workspace name too long");
        }
        
        if (!SAFE_WORKSPACE_NAME.matcher(workspaceName).matches()) {
            throw new SecurityException("Workspace name contains invalid characters");
        }
    }

    /**
     * Sanitizes workspace name to ensure it's safe for filesystem
     */
    private String sanitizeWorkspaceName(String workspaceName) {
        return workspaceName.replaceAll("[^a-zA-Z0-9_-]", "_");
    }

    /**
     * Sets secure permissions on workspace directory
     */
    private void secureWorkspacePermissions(Path workspacePath, Long userId) throws IOException {
        if (!enablePosixPermissions) {
            return; // Skip on Windows or when disabled
        }
        
        try {
            // Set owner read/write/execute only (700)
            Set<PosixFilePermission> permissions = PosixFilePermissions.fromString("rwx------");
            Files.setPosixFilePermissions(workspacePath, permissions);
            
            log.debug("Set secure permissions on workspace: {}", workspacePath);
        } catch (UnsupportedOperationException e) {
            log.warn("POSIX permissions not supported on this filesystem");
        }
    }

    /**
     * Sets secure permissions on directory
     */
    private void secureDirectoryPermissions(Path dirPath) throws IOException {
        if (!enablePosixPermissions) {
            return;
        }
        
        try {
            Set<PosixFilePermission> permissions = PosixFilePermissions.fromString("rwx------");
            Files.setPosixFilePermissions(dirPath, permissions);
        } catch (UnsupportedOperationException e) {
            log.debug("POSIX permissions not supported");
        }
    }

    /**
     * Creates security metadata file in workspace
     */
    private void createSecurityMetadata(Path workspacePath, Long userId, Long repositoryId) throws IOException {
        Path metadataFile = workspacePath.resolve(".codemate_security");
        
        String metadata = String.format(
            "# CodeMate Workspace Security Metadata\n" +
            "owner_user_id=%d\n" +
            "repository_id=%d\n" +
            "created_timestamp=%d\n" +
            "workspace_version=1.0\n",
            userId, repositoryId, System.currentTimeMillis()
        );
        
        Files.write(metadataFile, metadata.getBytes());
        
        // Hide metadata file if possible
        try {
            Files.setAttribute(metadataFile, "dos:hidden", true);
        } catch (Exception e) {
            // Ignore if not supported
        }
    }

    /**
     * Validates that workspace is within user's directory
     */
    private boolean isWithinUserDirectory(Path workspacePath, Long userId) {
        Path userDir = Paths.get(baseWorkspacePath, "user_" + userId);
        Path resolvedWorkspace = workspacePath.toAbsolutePath().normalize();
        Path resolvedUserDir = userDir.toAbsolutePath().normalize();
        
        return resolvedWorkspace.startsWith(resolvedUserDir);
    }

    /**
     * Validates workspace ownership through metadata
     */
    private void validateWorkspaceOwnership(Path workspacePath, Long userId) {
        Path metadataFile = workspacePath.resolve(".codemate_security");
        
        if (!Files.exists(metadataFile)) {
            log.warn("Security metadata missing for workspace: {}", workspacePath);
            throw new SecurityException("Workspace security metadata missing");
        }
        
        try {
            String metadata = Files.readString(metadataFile);
            if (!metadata.contains("owner_user_id=" + userId)) {
                throw new SecurityException("Workspace access denied: ownership mismatch");
            }
        } catch (IOException e) {
            throw new SecurityException("Failed to read workspace security metadata", e);
        }
    }

    /**
     * Calculates directory size in bytes
     */
    private long calculateDirectorySize(Path dirPath) throws IOException {
        return Files.walk(dirPath)
            .filter(Files::isRegularFile)
            .mapToLong(path -> {
                try {
                    return Files.size(path);
                } catch (IOException e) {
                    return 0;
                }
            })
            .sum();
    }

    /**
     * Securely deletes workspace directory and all contents
     */
    public void secureDeleteWorkspace(Path workspacePath, Long userId) {
        try {
            validateWorkspaceAccess(workspacePath, userId);
            
            // Recursively delete all files
            Files.walk(workspacePath)
                .sorted((a, b) -> b.compareTo(a)) // Delete files before directories
                .forEach(path -> {
                    try {
                        Files.delete(path);
                    } catch (IOException e) {
                        log.warn("Failed to delete workspace file: {}", path, e);
                    }
                });
                
            log.info("Securely deleted workspace: {}", workspacePath);
            
        } catch (Exception e) {
            log.error("Failed to securely delete workspace: {}", workspacePath, e);
            throw new SecurityException("Failed to delete workspace", e);
        }
    }
}
