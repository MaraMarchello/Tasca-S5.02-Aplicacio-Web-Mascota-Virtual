package com.codemate.service;

import com.codemate.model.GitScenario;
import com.codemate.model.GitUserProgress;
import com.codemate.repository.GitScenarioRepository;
import com.codemate.repository.GitUserProgressRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class GitScenarioService {

    private final GitScenarioRepository gitScenarioRepository;
    private final GitUserProgressRepository gitUserProgressRepository;
    private final PointTransactionService pointTransactionService;
    private final AchievementService achievementService;
    private final ObjectMapper objectMapper;

    /**
     * Gets all active scenarios ordered by difficulty and order index
     */
    public List<GitScenario> getAllActiveScenarios() {
        log.info("Fetching all active scenarios");
        return gitScenarioRepository.findByIsActiveOrderByOrderIndex(true);
    }

    /**
     * Gets scenarios by difficulty level
     */
    public List<GitScenario> getScenariosByLevel(GitScenario.GitScenarioLevel level) {
        log.info("Fetching scenarios for level: {}", level);
        return gitScenarioRepository.findByLevelAndIsActiveOrderByOrderIndex(level, true);
    }

    /**
     * Gets scenarios by category
     */
    public List<GitScenario> getScenariosByCategory(GitScenario.GitScenarioCategory category) {
        log.info("Fetching scenarios for category: {}", category);
        return gitScenarioRepository.findByCategoryAndIsActiveOrderByOrderIndex(category, true);
    }

    /**
     * Gets a specific scenario by ID
     */
    public GitScenario getScenarioById(String scenarioId) {
        log.info("Fetching scenario with ID: {}", scenarioId);
        return gitScenarioRepository.findByScenarioId(scenarioId)
            .orElseThrow(() -> new RuntimeException("Scenario not found: " + scenarioId));
    }

    /**
     * Starts a scenario for a user
     */
    public GitUserProgress startScenario(Long userId, String scenarioId) {
        log.info("Starting scenario: {} for user: {}", scenarioId, userId);
        
        GitScenario scenario = getScenarioById(scenarioId);
        
        // Check if user already has progress for this scenario
        Optional<GitUserProgress> existingProgress = gitUserProgressRepository
            .findByUserIdAndScenarioScenarioId(userId, scenarioId);
        
        if (existingProgress.isPresent()) {
            GitUserProgress progress = existingProgress.get();
            if (progress.getStatus() == GitUserProgress.GitProgressStatus.COMPLETED) {
                log.info("User: {} has already completed scenario: {}", userId, scenarioId);
                return progress;
            } else {
                // Reset progress for restart
                progress.setStatus(GitUserProgress.GitProgressStatus.IN_PROGRESS);
                progress.setCurrentStep(0);
                progress.setCommandsExecuted(0);
                progress.setHintsUsed(0);
                progress.setStartedAt(LocalDateTime.now());
                progress.setCompletedAt(null);
                return gitUserProgressRepository.save(progress);
            }
        }

        // Create new progress record
        GitUserProgress progress = GitUserProgress.builder()
            .userId(userId)
            .scenario(scenario)
            .status(GitUserProgress.GitProgressStatus.IN_PROGRESS)
            .currentStep(0)
            .totalSteps(calculateTotalSteps(scenario))
            .commandsExecuted(0)
            .hintsUsed(0)
            .pointsEarned(0)
            .startedAt(LocalDateTime.now())
            .build();

        return gitUserProgressRepository.save(progress);
    }

    /**
     * Updates user progress for a scenario
     */
    public GitUserProgress updateProgress(Long userId, String scenarioId, int currentStep, boolean stepCompleted) {
        log.info("Updating progress for user: {} scenario: {} step: {}", userId, scenarioId, currentStep);
        
        GitUserProgress progress = gitUserProgressRepository
            .findByUserIdAndScenarioScenarioId(userId, scenarioId)
            .orElseThrow(() -> new RuntimeException("Progress not found for user: " + userId + " and scenario: " + scenarioId));

        progress.setCurrentStep(currentStep);
        progress.setCommandsExecuted(progress.getCommandsExecuted() + 1);
        
        if (stepCompleted && currentStep >= progress.getTotalSteps()) {
            completeScenario(progress);
        }

        return gitUserProgressRepository.save(progress);
    }

    /**
     * Marks a scenario as completed for a user
     */
    public GitUserProgress completeScenario(GitUserProgress progress) {
        log.info("Completing scenario: {} for user: {}", progress.getScenario().getScenarioId(), progress.getUserId());
        
        progress.setStatus(GitUserProgress.GitProgressStatus.COMPLETED);
        progress.setCompletedAt(LocalDateTime.now());
        progress.setCurrentStep(progress.getTotalSteps());
        
        // Calculate points earned based on performance
        int pointsEarned = calculatePointsEarned(progress);
        progress.setPointsEarned(pointsEarned);
        
        // Award points to user
        if (pointsEarned > 0) {
            pointTransactionService.createTransaction(
                progress.getUserId(), 
                com.codemate.model.TransactionType.EARNED,
                com.codemate.model.PointSource.GIT_SCENARIO_COMPLETED,
                (long) pointsEarned,
                "Git scenario completed: " + progress.getScenario().getTitle()
            );
        }

        // Track achievements
        trackScenarioAchievements(progress);

        return gitUserProgressRepository.save(progress);
    }

    /**
     * Gets user progress for all scenarios
     */
    public List<GitUserProgress> getUserProgress(Long userId) {
        log.info("Fetching progress for user: {}", userId);
        return gitUserProgressRepository.findByUserId(userId);
    }

    /**
     * Gets user progress for a specific scenario
     */
    public Optional<GitUserProgress> getUserProgressForScenario(Long userId, String scenarioId) {
        log.info("Fetching progress for user: {} scenario: {}", userId, scenarioId);
        return gitUserProgressRepository.findByUserIdAndScenarioScenarioId(userId, scenarioId);
    }

    /**
     * Gets completed scenarios for a user
     */
    public List<GitUserProgress> getCompletedScenarios(Long userId) {
        log.info("Fetching completed scenarios for user: {}", userId);
        return gitUserProgressRepository.findCompletedScenariosByUser(userId);
    }

    /**
     * Gets in-progress scenarios for a user
     */
    public List<GitUserProgress> getInProgressScenarios(Long userId) {
        log.info("Fetching in-progress scenarios for user: {}", userId);
        return gitUserProgressRepository.findInProgressScenariosByUser(userId);
    }

    /**
     * Checks if a scenario step is completed based on command execution
     */
    public boolean checkScenarioStepCompletion(String scenarioId, int stepNumber, String command, String output) {
        log.info("Checking step completion for scenario: {} step: {} command: {}", scenarioId, stepNumber, command);
        
        GitScenario scenario = getScenarioById(scenarioId);
        return validateStepCompletion(scenario, stepNumber, command, output);
    }

    /**
     * Gets the next step guidance for a scenario
     */
    public String getNextStepGuidance(String scenarioId, int currentStep) {
        log.info("Getting next step guidance for scenario: {} step: {}", scenarioId, currentStep);
        
        GitScenario scenario = getScenarioById(scenarioId);
        return extractStepGuidance(scenario, currentStep);
    }

    /**
     * Provides a hint for the current step
     */
    public String getStepHint(Long userId, String scenarioId, int currentStep) {
        log.info("Getting hint for user: {} scenario: {} step: {}", userId, scenarioId, currentStep);
        
        // Update hint usage count
        GitUserProgress progress = gitUserProgressRepository
            .findByUserIdAndScenarioScenarioId(userId, scenarioId)
            .orElseThrow(() -> new RuntimeException("Progress not found"));
        
        progress.setHintsUsed(progress.getHintsUsed() + 1);
        gitUserProgressRepository.save(progress);
        
        // Track hint usage achievement
        achievementService.trackGitHintUsage(userId);
        
        GitScenario scenario = getScenarioById(scenarioId);
        return extractStepHint(scenario, currentStep);
    }

    /**
     * Gets user statistics for Git scenarios
     */
    public GitUserStats getUserStats(Long userId) {
        log.info("Getting Git stats for user: {}", userId);
        
        Long completedCount = gitUserProgressRepository.countCompletedScenariosByUser(userId);
        Long totalPoints = gitUserProgressRepository.sumPointsEarnedByUser(userId);
        Double avgCommands = gitUserProgressRepository.averageCommandsExecutedByUser(userId);
        
        return GitUserStats.builder()
            .userId(userId)
            .completedScenarios(completedCount != null ? completedCount : 0L)
            .totalPointsEarned(totalPoints != null ? totalPoints : 0L)
            .averageCommandsPerScenario(avgCommands != null ? avgCommands : 0.0)
            .build();
    }

    /**
     * Tracks command execution for achievements
     */
    public void trackCommandExecution(Long userId, String command) {
        log.info("Tracking command execution for user: {} command: {}", userId, command);
        
        // Track daily learning
        achievementService.trackDailyGitLearning(userId);
        
        // Track command usage
        achievementService.trackGitCommandUsage(userId, command);
    }

    // Private helper methods

    private void trackScenarioAchievements(GitUserProgress progress) {
        GitScenario scenario = progress.getScenario();
        
        // Calculate completion time in minutes
        long completionTimeMinutes = 0;
        if (progress.getStartedAt() != null && progress.getCompletedAt() != null) {
            completionTimeMinutes = java.time.Duration.between(
                progress.getStartedAt(), 
                progress.getCompletedAt()
            ).toMinutes();
        }
        
        // Determine if it's a perfect score (efficient completion with minimal hints)
        boolean perfectScore = progress.getHintsUsed() == 0 && 
                              progress.getCommandsExecuted() <= progress.getTotalSteps() * 1.2;
        
        // Track the achievement
        achievementService.trackGitScenarioCompletion(
            progress.getUserId(),
            scenario.getLevel().toString(),
            scenario.getCategory().toString(),
            progress.getHintsUsed() > 0,
            perfectScore,
            completionTimeMinutes
        );
    }

    private int calculateTotalSteps(GitScenario scenario) {
        try {
            if (scenario.getExpectedCommands() != null) {
                JsonNode expectedCommands = objectMapper.readTree(scenario.getExpectedCommands());
                if (expectedCommands.isArray()) {
                    return expectedCommands.size();
                }
            }
        } catch (JsonProcessingException e) {
            log.error("Error parsing expected commands for scenario: {}", scenario.getScenarioId(), e);
        }
        return 5; // Default number of steps
    }

    private int calculatePointsEarned(GitUserProgress progress) {
        GitScenario scenario = progress.getScenario();
        int basePoints = scenario.getPointsReward();
        
        // Apply multipliers based on performance
        double multiplier = 1.0;
        
        // Bonus for completing without hints
        if (progress.getHintsUsed() == 0) {
            multiplier += 0.2;
        }
        
        // Penalty for excessive hints
        if (progress.getHintsUsed() > 3) {
            multiplier -= 0.1;
        }
        
        // Bonus for efficient command usage
        if (progress.getCommandsExecuted() <= progress.getTotalSteps() * 1.5) {
            multiplier += 0.1;
        }
        
        return (int) (basePoints * multiplier);
    }

    private boolean validateStepCompletion(GitScenario scenario, int stepNumber, String command, String output) {
        try {
            if (scenario.getExpectedCommands() != null) {
                JsonNode expectedCommands = objectMapper.readTree(scenario.getExpectedCommands());
                if (expectedCommands.isArray() && stepNumber < expectedCommands.size()) {
                    JsonNode expectedStep = expectedCommands.get(stepNumber);
                    String expectedCommand = expectedStep.get("command").asText();
                    
                    // Simple command matching - can be enhanced with regex patterns
                    return command.toLowerCase().contains(expectedCommand.toLowerCase());
                }
            }
        } catch (JsonProcessingException e) {
            log.error("Error validating step completion for scenario: {}", scenario.getScenarioId(), e);
        }
        return false;
    }

    private String extractStepGuidance(GitScenario scenario, int stepNumber) {
        try {
            if (scenario.getExpectedCommands() != null) {
                JsonNode expectedCommands = objectMapper.readTree(scenario.getExpectedCommands());
                if (expectedCommands.isArray() && stepNumber < expectedCommands.size()) {
                    JsonNode step = expectedCommands.get(stepNumber);
                    return step.has("guidance") ? step.get("guidance").asText() : "Continue with the next step.";
                }
            }
        } catch (JsonProcessingException e) {
            log.error("Error extracting step guidance for scenario: {}", scenario.getScenarioId(), e);
        }
        return "Follow the scenario instructions to proceed.";
    }

    private String extractStepHint(GitScenario scenario, int stepNumber) {
        try {
            if (scenario.getExpectedCommands() != null) {
                JsonNode expectedCommands = objectMapper.readTree(scenario.getExpectedCommands());
                if (expectedCommands.isArray() && stepNumber < expectedCommands.size()) {
                    JsonNode step = expectedCommands.get(stepNumber);
                    return step.has("hint") ? step.get("hint").asText() : "Try using the appropriate git command for this step.";
                }
            }
        } catch (JsonProcessingException e) {
            log.error("Error extracting step hint for scenario: {}", scenario.getScenarioId(), e);
        }
        return "Check the git documentation for help with this command.";
    }

    // Data transfer objects

    @lombok.Builder
    @lombok.Data
    public static class GitUserStats {
        private Long userId;
        private Long completedScenarios;
        private Long totalPointsEarned;
        private Double averageCommandsPerScenario;
    }
} 