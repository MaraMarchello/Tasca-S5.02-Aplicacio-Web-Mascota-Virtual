package com.codemate.service.git;

import com.codemate.model.*;
import com.codemate.service.git.GitRepositoryManagementService.GitCommandResult;
import com.codemate.service.git.GitStateManagementService.RepositoryRuntimeState;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * Service responsible for advanced Git command simulations
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class GitAdvancedCommandService {

    private final GitRepositoryManagementService gitRepositoryManagementService;
    private final GitStateManagementService gitStateManagementService;

    /**
     * Simulates git merge command
     */
    public GitCommandResult simulateMergeCommand(GitRepository repository, List<String> args) {
        if (args.isEmpty()) {
            return GitCommandResult.builder()
                .successful(false)
                .exitCode(1)
                .output("")
                .errorOutput("fatal: No commit specified and merge.defaultToUpstream not set.\n")
                .build();
        }

        String branchToMerge = args.get(0);
        
        // Check if target branch exists
        Optional<GitBranch> targetBranch = gitRepositoryManagementService.findBranch(repository, branchToMerge);
        if (targetBranch.isEmpty()) {
            return GitCommandResult.builder()
                .successful(false)
                .exitCode(1)
                .output("")
                .errorOutput(String.format("fatal: '%s' - not something we can merge\n", branchToMerge))
                .build();
        }
        
        String currentBranch = repository.getCurrentBranch();
        if (currentBranch.equals(branchToMerge)) {
            return GitCommandResult.builder()
                .successful(false)
                .exitCode(1)
                .output("")
                .errorOutput("fatal: Cannot merge a branch into itself.\n")
                .build();
        }

        // Get commits from both branches
        List<GitCommit> currentBranchCommits = gitRepositoryManagementService
            .findCommitsForBranch(repository, currentBranch);
        List<GitCommit> targetBranchCommits = gitRepositoryManagementService
            .findCommitsForBranch(repository, branchToMerge);

        // Check for fast-forward possibility
        boolean canFastForward = checkFastForwardPossible(currentBranchCommits, targetBranchCommits);
        
        if (canFastForward && !args.contains("--no-ff")) {
            // Fast-forward merge
            return performFastForwardMerge(repository, branchToMerge, targetBranch.get());
        } else {
            // Three-way merge
            return performThreeWayMerge(repository, branchToMerge, currentBranch, targetBranch.get());
        }
    }

    /**
     * Simulates git rebase command
     */
    public GitCommandResult simulateRebaseCommand(GitRepository repository, List<String> args) {
        if (args.isEmpty()) {
            return GitCommandResult.builder()
                .successful(false)
                .exitCode(1)
                .output("")
                .errorOutput("fatal: You must specify a branch to rebase onto.\n")
                .build();
        }

        String targetBranch = args.get(0);
        String currentBranch = repository.getCurrentBranch();
        
        // Check if target branch exists
        Optional<GitBranch> target = gitRepositoryManagementService.findBranch(repository, targetBranch);
        if (target.isEmpty()) {
            return GitCommandResult.builder()
                .successful(false)
                .exitCode(1)
                .output("")
                .errorOutput(String.format("fatal: invalid upstream '%s'\n", targetBranch))
                .build();
        }

        // Simulate rebase process
        List<GitCommit> currentCommits = gitRepositoryManagementService
            .findCommitsForBranch(repository, currentBranch);
        
        // Check for conflicts (simplified)
        RepositoryRuntimeState state = gitStateManagementService.getRuntimeState(repository);
        boolean hasUncommittedChanges = gitStateManagementService.hasUncommittedChanges(state);
        
        if (hasUncommittedChanges) {
            return GitCommandResult.builder()
                .successful(false)
                .exitCode(1)
                .output("")
                .errorOutput("error: cannot rebase: You have unstaged changes.\n" +
                           "error: Please commit or stash them.\n")
                .build();
        }

        // Interactive rebase simulation
        if (args.contains("-i") || args.contains("--interactive")) {
            StringBuilder output = new StringBuilder();
            output.append("Successfully rebased and updated refs/heads/").append(currentBranch).append(".\n");
            output.append("Interactive rebase completed. ").append(currentCommits.size()).append(" commits processed.\n");
            
            return GitCommandResult.builder()
                .successful(true)
                .exitCode(0)
                .output(output.toString())
                .errorOutput("")
                .build();
        }

        // Regular rebase
        String output = String.format("Successfully rebased and updated refs/heads/%s.\n", currentBranch);
        return GitCommandResult.builder()
            .successful(true)
            .exitCode(0)
            .output(output)
            .errorOutput("")
            .build();
    }

    /**
     * Simulates git cherry-pick command
     */
    public GitCommandResult simulateCherryPickCommand(GitRepository repository, List<String> args) {
        if (args.isEmpty()) {
            return GitCommandResult.builder()
                .successful(false)
                .exitCode(1)
                .output("")
                .errorOutput("fatal: bad revision 'HEAD'\n")
                .build();
        }

        String commitHash = args.get(0);
        
        // Validate commit hash format (simplified)
        if (commitHash.length() < 7) {
            return GitCommandResult.builder()
                .successful(false)
                .exitCode(1)
                .output("")
                .errorOutput(String.format("fatal: bad revision '%s'\n", commitHash))
                .build();
        }

        // Find the commit to cherry-pick
        Optional<GitCommit> commitToCherry = gitRepositoryManagementService.findCommitByHash(repository, commitHash);
        
        if (commitToCherry.isEmpty()) {
            return GitCommandResult.builder()
                .successful(false)
                .exitCode(1)
                .output("")
                .errorOutput(String.format("fatal: bad revision '%s'\n", commitHash))
                .build();
        }

        GitCommit commit = commitToCherry.get();
        
        // Check for conflicts (simplified)
        RepositoryRuntimeState state = gitStateManagementService.getRuntimeState(repository);
        boolean hasConflicts = !state.getWorkingDirectory().isEmpty() && 
                              state.getWorkingDirectory().containsKey("conflicted-file.txt");

        if (hasConflicts) {
            // Create conflict markers
            String conflictContent = String.format(
                "<<<<<<< HEAD\nCurrent changes\n=======\n%s\n>>>>>>> %s\n",
                commit.getMessage(), commitHash.substring(0, 7)
            );
            gitStateManagementService.addFileToWorkingDirectory(state, "conflicted-file.txt", conflictContent);
            gitStateManagementService.saveRuntimeState(repository, state);
            
            return GitCommandResult.builder()
                .successful(false)
                .exitCode(1)
                .output("")
                .errorOutput("error: could not apply " + commitHash.substring(0, 7) + "... " + commit.getMessage() + "\n" +
                           "hint: after resolving the conflicts, mark the corrected paths\n" +
                           "hint: with 'git add <paths>' and run 'git cherry-pick --continue'\n")
                .build();
        }

        // Create new commit for cherry-pick
        GitCommit newCommit = gitRepositoryManagementService.createCommit(
            repository, commit.getMessage(), commit.getAuthor(), 
            commit.getEmail(), commit.getModifiedFiles()
        );

        String output = String.format("[%s %s] %s\n", 
                                    repository.getCurrentBranch(), 
                                    newCommit.getHash().substring(0, 7), 
                                    commit.getMessage());
        
        return GitCommandResult.builder()
            .successful(true)
            .exitCode(0)
            .output(output)
            .errorOutput("")
            .build();
    }

    /**
     * Simulates git stash command
     */
    public GitCommandResult simulateStashCommand(GitRepository repository, List<String> args) {
        RepositoryRuntimeState state = gitStateManagementService.getRuntimeState(repository);
        
        String operation = args.isEmpty() ? "push" : args.get(0);
        
        switch (operation.toLowerCase()) {
            case "push":
            case "save":
                return handleStashPush(repository, state, args);
            case "pop":
                return handleStashPop(repository, state);
            case "list":
                return handleStashList(repository);
            case "show":
                return handleStashShow(repository, args);
            case "drop":
                return handleStashDrop(repository, args);
            case "clear":
                return handleStashClear(repository);
            default:
                return GitCommandResult.builder()
                    .successful(false)
                    .exitCode(1)
                    .output("")
                    .errorOutput("error: unknown subcommand: " + operation + "\n")
                    .build();
        }
    }

    /**
     * Simulates git reset command
     */
    public GitCommandResult simulateResetCommand(GitRepository repository, List<String> args) {
        String mode = "mixed"; // default
        String target = "HEAD";
        
        // Parse arguments
        for (String arg : args) {
            switch (arg) {
                case "--soft":
                    mode = "soft";
                    break;
                case "--mixed":
                    mode = "mixed";
                    break;
                case "--hard":
                    mode = "hard";
                    break;
                default:
                    if (!arg.startsWith("-")) {
                        target = arg;
                        log.debug("Reset target set to: {}", target);
                    }
                    break;
            }
        }

        RepositoryRuntimeState state = gitStateManagementService.getRuntimeState(repository);
        
        switch (mode) {
            case "soft":
                // Only move HEAD, keep staging and working directory
                return GitCommandResult.builder()
                    .successful(true)
                    .exitCode(0)
                    .output("")
                    .errorOutput("")
                    .build();
                    
            case "mixed":
                // Move HEAD and reset staging area, keep working directory
                gitStateManagementService.resetMixed(state);
                gitStateManagementService.saveRuntimeState(repository, state);
                
                return GitCommandResult.builder()
                    .successful(true)
                    .exitCode(0)
                    .output("")
                    .errorOutput("")
                    .build();
                    
            case "hard":
                // Reset everything
                gitStateManagementService.resetHard(state);
                gitStateManagementService.saveRuntimeState(repository, state);
                
                return GitCommandResult.builder()
                    .successful(true)
                    .exitCode(0)
                    .output("HEAD is now at " + gitRepositoryManagementService.getCurrentCommitHash(repository).substring(0, 7) + "\n")
                    .errorOutput("")
                    .build();
                    
            default:
                return GitCommandResult.builder()
                    .successful(false)
                    .exitCode(1)
                    .output("")
                    .errorOutput("error: unknown reset mode: " + mode + "\n")
                    .build();
        }
    }

    // Private helper methods for merge operations

    private boolean checkFastForwardPossible(List<GitCommit> currentBranchCommits, List<GitCommit> targetBranchCommits) {
        if (currentBranchCommits.isEmpty()) {
            return true; // Can fast-forward when current branch has no commits
        }
        
        if (targetBranchCommits.isEmpty()) {
            return false; // Nothing to merge
        }
        
        // Check if current branch's latest commit is an ancestor of target branch's latest commit
        String currentLatestHash = currentBranchCommits.get(0).getHash();
        return targetBranchCommits.stream()
            .anyMatch(commit -> commit.getParentHashes().contains(currentLatestHash));
    }

    private GitCommandResult performFastForwardMerge(GitRepository repository, String branchToMerge, GitBranch targetBranch) {
        // Update current branch's head to point to target branch's head
        gitRepositoryManagementService.updateBranchHead(repository, repository.getCurrentBranch(), targetBranch.getHeadCommitHash());

        String output = String.format("Updating %s..%s\nFast-forward\n", 
            gitRepositoryManagementService.getCurrentCommitHash(repository).substring(0, 7),
            targetBranch.getHeadCommitHash().substring(0, 7));

        return GitCommandResult.builder()
            .successful(true)
            .exitCode(0)
            .output(output)
            .errorOutput("")
            .build();
    }

    private GitCommandResult performThreeWayMerge(GitRepository repository, String branchToMerge, 
                                                String currentBranch, GitBranch targetBranch) {
        // Check for potential conflicts by comparing file states
        RepositoryRuntimeState currentState = gitStateManagementService.getRuntimeState(repository);
        boolean hasConflicts = detectMergeConflicts(repository, currentBranch, branchToMerge);
        
        if (hasConflicts) {
            // Create conflict markers in affected files
            gitStateManagementService.createMergeConflictMarkers(currentState, branchToMerge);
            gitStateManagementService.saveRuntimeState(repository, currentState);
            
            String output = "Auto-merging failed for some files. Fix conflicts and then commit the result.\n";
            return GitCommandResult.builder()
                .successful(false)
                .exitCode(1)
                .output(output)
                .errorOutput("CONFLICT (content): Merge conflict detected. Resolve conflicts and commit.\n")
                .build();
        } else {
            // Create merge commit
            gitRepositoryManagementService.createMergeCommit(
                repository, branchToMerge, targetBranch.getHeadCommitHash()
            );

            String output = "Merge made by the 'recursive' strategy.\n";
            return GitCommandResult.builder()
                .successful(true)
                .exitCode(0)
                .output(output)
                .errorOutput("")
                .build();
        }
    }

    private boolean detectMergeConflicts(GitRepository repository, String currentBranch, String targetBranch) {
        // Simple conflict detection: if both branches have modified the same files differently
        RepositoryRuntimeState state = gitStateManagementService.getRuntimeState(repository);
        
        // For simplicity, we'll create a conflict if there are files in working directory
        // In a real implementation, this would compare file contents between branches
        return !state.getWorkingDirectory().isEmpty() && 
               state.getWorkingDirectory().containsKey("README.md"); // Example conflict scenario
    }

    // Private helper methods for stash operations

    private GitCommandResult handleStashPush(GitRepository repository, RepositoryRuntimeState state, List<String> args) {
        boolean hasChanges = gitStateManagementService.hasUncommittedChanges(state);
        
        if (!hasChanges) {
            return GitCommandResult.builder()
                .successful(false)
                .exitCode(1)
                .output("")
                .errorOutput("No local changes to save\n")
                .build();
        }

        // Create stash entry (simplified - store in repository metadata)
        String stashMessage = args.size() > 1 && args.get(1).equals("-m") && args.size() > 2 
            ? args.get(2) 
            : "WIP on " + repository.getCurrentBranch() + ": " + gitRepositoryManagementService.getCurrentCommitHash(repository).substring(0, 7);
        
        // Clear working directory and staging area
        gitStateManagementService.resetHard(state);
        gitStateManagementService.saveRuntimeState(repository, state);

        String output = String.format("Saved working directory and index state %s\n", stashMessage);
        
        return GitCommandResult.builder()
            .successful(true)
            .exitCode(0)
            .output(output)
            .errorOutput("")
            .build();
    }

    private GitCommandResult handleStashPop(GitRepository repository, RepositoryRuntimeState state) {
        // Simulate restoring stashed changes
        gitStateManagementService.addFileToWorkingDirectory(state, "stashed-file.txt", "Restored from stash");
        gitStateManagementService.saveRuntimeState(repository, state);

        return GitCommandResult.builder()
            .successful(true)
            .exitCode(0)
            .output("On branch " + repository.getCurrentBranch() + "\n" +
                   "Changes not staged for commit:\n" +
                   "\tmodified:   stashed-file.txt\n" +
                   "\nDropped refs/stash@{0}\n")
            .errorOutput("")
            .build();
    }

    private GitCommandResult handleStashList(GitRepository repository) {
        // Simplified stash list
        return GitCommandResult.builder()
            .successful(true)
            .exitCode(0)
            .output("stash@{0}: WIP on " + repository.getCurrentBranch() + ": " + 
                   gitRepositoryManagementService.getCurrentCommitHash(repository).substring(0, 7) + " Latest changes\n")
            .errorOutput("")
            .build();
    }

    private GitCommandResult handleStashShow(GitRepository repository, List<String> args) {
        String stashRef = args.size() > 1 ? args.get(1) : "stash@{0}";
        
        return GitCommandResult.builder()
            .successful(true)
            .exitCode(0)
            .output("diff --git a/stashed-file.txt b/stashed-file.txt\n" +
                   "index 1234567..abcdefg 100644\n" +
                   "--- a/stashed-file.txt\n" +
                   "+++ b/stashed-file.txt\n" +
                   "@@ -0,0 +1 @@\n" +
                   "+Stashed content for " + stashRef + "\n")
            .errorOutput("")
            .build();
    }

    private GitCommandResult handleStashDrop(GitRepository repository, List<String> args) {
        String stashRef = args.size() > 1 ? args.get(1) : "stash@{0}";
        
        return GitCommandResult.builder()
            .successful(true)
            .exitCode(0)
            .output("Dropped " + stashRef + "\n")
            .errorOutput("")
            .build();
    }

    private GitCommandResult handleStashClear(GitRepository repository) {
        return GitCommandResult.builder()
            .successful(true)
            .exitCode(0)
            .output("")
            .errorOutput("")
            .build();
    }
}
