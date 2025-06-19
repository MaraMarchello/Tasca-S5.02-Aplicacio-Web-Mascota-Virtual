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

@Entity
@Table(name = "git_user_progress")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class GitUserProgress {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    private Long userId;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "scenario_id", nullable = false)
    private GitScenario scenario;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private GitProgressStatus status = GitProgressStatus.NOT_STARTED;
    
    @Column(nullable = false)
    @Builder.Default
    private Integer currentStep = 0;
    
    @Column(nullable = false)
    @Builder.Default
    private Integer totalSteps = 0;
    
    @Column(nullable = false)
    @Builder.Default
    private Integer commandsExecuted = 0;
    
    @Column(nullable = false)
    @Builder.Default
    private Integer hintsUsed = 0;
    
    @Column(nullable = false)
    @Builder.Default
    private Integer pointsEarned = 0;
    
    @Column
    private LocalDateTime startedAt;
    
    @Column
    private LocalDateTime completedAt;
    
    @Column(columnDefinition = "TEXT")
    private String progressData; // JSON representation of detailed progress
    
    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @LastModifiedDate
    @Column(nullable = false)
    private LocalDateTime updatedAt;
    
    public enum GitProgressStatus {
        NOT_STARTED,
        IN_PROGRESS,
        COMPLETED,
        FAILED,
        ABANDONED
    }
} 