package com.codemate.service.scenario;

import com.codemate.model.GitScenario;
import com.codemate.model.GitUserProgress;
import com.codemate.repository.GitUserProgressRepository;
import com.codemate.service.PointTransactionService;
import com.codemate.service.AchievementService;
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

/**
 * Service responsible for managing user progress in Git scenarios
 * Handles progress tracking, updates, completion, and statistics
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class ScenarioProgressService {

    private final GitUserProgressRepository gitUserProgressRepository;
    private final PointTransactionService pointTransactionService;
    private final AchievementService achievementService;
    private final ScenarioManagementService scenarioManagementService;
    private final ObjectMapper objectMapper;

    /**
     * Starts a scenario for a user
     */
    public GitUserProgress startScenario(Long userId, String scenarioId) {
        log.info("Starting scenario: {} for user: {}", scenarioId, userId);
        
        GitScenario scenario = scenarioManagementService.getScenarioById(scenarioId);
        
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
                return resetProgress(progress);
            }
        }

        // Create new progress record
        return createNewProgress(userId, scenario);
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
        
        // Track step completion achievement
        if (stepCompleted) {
            try {
                achievementService.trackGitStepCompletion(userId);
            } catch (Exception e) {
                log.warn("Failed to track step completion for user: {}", userId, e);
            }
        }
        
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
     * Increments hint usage for a user's scenario progress
     */
    public GitUserProgress incrementHintUsage(Long userId, String scenarioId) {
        log.info("Incrementing hint usage for user: {} scenario: {}", userId, scenarioId);
        
        GitUserProgress progress = gitUserProgressRepository
            .findByUserIdAndScenarioScenarioId(userId, scenarioId)
            .orElseThrow(() -> new RuntimeException("Progress not found"));
        
        progress.setHintsUsed(progress.getHintsUsed() + 1);
        
        // Track hint usage achievement
        achievementService.trackGitHintUsage(userId);
        
        return gitUserProgressRepository.save(progress);
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

    // Private helper methods

    private GitUserProgress resetProgress(GitUserProgress progress) {
        progress.setStatus(GitUserProgress.GitProgressStatus.IN_PROGRESS);
        progress.setCurrentStep(0);
        progress.setCommandsExecuted(0);
        progress.setHintsUsed(0);
        progress.setStartedAt(LocalDateTime.now());
        progress.setCompletedAt(null);
        progress.setPointsEarned(0);
        return gitUserProgressRepository.save(progress);
    }

    private GitUserProgress createNewProgress(Long userId, GitScenario scenario) {
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

    private int calculateTotalSteps(GitScenario scenario) {
        try {
            if (scenario.getExpectedCommands() != null) {
                JsonNode expectedCommands = objectMapper.readTree(scenario.getExpectedCommands());
                
                // Handle new schema format with steps array
                if (expectedCommands.has("steps") && expectedCommands.get("steps").isArray()) {
                    return expectedCommands.get("steps").size();
                }
                
                // Handle legacy format (direct array)
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
