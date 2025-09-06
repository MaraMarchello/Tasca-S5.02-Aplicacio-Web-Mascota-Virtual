package com.codemate.service.scenario;

import com.codemate.model.GitScenario;
import com.codemate.model.GitUserProgress;
import com.codemate.model.UserBadge;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Facade service that coordinates all scenario-related operations
 * This is the main entry point for scenario functionality, delegating to specialized services
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class GitScenarioFacadeService {

    private final ScenarioManagementService scenarioManagementService;
    private final ScenarioProgressService scenarioProgressService;
    private final ScenarioValidationService scenarioValidationService;
    private final ScenarioGuidanceService scenarioGuidanceService;
    private final ScenarioGamificationService scenarioGamificationService;

    // === Scenario Management Operations ===

    /**
     * Gets all active scenarios
     */
    public List<GitScenario> getAllActiveScenarios() {
        return scenarioManagementService.getAllActiveScenarios();
    }

    /**
     * Gets scenarios by level
     */
    public List<GitScenario> getScenariosByLevel(GitScenario.GitScenarioLevel level) {
        return scenarioManagementService.getScenariosByLevel(level);
    }

    /**
     * Gets scenarios by category
     */
    public List<GitScenario> getScenariosByCategory(GitScenario.GitScenarioCategory category) {
        return scenarioManagementService.getScenariosByCategory(category);
    }

    /**
     * Gets a specific scenario by ID
     */
    public GitScenario getScenarioById(String scenarioId) {
        return scenarioManagementService.getScenarioById(scenarioId);
    }

    // === Progress Management Operations ===

    /**
     * Starts a scenario for a user
     */
    public GitUserProgress startScenario(Long userId, String scenarioId) {
        return scenarioProgressService.startScenario(userId, scenarioId);
    }

    /**
     * Updates user progress for a scenario
     */
    public GitUserProgress updateProgress(Long userId, String scenarioId, int currentStep, boolean stepCompleted) {
        GitUserProgress progress = scenarioProgressService.updateProgress(userId, scenarioId, currentStep, stepCompleted);
        
        // Process gamification for step completion
        if (stepCompleted) {
            scenarioGamificationService.processStepCompletion(userId, scenarioId, currentStep);
        }
        
        return progress;
    }

    /**
     * Completes a scenario for a user with full gamification processing
     */
    public ScenarioCompletionResult completeScenario(GitUserProgress progress) {
        GitUserProgress completedProgress = scenarioProgressService.completeScenario(progress);
        
        // Process gamification for scenario completion
        List<UserBadge> newBadges = scenarioGamificationService.processScenarioCompletion(
            progress.getUserId(), 
            progress.getScenario(), 
            completedProgress, 
            completedProgress.getPointsEarned()
        );
        
        return ScenarioCompletionResult.builder()
            .progress(completedProgress)
            .newBadges(newBadges)
            .pointsEarned(completedProgress.getPointsEarned())
            .build();
    }

    /**
     * Gets user progress for all scenarios
     */
    public List<GitUserProgress> getUserProgress(Long userId) {
        return scenarioProgressService.getUserProgress(userId);
    }

    /**
     * Gets user progress for a specific scenario
     */
    public Optional<GitUserProgress> getUserProgressForScenario(Long userId, String scenarioId) {
        return scenarioProgressService.getUserProgressForScenario(userId, scenarioId);
    }

    /**
     * Gets completed scenarios for a user
     */
    public List<GitUserProgress> getCompletedScenarios(Long userId) {
        return scenarioProgressService.getCompletedScenarios(userId);
    }

    /**
     * Gets in-progress scenarios for a user
     */
    public List<GitUserProgress> getInProgressScenarios(Long userId) {
        return scenarioProgressService.getInProgressScenarios(userId);
    }

    /**
     * Gets user statistics
     */
    public ScenarioProgressService.GitUserStats getUserStats(Long userId) {
        return scenarioProgressService.getUserStats(userId);
    }

    // === Validation Operations ===

    /**
     * Checks if a scenario step is completed
     */
    public boolean checkScenarioStepCompletion(String scenarioId, int stepNumber, String command, String output) {
        return scenarioValidationService.checkScenarioStepCompletion(scenarioId, stepNumber, command, output);
    }

    /**
     * Validates a step with detailed feedback
     */
    public ScenarioValidationService.ValidationFeedback validateStepWithMessage(Long repositoryId, String scenarioId, int stepNumber, String command, String output) {
        return scenarioValidationService.validateStepWithMessage(repositoryId, scenarioId, stepNumber, command, output);
    }

    // === Guidance Operations ===

    /**
     * Gets next step guidance
     */
    public String getNextStepGuidance(String scenarioId, int currentStep) {
        return scenarioGuidanceService.getNextStepGuidance(scenarioId, currentStep);
    }

    /**
     * Gets step hint
     */
    public String getStepHint(Long userId, String scenarioId, int currentStep) {
        return scenarioGuidanceService.getStepHint(userId, scenarioId, currentStep);
    }

    /**
     * Gets complete step information
     */
    public ScenarioGuidanceService.StepInformation getCompleteStepInformation(String scenarioId, int stepNumber) {
        return scenarioGuidanceService.getCompleteStepInformation(scenarioId, stepNumber);
    }

    // === Gamification Operations ===

    /**
     * Tracks command execution for achievements and badges
     */
    public List<UserBadge> trackCommandExecution(Long userId, String command) {
        return scenarioGamificationService.processCommandExecution(userId, command);
    }

    /**
     * Gets gamification dashboard
     */
    public ScenarioGamificationService.GamificationResult getGamificationDashboard(Long userId) {
        var dashboard = scenarioGamificationService.getGamificationDashboard(userId);
        var badges = scenarioGamificationService.getUserBadges(userId);
        
        return ScenarioGamificationService.GamificationResult.builder()
            .newBadges(badges)
            .pointsAwarded(dashboard.getTotalPoints().intValue())
            .achievementMessage(dashboard.getStreakStatusMessage())
            .build();
    }

    /**
     * Gets user badges
     */
    public List<UserBadge> getUserBadges(Long userId) {
        return scenarioGamificationService.getUserBadges(userId);
    }

    // === Combined Operations ===

    /**
     * Executes a command and processes all related operations (validation, progress, gamification)
     */
    public CommandExecutionResult executeCommand(Long userId, String scenarioId, Long repositoryId, int currentStep, String command, String output) {
        log.info("Executing command for user: {} scenario: {} step: {} command: {}", userId, scenarioId, currentStep, command);
        
        // Validate the command
        ScenarioValidationService.ValidationFeedback validation = validateStepWithMessage(repositoryId, scenarioId, currentStep, command, output);
        
        // Update progress if step completed
        GitUserProgress progress = null;
        if (validation.isStepCompleted()) {
            progress = updateProgress(userId, scenarioId, currentStep + 1, true);
        }
        
        // Track command execution for gamification
        List<UserBadge> newBadges = trackCommandExecution(userId, command);
        
        return CommandExecutionResult.builder()
            .stepCompleted(validation.isStepCompleted())
            .validationMessage(validation.getMessage())
            .progress(progress)
            .newBadges(newBadges)
            .nextStepGuidance(validation.isStepCompleted() ? getNextStepGuidance(scenarioId, currentStep + 1) : null)
            .build();
    }

    /**
     * Gets complete scenario overview for a user
     */
    public ScenarioOverview getScenarioOverview(Long userId, String scenarioId) {
        GitScenario scenario = getScenarioById(scenarioId);
        Optional<GitUserProgress> progressOpt = getUserProgressForScenario(userId, scenarioId);
        
        ScenarioOverview.ScenarioOverviewBuilder builder = ScenarioOverview.builder()
            .scenario(scenario)
            .isStarted(progressOpt.isPresent())
            .isCompleted(progressOpt.map(p -> p.getStatus() == GitUserProgress.GitProgressStatus.COMPLETED).orElse(false));
        
        if (progressOpt.isPresent()) {
            GitUserProgress progress = progressOpt.get();
            builder.progress(progress)
                   .currentStepInfo(getCompleteStepInformation(scenarioId, progress.getCurrentStep()));
        }
        
        return builder.build();
    }

    // Data transfer objects

    @lombok.Builder
    @lombok.Data
    public static class CommandExecutionResult {
        private boolean stepCompleted;
        private String validationMessage;
        private GitUserProgress progress;
        private List<UserBadge> newBadges;
        private String nextStepGuidance;
    }

    @lombok.Builder
    @lombok.Data
    public static class ScenarioCompletionResult {
        private GitUserProgress progress;
        private List<UserBadge> newBadges;
        private int pointsEarned;
    }

    @lombok.Builder
    @lombok.Data
    public static class ScenarioOverview {
        private GitScenario scenario;
        private boolean isStarted;
        private boolean isCompleted;
        private GitUserProgress progress;
        private ScenarioGuidanceService.StepInformation currentStepInfo;
    }
}
