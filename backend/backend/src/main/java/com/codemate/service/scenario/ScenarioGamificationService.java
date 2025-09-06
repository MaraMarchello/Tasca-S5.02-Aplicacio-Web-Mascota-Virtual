package com.codemate.service.scenario;

import com.codemate.model.GitScenario;
import com.codemate.model.GitUserProgress;
import com.codemate.model.UserBadge;
import com.codemate.service.GamificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Service responsible for integrating gamification with Git scenarios
 * Handles badge awarding, achievement tracking, and progress gamification
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ScenarioGamificationService {

    private final GamificationService gamificationService;

    /**
     * Processes gamification for scenario completion
     */
    public List<UserBadge> processScenarioCompletion(Long userId, GitScenario scenario, GitUserProgress progress, int pointsEarned) {
        log.info("Processing gamification for scenario completion - user: {}, scenario: {}", userId, scenario.getScenarioId());
        
        Map<String, Object> metadata = createScenarioCompletionMetadata(scenario, progress, pointsEarned);

        List<UserBadge> newBadges = gamificationService.processUserActivity(
            userId, 
            GamificationService.ActivityType.SCENARIO_COMPLETED, 
            metadata
        );

        if (!newBadges.isEmpty()) {
            log.info("User {} earned {} new badges for completing scenario {}", 
                    userId, newBadges.size(), scenario.getScenarioId());
        }

        return newBadges;
    }

    /**
     * Tracks command execution for gamification
     */
    public List<UserBadge> processCommandExecution(Long userId, String command) {
        log.debug("Processing command execution gamification for user: {} command: {}", userId, command);
        
        Map<String, Object> metadata = createCommandExecutionMetadata(command);

        try {
            // Check for first-time command usage and award badges
            List<UserBadge> newBadges = processFirstTimeCommands(userId, command, metadata);
            
            // Track general command execution
            List<UserBadge> executionBadges = gamificationService.processUserActivity(
                userId, 
                GamificationService.ActivityType.COMMAND_EXECUTED, 
                metadata
            );
            
            newBadges.addAll(executionBadges);
            return newBadges;
            
        } catch (Exception e) {
            log.warn("Failed to track command execution gamification for user: {}", userId, e);
            return List.of();
        }
    }

    /**
     * Processes step completion for gamification
     */
    public void processStepCompletion(Long userId, String scenarioId, int step) {
        log.debug("Processing step completion gamification for user: {} scenario: {} step: {}", userId, scenarioId, step);
        
        Map<String, Object> metadata = createStepCompletionMetadata(scenarioId, step);

        try {
            gamificationService.processUserActivity(
                userId, 
                GamificationService.ActivityType.COMMAND_EXECUTED, 
                metadata
            );
        } catch (Exception e) {
            log.warn("Failed to process step completion gamification for user: {}", userId, e);
        }
    }

    /**
     * Processes streak milestones
     */
    public List<UserBadge> processStreakMilestone(Long userId, int streakDays) {
        log.info("Processing streak milestone for user: {} streak: {} days", userId, streakDays);
        
        Map<String, Object> metadata = createStreakMilestoneMetadata(streakDays);

        try {
            return gamificationService.processUserActivity(
                userId, 
                GamificationService.ActivityType.STREAK_MILESTONE, 
                metadata
            );
        } catch (Exception e) {
            log.warn("Failed to process streak milestone gamification for user: {}", userId, e);
            return List.of();
        }
    }

    /**
     * Gets gamification dashboard for a user
     */
    public GamificationService.GamificationDashboard getGamificationDashboard(Long userId) {
        return gamificationService.getGamificationDashboard(userId);
    }

    /**
     * Gets user's badges
     */
    public List<UserBadge> getUserBadges(Long userId) {
        return gamificationService.getUserBadges(userId);
    }

    // Private helper methods

    private Map<String, Object> createScenarioCompletionMetadata(GitScenario scenario, GitUserProgress progress, int pointsEarned) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("scenarios", 1);
        metadata.put("points", pointsEarned);
        metadata.put("level", scenario.getLevel().toString());
        metadata.put("usedHints", progress.getHintsUsed() > 0);
        metadata.put("estimatedTime", scenario.getEstimatedMinutes() * 60); // Convert to seconds
        
        if (progress.getCompletedAt() != null && progress.getStartedAt() != null) {
            long executionTimeSeconds = java.time.Duration.between(progress.getStartedAt(), progress.getCompletedAt()).getSeconds();
            metadata.put("executionTime", (int) executionTimeSeconds);
        }
        
        return metadata;
    }

    private Map<String, Object> createCommandExecutionMetadata(String command) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("command", command);
        metadata.put("commands", 1);
        return metadata;
    }

    private Map<String, Object> createStepCompletionMetadata(String scenarioId, int step) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("commands", 1);
        metadata.put("scenarioId", scenarioId);
        metadata.put("step", step);
        return metadata;
    }

    private Map<String, Object> createStreakMilestoneMetadata(int streakDays) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("streakDays", streakDays);
        return metadata;
    }

    private List<UserBadge> processFirstTimeCommands(Long userId, String command, Map<String, Object> metadata) {
        List<UserBadge> badges = new java.util.ArrayList<>();
        
        // Check for first-time command usage
        String lowerCommand = command.toLowerCase();
        
        if (lowerCommand.contains("commit") && !hasUsedCommandBefore(userId, "commit")) {
            badges.addAll(gamificationService.processUserActivity(userId, GamificationService.ActivityType.FIRST_COMMIT, metadata));
        }
        if (lowerCommand.contains("branch") && !hasUsedCommandBefore(userId, "branch")) {
            badges.addAll(gamificationService.processUserActivity(userId, GamificationService.ActivityType.FIRST_BRANCH, metadata));
        }
        if (lowerCommand.contains("merge") && !hasUsedCommandBefore(userId, "merge")) {
            badges.addAll(gamificationService.processUserActivity(userId, GamificationService.ActivityType.FIRST_MERGE, metadata));
        }
        if (lowerCommand.contains("rebase") && !hasUsedCommandBefore(userId, "rebase")) {
            badges.addAll(gamificationService.processUserActivity(userId, GamificationService.ActivityType.FIRST_REBASE, metadata));
        }
        if (lowerCommand.contains("cherry-pick") && !hasUsedCommandBefore(userId, "cherry-pick")) {
            badges.addAll(gamificationService.processUserActivity(userId, GamificationService.ActivityType.FIRST_CHERRY_PICK, metadata));
        }
        if (lowerCommand.contains("stash") && !hasUsedCommandBefore(userId, "stash")) {
            badges.addAll(gamificationService.processUserActivity(userId, GamificationService.ActivityType.FIRST_STASH, metadata));
        }
        
        return badges;
    }

    /**
     * Checks if user has used a specific command before
     * This is a simplified implementation - in production, you'd query the GitCommand table
     */
    private boolean hasUsedCommandBefore(Long userId, String commandType) {
        // This is a simplified check - in a real implementation you'd query the GitCommand table
        // For now, we'll use a basic heuristic that ensures first-time badges are awarded once per command type
        return false;
    }

    /**
     * Data transfer object for command execution results
     */
    @lombok.Builder
    @lombok.Data
    public static class GamificationResult {
        private List<UserBadge> newBadges;
        private boolean streakUpdated;
        private int pointsAwarded;
        private String achievementMessage;
    }
}
