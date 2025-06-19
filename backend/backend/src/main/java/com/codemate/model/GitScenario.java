package com.codemate.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "git_scenarios")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class GitScenario {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false, unique = true)
    private String scenarioId;
    
    @Column(nullable = false)
    private String title;
    
    @Column(columnDefinition = "TEXT")
    private String description;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private GitScenarioLevel level;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private GitScenarioCategory category;
    
    @Column(columnDefinition = "TEXT")
    private String initialState; // JSON representation of initial repository state
    
    @Column(columnDefinition = "TEXT")
    private String expectedCommands; // JSON array of expected commands
    
    @Column(columnDefinition = "TEXT")
    private String successCriteria; // JSON representation of success criteria
    
    @Column(nullable = false)
    @Builder.Default
    private Integer pointsReward = 0;
    
    @Column(nullable = false)
    @Builder.Default
    private Integer estimatedMinutes = 10;
    
    @Column(nullable = false)
    @Builder.Default
    private Boolean isActive = true;
    
    @Column(nullable = false)
    @Builder.Default
    private Integer orderIndex = 0;
    
    @ElementCollection
    @CollectionTable(name = "git_scenario_tags", joinColumns = @JoinColumn(name = "scenario_id"))
    @Column(name = "tag")
    private List<String> tags;
    
    @OneToMany(mappedBy = "scenario", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<GitUserProgress> userProgress;
    
    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @LastModifiedDate
    @Column(nullable = false)
    private LocalDateTime updatedAt;
    
    public enum GitScenarioLevel {
        BEGINNER,
        INTERMEDIATE,
        ADVANCED,
        EXPERT
    }
    
    public enum GitScenarioCategory {
        BASICS,
        BRANCHING,
        MERGING,
        CONFLICTS,
        COLLABORATION,
        ADVANCED_WORKFLOWS
    }
} 