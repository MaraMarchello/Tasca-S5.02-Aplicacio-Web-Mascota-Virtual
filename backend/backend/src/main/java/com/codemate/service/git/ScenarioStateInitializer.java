package com.codemate.service.git;

import com.codemate.model.GitScenario;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Service responsible for initializing Git repositories with scenario-specific states
 * Handles complex scenario setups including files, commits, branches, and configurations
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ScenarioStateInitializer {

    private final ObjectMapper objectMapper;

    /**
     * Initializes a repository with comprehensive scenario state
     */
    public void initializeScenarioState(Path workspacePath, GitScenario scenario, String username) throws IOException {
        if (scenario == null || scenario.getInitialState() == null) {
            log.debug("No scenario state to initialize");
            return;
        }

        log.info("Initializing scenario state for: {} at: {}", scenario.getScenarioId(), workspacePath);

        try {
            JsonNode initialState = objectMapper.readTree(scenario.getInitialState());
            
            // Initialize in specific order to maintain Git consistency
            initializeFileSystem(workspacePath, initialState);
            initializeGitConfiguration(workspacePath, initialState, username);
            initializeCommitHistory(workspacePath, initialState, username);
            initializeBranches(workspacePath, initialState);
            initializeWorkingState(workspacePath, initialState);
            initializeAdvancedFeatures(workspacePath, initialState);
            
            log.info("Successfully initialized scenario state for: {}", scenario.getScenarioId());
            
        } catch (Exception e) {
            log.error("Failed to initialize scenario state for: {}", scenario.getScenarioId(), e);
            throw new IOException("Scenario state initialization failed", e);
        }
    }

    /**
     * Initializes file system with scenario files and directories
     */
    private void initializeFileSystem(Path workspacePath, JsonNode initialState) throws IOException {
        if (!initialState.has("filesystem")) {
            return;
        }

        JsonNode filesystem = initialState.get("filesystem");
        log.debug("Initializing file system structure");

        // Create directories
        if (filesystem.has("directories")) {
            createDirectories(workspacePath, filesystem.get("directories"));
        }

        // Create files
        if (filesystem.has("files")) {
            createFiles(workspacePath, filesystem.get("files"));
        }

        // Create templates
        if (filesystem.has("templates")) {
            createFromTemplates(workspacePath, filesystem.get("templates"));
        }
    }

    /**
     * Creates directories from scenario configuration
     */
    private void createDirectories(Path workspacePath, JsonNode directories) throws IOException {
        if (directories.isArray()) {
            for (JsonNode dirNode : directories) {
                String dirPath = dirNode.asText();
                Path fullPath = workspacePath.resolve(dirPath);
                Files.createDirectories(fullPath);
                log.debug("Created directory: {}", dirPath);
            }
        }
    }

    /**
     * Creates files with content from scenario configuration
     */
    private void createFiles(Path workspacePath, JsonNode files) throws IOException {
        if (files.isObject()) {
            files.fields().forEachRemaining(entry -> {
                String filePath = entry.getKey();
                JsonNode fileConfig = entry.getValue();
                
                try {
                    createSingleFile(workspacePath, filePath, fileConfig);
                } catch (IOException e) {
                    log.warn("Failed to create file {}: {}", filePath, e.getMessage());
                }
            });
        }
    }

    /**
     * Creates a single file with advanced configuration
     */
    private void createSingleFile(Path workspacePath, String filePath, JsonNode fileConfig) throws IOException {
        Path fullPath = workspacePath.resolve(filePath);
        Files.createDirectories(fullPath.getParent());

        String content;
        if (fileConfig.isTextual()) {
            // Simple string content
            content = fileConfig.asText();
        } else if (fileConfig.isObject()) {
            // Complex file configuration
            content = fileConfig.get("content").asText();
            
            // Handle file metadata if present
            if (fileConfig.has("executable") && fileConfig.get("executable").asBoolean()) {
                // Will set executable after creation
            }
        } else {
            content = "";
        }

        // Process content templates
        content = processContentTemplates(content);

        Files.write(fullPath, content.getBytes(), StandardOpenOption.CREATE);
        log.debug("Created file: {} ({} bytes)", filePath, content.length());
    }

    /**
     * Creates files from predefined templates
     */
    private void createFromTemplates(Path workspacePath, JsonNode templates) throws IOException {
        if (templates.isArray()) {
            for (JsonNode template : templates) {
                String templateType = template.get("type").asText();
                String fileName = template.has("name") ? template.get("name").asText() : getDefaultFileName(templateType);
                
                String content = generateTemplateContent(templateType, template);
                Path filePath = workspacePath.resolve(fileName);
                
                Files.write(filePath, content.getBytes(), StandardOpenOption.CREATE);
                log.debug("Created template file: {} ({})", fileName, templateType);
            }
        }
    }

    /**
     * Initializes Git configuration for the scenario
     */
    private void initializeGitConfiguration(Path workspacePath, JsonNode initialState, String username) throws IOException {
        // Set basic Git configuration
        executeGitCommand(workspacePath, "git", "config", "user.name", username);
        executeGitCommand(workspacePath, "git", "config", "user.email", username + "@codemate.local");
        
        // Apply scenario-specific Git configuration
        if (initialState.has("gitConfig")) {
            JsonNode gitConfig = initialState.get("gitConfig");
            
            gitConfig.fields().forEachRemaining(entry -> {
                try {
                    String key = entry.getKey();
                    String value = entry.getValue().asText();
                    executeGitCommand(workspacePath, "git", "config", key, value);
                    log.debug("Set Git config: {} = {}", key, value);
                } catch (Exception e) {
                    log.warn("Failed to set Git config {}: {}", entry.getKey(), e.getMessage());
                }
            });
        }
    }

    /**
     * Initializes commit history from scenario configuration
     */
    private void initializeCommitHistory(Path workspacePath, JsonNode initialState, String username) throws IOException {
        if (!initialState.has("commits")) {
            return;
        }

        JsonNode commits = initialState.get("commits");
        if (!commits.isArray()) {
            return;
        }

        log.debug("Creating commit history with {} commits", commits.size());

        for (JsonNode commitNode : commits) {
            createScenarioCommit(workspacePath, commitNode, username);
        }
    }

    /**
     * Creates a single commit from scenario configuration
     */
    private void createScenarioCommit(Path workspacePath, JsonNode commitNode, String username) throws IOException {
        String message = commitNode.get("message").asText();
        String author = commitNode.has("author") ? commitNode.get("author").asText() : username;
        
        // Handle file changes for this commit
        if (commitNode.has("changes")) {
            applyCommitChanges(workspacePath, commitNode.get("changes"));
        }

        // Add files to staging
        if (commitNode.has("addFiles")) {
            JsonNode addFiles = commitNode.get("addFiles");
            if (addFiles.isArray()) {
                for (JsonNode fileNode : addFiles) {
                    executeGitCommand(workspacePath, "git", "add", fileNode.asText());
                }
            } else if (addFiles.asText().equals("all")) {
                executeGitCommand(workspacePath, "git", "add", ".");
            }
        } else {
            // Default: add all files
            executeGitCommand(workspacePath, "git", "add", ".");
        }

        // Create commit
        List<String> commitCommand = new ArrayList<>();
        commitCommand.add("git");
        commitCommand.add("commit");
        commitCommand.add("-m");
        commitCommand.add(message);
        commitCommand.add("--author");
        commitCommand.add(author + " <" + author + "@codemate.local>");

        // Handle commit date if specified
        if (commitNode.has("date")) {
            commitCommand.add("--date");
            commitCommand.add(commitNode.get("date").asText());
        }

        executeGitCommand(workspacePath, commitCommand.toArray(new String[0]));
        log.debug("Created commit: {}", message);
    }

    /**
     * Applies file changes for a specific commit
     */
    private void applyCommitChanges(Path workspacePath, JsonNode changes) throws IOException {
        if (changes.has("modified")) {
            modifyFiles(workspacePath, changes.get("modified"));
        }
        
        if (changes.has("added")) {
            addFiles(workspacePath, changes.get("added"));
        }
        
        if (changes.has("deleted")) {
            deleteFiles(workspacePath, changes.get("deleted"));
        }
    }

    /**
     * Modifies existing files
     */
    private void modifyFiles(Path workspacePath, JsonNode modifiedFiles) throws IOException {
        modifiedFiles.fields().forEachRemaining(entry -> {
            try {
                String filePath = entry.getKey();
                String newContent = entry.getValue().asText();
                Path fullPath = workspacePath.resolve(filePath);
                Files.write(fullPath, newContent.getBytes(), StandardOpenOption.TRUNCATE_EXISTING);
                log.debug("Modified file: {}", filePath);
            } catch (IOException e) {
                log.warn("Failed to modify file {}: {}", entry.getKey(), e.getMessage());
            }
        });
    }

    /**
     * Adds new files
     */
    private void addFiles(Path workspacePath, JsonNode addedFiles) throws IOException {
        addedFiles.fields().forEachRemaining(entry -> {
            try {
                String filePath = entry.getKey();
                String content = entry.getValue().asText();
                Path fullPath = workspacePath.resolve(filePath);
                Files.createDirectories(fullPath.getParent());
                Files.write(fullPath, content.getBytes(), StandardOpenOption.CREATE);
                log.debug("Added file: {}", filePath);
            } catch (IOException e) {
                log.warn("Failed to add file {}: {}", entry.getKey(), e.getMessage());
            }
        });
    }

    /**
     * Deletes files
     */
    private void deleteFiles(Path workspacePath, JsonNode deletedFiles) throws IOException {
        if (deletedFiles.isArray()) {
            for (JsonNode fileNode : deletedFiles) {
                try {
                    String filePath = fileNode.asText();
                    Path fullPath = workspacePath.resolve(filePath);
                    Files.deleteIfExists(fullPath);
                    log.debug("Deleted file: {}", filePath);
                } catch (IOException e) {
                    log.warn("Failed to delete file {}: {}", fileNode.asText(), e.getMessage());
                }
            }
        }
    }

    /**
     * Initializes branches from scenario configuration
     */
    private void initializeBranches(Path workspacePath, JsonNode initialState) throws IOException {
        if (!initialState.has("branches")) {
            return;
        }

        JsonNode branches = initialState.get("branches");
        if (!branches.isArray()) {
            return;
        }

        log.debug("Creating branches");

        for (JsonNode branchNode : branches) {
            if (branchNode.isTextual()) {
                // Simple branch name
                String branchName = branchNode.asText();
                executeGitCommand(workspacePath, "git", "branch", branchName);
                log.debug("Created branch: {}", branchName);
            } else if (branchNode.isObject()) {
                // Complex branch configuration
                createComplexBranch(workspacePath, branchNode);
            }
        }
    }

    /**
     * Creates a branch with complex configuration
     */
    private void createComplexBranch(Path workspacePath, JsonNode branchConfig) throws IOException {
        String branchName = branchConfig.get("name").asText();
        
        // Create branch
        executeGitCommand(workspacePath, "git", "branch", branchName);
        
        // Switch to branch if specified
        if (branchConfig.has("checkout") && branchConfig.get("checkout").asBoolean()) {
            executeGitCommand(workspacePath, "git", "checkout", branchName);
            
            // Apply branch-specific changes
            if (branchConfig.has("changes")) {
                applyCommitChanges(workspacePath, branchConfig.get("changes"));
                executeGitCommand(workspacePath, "git", "add", ".");
                executeGitCommand(workspacePath, "git", "commit", "-m", "Branch-specific changes");
            }
            
            // Return to main branch
            executeGitCommand(workspacePath, "git", "checkout", "main");
        }
        
        log.debug("Created complex branch: {}", branchName);
    }

    /**
     * Initializes working directory state
     */
    private void initializeWorkingState(Path workspacePath, JsonNode initialState) throws IOException {
        if (!initialState.has("workingState")) {
            return;
        }

        JsonNode workingState = initialState.get("workingState");
        
        // Apply unstaged changes
        if (workingState.has("unstagedChanges")) {
            applyCommitChanges(workspacePath, workingState.get("unstagedChanges"));
        }
        
        // Apply staged changes
        if (workingState.has("stagedChanges")) {
            applyCommitChanges(workspacePath, workingState.get("stagedChanges"));
            executeGitCommand(workspacePath, "git", "add", ".");
        }
    }

    /**
     * Initializes advanced Git features
     */
    private void initializeAdvancedFeatures(Path workspacePath, JsonNode initialState) throws IOException {
        // Initialize remotes
        if (initialState.has("remotes")) {
            initializeRemotes(workspacePath, initialState.get("remotes"));
        }
        
        // Initialize tags
        if (initialState.has("tags")) {
            initializeTags(workspacePath, initialState.get("tags"));
        }
        
        // Initialize stashes
        if (initialState.has("stashes")) {
            initializeStashes(workspacePath, initialState.get("stashes"));
        }
    }

    /**
     * Initializes Git remotes
     */
    private void initializeRemotes(Path workspacePath, JsonNode remotes) {
        remotes.fields().forEachRemaining(entry -> {
            try {
                String remoteName = entry.getKey();
                String remoteUrl = entry.getValue().asText();
                executeGitCommand(workspacePath, "git", "remote", "add", remoteName, remoteUrl);
                log.debug("Added remote: {} -> {}", remoteName, remoteUrl);
            } catch (Exception e) {
                log.warn("Failed to add remote {}: {}", entry.getKey(), e.getMessage());
            }
        });
    }

    /**
     * Initializes Git tags
     */
    private void initializeTags(Path workspacePath, JsonNode tags) {
        if (tags.isArray()) {
            for (JsonNode tagNode : tags) {
                try {
                    if (tagNode.isTextual()) {
                        executeGitCommand(workspacePath, "git", "tag", tagNode.asText());
                    } else if (tagNode.isObject()) {
                        String tagName = tagNode.get("name").asText();
                        String message = tagNode.has("message") ? tagNode.get("message").asText() : null;
                        
                        if (message != null) {
                            executeGitCommand(workspacePath, "git", "tag", "-a", tagName, "-m", message);
                        } else {
                            executeGitCommand(workspacePath, "git", "tag", tagName);
                        }
                    }
                    log.debug("Created tag: {}", tagNode);
                } catch (Exception e) {
                    log.warn("Failed to create tag {}: {}", tagNode, e.getMessage());
                }
            }
        }
    }

    /**
     * Initializes Git stashes (simulated)
     */
    private void initializeStashes(Path workspacePath, JsonNode stashes) {
        // Note: This would require complex simulation since stashes are ephemeral
        log.debug("Stash initialization not implemented (stashes are ephemeral)");
    }

    /**
     * Processes content templates with variable substitution
     */
    private String processContentTemplates(String content) {
        // Replace common template variables
        content = content.replace("{{USERNAME}}", "codemate_user");
        content = content.replace("{{DATE}}", java.time.LocalDate.now().toString());
        content = content.replace("{{TIMESTAMP}}", String.valueOf(System.currentTimeMillis()));
        
        return content;
    }

    /**
     * Generates content for predefined templates
     */
    private String generateTemplateContent(String templateType, JsonNode templateConfig) {
        switch (templateType.toLowerCase()) {
            case "javascript":
                return generateJavaScriptTemplate(templateConfig);
            case "python":
                return generatePythonTemplate(templateConfig);
            case "java":
                return generateJavaTemplate(templateConfig);
            case "readme":
                return generateReadmeTemplate(templateConfig);
            case "gitignore":
                return generateGitignoreTemplate(templateConfig);
            default:
                return "// Generated template: " + templateType + "\n";
        }
    }

    /**
     * Gets default file name for template type
     */
    private String getDefaultFileName(String templateType) {
        switch (templateType.toLowerCase()) {
            case "javascript": return "app.js";
            case "python": return "main.py";
            case "java": return "Main.java";
            case "readme": return "README.md";
            case "gitignore": return ".gitignore";
            default: return "template." + templateType;
        }
    }

    // Template generators
    private String generateJavaScriptTemplate(JsonNode config) {
        return "// JavaScript Application\n" +
               "console.log('Hello, CodeMate!');\n\n" +
               "function main() {\n" +
               "    // Your code here\n" +
               "}\n\n" +
               "main();\n";
    }

    private String generatePythonTemplate(JsonNode config) {
        return "#!/usr/bin/env python3\n" +
               "# Python Application\n\n" +
               "def main():\n" +
               "    print('Hello, CodeMate!')\n" +
               "    # Your code here\n\n" +
               "if __name__ == '__main__':\n" +
               "    main()\n";
    }

    private String generateJavaTemplate(JsonNode config) {
        return "public class Main {\n" +
               "    public static void main(String[] args) {\n" +
               "        System.out.println(\"Hello, CodeMate!\");\n" +
               "        // Your code here\n" +
               "    }\n" +
               "}\n";
    }

    private String generateReadmeTemplate(JsonNode config) {
        String projectName = config.has("projectName") ? config.get("projectName").asText() : "CodeMate Project";
        return "# " + projectName + "\n\n" +
               "This is a Git learning project created with CodeMate.\n\n" +
               "## Getting Started\n\n" +
               "Follow the scenario instructions to complete the exercises.\n\n" +
               "## Commands to Try\n\n" +
               "- `git status` - Check repository status\n" +
               "- `git log` - View commit history\n" +
               "- `git add .` - Stage all changes\n" +
               "- `git commit -m \"message\"` - Create commit\n";
    }

    private String generateGitignoreTemplate(JsonNode config) {
        return "# Dependencies\n" +
               "node_modules/\n" +
               "__pycache__/\n" +
               "*.pyc\n\n" +
               "# Build outputs\n" +
               "build/\n" +
               "dist/\n" +
               "*.class\n\n" +
               "# IDE files\n" +
               ".vscode/\n" +
               ".idea/\n" +
               "*.swp\n" +
               "*.swo\n\n" +
               "# OS files\n" +
               ".DS_Store\n" +
               "Thumbs.db\n";
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
}
