package com.codemate.service;

import com.codemate.model.*;
import com.codemate.repository.*;
import com.codemate.util.PerformanceMonitor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class GamificationService {
    
    private final UserBadgeRepository userBadgeRepository;
    private final LearningStreakRepository learningStreakRepository;
    private final GitUserProgressRepository gitUserProgressRepository;
    private final GitCommandRepository gitCommandRepository;
    private final PerformanceMonitor performanceMonitor;
    
    /**
     * Processes user activity and awards appropriate badges
     */
    public List<UserBadge> processUserActivity(Long userId, ActivityType activityType, Map<String, Object> metadata) {
        performanceMonitor.startOperation("gamification_processing", userId, null, null);
        
        List<UserBadge> newBadges = new ArrayList<>();
        
        try {
            // Update learning streak
            updateLearningStreak(userId, activityType, metadata);
            
            // Check and award badges based on activity
            switch (activityType) {
                case FIRST_COMMIT:
                    newBadges.addAll(awardFirstTimeBadges(userId, UserBadge.BadgeType.FIRST_COMMIT));
                    break;
                case FIRST_BRANCH:
                    newBadges.addAll(awardFirstTimeBadges(userId, UserBadge.BadgeType.FIRST_BRANCH));
                    break;
                case FIRST_MERGE:
                    newBadges.addAll(awardFirstTimeBadges(userId, UserBadge.BadgeType.FIRST_MERGE));
                    break;
                case FIRST_REBASE:
                    newBadges.addAll(awardFirstTimeBadges(userId, UserBadge.BadgeType.FIRST_REBASE));
                    break;
                case FIRST_CHERRY_PICK:
                    newBadges.addAll(awardFirstTimeBadges(userId, UserBadge.BadgeType.FIRST_CHERRY_PICK));
                    break;
                case FIRST_STASH:
                    newBadges.addAll(awardFirstTimeBadges(userId, UserBadge.BadgeType.FIRST_STASH));
                    break;
                case SCENARIO_COMPLETED:
                    newBadges.addAll(processScenarioCompletion(userId, metadata));
                    break;
                case COMMAND_EXECUTED:
                    newBadges.addAll(processCommandExecution(userId, metadata));
                    break;
                case STREAK_MILESTONE:
                    newBadges.addAll(processStreakMilestone(userId, metadata));
                    break;
            }
            
            // Check for cumulative milestones
            newBadges.addAll(checkCumulativeMilestones(userId));
            
            long duration = performanceMonitor.endOperation("gamification_processing", userId, null);
            log.debug("Gamification processing completed in {}ms for user: {}, awarded {} badges", 
                     duration, userId, newBadges.size());
            
            return newBadges;
            
        } catch (Exception e) {
            performanceMonitor.endOperation("gamification_processing", userId, null);
            log.error("Error processing gamification for user: {}", userId, e);
            throw e;
        }
    }
    
    /**
     * Updates learning streak for user activity
     */
    private void updateLearningStreak(Long userId, ActivityType activityType, Map<String, Object> metadata) {
        LearningStreak streak = learningStreakRepository.findByUserId(userId)
            .orElse(LearningStreak.builder()
                .userId(userId)
                .currentStreak(0)
                .longestStreak(0)
                .totalActiveDays(0)
                .lastActivityDate(LocalDate.now().minusDays(1)) // Force update on first activity
                .streakStartDate(LocalDate.now())
                .build());
        
        // Add today's activity
        int scenarios = (Integer) metadata.getOrDefault("scenarios", 0);
        int commands = (Integer) metadata.getOrDefault("commands", 1);
        int points = (Integer) metadata.getOrDefault("points", 0);
        
        streak.addTodayActivity(scenarios, commands, points);
        learningStreakRepository.save(streak);
        
        log.debug("Updated learning streak for user: {}, current streak: {}", userId, streak.getCurrentStreak());
    }
    
    /**
     * Awards first-time achievement badges
     */
    private List<UserBadge> awardFirstTimeBadges(Long userId, UserBadge.BadgeType badgeType) {
        List<UserBadge> badges = new ArrayList<>();
        
        if (!userBadgeRepository.existsByUserIdAndBadgeType(userId, badgeType)) {
            UserBadge badge = createBadge(userId, badgeType);
            userBadgeRepository.save(badge);
            badges.add(badge);
            log.info("Awarded first-time badge: {} to user: {}", badgeType, userId);
        }
        
        return badges;
    }
    
    /**
     * Processes scenario completion for badges
     */
    private List<UserBadge> processScenarioCompletion(Long userId, Map<String, Object> metadata) {
        List<UserBadge> badges = new ArrayList<>();
        
        String scenarioLevel = (String) metadata.get("level");
        Boolean usedHints = (Boolean) metadata.getOrDefault("usedHints", false);
        Integer executionTime = (Integer) metadata.get("executionTime");
        Integer estimatedTime = (Integer) metadata.get("estimatedTime");
        
        // Perfectionist badge (no hints used)
        if (!usedHints && !userBadgeRepository.existsByUserIdAndBadgeType(userId, UserBadge.BadgeType.PERFECTIONIST)) {
            badges.add(awardBadge(userId, UserBadge.BadgeType.PERFECTIONIST));
        }
        
        // Speed demon badge (completed under estimated time)
        if (executionTime != null && estimatedTime != null && 
            executionTime < estimatedTime && 
            !userBadgeRepository.existsByUserIdAndBadgeType(userId, UserBadge.BadgeType.SPEED_DEMON)) {
            badges.add(awardBadge(userId, UserBadge.BadgeType.SPEED_DEMON));
        }
        
        // Level-based completion badges
        badges.addAll(checkLevelCompletionBadges(userId));
        
        return badges;
    }
    
    /**
     * Processes command execution for badges
     */
    private List<UserBadge> processCommandExecution(Long userId, Map<String, Object> metadata) {
        List<UserBadge> badges = new ArrayList<>();
        
        String command = (String) metadata.get("command");
        
        // Check for advanced command usage
        if (isAdvancedCommand(command)) {
            if (!userBadgeRepository.existsByUserIdAndBadgeType(userId, UserBadge.BadgeType.EXPLORER)) {
                badges.add(awardBadge(userId, UserBadge.BadgeType.EXPLORER));
            }
        }
        
        return badges;
    }
    
    /**
     * Processes streak milestones
     */
    private List<UserBadge> processStreakMilestone(Long userId, Map<String, Object> metadata) {
        List<UserBadge> badges = new ArrayList<>();
        
        Integer streakDays = (Integer) metadata.get("streakDays");
        
        if (streakDays != null) {
            if (streakDays >= 7 && !userBadgeRepository.existsByUserIdAndBadgeType(userId, UserBadge.BadgeType.DAILY_LEARNER)) {
                badges.add(awardBadge(userId, UserBadge.BadgeType.DAILY_LEARNER));
            }
            if (streakDays >= 30 && !userBadgeRepository.existsByUserIdAndBadgeType(userId, UserBadge.BadgeType.WEEKLY_WARRIOR)) {
                badges.add(awardBadge(userId, UserBadge.BadgeType.WEEKLY_WARRIOR));
            }
            if (streakDays >= 100 && !userBadgeRepository.existsByUserIdAndBadgeType(userId, UserBadge.BadgeType.MONTHLY_MASTER)) {
                badges.add(awardBadge(userId, UserBadge.BadgeType.MONTHLY_MASTER));
            }
        }
        
        return badges;
    }
    
    /**
     * Checks cumulative milestone badges
     */
    private List<UserBadge> checkCumulativeMilestones(Long userId) {
        List<UserBadge> badges = new ArrayList<>();
        
        // Check points milestones
        Long totalPoints = userBadgeRepository.sumPointsByUserId(userId);
        if (totalPoints != null) {
            if (totalPoints >= 1000 && !userBadgeRepository.existsByUserIdAndBadgeType(userId, UserBadge.BadgeType.POINTS_COLLECTOR)) {
                badges.add(awardBadge(userId, UserBadge.BadgeType.POINTS_COLLECTOR));
            }
            if (totalPoints >= 5000 && !userBadgeRepository.existsByUserIdAndBadgeType(userId, UserBadge.BadgeType.POINTS_HOARDER)) {
                badges.add(awardBadge(userId, UserBadge.BadgeType.POINTS_HOARDER));
            }
            if (totalPoints >= 10000 && !userBadgeRepository.existsByUserIdAndBadgeType(userId, UserBadge.BadgeType.POINTS_LEGEND)) {
                badges.add(awardBadge(userId, UserBadge.BadgeType.POINTS_LEGEND));
            }
        }
        
        return badges;
    }
    
    /**
     * Checks level completion badges
     */
    private List<UserBadge> checkLevelCompletionBadges(Long userId) {
        List<UserBadge> badges = new ArrayList<>();
        
        // Count completed scenarios by level
        List<GitUserProgress> completedProgress = gitUserProgressRepository.findCompletedScenariosByUser(userId);
        
        Map<GitScenario.GitScenarioLevel, Long> completionsByLevel = completedProgress.stream()
            .collect(Collectors.groupingBy(
                progress -> progress.getScenario().getLevel(),
                Collectors.counting()
            ));
        
        // Get total scenarios by level (this would need to be implemented)
        // For now, using hardcoded values
        if (completionsByLevel.getOrDefault(GitScenario.GitScenarioLevel.BEGINNER, 0L) >= 2 &&
            !userBadgeRepository.existsByUserIdAndBadgeType(userId, UserBadge.BadgeType.BEGINNER_GRADUATE)) {
            badges.add(awardBadge(userId, UserBadge.BadgeType.BEGINNER_GRADUATE));
        }
        
        if (completionsByLevel.getOrDefault(GitScenario.GitScenarioLevel.INTERMEDIATE, 0L) >= 3 &&
            !userBadgeRepository.existsByUserIdAndBadgeType(userId, UserBadge.BadgeType.INTERMEDIATE_ACE)) {
            badges.add(awardBadge(userId, UserBadge.BadgeType.INTERMEDIATE_ACE));
        }
        
        if (completionsByLevel.getOrDefault(GitScenario.GitScenarioLevel.ADVANCED, 0L) >= 3 &&
            !userBadgeRepository.existsByUserIdAndBadgeType(userId, UserBadge.BadgeType.ADVANCED_EXPERT)) {
            badges.add(awardBadge(userId, UserBadge.BadgeType.ADVANCED_EXPERT));
        }
        
        return badges;
    }
    
    /**
     * Awards a specific badge to a user
     */
    private UserBadge awardBadge(Long userId, UserBadge.BadgeType badgeType) {
        UserBadge badge = createBadge(userId, badgeType);
        userBadgeRepository.save(badge);
        log.info("Awarded badge: {} to user: {}", badgeType, userId);
        return badge;
    }
    
    /**
     * Creates a badge instance with appropriate metadata
     */
    private UserBadge createBadge(Long userId, UserBadge.BadgeType badgeType) {
        BadgeMetadata metadata = getBadgeMetadata(badgeType);
        
        return UserBadge.builder()
            .userId(userId)
            .badgeType(badgeType)
            .badgeName(metadata.name)
            .description(metadata.description)
            .iconUrl(metadata.iconUrl)
            .rarity(metadata.rarity)
            .pointsAwarded(metadata.points)
            .criteria(metadata.criteria)
            .isVisible(true)
            .build();
    }
    
    /**
     * Gets badge metadata for different badge types
     */
    private BadgeMetadata getBadgeMetadata(UserBadge.BadgeType badgeType) {
        switch (badgeType) {
            case FIRST_COMMIT:
                return new BadgeMetadata("First Commit", "Made your first commit!", "/badges/first-commit.png", 
                                       UserBadge.BadgeRarity.COMMON, 10, "Make your first commit");
            case FIRST_BRANCH:
                return new BadgeMetadata("Branch Explorer", "Created your first branch!", "/badges/first-branch.png", 
                                       UserBadge.BadgeRarity.COMMON, 15, "Create your first branch");
            case FIRST_MERGE:
                return new BadgeMetadata("Merge Master", "Completed your first merge!", "/badges/first-merge.png", 
                                       UserBadge.BadgeRarity.UNCOMMON, 25, "Complete your first merge");
            case FIRST_REBASE:
                return new BadgeMetadata("Rebase Rookie", "Performed your first rebase!", "/badges/first-rebase.png", 
                                       UserBadge.BadgeRarity.RARE, 40, "Perform your first rebase");
            case FIRST_CHERRY_PICK:
                return new BadgeMetadata("Cherry Picker", "Made your first cherry-pick!", "/badges/first-cherry-pick.png", 
                                       UserBadge.BadgeRarity.RARE, 35, "Perform your first cherry-pick");
            case FIRST_STASH:
                return new BadgeMetadata("Stash Saver", "Used stash for the first time!", "/badges/first-stash.png", 
                                       UserBadge.BadgeRarity.UNCOMMON, 20, "Use stash for the first time");
            case DAILY_LEARNER:
                return new BadgeMetadata("Daily Learner", "7-day learning streak!", "/badges/daily-learner.png", 
                                       UserBadge.BadgeRarity.UNCOMMON, 50, "Maintain a 7-day learning streak");
            case WEEKLY_WARRIOR:
                return new BadgeMetadata("Weekly Warrior", "30-day learning streak!", "/badges/weekly-warrior.png", 
                                       UserBadge.BadgeRarity.RARE, 150, "Maintain a 30-day learning streak");
            case MONTHLY_MASTER:
                return new BadgeMetadata("Monthly Master", "100-day learning streak!", "/badges/monthly-master.png", 
                                       UserBadge.BadgeRarity.LEGENDARY, 500, "Maintain a 100-day learning streak");
            case PERFECTIONIST:
                return new BadgeMetadata("Perfectionist", "Completed scenario without hints!", "/badges/perfectionist.png", 
                                       UserBadge.BadgeRarity.EPIC, 75, "Complete a scenario without using hints");
            case SPEED_DEMON:
                return new BadgeMetadata("Speed Demon", "Completed scenario under time limit!", "/badges/speed-demon.png", 
                                       UserBadge.BadgeRarity.EPIC, 100, "Complete a scenario faster than estimated time");
            case EXPLORER:
                return new BadgeMetadata("Explorer", "Used advanced Git commands!", "/badges/explorer.png", 
                                       UserBadge.BadgeRarity.RARE, 60, "Use advanced Git commands");
            case BEGINNER_GRADUATE:
                return new BadgeMetadata("Beginner Graduate", "Completed all beginner scenarios!", "/badges/beginner-graduate.png", 
                                       UserBadge.BadgeRarity.UNCOMMON, 100, "Complete all beginner scenarios");
            case INTERMEDIATE_ACE:
                return new BadgeMetadata("Intermediate Ace", "Completed all intermediate scenarios!", "/badges/intermediate-ace.png", 
                                       UserBadge.BadgeRarity.RARE, 200, "Complete all intermediate scenarios");
            case ADVANCED_EXPERT:
                return new BadgeMetadata("Advanced Expert", "Completed all advanced scenarios!", "/badges/advanced-expert.png", 
                                       UserBadge.BadgeRarity.EPIC, 300, "Complete all advanced scenarios");
            case POINTS_COLLECTOR:
                return new BadgeMetadata("Points Collector", "Earned 1,000 points!", "/badges/points-collector.png", 
                                       UserBadge.BadgeRarity.UNCOMMON, 50, "Earn 1,000 total points");
            case POINTS_HOARDER:
                return new BadgeMetadata("Points Hoarder", "Earned 5,000 points!", "/badges/points-hoarder.png", 
                                       UserBadge.BadgeRarity.RARE, 100, "Earn 5,000 total points");
            case POINTS_LEGEND:
                return new BadgeMetadata("Points Legend", "Earned 10,000 points!", "/badges/points-legend.png", 
                                       UserBadge.BadgeRarity.LEGENDARY, 200, "Earn 10,000 total points");
            default:
                return new BadgeMetadata("Unknown Badge", "Mystery achievement!", "/badges/default.png", 
                                       UserBadge.BadgeRarity.COMMON, 10, "Unknown criteria");
        }
    }
    
    private boolean isAdvancedCommand(String command) {
        return command != null && (
            command.contains("rebase") ||
            command.contains("cherry-pick") ||
            command.contains("stash") ||
            command.contains("reset --hard") ||
            command.contains("reflog") ||
            command.contains("bisect")
        );
    }
    
    public LearningStreak getUserStreak(Long userId) {
        return learningStreakRepository.findByUserId(userId)
            .orElse(LearningStreak.builder()
                .userId(userId)
                .currentStreak(0)
                .longestStreak(0)
                .totalActiveDays(0)
                .build());
    }
    
    public List<UserBadge> getUserBadges(Long userId) {
        return userBadgeRepository.findByUserIdAndIsVisibleTrueOrderByEarnedAtDesc(userId);
    }
    
    public GamificationDashboard getGamificationDashboard(Long userId) {
        LearningStreak streak = getUserStreak(userId);
        List<UserBadge> badges = getUserBadges(userId);
        
        Long totalPoints = userBadgeRepository.sumPointsByUserId(userId);
        Long badgeCount = userBadgeRepository.countByUserId(userId);
        
        return GamificationDashboard.builder()
            .currentStreak(streak.getCurrentStreak())
            .longestStreak(streak.getLongestStreak())
            .totalActiveDays(streak.getTotalActiveDays())
            .streakStatusMessage(streak.getStreakStatusMessage())
            .nextMilestone(streak.getNextMilestone())
            .totalPoints(totalPoints != null ? totalPoints : 0L)
            .totalBadges(badgeCount != null ? badgeCount : 0L)
            .recentBadges(badges.stream().limit(5).collect(Collectors.toList()))
            .todayStats(DailyStats.builder()
                .scenariosCompleted(streak.getScenariosCompletedToday())
                .commandsExecuted(streak.getCommandsExecutedToday())
                .pointsEarned(streak.getPointsEarnedToday())
                .build())
            .build();
    }
    
    public enum ActivityType {
        FIRST_COMMIT,
        FIRST_BRANCH,
        FIRST_MERGE,
        FIRST_REBASE,
        FIRST_CHERRY_PICK,
        FIRST_STASH,
        SCENARIO_COMPLETED,
        COMMAND_EXECUTED,
        STREAK_MILESTONE
    }
    
    @lombok.Data
    @lombok.AllArgsConstructor
    private static class BadgeMetadata {
        private String name;
        private String description;
        private String iconUrl;
        private UserBadge.BadgeRarity rarity;
        private Integer points;
        private String criteria;
    }
    
    @lombok.Data
    @lombok.Builder
    public static class GamificationDashboard {
        private Integer currentStreak;
        private Integer longestStreak;
        private Integer totalActiveDays;
        private String streakStatusMessage;
        private LearningStreak.StreakMilestone nextMilestone;
        private Long totalPoints;
        private Long totalBadges;
        private List<UserBadge> recentBadges;
        private DailyStats todayStats;
    }
    
    @lombok.Data
    @lombok.Builder
    public static class DailyStats {
        private Integer scenariosCompleted;
        private Integer commandsExecuted;
        private Integer pointsEarned;
    }
}
