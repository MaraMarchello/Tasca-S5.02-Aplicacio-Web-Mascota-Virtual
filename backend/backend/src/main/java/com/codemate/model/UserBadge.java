package com.codemate.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "user_badges")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserBadge {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    private Long userId;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BadgeType badgeType;
    
    @Column(nullable = false)
    private String badgeName;
    
    @Column(length = 500)
    private String description;
    
    @Column(nullable = false)
    private String iconUrl;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BadgeRarity rarity;
    
    @Column(nullable = false)
    private Integer pointsAwarded;
    
    @CreationTimestamp
    @Column(nullable = false)
    private LocalDateTime earnedAt;
    
    @Column(length = 1000)
    private String criteria;
    
    @Column(nullable = false)
    @Builder.Default
    private Boolean isVisible = true;
    
    public enum BadgeType {
        // Learning Badges
        FIRST_COMMIT,
        FIRST_BRANCH,
        FIRST_MERGE,
        FIRST_REBASE,
        FIRST_CHERRY_PICK,
        FIRST_STASH,
        
        // Mastery Badges
        COMMIT_MASTER,      // 100 commits
        BRANCH_NINJA,       // 50 branches created
        MERGE_EXPERT,       // 25 successful merges
        CONFLICT_RESOLVER,  // 10 conflicts resolved
        REBASE_GURU,        // 10 interactive rebases
        
        // Streak Badges
        DAILY_LEARNER,      // 7 days streak
        WEEKLY_WARRIOR,     // 30 days streak
        MONTHLY_MASTER,     // 100 days streak
        
        // Scenario Badges
        BEGINNER_GRADUATE,  // Complete all beginner scenarios
        INTERMEDIATE_ACE,   // Complete all intermediate scenarios
        ADVANCED_EXPERT,    // Complete all advanced scenarios
        SCENARIO_COMPLETIONIST, // Complete all scenarios
        
        // Speed Badges
        SPEED_DEMON,        // Complete scenario under time limit
        EFFICIENCY_EXPERT,  // Complete scenario with minimal commands
        
        // Special Badges
        EARLY_ADOPTER,      // First 100 users
        PERFECTIONIST,      // Complete scenario without hints
        HELPER,             // Use help system effectively
        EXPLORER,           // Try advanced commands
        
        // Milestone Badges
        POINTS_COLLECTOR,   // 1000 points
        POINTS_HOARDER,     // 5000 points
        POINTS_LEGEND,      // 10000 points
        
        // Social Badges (for future collaborative features)
        TEAM_PLAYER,        // Collaborate on scenarios
        MENTOR,             // Help other users
        
        // Easter Egg Badges
        COMMAND_DISCOVERER, // Find hidden commands
        SECRET_ACHIEVEMENT // Special accomplishments
    }
    
    public enum BadgeRarity {
        COMMON,
        UNCOMMON,
        RARE,
        EPIC,
        LEGENDARY
    }
}
