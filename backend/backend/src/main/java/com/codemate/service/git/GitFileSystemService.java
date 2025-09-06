package com.codemate.service.git;

import com.codemate.model.GitRepository;
import com.codemate.service.git.GitRepositoryManagementService.GitCommandResult;
import com.codemate.service.git.GitStateManagementService.RepositoryRuntimeState;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Service responsible for custom file system operations within Git simulation
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class GitFileSystemService {

    private final GitStateManagementService gitStateManagementService;

    /**
     * Simulates custom fs command for file operations
     */
    public GitCommandResult simulateFsCommand(GitRepository repository, List<String> args) {
        if (args.isEmpty()) {
            return GitCommandResult.builder()
                .successful(false)
                .exitCode(1)
                .output("")
                .errorOutput("fs command requires an operation (create|edit|rm|ls|cat|mkdir)\n")
                .build();
        }
        
        String op = args.get(0).toLowerCase();
        RepositoryRuntimeState state = gitStateManagementService.getRuntimeState(repository);
        
        switch (op) {
            case "create":
                return handleFsCreate(args, state, repository);
            case "edit":
                return handleFsEdit(args, state, repository);
            case "rm":
                return handleFsRemove(args, state, repository);
            case "ls":
                return handleFsList(args, state);
            case "cat":
                return handleFsCat(args, state);
            case "mkdir":
                return handleFsMkdir(args, state, repository);
            default:
                return GitCommandResult.builder()
                    .successful(false)
                    .exitCode(1)
                    .output("")
                    .errorOutput("unknown fs operation: " + op + ". Available: create, edit, rm, ls, cat, mkdir\n")
                    .build();
        }
    }

    // Private helper methods for file operations

    private GitCommandResult handleFsCreate(List<String> args, RepositoryRuntimeState state, GitRepository repository) {
        if (args.size() < 2) {
            return GitCommandResult.builder()
                .successful(false)
                .exitCode(1)
                .output("")
                .errorOutput("usage: git fs create <file> [content]\n")
                .build();
        }
        
        String file = args.get(1);
        
        // Validate file path
        if (!isValidFilePath(file)) {
            return GitCommandResult.builder()
                .successful(false)
                .exitCode(1)
                .output("")
                .errorOutput("invalid file path: " + file + "\n")
                .build();
        }
        
        // Check if file already exists
        if (gitStateManagementService.fileExists(state, file)) {
            return GitCommandResult.builder()
                .successful(false)
                .exitCode(1)
                .output("")
                .errorOutput("file already exists: " + file + "\n")
                .build();
        }
        
        String content = args.size() > 2 ? String.join(" ", args.subList(2, args.size())) : "";
        
        // Create directory structure if needed
        createDirectoryStructure(file, state);
        
        gitStateManagementService.addFileToWorkingDirectory(state, file, content);
        gitStateManagementService.saveRuntimeState(repository, state);
        
        return GitCommandResult.builder()
            .successful(true)
            .exitCode(0)
            .output("created " + file + " (" + content.length() + " bytes)\n")
            .errorOutput("")
            .build();
    }

    private GitCommandResult handleFsEdit(List<String> args, RepositoryRuntimeState state, GitRepository repository) {
        if (args.size() < 2) {
            return GitCommandResult.builder()
                .successful(false)
                .exitCode(1)
                .output("")
                .errorOutput("usage: git fs edit <file> [content]\n")
                .build();
        }
        
        String file = args.get(1);
        
        // Check if file exists
        if (!gitStateManagementService.fileExists(state, file)) {
            return GitCommandResult.builder()
                .successful(false)
                .exitCode(1)
                .output("")
                .errorOutput("file not found: " + file + "\n")
                .build();
        }
        
        String oldContent = gitStateManagementService.getFileContent(state, file);
        String newContent = args.size() > 2 ? String.join(" ", args.subList(2, args.size())) : "";
        
        gitStateManagementService.addFileToWorkingDirectory(state, file, newContent);
        gitStateManagementService.saveRuntimeState(repository, state);
        
        int oldLength = oldContent != null ? oldContent.length() : 0;
        return GitCommandResult.builder()
            .successful(true)
            .exitCode(0)
            .output("edited " + file + " (" + oldLength + " → " + newContent.length() + " bytes)\n")
            .errorOutput("")
            .build();
    }

    private GitCommandResult handleFsRemove(List<String> args, RepositoryRuntimeState state, GitRepository repository) {
        if (args.size() < 2) {
            return GitCommandResult.builder()
                .successful(false)
                .exitCode(1)
                .output("")
                .errorOutput("usage: git fs rm <file>\n")
                .build();
        }
        
        String file = args.get(1);
        boolean fileExisted = gitStateManagementService.removeFile(state, file);
        
        if (!fileExisted) {
            return GitCommandResult.builder()
                .successful(false)
                .exitCode(1)
                .output("")
                .errorOutput("file not found: " + file + "\n")
                .build();
        }
        
        gitStateManagementService.saveRuntimeState(repository, state);
        
        return GitCommandResult.builder()
            .successful(true)
            .exitCode(0)
            .output("removed " + file + "\n")
            .errorOutput("")
            .build();
    }

    private GitCommandResult handleFsList(List<String> args, RepositoryRuntimeState state) {
        String directory = args.size() > 1 ? args.get(1) : "";
        
        StringBuilder output = new StringBuilder();
        Set<String> allFiles = new HashSet<>();
        allFiles.addAll(state.getWorkingDirectory().keySet());
        allFiles.addAll(state.getStagingArea().keySet());
        
        // Filter files by directory if specified
        if (!directory.isEmpty()) {
            allFiles = allFiles.stream()
                .filter(file -> file.startsWith(directory))
                .collect(Collectors.toSet());
        }
        
        if (allFiles.isEmpty()) {
            output.append("no files found\n");
        } else {
            allFiles.stream().sorted().forEach(file -> {
                String status = "";
                if (state.getStagingArea().containsKey(file)) {
                    status = " (staged)";
                } else if (state.getWorkingDirectory().containsKey(file)) {
                    status = " (modified)";
                }
                output.append(file).append(status).append("\n");
            });
        }
        
        return GitCommandResult.builder()
            .successful(true)
            .exitCode(0)
            .output(output.toString())
            .errorOutput("")
            .build();
    }

    private GitCommandResult handleFsCat(List<String> args, RepositoryRuntimeState state) {
        if (args.size() < 2) {
            return GitCommandResult.builder()
                .successful(false)
                .exitCode(1)
                .output("")
                .errorOutput("usage: git fs cat <file>\n")
                .build();
        }
        
        String file = args.get(1);
        String content = gitStateManagementService.getFileContent(state, file);
        
        if (content == null) {
            return GitCommandResult.builder()
                .successful(false)
                .exitCode(1)
                .output("")
                .errorOutput("file not found: " + file + "\n")
                .build();
        }
        
        return GitCommandResult.builder()
            .successful(true)
            .exitCode(0)
            .output(content + "\n")
            .errorOutput("")
            .build();
    }

    private GitCommandResult handleFsMkdir(List<String> args, RepositoryRuntimeState state, GitRepository repository) {
        if (args.size() < 2) {
            return GitCommandResult.builder()
                .successful(false)
                .exitCode(1)
                .output("")
                .errorOutput("usage: git fs mkdir <directory>\n")
                .build();
        }
        
        String directory = args.get(1);
        
        // Validate directory path
        if (!isValidDirectoryPath(directory)) {
            return GitCommandResult.builder()
                .successful(false)
                .exitCode(1)
                .output("")
                .errorOutput("invalid directory path: " + directory + "\n")
                .build();
        }
        
        // Create a .gitkeep file to simulate directory creation
        String gitkeepFile = directory + "/.gitkeep";
        gitStateManagementService.addFileToWorkingDirectory(state, gitkeepFile, "");
        gitStateManagementService.saveRuntimeState(repository, state);
        
        return GitCommandResult.builder()
            .successful(true)
            .exitCode(0)
            .output("created directory " + directory + "\n")
            .errorOutput("")
            .build();
    }

    // Validation methods

    private boolean isValidFilePath(String path) {
        // Basic validation for file paths
        return path != null && 
               !path.trim().isEmpty() && 
               !path.contains("..") && 
               !path.startsWith("/") &&
               path.matches("^[a-zA-Z0-9._/-]+$");
    }

    private boolean isValidDirectoryPath(String path) {
        // Basic validation for directory paths
        return path != null && 
               !path.trim().isEmpty() && 
               !path.contains("..") && 
               !path.startsWith("/") &&
               !path.endsWith("/") &&
               path.matches("^[a-zA-Z0-9._-]+(/[a-zA-Z0-9._-]+)*$");
    }

    private void createDirectoryStructure(String filePath, RepositoryRuntimeState state) {
        // If file path contains directories, create them
        if (filePath.contains("/")) {
            String[] parts = filePath.split("/");
            StringBuilder dirPath = new StringBuilder();
            
            for (int i = 0; i < parts.length - 1; i++) {
                if (i > 0) dirPath.append("/");
                dirPath.append(parts[i]);
                
                String gitkeepFile = dirPath.toString() + "/.gitkeep";
                if (!gitStateManagementService.fileExists(state, gitkeepFile)) {
                    gitStateManagementService.addFileToWorkingDirectory(state, gitkeepFile, "");
                }
            }
        }
    }
}
