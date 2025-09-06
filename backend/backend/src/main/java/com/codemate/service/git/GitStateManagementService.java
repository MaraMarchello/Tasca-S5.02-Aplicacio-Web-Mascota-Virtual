package com.codemate.service.git;

import com.codemate.model.GitRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * Service responsible for managing Git repository runtime state
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class GitStateManagementService {

    private final ObjectMapper objectMapper;

    /**
     * Gets the current runtime state of a repository
     */
    public RepositoryRuntimeState getRuntimeState(GitRepository repository) {
        try {
            if (repository.getCurrentState() == null || repository.getCurrentState().isEmpty()) {
                return new RepositoryRuntimeState();
            }
            return objectMapper.readValue(repository.getCurrentState(), RepositoryRuntimeState.class);
        } catch (Exception e) {
            log.warn("Failed to parse repository state for repo {}", repository.getId(), e);
            return new RepositoryRuntimeState();
        }
    }

    /**
     * Saves the runtime state to the repository and evicts cache
     */
    @CacheEvict(value = "repositoryState", key = "#repository.id")
    public void saveRuntimeState(GitRepository repository, RepositoryRuntimeState state) {
        try {
            repository.setCurrentState(objectMapper.writeValueAsString(state));
            log.debug("Repository runtime state saved and cache evicted for repository: {}", repository.getId());
        } catch (Exception e) {
            log.warn("Failed to save repository state for repo {}", repository.getId(), e);
            throw new RuntimeException("Failed to save repository state", e);
        }
    }

    /**
     * Creates a new empty runtime state
     */
    public RepositoryRuntimeState createEmptyState() {
        return new RepositoryRuntimeState();
    }

    /**
     * Checks if the repository has any uncommitted changes
     */
    public boolean hasUncommittedChanges(RepositoryRuntimeState state) {
        return !state.getWorkingDirectory().isEmpty() || !state.getStagingArea().isEmpty();
    }

    /**
     * Stages all files from working directory to staging area
     */
    public void stageAllFiles(RepositoryRuntimeState state) {
        state.getStagingArea().putAll(state.getWorkingDirectory());
        state.getWorkingDirectory().clear();
    }

    /**
     * Stages specific files from working directory to staging area
     */
    public void stageFiles(RepositoryRuntimeState state, java.util.List<String> files) {
        for (String file : files) {
            if (state.getWorkingDirectory().containsKey(file)) {
                state.getStagingArea().put(file, state.getWorkingDirectory().remove(file));
            }
        }
    }

    /**
     * Clears the staging area (used after commit)
     */
    public void clearStagingArea(RepositoryRuntimeState state) {
        state.getStagingArea().clear();
    }

    /**
     * Resets working directory and staging area (hard reset)
     */
    public void resetHard(RepositoryRuntimeState state) {
        state.getWorkingDirectory().clear();
        state.getStagingArea().clear();
    }

    /**
     * Resets only staging area (mixed reset)
     */
    public void resetMixed(RepositoryRuntimeState state) {
        state.getStagingArea().clear();
    }

    /**
     * Adds a file to the working directory
     */
    public void addFileToWorkingDirectory(RepositoryRuntimeState state, String filename, String content) {
        state.getWorkingDirectory().put(filename, content);
    }

    /**
     * Removes a file from both working directory and staging area
     */
    public boolean removeFile(RepositoryRuntimeState state, String filename) {
        boolean existedInWorking = state.getWorkingDirectory().containsKey(filename);
        boolean existedInStaging = state.getStagingArea().containsKey(filename);
        
        state.getWorkingDirectory().remove(filename);
        state.getStagingArea().remove(filename);
        
        return existedInWorking || existedInStaging;
    }

    /**
     * Gets file content from either working directory or staging area
     */
    public String getFileContent(RepositoryRuntimeState state, String filename) {
        String content = state.getWorkingDirectory().get(filename);
        if (content == null) {
            content = state.getStagingArea().get(filename);
        }
        return content;
    }

    /**
     * Checks if a file exists in either working directory or staging area
     */
    public boolean fileExists(RepositoryRuntimeState state, String filename) {
        return state.getWorkingDirectory().containsKey(filename) || 
               state.getStagingArea().containsKey(filename);
    }

    /**
     * Creates conflict markers in affected files
     */
    public void createMergeConflictMarkers(RepositoryRuntimeState state, String branchName) {
        for (Map.Entry<String, String> entry : state.getWorkingDirectory().entrySet()) {
            String filename = entry.getKey();
            String content = entry.getValue();
            
            String conflictContent = String.format(
                "<<<<<<< HEAD\n%s\n=======\n%s (from %s)\n>>>>>>> %s\n",
                content, content, branchName, branchName
            );
            
            state.getWorkingDirectory().put(filename, conflictContent);
        }
    }

    // Runtime file/index state stored in GitRepository.currentState as JSON
    @Data
    public static class RepositoryRuntimeState {
        private Map<String, String> workingDirectory = new HashMap<>();
        private Map<String, String> stagingArea = new HashMap<>();
    }
}
