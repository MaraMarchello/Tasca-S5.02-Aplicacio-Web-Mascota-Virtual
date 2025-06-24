package com.codemate.service;

import com.codemate.model.Achievement;
import com.codemate.model.UserAchievement;
import com.codemate.model.TransactionType;
import com.codemate.model.PointSource;
import com.codemate.repository.AchievementRepository;
import com.codemate.repository.UserAchievementRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;

@Service
@Transactional
public class AchievementService {
    
    private final AchievementRepository achievementRepository;
    private final UserAchievementRepository userAchievementRepository;
    private final PointTransactionService pointTransactionService;
    
    public AchievementService(AchievementRepository achievementRepository,
                             UserAchievementRepository userAchievementRepository,
                             PointTransactionService pointTransactionService) {
        this.achievementRepository = achievementRepository;
        this.userAchievementRepository = userAchievementRepository;
        this.pointTransactionService = pointTransactionService;
    }
    
    /**
     * Progress an achievement for a user
     */
    public void progressAchievement(Long userId, String achievementCode, int progressIncrement) {
        Achievement achievement = achievementRepository.findByCodeAndActiveTrue(achievementCode)
            .orElse(null);
            
        if (achievement == null) {
            return; // Achievement doesn't exist or is inactive
        }
        
        UserAchievement userAchievement = userAchievementRepository
            .findByUserIdAndAchievementId(userId, achievement.getId())
            .orElseGet(() -> {
                UserAchievement newUA = new UserAchievement();
                newUA.setUserId(userId);
                newUA.setAchievement(achievement);
                return newUA;
            });
        
        if (userAchievement.getCompleted()) {
            return; // Already completed
        }
        
        // Increment progress
        userAchievement.setCurrentProgress(userAchievement.getCurrentProgress() + progressIncrement);
        
        // Check if completed
        if (userAchievement.getCurrentProgress() >= achievement.getTargetValue()) {
            userAchievement.setCompleted(true);
            userAchievement.setCompletedAt(new Date());
            
            // Award points for completing achievement
            pointTransactionService.createTransaction(userId, TransactionType.EARNED,
                PointSource.ACHIEVEMENT_COMPLETED, achievement.getPointsReward(),
                "Completed achievement: " + achievement.getName(),
                achievement.getCode());
        }
        
        userAchievementRepository.save(userAchievement);
    }
    
    /**
     * Get all achievements for a user (completed and in-progress)
     */
    @Transactional(readOnly = true)
    public List<UserAchievement> getUserAchievements(Long userId) {
        return userAchievementRepository.findByUserIdWithAchievement(userId);
    }
    
    /**
     * Get completed achievements for a user
     */
    @Transactional(readOnly = true)
    public List<UserAchievement> getCompletedAchievements(Long userId) {
        return userAchievementRepository.findByUserIdAndCompletedTrue(userId);
    }
    
    /**
     * Get in-progress achievements for a user
     */
    @Transactional(readOnly = true)
    public List<UserAchievement> getInProgressAchievements(Long userId) {
        return userAchievementRepository.findByUserIdAndCompletedFalse(userId);
    }
    
    /**
     * Get all available achievements in the system
     */
    @Transactional(readOnly = true)
    public List<Achievement> getAvailableAchievements() {
        return achievementRepository.findByActiveTrueOrderByName();
    }
    
    /**
     * Get current progress for a specific achievement
     */
    @Transactional(readOnly = true)
    public int getCurrentProgress(Long userId, String achievementCode) {
        return userAchievementRepository.getCurrentProgress(userId, achievementCode)
            .orElse(0);
    }
    
    /**
     * Count completed achievements for a user
     */
    @Transactional(readOnly = true)
    public long getCompletedAchievementCount(Long userId) {
        return userAchievementRepository.countByUserIdAndCompletedTrue(userId);
    }
    
    // Specific achievement tracking methods
    
    /**
     * Track stack trace resolution achievement
     */
    public void trackStackTraceResolution(Long userId) {
        progressAchievement(userId, "PROBLEM_SOLVER", 1);
    }
    
    /**
     * Track AI chat usage achievement
     */
    public void trackAIUsage(Long userId) {
        progressAchievement(userId, "AI_USER", 1);
    }
    
    /**
     * Track pet feeding achievement
     */
    public void trackPetFeeding(Long userId) {
        progressAchievement(userId, "PET_FEEDER", 1);
    }
    
    /**
     * Track shopping achievement
     */
    public void trackShopping(Long userId) {
        progressAchievement(userId, "SHOPPER", 1);
    }
    
    /**
     * Track pet creation achievement
     */
    public void trackPetCreation(Long userId) {
        progressAchievement(userId, "PET_OWNER", 1);
    }
    
    // Git-specific achievement tracking methods
    
    /**
     * Track Git scenario completion achievements
     */
    public void trackGitScenarioCompletion(Long userId, String scenarioLevel, String scenarioCategory, boolean usedHints, boolean perfectScore, long completionTimeMinutes) {
        // Basic completion achievements
        progressAchievement(userId, "GIT_FIRST_STEPS", 1);
        progressAchievement(userId, "GIT_PERSISTENT", 1);
        
        // Level-specific achievements
        switch (scenarioLevel.toUpperCase()) {
            case "BEGINNER":
                progressAchievement(userId, "GIT_BEGINNER", 1);
                progressAchievement(userId, "GIT_BASICS_MASTER", 1);
                break;
            case "INTERMEDIATE":
            case "ADVANCED":
                progressAchievement(userId, "GIT_ADVANCED_USER", 1);
                break;
        }
        
        // Category-specific achievements
        switch (scenarioCategory.toUpperCase()) {
            case "BRANCHING":
                progressAchievement(userId, "GIT_BRANCHING_PRO", 1);
                break;
            case "MERGING":
                progressAchievement(userId, "GIT_MERGE_MASTER", 1);
                break;
            case "CONFLICTS":
                progressAchievement(userId, "GIT_CONFLICT_RESOLVER", 1);
                break;
            case "COLLABORATION":
                progressAchievement(userId, "GIT_WORKFLOW_EXPERT", 1);
                break;
        }
        
        // Performance-based achievements
        if (!usedHints) {
            progressAchievement(userId, "GIT_EFFICIENT", 1);
        }
        
        if (perfectScore) {
            progressAchievement(userId, "GIT_PERFECTIONIST", 1);
        }
        
        if (completionTimeMinutes <= 5) {
            progressAchievement(userId, "GIT_SPEED_DEMON", 1);
        }
        
        // Overall mastery
        progressAchievement(userId, "GIT_GURU", 1);
    }
    
    /**
     * Track Git hint usage
     */
    public void trackGitHintUsage(Long userId) {
        progressAchievement(userId, "GIT_HELP_SEEKER", 1);
    }
    
    /**
     * Track Git command usage
     */
    public void trackGitCommandUsage(Long userId, String command) {
        // Track first command execution
        progressAchievement(userId, "GIT_FIRST_COMMAND", 1);
        
        // Track general command usage
        progressAchievement(userId, "GIT_EXPERIMENTER", 1);
        progressAchievement(userId, "GIT_PRACTITIONER", 1);
        progressAchievement(userId, "GIT_CURIOUS", 1);
        
        // Track specific commands for command master achievement
        String[] masterCommands = {"init", "add", "commit", "push", "pull", "merge"};
        for (String masterCommand : masterCommands) {
            if (command.toLowerCase().contains(masterCommand)) {
                progressAchievement(userId, "GIT_COMMAND_MASTER", 1);
                break;
            }
        }
    }
    
    /**
     * Track daily Git learning consistency
     */
    public void trackDailyGitLearning(Long userId) {
        progressAchievement(userId, "GIT_DAILY_LEARNER", 1);
        progressAchievement(userId, "GIT_DEDICATED", 1);
    }
    
    // Simple Git Coach achievement tracking methods for quick wins
    
    /**
     * Track Git Coach page visit
     */
    public void trackGitCoachVisit(Long userId) {
        progressAchievement(userId, "GIT_VISITOR", 1);
    }
    
    /**
     * Track Git terminal interface usage
     */
    public void trackGitTerminalUsage(Long userId) {
        progressAchievement(userId, "GIT_TERMINAL_USER", 1);
    }
    
    /**
     * Track Git visualization interface usage
     */
    public void trackGitVisualizationUsage(Long userId) {
        progressAchievement(userId, "GIT_VISUALIZATION_USER", 1);
    }
    
    /**
     * Track scenario start
     */
    public void trackGitScenarioStart(Long userId) {
        progressAchievement(userId, "GIT_SCENARIO_STARTER", 1);
    }
    
    /**
     * Track scenario step completion
     */
    public void trackGitStepCompletion(Long userId) {
        progressAchievement(userId, "GIT_STUDENT", 1);
    }
    
    // New achievement tracking methods for enhanced gamification
    
    /**
     * Track first login achievement
     */
    public void trackFirstLogin(Long userId) {
        progressAchievement(userId, "FIRST_LOGIN", 1);
    }
    
    /**
     * Track profile visit achievement
     */
    public void trackProfileVisit(Long userId) {
        progressAchievement(userId, "PROFILE_EXPLORER", 1);
    }
    
    /**
     * Track dashboard visit achievement
     */
    public void trackDashboardVisit(Long userId) {
        progressAchievement(userId, "DASHBOARD_VISITOR", 1);
    }
    
    /**
     * Track pet naming achievement
     */
    public void trackPetNaming(Long userId) {
        progressAchievement(userId, "PET_NAMER", 1);
    }
    
    /**
     * Track pet interaction achievement
     */
    public void trackPetInteraction(Long userId) {
        progressAchievement(userId, "PET_INTERACTOR", 1);
    }
    
    /**
     * Track first purchase achievement
     */
    public void trackFirstPurchase(Long userId) {
        progressAchievement(userId, "FIRST_PURCHASE", 1);
    }
    
    /**
     * Track item collection achievement
     */
    public void trackItemCollection(Long userId, int uniqueItemsOwned) {
        if (uniqueItemsOwned >= 3) {
            progressAchievement(userId, "ITEM_COLLECTOR", 1);
        }
        if (uniqueItemsOwned >= 20) {
            progressAchievement(userId, "INVENTORY_MANAGER", 1);
        }
    }
    
    /**
     * Track pet happiness achievements
     */
    public void trackPetHappiness(Long userId, int happiness) {
        if (happiness == 100) {
            progressAchievement(userId, "PET_HAPPINESS_MASTER", 1);
        }
        if (happiness >= 80) {
            progressAchievement(userId, "PET_CARETAKER", 1);
        }
    }
    
    /**
     * Track daily feeding achievement
     */
    public void trackDailyFeeding(Long userId) {
        progressAchievement(userId, "DAILY_FEEDER", 1);
    }
    
    /**
     * Track pet accessory achievements
     */
    public void trackPetAccessory(Long userId, int accessoriesEquipped) {
        if (accessoriesEquipped >= 5) {
            progressAchievement(userId, "PET_FASHIONISTA", 1);
        }
    }
    
    /**
     * Track food variety achievement
     */
    public void trackFoodVariety(Long userId, int uniqueFoodItems) {
        if (uniqueFoodItems >= 10) {
            progressAchievement(userId, "FOOD_CONNOISSEUR", 1);
        }
    }
    
    /**
     * Track quick task completion
     */
    public void trackQuickCompletion(Long userId, long completionTimeSeconds) {
        if (completionTimeSeconds <= 120) { // 2 minutes
            progressAchievement(userId, "QUICK_LEARNER", 1);
        }
    }
    
    /**
     * Track help usage achievement
     */
    public void trackHelpUsage(Long userId) {
        progressAchievement(userId, "HELP_SEEKER", 1);
    }
    
    /**
     * Track activity variety achievement
     */
    public void trackActivityVariety(Long userId, int differentActivities) {
        if (differentActivities >= 3) {
            progressAchievement(userId, "KNOWLEDGE_SEEKER", 1);
        }
        if (differentActivities >= 5) {
            progressAchievement(userId, "FEATURE_TESTER", 1);
        }
    }
    
    /**
     * Track daily usage achievements
     */
    public void trackDailyUsage(Long userId, int consecutiveDays) {
        if (consecutiveDays >= 3) {
            progressAchievement(userId, "CONSISTENT_USER", 1);
        }
        if (consecutiveDays >= 7) {
            progressAchievement(userId, "WEEK_WARRIOR", 1);
        }
        if (consecutiveDays >= 14) {
            progressAchievement(userId, "DEDICATION_MASTER", 1);
        }
        if (consecutiveDays >= 30) {
            progressAchievement(userId, "MONTH_MASTER", 1);
        }
    }
    
    /**
     * Track total usage days
     */
    public void trackTotalUsageDays(Long userId, int totalDays) {
        if (totalDays >= 30) {
            progressAchievement(userId, "MONTH_MASTER", 1);
        }
        if (totalDays >= 60) {
            progressAchievement(userId, "VETERAN_USER", 1);
        }
    }
    
    /**
     * Track AI conversation achievements
     */
    public void trackAIConversation(Long userId) {
        progressAchievement(userId, "AI_CURIOUS", 1);
        progressAchievement(userId, "AI_CONVERSATIONALIST", 1);
        progressAchievement(userId, "AI_POWER_USER", 1);
    }
    
    /**
     * Track code help achievement
     */
    public void trackCodeHelp(Long userId) {
        progressAchievement(userId, "CODE_HELPER", 1);
    }
    
    /**
     * Track first error resolution
     */
    public void trackFirstError(Long userId) {
        progressAchievement(userId, "DEBUGGER", 1);
    }
    
    /**
     * Track error variety achievement
     */
    public void trackErrorVariety(Long userId, int errorTypes) {
        if (errorTypes >= 3) {
            progressAchievement(userId, "ERROR_HUNTER", 1);
        }
    }
    
    /**
     * Track advanced problem solving
     */
    public void trackAdvancedProblemSolving(Long userId, int totalProblems) {
        if (totalProblems >= 25) {
            progressAchievement(userId, "STACK_TRACE_EXPERT", 1);
        }
        if (totalProblems >= 50) {
            progressAchievement(userId, "PROBLEM_CRUSHER", 1);
        }
    }
    
    /**
     * Track section exploration
     */
    public void trackSectionExploration(Long userId, int sectionsVisited) {
        if (sectionsVisited >= 5) { // Assuming 5 main sections
            progressAchievement(userId, "EXPLORER", 1);
        }
    }
    
    /**
     * Track point milestones
     */
    public void trackPointMilestones(Long userId, long totalPoints) {
        if (totalPoints >= 1000) {
            progressAchievement(userId, "POWER_USER", 1);
        }
        if (totalPoints >= 5000) {
            progressAchievement(userId, "POINT_COLLECTOR", 1);
        }
    }
    
    /**
     * Track achievement milestones
     */
    public void trackAchievementMilestones(Long userId, int completedAchievements) {
        if (completedAchievements >= 10) {
            progressAchievement(userId, "ACHIEVEMENT_HUNTER", 1);
        }
        if (completedAchievements >= 25) {
            progressAchievement(userId, "COMPLETIONIST", 1);
        }
    }
    
    /**
     * Track spending achievements
     */
    public void trackSpending(Long userId, long totalSpent, int itemPrice) {
        if (totalSpent >= 500) {
            progressAchievement(userId, "BIG_SPENDER", 1);
        }
        if (itemPrice < 50) {
            progressAchievement(userId, "BARGAIN_HUNTER", 1);
        }
        if (itemPrice > 100) {
            progressAchievement(userId, "PREMIUM_BUYER", 1);
        }
    }
    
    /**
     * Track time-based achievements
     */
    public void trackTimeBasedActivity(Long userId, int hourOfDay, boolean isWeekend) {
        if (hourOfDay < 9) {
            progressAchievement(userId, "EARLY_BIRD", 1);
        }
        if (hourOfDay >= 22) {
            progressAchievement(userId, "NIGHT_OWL", 1);
        }
        if (isWeekend) {
            progressAchievement(userId, "WEEKEND_WARRIOR", 1);
        }
    }
    
    /**
     * Track speed achievements
     */
    public void trackSpeedActivity(Long userId, int actionsCompleted, long timeSpentMinutes) {
        if (actionsCompleted >= 5 && timeSpentMinutes <= 10) {
            progressAchievement(userId, "SPEED_USER", 1);
        }
    }
    
    /**
     * Track total action milestones
     */
    public void trackTotalActions(Long userId, int totalActions) {
        if (totalActions >= 100) {
            progressAchievement(userId, "HUNDRED_CLUB", 1);
        }
    }
} 