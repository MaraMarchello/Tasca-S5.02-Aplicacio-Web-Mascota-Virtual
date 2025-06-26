package com.codemate.payload.response;

import com.codemate.model.GitScenario;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class GitScenarioResponse {
    private Long id;
    private String scenarioId;
    private String title;
    private String description;
    private GitScenario.GitScenarioLevel level;
    private GitScenario.GitScenarioCategory category;
    private Integer pointsReward;
    private Integer estimatedMinutes;
    private Boolean isActive;
    private Integer orderIndex;
    private List<String> tags;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static GitScenarioResponse from(GitScenario scenario) {
        return GitScenarioResponse.builder()
            .id(scenario.getId())
            .scenarioId(scenario.getScenarioId())
            .title(scenario.getTitle())
            .description(scenario.getDescription())
            .level(scenario.getLevel())
            .category(scenario.getCategory())
            .pointsReward(scenario.getPointsReward())
            .estimatedMinutes(scenario.getEstimatedMinutes())
            .isActive(scenario.getIsActive())
            .orderIndex(scenario.getOrderIndex())
            .tags(scenario.getTags())
            .createdAt(scenario.getCreatedAt())
            .updatedAt(scenario.getUpdatedAt())
            .build();
    }
} 