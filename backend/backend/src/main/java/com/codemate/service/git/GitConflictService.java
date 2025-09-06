package com.codemate.service.git;

import com.codemate.model.GitRepository;
import com.codemate.service.git.GitStateManagementService.RepositoryRuntimeState;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Service responsible for generating and managing Git conflicts
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class GitConflictService {

    private final GitStateManagementService gitStateManagementService;

    /**
     * Generates a conflict scenario in the repository
     */
    public void generateConflictScenario(GitRepository repository, String conflictType) {
        log.info("Generating conflict scenario of type: {} in repository: {}", conflictType, repository.getId());
        
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
     * Generates a merge conflict scenario
     */
    public void generateMergeConflict(GitRepository repository) {
        log.info("Generating merge conflict for repository: {}", repository.getId());
        
        RepositoryRuntimeState state = gitStateManagementService.getRuntimeState(repository);
        
        // Create a conflicted file
        String conflictedContent = createMergeConflictContent("Content from current branch", "Content from feature branch", "feature-branch");
        gitStateManagementService.addFileToWorkingDirectory(state, "conflicted-file.txt", conflictedContent);
        
        // Create a regular file that doesn't conflict
        gitStateManagementService.addFileToWorkingDirectory(state, "normal-file.txt", "This file has no conflicts");
        
        gitStateManagementService.saveRuntimeState(repository, state);
        log.info("Created merge conflict scenario in repository: {}", repository.getId());
    }

    /**
     * Generates a rebase conflict scenario
     */
    public void generateRebaseConflict(GitRepository repository) {
        log.info("Generating rebase conflict for repository: {}", repository.getId());
        
        RepositoryRuntimeState state = gitStateManagementService.getRuntimeState(repository);
        
        // Create a rebase conflict scenario
        String rebaseConflictContent = createRebaseConflictContent("Changes from the current branch", "Changes being rebased", "commit-hash");
        gitStateManagementService.addFileToWorkingDirectory(state, "rebase-conflict.txt", rebaseConflictContent);
        
        gitStateManagementService.saveRuntimeState(repository, state);
        log.info("Created rebase conflict scenario in repository: {}", repository.getId());
    }

    /**
     * Creates merge conflict markers for a specific scenario
     */
    public String createMergeConflictContent(String currentContent, String incomingContent, String branchName) {
        return String.format(
            "<<<<<<< HEAD\n%s\n=======\n%s\n>>>>>>> %s\n",
            currentContent, incomingContent, branchName
        );
    }

    /**
     * Creates rebase conflict markers for a specific scenario
     */
    public String createRebaseConflictContent(String currentContent, String rebasedContent, String commitHash) {
        return String.format(
            "<<<<<<< HEAD\n%s\n=======\n%s\n>>>>>>> %s\n",
            currentContent, rebasedContent, commitHash
        );
    }

    /**
     * Creates cherry-pick conflict markers
     */
    public String createCherryPickConflictContent(String currentContent, String cherryPickContent, String commitHash) {
        return String.format(
            "<<<<<<< HEAD\n%s\n=======\n%s (cherry-picked from %s)\n>>>>>>> %s\n",
            currentContent, cherryPickContent, commitHash.substring(0, 7), commitHash.substring(0, 7)
        );
    }

    /**
     * Detects if there are conflict markers in any files
     */
    public boolean hasConflictMarkers(RepositoryRuntimeState state) {
        return state.getWorkingDirectory().values().stream()
            .anyMatch(content -> content.contains("<<<<<<<") && content.contains("=======") && content.contains(">>>>>>>"));
    }

    /**
     * Resolves conflicts by replacing conflict markers with resolved content
     */
    public void resolveConflicts(RepositoryRuntimeState state, String filename, String resolvedContent) {
        if (gitStateManagementService.fileExists(state, filename)) {
            gitStateManagementService.addFileToWorkingDirectory(state, filename, resolvedContent);
            log.info("Resolved conflicts in file: {}", filename);
        }
    }

    /**
     * Lists all files that currently have conflict markers
     */
    public java.util.List<String> getConflictedFiles(RepositoryRuntimeState state) {
        return state.getWorkingDirectory().entrySet().stream()
            .filter(entry -> hasConflictMarkersInContent(entry.getValue()))
            .map(java.util.Map.Entry::getKey)
            .collect(java.util.stream.Collectors.toList());
    }

    /**
     * Checks if a specific file content has conflict markers
     */
    public boolean hasConflictMarkersInContent(String content) {
        return content != null && 
               content.contains("<<<<<<<") && 
               content.contains("=======") && 
               content.contains(">>>>>>>");
    }

    /**
     * Generates complex conflict scenarios with multiple files
     */
    public void generateComplexConflictScenario(GitRepository repository, String scenarioType) {
        log.info("Generating complex conflict scenario: {} for repository: {}", scenarioType, repository.getId());
        
        RepositoryRuntimeState state = gitStateManagementService.getRuntimeState(repository);
        
        switch (scenarioType.toLowerCase()) {
            case "multiple_files":
                generateMultipleFileConflicts(state);
                break;
            case "nested_directories":
                generateNestedDirectoryConflicts(state);
                break;
            case "binary_conflict":
                generateBinaryFileConflict(state);
                break;
            default:
                throw new IllegalArgumentException("Unknown complex conflict scenario: " + scenarioType);
        }
        
        gitStateManagementService.saveRuntimeState(repository, state);
        log.info("Created complex conflict scenario: {} in repository: {}", scenarioType, repository.getId());
    }

    // Private helper methods

    private void generateMultipleFileConflicts(RepositoryRuntimeState state) {
        // Create conflicts in multiple files
        String conflict1 = createMergeConflictContent("Main branch config", "Feature branch config", "feature/config-update");
        gitStateManagementService.addFileToWorkingDirectory(state, "config.yml", conflict1);
        
        String conflict2 = createMergeConflictContent("Original documentation", "Updated documentation", "feature/docs-update");
        gitStateManagementService.addFileToWorkingDirectory(state, "README.md", conflict2);
        
        String conflict3 = createMergeConflictContent("Version 1.0.0", "Version 1.1.0", "feature/version-bump");
        gitStateManagementService.addFileToWorkingDirectory(state, "package.json", conflict3);
    }

    private void generateNestedDirectoryConflicts(RepositoryRuntimeState state) {
        // Create conflicts in nested directory structures
        String utilConflict = createMergeConflictContent("Utils v1", "Utils v2", "feature/utils-refactor");
        gitStateManagementService.addFileToWorkingDirectory(state, "src/utils/helper.js", utilConflict);
        
        String componentConflict = createMergeConflictContent("Component A", "Component B", "feature/component-update");
        gitStateManagementService.addFileToWorkingDirectory(state, "src/components/Button.tsx", componentConflict);
        
        String testConflict = createMergeConflictContent("Test case 1", "Test case 2", "feature/test-improvement");
        gitStateManagementService.addFileToWorkingDirectory(state, "tests/unit/button.test.js", testConflict);
    }

    private void generateBinaryFileConflict(RepositoryRuntimeState state) {
        // Simulate binary file conflict (represented as text for our simulation)
        String binaryConflict = createMergeConflictContent(
            "Binary file content (current)", 
            "Binary file content (incoming)", 
            "feature/asset-update"
        );
        gitStateManagementService.addFileToWorkingDirectory(state, "assets/logo.png", binaryConflict);
    }
}
