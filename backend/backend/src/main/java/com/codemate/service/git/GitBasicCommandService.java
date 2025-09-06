package com.codemate.service.git;

import com.codemate.model.*;
import com.codemate.service.git.GitRepositoryManagementService.GitCommandResult;
import com.codemate.service.git.GitStateManagementService.RepositoryRuntimeState;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Service responsible for basic Git command simulations
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class GitBasicCommandService {

    private final GitRepositoryManagementService gitRepositoryManagementService;
    private final GitStateManagementService gitStateManagementService;

    /**
     * Simulates git status command
     */
    public GitCommandResult simulateStatusCommand(GitRepository repository) {
        RepositoryRuntimeState state = gitStateManagementService.getRuntimeState(repository);
        StringBuilder output = new StringBuilder();
        output.append("On branch ").append(repository.getCurrentBranch()).append("\n");

        // Get current commit information
        List<GitCommit> commits = gitRepositoryManagementService
            .findCommitsForBranch(repository, repository.getCurrentBranch());
        
        if (!commits.isEmpty()) {
            output.append("Your branch is up to date with 'origin/").append(repository.getCurrentBranch()).append("'.\n");
        } else {
            output.append("No commits yet\n");
        }

        boolean hasStaged = !state.getStagingArea().isEmpty();
        boolean hasUntracked = !state.getWorkingDirectory().isEmpty();
        boolean hasModified = false;

        // Check for modified files (files that exist in staging and working directory)
        Set<String> modifiedFiles = new HashSet<>();
        for (String file : state.getWorkingDirectory().keySet()) {
            if (state.getStagingArea().containsKey(file)) {
                String workingContent = state.getWorkingDirectory().get(file);
                String stagedContent = state.getStagingArea().get(file);
                if (!workingContent.equals(stagedContent)) {
                    modifiedFiles.add(file);
                    hasModified = true;
                }
            }
        }

        if (hasStaged) {
            output.append("\nChanges to be committed:\n");
            output.append("  (use \"git restore --staged <file>...\" to unstage)\n");
            for (String file : state.getStagingArea().keySet()) {
                String status = commits.isEmpty() ? "new file" : "modified";
                output.append("\t").append(status).append(":   ").append(file);
                
                // Show file size if available
                String content = state.getStagingArea().get(file);
                if (content != null && !content.isEmpty()) {
                    output.append(" (").append(content.length()).append(" bytes)");
                }
                output.append("\n");
            }
        }

        if (hasModified) {
            output.append("\nChanges not staged for commit:\n");
            output.append("  (use \"git add <file>...\" to update what will be committed)\n");
            output.append("  (use \"git restore <file>...\" to discard changes in working directory)\n");
            for (String file : modifiedFiles) {
                output.append("\tmodified:   ").append(file).append("\n");
            }
        }

        if (hasUntracked) {
            output.append("\nUntracked files:\n");
            output.append("  (use \"git add <file>...\" to include in what will be committed)\n");
            
            Set<String> untrackedFiles = new HashSet<>(state.getWorkingDirectory().keySet());
            untrackedFiles.removeAll(state.getStagingArea().keySet());
            
            for (String file : untrackedFiles.stream().sorted().collect(Collectors.toList())) {
                String content = state.getWorkingDirectory().get(file);
                output.append("\t").append(file);
                if (content != null && !content.isEmpty()) {
                    output.append(" (").append(content.length()).append(" bytes)");
                }
                output.append("\n");
            }
        }

        if (!hasStaged && !hasUntracked && !hasModified) {
            output.append("\nnothing to commit, working tree clean\n");
        } else if (hasStaged || hasModified) {
            output.append("\n");
        }

        return GitCommandResult.builder()
            .successful(true)
            .exitCode(0)
            .output(output.toString())
            .errorOutput("")
            .build();
    }

    /**
     * Simulates git add command
     */
    public GitCommandResult simulateAddCommand(GitRepository repository, List<String> args) {
        if (args.isEmpty()) {
            return GitCommandResult.builder()
                .successful(false)
                .exitCode(1)
                .output("")
                .errorOutput("Nothing specified, nothing added.\n")
                .build();
        }
        
        RepositoryRuntimeState state = gitStateManagementService.getRuntimeState(repository);
        boolean stageAll = ".".equals(args.get(0)) || "-A".equals(args.get(0)) || "--all".equals(args.get(0));
        
        if (stageAll) {
            gitStateManagementService.stageAllFiles(state);
        } else {
            gitStateManagementService.stageFiles(state, args);
        }
        
        gitStateManagementService.saveRuntimeState(repository, state);

        return GitCommandResult.builder()
            .successful(true)
            .exitCode(0)
            .output("")
            .errorOutput("")
            .build();
    }

    /**
     * Simulates git commit command
     */
    public GitCommandResult simulateCommitCommand(GitRepository repository, List<String> args, Long userId) {
        String message = extractCommitMessage(args);
        if (message.isEmpty()) {
            return GitCommandResult.builder()
                .successful(false)
                .exitCode(1)
                .output("")
                .errorOutput("Aborting commit due to empty commit message.\n")
                .build();
        }
        
        RepositoryRuntimeState state = gitStateManagementService.getRuntimeState(repository);
        if (state.getStagingArea().isEmpty()) {
            return GitCommandResult.builder()
                .successful(false)
                .exitCode(1)
                .output("")
                .errorOutput("nothing to commit, working tree clean\n")
                .build();
        }

        // Create new commit
        GitCommit commit = gitRepositoryManagementService.createCommit(
            repository, message, "User", "user@codemate.com", 
            new ArrayList<>(state.getStagingArea().keySet())
        );

        // Clear staging area after commit
        gitStateManagementService.clearStagingArea(state);
        gitStateManagementService.saveRuntimeState(repository, state);

        String output = String.format("[%s %s] %s\n", 
            repository.getCurrentBranch(), 
            commit.getHash().substring(0, 7), 
            message);

        return GitCommandResult.builder()
            .successful(true)
            .exitCode(0)
            .output(output)
            .errorOutput("")
            .commitHash(commit.getHash())
            .build();
    }

    /**
     * Simulates git branch command
     */
    public GitCommandResult simulateBranchCommand(GitRepository repository, List<String> args) {
        if (args.isEmpty()) {
            // List branches
            List<GitBranch> branches = gitRepositoryManagementService.findAllBranches(repository);
            StringBuilder output = new StringBuilder();
            for (GitBranch branch : branches) {
                if (branch.getName().equals(repository.getCurrentBranch())) {
                    output.append("* ").append(branch.getName()).append("\n");
                } else {
                    output.append("  ").append(branch.getName()).append("\n");
                }
            }

            return GitCommandResult.builder()
                .successful(true)
                .exitCode(0)
                .output(output.toString())
                .errorOutput("")
                .build();
        }

        // Create new branch
        String branchName = args.get(0);
        gitRepositoryManagementService.createBranch(repository, branchName, repository.getCurrentBranch());

        return GitCommandResult.builder()
            .successful(true)
            .exitCode(0)
            .output("")
            .errorOutput("")
            .build();
    }

    /**
     * Simulates git checkout command
     */
    public GitCommandResult simulateCheckoutCommand(GitRepository repository, List<String> args) {
        if (args.isEmpty()) {
            return GitCommandResult.builder()
                .successful(false)
                .exitCode(1)
                .output("")
                .errorOutput("error: pathspec '' did not match any file(s) known to git\n")
                .build();
        }

        String branchName = args.get(0);
        
        // Handle checkout -b (create and switch to new branch)
        if (args.size() > 1 && "-b".equals(args.get(0))) {
            branchName = args.get(1);
            gitRepositoryManagementService.createBranch(repository, branchName, repository.getCurrentBranch());
        }
        
        Optional<GitBranch> branch = gitRepositoryManagementService.findBranch(repository, branchName);
        
        if (branch.isEmpty()) {
            return GitCommandResult.builder()
                .successful(false)
                .exitCode(1)
                .output("")
                .errorOutput(String.format("error: pathspec '%s' did not match any file(s) known to git\n", branchName))
                .build();
        }

        // Update current branch
        gitRepositoryManagementService.switchBranch(repository, branchName);

        String output = String.format("Switched to branch '%s'\n", branchName);

        return GitCommandResult.builder()
            .successful(true)
            .exitCode(0)
            .output(output)
            .errorOutput("")
            .build();
    }

    /**
     * Simulates git log command
     */
    public GitCommandResult simulateLogCommand(GitRepository repository, List<String> args) {
        List<GitCommit> commits = gitRepositoryManagementService
            .findCommitsForBranch(repository, repository.getCurrentBranch());

        StringBuilder output = new StringBuilder();
        boolean oneline = args.stream().anyMatch(a -> a.equals("--oneline"));
        
        for (GitCommit commit : commits) {
            if (oneline) {
                output.append(commit.getHash().substring(0, 7)).append(" ").append(commit.getMessage()).append("\n");
            } else {
                output.append(String.format("commit %s\n", commit.getHash()));
                output.append(String.format("Author: %s <%s>\n", commit.getAuthor(), commit.getEmail()));
                output.append(String.format("Date: %s\n\n", commit.getCommitTime()));
                output.append(String.format("    %s\n\n", commit.getMessage()));
            }
        }

        return GitCommandResult.builder()
            .successful(true)
            .exitCode(0)
            .output(output.toString())
            .errorOutput("")
            .build();
    }

    /**
     * Simulates git diff command
     */
    public GitCommandResult simulateDiffCommand(GitRepository repository, List<String> args) {
        // Simplified diff output for demonstration
        String output = "diff --git a/file.txt b/file.txt\nindex abc123..def456 100644\n--- a/file.txt\n+++ b/file.txt\n@@ -1,3 +1,3 @@\n line1\n-line2\n+modified line2\n line3\n";

        return GitCommandResult.builder()
            .successful(true)
            .exitCode(0)
            .output(output)
            .errorOutput("")
            .build();
    }

    // Private helper methods

    private String extractCommitMessage(List<String> args) {
        for (int i = 0; i < args.size(); i++) {
            if (args.get(i).equals("-m") && i + 1 < args.size()) {
                return args.get(i + 1);
            }
        }
        return "";
    }
}
