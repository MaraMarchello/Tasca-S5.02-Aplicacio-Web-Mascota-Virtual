package com.codemate.service;

import com.codemate.model.GitScenario;
import com.codemate.model.GitUserProgress;
import com.codemate.model.UserAchievement;
import com.codemate.payload.response.GitDashboardResponse;
import com.codemate.payload.response.GitScenarioResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GitDashboardService {

    private final GitScenarioService gitScenarioService;
    private final AchievementService achievementService;
    private final PointTransactionService pointTransactionService;

    /**
     * Gets comprehensive dashboard data for a user
     */
    public GitDashboardResponse getDashboardData(Long userId) {
        log.info("Getting Git dashboard data for user: {}", userId);

        // Get user progress and stats
        GitScenarioService.GitUserStats userStats = gitScenarioService.getUserStats(userId);
        List<GitUserProgress> userProgress = gitScenarioService.getUserProgress(userId);
        List<GitUserProgress> completedScenarios = gitScenarioService.getCompletedScenarios(userId);
        List<GitUserProgress> inProgressScenarios = gitScenarioService.getInProgressScenarios(userId);

        // Get achievement data
        List<UserAchievement> allAchievements = achievementService.getUserAchievements(userId);
        List<UserAchievement> completedAchievements = achievementService.getCompletedAchievements(userId);
        List<UserAchievement> inProgressAchievements = achievementService.getInProgressAchievements(userId);

        // Get point data
        Long currentPoints = pointTransactionService.getCurrentPoints(userId);
        Long totalPointsEarned = pointTransactionService.getTotalPointsEarned(userId);

        // Calculate progress by category and level
        Map<GitScenario.GitScenarioCategory, GitDashboardResponse.CategoryProgress> categoryProgress = calculateCategoryProgress(userProgress);
        Map<GitScenario.GitScenarioLevel, GitDashboardResponse.LevelProgress> levelProgress = calculateLevelProgress(userProgress);

        // Calculate learning streak and recent activity
        int learningStreak = calculateLearningStreak(completedScenarios);
        List<GitDashboardResponse.RecentActivity> recentActivity = calculateRecentActivity(userProgress);

        // Get recommended next scenarios
        List<GitScenarioResponse> recommendedScenarios = getRecommendedScenarios(userId, userProgress);

        return GitDashboardResponse.builder()
            .userId(userId)
            .userStats(userStats)
            .currentPoints(currentPoints)
            .totalPointsEarned(totalPointsEarned)
            .completedScenarios(completedScenarios.size())
            .inProgressScenarios(inProgressScenarios.size())
            .totalAchievements(allAchievements.size())
            .completedAchievements(completedAchievements.size())
            .categoryProgress(categoryProgress)
            .levelProgress(levelProgress)
            .learningStreak(learningStreak)
            .recentActivity(recentActivity)
            .recommendedScenarios(recommendedScenarios)
            .recentCompletedScenarios(completedScenarios.stream()
                .filter(p -> p.getCompletedAt() != null && 
                           p.getCompletedAt().isAfter(LocalDateTime.now().minusDays(7)))
                .map(GitDashboardResponse.GitUserProgressResponse::from)
                .collect(Collectors.toList()))
            .build();
    }

    /**
     * Gets Git learning statistics for admin dashboard
     */
    public GitDashboardResponse.GitLearningStats getGitLearningStats() {
        log.info("Getting overall Git learning statistics");

        List<GitScenario> allScenarios = gitScenarioService.getAllActiveScenarios();
        
        // This would require additional repository methods for global stats
        return GitDashboardResponse.GitLearningStats.builder()
            .totalScenarios(allScenarios.size())
            .totalUsers(0L) // Would need user count query
            .totalCompletions(0L) // Would need completion count query
            .averageCompletionTime(0.0) // Would need average calculation
            .popularScenarios(List.of()) // Would need popularity query
            .build();
    }

    // Private helper methods

    private Map<GitScenario.GitScenarioCategory, GitDashboardResponse.CategoryProgress> calculateCategoryProgress(List<GitUserProgress> userProgress) {
        return userProgress.stream()
            .collect(Collectors.groupingBy(
                p -> p.getScenario().getCategory(),
                Collectors.collectingAndThen(
                    Collectors.toList(),
                    progresses -> {
                        long completed = progresses.stream()
                            .mapToLong(p -> p.getStatus() == GitUserProgress.GitProgressStatus.COMPLETED ? 1 : 0)
                            .sum();
                        return GitDashboardResponse.CategoryProgress.builder()
                            .category(progresses.get(0).getScenario().getCategory())
                            .totalScenarios(progresses.size())
                            .completedScenarios((int) completed)
                            .completionPercentage((double) completed / progresses.size() * 100)
                            .build();
                    }
                )
            ));
    }

    private Map<GitScenario.GitScenarioLevel, GitDashboardResponse.LevelProgress> calculateLevelProgress(List<GitUserProgress> userProgress) {
        return userProgress.stream()
            .collect(Collectors.groupingBy(
                p -> p.getScenario().getLevel(),
                Collectors.collectingAndThen(
                    Collectors.toList(),
                    progresses -> {
                        long completed = progresses.stream()
                            .mapToLong(p -> p.getStatus() == GitUserProgress.GitProgressStatus.COMPLETED ? 1 : 0)
                            .sum();
                        return GitDashboardResponse.LevelProgress.builder()
                            .level(progresses.get(0).getScenario().getLevel())
                            .totalScenarios(progresses.size())
                            .completedScenarios((int) completed)
                            .completionPercentage((double) completed / progresses.size() * 100)
                            .build();
                    }
                )
            ));
    }

    private int calculateLearningStreak(List<GitUserProgress> completedScenarios) {
        if (completedScenarios.isEmpty()) {
            return 0;
        }

        // Sort by completion date
        List<LocalDate> completionDates = completedScenarios.stream()
            .filter(p -> p.getCompletedAt() != null)
            .map(p -> p.getCompletedAt().toLocalDate())
            .distinct()
            .sorted()
            .collect(Collectors.toList());

        if (completionDates.isEmpty()) {
            return 0;
        }

        // Calculate consecutive days from the most recent date
        LocalDate today = LocalDate.now();
        LocalDate yesterday = today.minusDays(1);
        
        // Check if user has activity today or yesterday
        if (!completionDates.contains(today) && !completionDates.contains(yesterday)) {
            return 0;
        }

        int streak = 0;
        LocalDate currentDate = completionDates.contains(today) ? today : yesterday;
        
        for (int i = completionDates.size() - 1; i >= 0; i--) {
            if (completionDates.get(i).equals(currentDate)) {
                streak++;
                currentDate = currentDate.minusDays(1);
            } else if (completionDates.get(i).isBefore(currentDate)) {
                break;
            }
        }

        return streak;
    }

    private List<GitDashboardResponse.RecentActivity> calculateRecentActivity(List<GitUserProgress> userProgress) {
        return userProgress.stream()
            .filter(p -> p.getCompletedAt() != null && 
                       p.getCompletedAt().isAfter(LocalDateTime.now().minusDays(30)))
            .map(p -> GitDashboardResponse.RecentActivity.builder()
                .scenarioTitle(p.getScenario().getTitle())
                .completedAt(p.getCompletedAt())
                .pointsEarned(p.getPointsEarned())
                .category(p.getScenario().getCategory())
                .level(p.getScenario().getLevel())
                .build())
            .sorted((a, b) -> b.getCompletedAt().compareTo(a.getCompletedAt()))
            .limit(10)
            .collect(Collectors.toList());
    }

    private List<GitScenarioResponse> getRecommendedScenarios(Long userId, List<GitUserProgress> userProgress) {
        // Get all scenarios
        List<GitScenario> allScenarios = gitScenarioService.getAllActiveScenarios();
        
        // Get completed scenario IDs
        List<String> completedScenarioIds = userProgress.stream()
            .filter(p -> p.getStatus() == GitUserProgress.GitProgressStatus.COMPLETED)
            .map(p -> p.getScenario().getScenarioId())
            .collect(Collectors.toList());

        // Filter out completed scenarios and return recommendations
        return allScenarios.stream()
            .filter(s -> !completedScenarioIds.contains(s.getScenarioId()))
            .limit(5)
            .map(GitScenarioResponse::from)
            .collect(Collectors.toList());
    }

} 