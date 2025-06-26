package com.codemate.payload.response;

import com.codemate.model.GitScenario;
import com.codemate.model.GitUserProgress;
import com.codemate.service.GitScenarioService;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Data
@Builder
public class GitDashboardResponse {
    private Long userId;
    private GitScenarioService.GitUserStats userStats;
    private Long currentPoints;
    private Long totalPointsEarned;
    private Integer completedScenarios;
    private Integer inProgressScenarios;
    private Integer totalAchievements;
    private Integer completedAchievements;
    private Map<GitScenario.GitScenarioCategory, CategoryProgress> categoryProgress;
    private Map<GitScenario.GitScenarioLevel, LevelProgress> levelProgress;
    private Integer learningStreak;
    private List<RecentActivity> recentActivity;
    private List<GitScenarioResponse> recommendedScenarios;
    private List<GitUserProgressResponse> recentCompletedScenarios;

    @Data
    @Builder
    public static class CategoryProgress {
        private GitScenario.GitScenarioCategory category;
        private Integer totalScenarios;
        private Integer completedScenarios;
        private Double completionPercentage;
    }

    @Data
    @Builder
    public static class LevelProgress {
        private GitScenario.GitScenarioLevel level;
        private Integer totalScenarios;
        private Integer completedScenarios;
        private Double completionPercentage;
    }

    @Data
    @Builder
    public static class RecentActivity {
        private String scenarioTitle;
        private LocalDateTime completedAt;
        private Integer pointsEarned;
        private GitScenario.GitScenarioCategory category;
        private GitScenario.GitScenarioLevel level;
    }

    @Data
    @Builder
    public static class GitLearningStats {
        private Integer totalScenarios;
        private Long totalUsers;
        private Long totalCompletions;
        private Double averageCompletionTime;
        private List<GitScenarioResponse> popularScenarios;
    }

    @Data
    @Builder
    public static class GitUserProgressResponse {
        private Long id;
        private String scenarioId;
        private String scenarioTitle;
        private GitUserProgress.GitProgressStatus status;
        private Integer currentStep;
        private Integer totalSteps;
        private Integer pointsEarned;
        private Integer commandsExecuted;
        private Integer hintsUsed;
        private LocalDateTime startedAt;
        private LocalDateTime completedAt;
        private LocalDateTime updatedAt;

        public static GitUserProgressResponse from(GitUserProgress progress) {
            return GitUserProgressResponse.builder()
                .id(progress.getId())
                .scenarioId(progress.getScenario().getScenarioId())
                .scenarioTitle(progress.getScenario().getTitle())
                .status(progress.getStatus())
                .currentStep(progress.getCurrentStep())
                .totalSteps(progress.getTotalSteps())
                .pointsEarned(progress.getPointsEarned())
                .commandsExecuted(progress.getCommandsExecuted())
                .hintsUsed(progress.getHintsUsed())
                .startedAt(progress.getStartedAt())
                .completedAt(progress.getCompletedAt())
                .updatedAt(progress.getUpdatedAt())
                .build();
        }
    }
} 