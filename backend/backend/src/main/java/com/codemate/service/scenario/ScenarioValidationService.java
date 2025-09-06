package com.codemate.service.scenario;

import com.codemate.model.GitScenario;
import com.codemate.service.git.GitSimulatorFacadeService;
import com.codemate.service.git.GitRepositoryManagementService;
import com.codemate.util.PerformanceMonitor;
import com.codemate.scenario.ScenarioDefinition;
import com.codemate.scenario.ScenarioStepDefinition;
import com.codemate.scenario.ValidationRuleDefinition;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Service responsible for validating scenario steps and commands
 * Handles step completion validation, state assertions, and command matching
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ScenarioValidationService {

    private final GitSimulatorFacadeService gitSimulatorFacadeService;
    private final ScenarioManagementService scenarioManagementService;
    private final PerformanceMonitor performanceMonitor;
    private final ObjectMapper objectMapper;

    /**
     * Checks if a scenario step is completed based on command execution
     */
    public boolean checkScenarioStepCompletion(String scenarioId, int stepNumber, String command, String output) {
        log.info("Checking step completion for scenario: {} step: {} command: {}", scenarioId, stepNumber, command);
        
        GitScenario scenario = scenarioManagementService.getScenarioById(scenarioId);
        return validateStepCompletion(scenario, stepNumber, command, output);
    }

    /**
     * Validates a step and returns both status and an explanatory message when available.
     */
    public ValidationFeedback validateStepWithMessage(Long repositoryId, String scenarioId, int stepNumber, String command, String output) {
        performanceMonitor.startOperation("scenario_validation", null, repositoryId, scenarioId);
        
        try {
            GitScenario scenario = scenarioManagementService.getScenarioById(scenarioId);
            ValidationResult vr = validateStepWithSchema(repositoryId, scenario, stepNumber, command, output);
            
            ValidationFeedback feedback;
            if (vr.status == ValidationStatus.SUCCESS) {
                feedback = new ValidationFeedback(true, "Step completed.");
            } else if (vr.status == ValidationStatus.FAIL) {
                feedback = new ValidationFeedback(false, vr.message != null ? vr.message : "Command did not satisfy step requirements.");
            } else {
                // Fallback to old format
                boolean ok = validateStepCompletion(scenario, stepNumber, command, output);
                feedback = new ValidationFeedback(ok, ok ? "Step completed." : "Command did not match expected step.");
            }
            
            long duration = performanceMonitor.endOperation("scenario_validation", null, repositoryId);
            log.debug("Scenario validation completed in {}ms for scenario: {} step: {}", duration, scenarioId, stepNumber);
            
            return feedback;
        } catch (Exception e) {
            performanceMonitor.endOperation("scenario_validation", null, repositoryId);
            log.error("Error validating scenario step: {} step: {} command: {}", scenarioId, stepNumber, command, e);
            throw e;
        }
    }

    /**
     * Validates a step using the new scenario schema when present.
     */
    private ValidationResult validateStepWithSchema(Long repositoryId, GitScenario scenario, int stepNumber, String command, String output) {
        try {
            if (scenario.getExpectedCommands() == null) {
                return ValidationResult.fail("No scenario steps defined.");
            }
            ScenarioDefinition def = objectMapper.readValue(scenario.getExpectedCommands(), ScenarioDefinition.class);
            if (def.getSteps() == null || def.getSteps().isEmpty()) {
                return ValidationResult.fail("Scenario has no steps.");
            }
            if (stepNumber < 0 || stepNumber >= def.getSteps().size()) {
                return ValidationResult.fail("Invalid step number.");
            }
            ScenarioStepDefinition step = def.getSteps().get(stepNumber);

            // 1) Check acceptable command patterns (any matches)
            if (step.getAcceptableCommands() != null && !step.getAcceptableCommands().isEmpty()) {
                boolean anyMatch = step.getAcceptableCommands().stream().anyMatch(pattern ->
                    command.toLowerCase().matches(pattern) || command.toLowerCase().contains(pattern.toLowerCase())
                );
                if (!anyMatch) {
                    return ValidationResult.fail("Expected a command matching one of: " + String.join(", ", step.getAcceptableCommands()));
                }
            }

            // 2) Evaluate state assertions we support in Phase 3 MVP
            if (step.getStateAssertions() != null) {
                for (ValidationRuleDefinition rule : step.getStateAssertions()) {
                    ValidationResult ruleResult = validateStateAssertion(repositoryId, rule, command, output);
                    if (ruleResult.status == ValidationStatus.FAIL) {
                        return ruleResult;
                    }
                }
            }

            return ValidationResult.success(step.getGuidance());
        } catch (JsonProcessingException e) {
            log.warn("Failed to parse scenario schema for scenario: {}", scenario.getScenarioId(), e);
            return ValidationResult.unknown();
        }
    }

    /**
     * Validates individual state assertions
     */
    private ValidationResult validateStateAssertion(Long repositoryId, ValidationRuleDefinition rule, String command, String output) {
        switch (rule.getType()) {
            case BRANCH_EXISTS:
                if (!branchExists(repositoryId, rule.getTarget())) {
                    return ValidationResult.fail(rule.getErrorMessage() != null ? rule.getErrorMessage() : "Expected branch does not exist: " + rule.getTarget());
                }
                break;
            case CURRENT_BRANCH:
                if (rule.getTarget() != null && !isOnBranch(repositoryId, rule.getTarget())) {
                    return ValidationResult.fail(rule.getErrorMessage() != null ? rule.getErrorMessage() : ("Expected to be on branch: " + rule.getTarget()));
                }
                break;
            case COMMIT_MESSAGE:
                if (!validateCommitMessage(repositoryId, command, output, rule.getCondition())) {
                    return ValidationResult.fail(rule.getErrorMessage() != null ? rule.getErrorMessage() : "Commit message did not contain: '" + rule.getCondition() + "'.");
                }
                break;
            case MERGE_COMPLETED:
                if (!validateMergeCompletion(repositoryId, command, output)) {
                    return ValidationResult.fail(rule.getErrorMessage() != null ? rule.getErrorMessage() : "Merge step not yet completed.");
                }
                break;
            case FILE_EXISTS:
                if (rule.getTarget() != null && !repositoryHasFile(repositoryId, rule.getTarget())) {
                    return ValidationResult.fail(rule.getErrorMessage() != null ? rule.getErrorMessage() : ("Expected file to exist: " + rule.getTarget()));
                }
                break;
            case FILE_CONTENT:
                if (rule.getTarget() != null && rule.getCondition() != null && !repositoryFileContains(repositoryId, rule.getTarget(), rule.getCondition())) {
                    return ValidationResult.fail(rule.getErrorMessage() != null ? rule.getErrorMessage() : ("Expected file content to include '" + rule.getCondition() + "' in " + rule.getTarget()));
                }
                break;
            default:
                log.warn("Unknown validation rule type: {}", rule.getType());
                break;
        }
        return ValidationResult.success(null);
    }

    private boolean branchExists(Long repositoryId, String branchName) {
        if (repositoryId == null || branchName == null) {
            log.warn("Invalid parameters for branchExists: repositoryId={}, branchName={}", repositoryId, branchName);
            return false;
        }
        try {
            GitRepositoryManagementService.GitRepositoryState state = gitSimulatorFacadeService.getRepositoryState(repositoryId);
            boolean exists = state.getBranches().stream()
                .anyMatch(branch -> branchName.equals(branch.getName()));
            log.debug("Branch '{}' exists in repository {}: {}", branchName, repositoryId, exists);
            return exists;
        } catch (Exception e) {
            log.error("Error checking if branch '{}' exists in repository {}", branchName, repositoryId, e);
            return false;
        }
    }

    private boolean repositoryHasFile(Long repositoryId, String path) {
        if (repositoryId == null || path == null) {
            log.warn("Invalid parameters for repositoryHasFile: repositoryId={}, path={}", repositoryId, path);
            return false;
        }
        try {
            GitRepositoryManagementService.GitRepositoryState state = gitSimulatorFacadeService.getRepositoryState(repositoryId);
            boolean hasInWorking = state.getWorkingDirectory() != null && state.getWorkingDirectory().containsKey(path);
            boolean hasInStaging = state.getStagingArea() != null && state.getStagingArea().containsKey(path);
            boolean hasFile = hasInWorking || hasInStaging;
            log.debug("File '{}' exists in repository {}: {} (working: {}, staging: {})", 
                     path, repositoryId, hasFile, hasInWorking, hasInStaging);
            return hasFile;
        } catch (Exception e) {
            log.error("Error checking if file '{}' exists in repository {}", path, repositoryId, e);
            return false;
        }
    }

    private boolean repositoryFileContains(Long repositoryId, String path, String needle) {
        if (repositoryId == null || path == null) {
            log.warn("Invalid parameters for repositoryFileContains: repositoryId={}, path={}", repositoryId, path);
            return false;
        }
        try {
            GitRepositoryManagementService.GitRepositoryState state = gitSimulatorFacadeService.getRepositoryState(repositoryId);
            String content = null;
            
            // Check working directory first, then staging area
            if (state.getWorkingDirectory() != null && state.getWorkingDirectory().containsKey(path)) {
                content = state.getWorkingDirectory().get(path);
            } else if (state.getStagingArea() != null && state.getStagingArea().containsKey(path)) {
                content = state.getStagingArea().get(path);
            }
            
            boolean contains = content != null && (needle == null || content.contains(needle));
            log.debug("File '{}' in repository {} contains '{}': {} (content length: {})", 
                     path, repositoryId, needle, contains, content != null ? content.length() : 0);
            return contains;
        } catch (Exception e) {
            log.error("Error checking if file '{}' contains '{}' in repository {}", path, needle, repositoryId, e);
            return false;
        }
    }

    private boolean isOnBranch(Long repositoryId, String expectedBranch) {
        if (repositoryId == null || expectedBranch == null) {
            log.warn("Invalid parameters for isOnBranch: repositoryId={}, expectedBranch={}", repositoryId, expectedBranch);
            return false;
        }
        try {
            GitRepositoryManagementService.GitRepositoryState state = gitSimulatorFacadeService.getRepositoryState(repositoryId);
            String currentBranch = state.getCurrentBranch();
            boolean isOnExpectedBranch = currentBranch != null && currentBranch.equals(expectedBranch);
            log.debug("Repository {} is on branch '{}', expected '{}': {}", 
                     repositoryId, currentBranch, expectedBranch, isOnExpectedBranch);
            return isOnExpectedBranch;
        } catch (Exception e) {
            log.error("Error checking current branch in repository {}", repositoryId, e);
            return false;
        }
    }

    private boolean validateCommitMessage(Long repositoryId, String command, String output, String expectedContent) {
        if (repositoryId == null || command == null) {
            log.warn("Invalid parameters for validateCommitMessage");
            return false;
        }
        
        try {
            // Check if command was a commit command
            if (!command.toLowerCase().contains("commit")) {
                log.debug("Command '{}' is not a commit command", command);
                return false;
            }
            
            // If no specific content is expected, just check if commit was successful
            if (expectedContent == null || expectedContent.trim().isEmpty()) {
                return output != null && (output.contains("1 file changed") || 
                                        output.contains("create mode") ||
                                        output.contains("[") && output.contains("]"));
            }
            
            // Check if the command itself contains the expected message content
            boolean messageInCommand = command.toLowerCase().contains(expectedContent.toLowerCase());
            
            // Check the latest commit in repository state
            GitRepositoryManagementService.GitRepositoryState state = gitSimulatorFacadeService.getRepositoryState(repositoryId);
            boolean messageInLatestCommit = false;
            
            if (!state.getCommits().isEmpty()) {
                String latestCommitMessage = state.getCommits().get(0).getMessage();
                messageInLatestCommit = latestCommitMessage != null && 
                                      latestCommitMessage.toLowerCase().contains(expectedContent.toLowerCase());
            }
            
            log.debug("Commit message validation - expected: '{}', in command: {}, in latest commit: {}", 
                     expectedContent, messageInCommand, messageInLatestCommit);
            
            return messageInCommand || messageInLatestCommit;
        } catch (Exception e) {
            log.error("Error validating commit message for repository {}", repositoryId, e);
            return false;
        }
    }

    private boolean validateMergeCompletion(Long repositoryId, String command, String output) {
        if (repositoryId == null || command == null || output == null) {
            log.warn("Invalid parameters for validateMergeCompletion");
            return false;
        }
        
        try {
            // Check if command was a merge command
            if (!command.toLowerCase().contains("merge")) {
                log.debug("Command '{}' is not a merge command", command);
                return false;
            }
            
            // Check if merge was successful by examining output
            String lowerOutput = output.toLowerCase();
            boolean mergeSuccessful = lowerOutput.contains("merge made") || 
                                    lowerOutput.contains("fast-forward") ||
                                    lowerOutput.contains("already up to date");
            
            // Check if merge had conflicts
            boolean hasConflicts = lowerOutput.contains("conflict") || 
                                 lowerOutput.contains("automatic merge failed");
            
            if (hasConflicts) {
                log.debug("Merge command had conflicts, not considered complete");
                return false;
            }
            
            if (mergeSuccessful) {
                // Additional verification: check if repository state shows merge commit
                GitRepositoryManagementService.GitRepositoryState state = gitSimulatorFacadeService.getRepositoryState(repositoryId);
                boolean hasMergeCommit = state.getCommits().stream()
                    .anyMatch(commit -> commit.getMessage().toLowerCase().contains("merge") ||
                                      commit.getParentHashes().size() > 1);
                
                log.debug("Merge validation - output success: {}, merge commit found: {}", 
                         mergeSuccessful, hasMergeCommit);
                return hasMergeCommit;
            }
            
            return false;
        } catch (Exception e) {
            log.error("Error validating merge completion for repository {}", repositoryId, e);
            return false;
        }
    }

    private boolean validateStepCompletion(GitScenario scenario, int stepNumber, String command, String output) {
        // New schema path with repository context
        ValidationResult vr = validateStepWithSchema(null, scenario, stepNumber, command, output);
        if (vr.status == ValidationStatus.SUCCESS) return true;
        if (vr.status == ValidationStatus.FAIL) return false;

        // Fallback: old simple array format with improved matching
        try {
            if (scenario.getExpectedCommands() != null) {
                JsonNode expectedCommands = objectMapper.readTree(scenario.getExpectedCommands());
                if (expectedCommands.isArray() && stepNumber < expectedCommands.size()) {
                    JsonNode expectedStep = expectedCommands.get(stepNumber);
                    
                    // Check for either "command" field or direct string
                    String expectedCommand = null;
                    if (expectedStep.has("command")) {
                        expectedCommand = expectedStep.get("command").asText();
                    } else if (expectedStep.isTextual()) {
                        expectedCommand = expectedStep.asText();
                    }
                    
                    if (expectedCommand != null) {
                        // Improved command matching with better patterns
                        String normalizedCommand = command.toLowerCase().trim();
                        String normalizedExpected = expectedCommand.toLowerCase().trim();
                        
                        // Direct contains check
                        if (normalizedCommand.contains(normalizedExpected)) {
                            return true;
                        }
                        
                        // Check for git command patterns
                        if (normalizedExpected.startsWith("git ")) {
                            normalizedExpected = normalizedExpected.substring(4); // Remove "git " prefix
                        }
                        
                        // Pattern matching for common variations
                        return normalizedCommand.matches(".*\\b" + normalizedExpected.replace(" ", "\\s+") + "\\b.*");
                    }
                }
            }
        } catch (JsonProcessingException e) {
            log.error("Error validating step completion for scenario: {}", scenario.getScenarioId(), e);
        }
        
        log.debug("Step validation failed for scenario: {} step: {} command: {}", 
                 scenario.getScenarioId(), stepNumber, command);
        return false;
    }

    // Lightweight validation result to surface tutor messages
    private enum ValidationStatus { SUCCESS, FAIL, UNKNOWN }
    
    private static class ValidationResult {
        final ValidationStatus status;
        final String message;
        
        private ValidationResult(ValidationStatus status, String message) {
            this.status = status;
            this.message = message;
        }
        
        static ValidationResult success(String message) { 
            return new ValidationResult(ValidationStatus.SUCCESS, message); 
        }
        
        static ValidationResult fail(String message) { 
            return new ValidationResult(ValidationStatus.FAIL, message); 
        }
        
        static ValidationResult unknown() { 
            return new ValidationResult(ValidationStatus.UNKNOWN, null); 
        }
    }

    // General feedback class for step validation with user-facing messages
    @lombok.Data
    @lombok.AllArgsConstructor
    public static class ValidationFeedback {
        private boolean stepCompleted;
        private String message;
    }
}
