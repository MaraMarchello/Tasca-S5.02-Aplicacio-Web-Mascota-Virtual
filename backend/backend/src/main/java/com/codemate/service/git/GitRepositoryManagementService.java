package com.codemate.service.git;

import com.codemate.model.*;
import com.codemate.repository.*;
import com.codemate.service.git.GitStateManagementService.RepositoryRuntimeState;
import com.codemate.util.PerformanceMonitor;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Service responsible for Git repository management and state building
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class GitRepositoryManagementService {

    private final GitRepositoryRepository gitRepositoryRepository;
    private final GitCommitRepository gitCommitRepository;
    private final GitBranchRepository gitBranchRepository;
    private final GitCommandRepository gitCommandRepository;
    private final GitScenarioRepository gitScenarioRepository;
    private final GitStateManagementService gitStateManagementService;
    private final ObjectMapper objectMapper;
    private final PerformanceMonitor performanceMonitor;

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
     * Gets the current state of a virtual repository with caching
     */
    @Cacheable(value = "repositoryState", key = "#repositoryId")
    public GitRepositoryState getRepositoryState(Long repositoryId) {
        performanceMonitor.startOperation("repository_state_fetch", null, repositoryId, null);
        
        log.debug("Getting repository state for repository: {} (cache miss)", repositoryId);
        
        try {
            GitRepository repository = gitRepositoryRepository.findById(repositoryId)
                .orElseThrow(() -> new RuntimeException("Repository not found: " + repositoryId));

            GitRepositoryState state = buildRepositoryState(repository);
            
            long duration = performanceMonitor.endOperation("repository_state_fetch", null, repositoryId);
            log.debug("Repository state fetched in {}ms for repository: {}", duration, repositoryId);
            
            return state;
        } catch (Exception e) {
            performanceMonitor.endOperation("repository_state_fetch", null, repositoryId);
            log.error("Error fetching repository state for repository: {}", repositoryId, e);
            throw e;
        }
    }

    /**
     * Updates repository state and evicts cache
     */
    @CacheEvict(value = "repositoryState", key = "#repository.id")
    public void updateRepositoryState(GitRepository repository) {
        repository.setUpdatedAt(LocalDateTime.now());
        gitRepositoryRepository.save(repository);
        log.debug("Repository state cache evicted for repository: {}", repository.getId());
    }

    /**
     * Saves a command execution record
     */
    public void saveCommandExecution(GitRepository repository, String command, 
                                   GitCommandResult result, Long userId) {
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

    /**
     * Gets the current commit hash for a repository
     */
    public String getCurrentCommitHash(GitRepository repository) {
        List<GitCommit> commits = gitCommitRepository
            .findByRepositoryAndBranchNameOrderByCommitTimeDesc(repository, repository.getCurrentBranch());
        
        return commits.isEmpty() ? generateCommitHash("initial", "system") : commits.get(0).getHash();
    }

    /**
     * Gets the parent commit hashes for the latest commit
     */
    public List<String> getLastCommitHashes(GitRepository repository) {
        List<GitCommit> lastCommits = gitCommitRepository
            .findByRepositoryAndBranchNameOrderByCommitTimeDesc(repository, repository.getCurrentBranch());
        
        if (!lastCommits.isEmpty()) {
            return Arrays.asList(lastCommits.get(0).getHash());
        }
        return new ArrayList<>();
    }

    /**
     * Creates a new commit in the repository
     */
    public GitCommit createCommit(GitRepository repository, String message, String author, 
                                String email, List<String> modifiedFiles) {
        String commitHash = generateCommitHash(message, email);
        GitCommit commit = GitCommit.builder()
            .hash(commitHash)
            .message(message)
            .author(author)
            .email(email)
            .branchName(repository.getCurrentBranch())
            .repository(repository)
            .parentHashes(getLastCommitHashes(repository))
            .modifiedFiles(new ArrayList<>(modifiedFiles))
            .build();

        return gitCommitRepository.save(commit);
    }

    /**
     * Creates a merge commit
     */
    public GitCommit createMergeCommit(GitRepository repository, String branchToMerge, 
                                     String targetBranchCommitHash) {
        String mergeCommitHash = generateCommitHash(
            String.format("Merge branch '%s' into %s", branchToMerge, repository.getCurrentBranch()), 
            "user@codemate.com"
        );
        
        List<String> parentHashes = Arrays.asList(
            getCurrentCommitHash(repository),
            targetBranchCommitHash
        );

        GitCommit mergeCommit = GitCommit.builder()
            .hash(mergeCommitHash)
            .message(String.format("Merge branch '%s'", branchToMerge))
            .author("User")
            .email("user@codemate.com")
            .branchName(repository.getCurrentBranch())
            .repository(repository)
            .parentHashes(parentHashes)
            .modifiedFiles(new ArrayList<>())
            .build();

        return gitCommitRepository.save(mergeCommit);
    }

    /**
     * Creates a new branch
     */
    public GitBranch createBranch(GitRepository repository, String branchName, String parentBranch) {
        String currentCommitHash = getCurrentCommitHash(repository);
        
        GitBranch newBranch = GitBranch.builder()
            .name(branchName)
            .headCommitHash(currentCommitHash)
            .isActive(false)
            .parentBranch(parentBranch)
            .repository(repository)
            .build();

        return gitBranchRepository.save(newBranch);
    }

    /**
     * Switches to a different branch
     */
    public void switchBranch(GitRepository repository, String branchName) {
        repository.setCurrentBranch(branchName);
        gitRepositoryRepository.save(repository);
    }

    /**
     * Updates branch head commit
     */
    public void updateBranchHead(GitRepository repository, String branchName, String commitHash) {
        Optional<GitBranch> branch = gitBranchRepository.findByRepositoryAndName(repository, branchName);
        if (branch.isPresent()) {
            GitBranch b = branch.get();
            b.setHeadCommitHash(commitHash);
            gitBranchRepository.save(b);
        }
    }

    /**
     * Finds a branch by name
     */
    public Optional<GitBranch> findBranch(GitRepository repository, String branchName) {
        return gitBranchRepository.findByRepositoryAndName(repository, branchName);
    }

    /**
     * Finds all branches for a repository
     */
    public List<GitBranch> findAllBranches(GitRepository repository) {
        return gitBranchRepository.findByRepositoryOrderByName(repository);
    }

    /**
     * Finds commits for a specific branch
     */
    public List<GitCommit> findCommitsForBranch(GitRepository repository, String branchName) {
        return gitCommitRepository.findByRepositoryAndBranchNameOrderByCommitTimeDesc(repository, branchName);
    }

    /**
     * Finds all commits for a repository
     */
    public List<GitCommit> findAllCommits(GitRepository repository) {
        return gitCommitRepository.findByRepositoryOrderByCommitTimeDesc(repository);
    }

    /**
     * Finds a commit by hash (supports partial hash)
     */
    public Optional<GitCommit> findCommitByHash(GitRepository repository, String commitHash) {
        return gitCommitRepository.findByRepositoryAndHashStartingWith(repository, commitHash);
    }

    // Private helper methods

    private void initializeRepositoryFromScenario(GitRepository repository, GitScenario scenario) {
        try {
            if (scenario.getInitialState() != null) {
                // Parse to validate JSON; detailed state usage will be added as simulator expands
                JsonNode node = objectMapper.readTree(scenario.getInitialState());
                RepositoryRuntimeState runtimeState = new RepositoryRuntimeState();
                if (node.has("workingDirectory") && node.get("workingDirectory").isObject()) {
                    node.get("workingDirectory").fields().forEachRemaining(e -> 
                        runtimeState.getWorkingDirectory().put(e.getKey(), e.getValue().asText()));
                }
                if (node.has("stagingArea") && node.get("stagingArea").isObject()) {
                    node.get("stagingArea").fields().forEachRemaining(e -> 
                        runtimeState.getStagingArea().put(e.getKey(), e.getValue().asText()));
                }
                repository.setCurrentState(objectMapper.writeValueAsString(runtimeState));
                gitRepositoryRepository.save(repository);
                
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

    private GitRepositoryState buildRepositoryState(GitRepository repository) {
        // Optimize by limiting commit history and using efficient queries
        List<GitCommit> allCommits = gitCommitRepository.findByRepositoryOrderByCommitTimeDesc(repository);
        List<GitCommit> commits = allCommits.size() > 20 ? allCommits.subList(0, 20) : allCommits;
        List<GitBranch> branches = gitBranchRepository.findByRepositoryOrderByName(repository);

        String currentBranch = repository.getCurrentBranch();
        String currentCommitHash = getCurrentCommitHash(repository);

        // Minimal remotes model for now: origin/main points to current commit
        Map<String, Map<String, String>> remotes = new HashMap<>();
        Map<String, String> originRefs = new HashMap<>();
        originRefs.put("refs/heads/" + currentBranch, currentCommitHash);
        remotes.put("origin", originRefs);

        RepositoryRuntimeState runtimeState = gitStateManagementService.getRuntimeState(repository);

        log.debug("Built repository state for repo {}: {} commits, {} branches", 
                 repository.getId(), commits.size(), branches.size());

        return GitRepositoryState.builder()
            .repositoryId(repository.getId())
            .currentBranch(currentBranch)
            .commits(commits)
            .branches(branches)
            .headRef("refs/heads/" + currentBranch)
            .detachedHead(false)
            .stagingArea(runtimeState.getStagingArea())
            .workingDirectory(runtimeState.getWorkingDirectory())
            .remotes(remotes)
            .build();
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

    // Data transfer objects

    @Builder
    @Data
    public static class GitCommandResult {
        private boolean successful;
        private int exitCode;
        private String output;
        private String errorOutput;
        private String commitHash;
    }

    @Builder
    @Data
    public static class GitRepositoryState {
        private Long repositoryId;
        private String currentBranch;
        private List<GitCommit> commits;
        private List<GitBranch> branches;
        private String headRef;
        private boolean detachedHead;
        private Map<String, String> stagingArea;
        private Map<String, String> workingDirectory;
        private Map<String, Map<String, String>> remotes;
    }
}
