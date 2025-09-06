package com.codemate.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "learning_streaks")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LearningStreak {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false, unique = true)
    private Long userId;
    
    @Column(nullable = false)
    @Builder.Default
    private Integer currentStreak = 0;
    
    @Column(nullable = false)
    @Builder.Default
    private Integer longestStreak = 0;
    
    @Column(nullable = false)
    private LocalDate lastActivityDate;
    
    @Column(nullable = false)
    @Builder.Default
    private Integer totalActiveDays = 0;
    
    @Column(nullable = false)
    @Builder.Default
    private Integer scenariosCompletedToday = 0;
    
    @Column(nullable = false)
    @Builder.Default
    private Integer commandsExecutedToday = 0;
    
    @Column(nullable = false)
    @Builder.Default
    private Integer pointsEarnedToday = 0;
    
    @Column(nullable = false)
    private LocalDate streakStartDate;
    
    @CreationTimestamp
    @Column(nullable = false)
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;
    
    // Streak milestones for badge calculations
    @Column(nullable = false)
    @Builder.Default
    private Boolean weekStreakAchieved = false;
    
    @Column(nullable = false)
    @Builder.Default
    private Boolean monthStreakAchieved = false;
    
    @Column(nullable = false)
    @Builder.Default
    private Boolean hundredDayStreakAchieved = false;
    
    /**
     * Updates the streak based on today's activity
     */
    public void updateStreak() {
        LocalDate today = LocalDate.now();
        
        if (lastActivityDate == null) {
            // First activity ever
            currentStreak = 1;
            longestStreak = 1;
            lastActivityDate = today;
            streakStartDate = today;
            totalActiveDays = 1;
        } else if (lastActivityDate.equals(today)) {
            // Already active today, no streak change
            return;
        } else if (lastActivityDate.equals(today.minusDays(1))) {
            // Consecutive day
            currentStreak++;
            if (currentStreak > longestStreak) {
                longestStreak = currentStreak;
            }
            lastActivityDate = today;
            totalActiveDays++;
            resetDailyCounters();
        } else {
            // Streak broken
            currentStreak = 1;
            lastActivityDate = today;
            streakStartDate = today;
            totalActiveDays++;
            resetDailyCounters();
        }
        
        checkStreakMilestones();
    }
    
    /**
     * Resets daily counters for a new day
     */
    private void resetDailyCounters() {
        scenariosCompletedToday = 0;
        commandsExecutedToday = 0;
        pointsEarnedToday = 0;
    }
    
    /**
     * Checks and updates streak milestone achievements
     */
    private void checkStreakMilestones() {
        if (currentStreak >= 7 && !weekStreakAchieved) {
            weekStreakAchieved = true;
        }
        if (currentStreak >= 30 && !monthStreakAchieved) {
            monthStreakAchieved = true;
        }
        if (currentStreak >= 100 && !hundredDayStreakAchieved) {
            hundredDayStreakAchieved = true;
        }
    }
    
    /**
     * Adds activity for today
     */
    public void addTodayActivity(int scenarios, int commands, int points) {
        updateStreak();
        scenariosCompletedToday += scenarios;
        commandsExecutedToday += commands;
        pointsEarnedToday += points;
    }
    
    /**
     * Checks if user is active today
     */
    public boolean isActiveToday() {
        return lastActivityDate != null && lastActivityDate.equals(LocalDate.now());
    }
    
    /**
     * Gets streak status message
     */
    public String getStreakStatusMessage() {
        if (currentStreak == 0) {
            return "Start your learning journey today!";
        } else if (currentStreak == 1) {
            return "Great start! Come back tomorrow to build your streak.";
        } else if (currentStreak < 7) {
            return String.format("🔥 %d day streak! Keep it up!", currentStreak);
        } else if (currentStreak < 30) {
            return String.format("🔥🔥 Amazing %d day streak! You're on fire!", currentStreak);
        } else {
            return String.format("🔥🔥🔥 LEGENDARY %d day streak! Git master in the making!", currentStreak);
        }
    }
    
    /**
     * Gets progress towards next milestone
     */
    public StreakMilestone getNextMilestone() {
        if (currentStreak < 7) {
            return new StreakMilestone(7, "Week Warrior", currentStreak);
        } else if (currentStreak < 30) {
            return new StreakMilestone(30, "Month Master", currentStreak);
        } else if (currentStreak < 100) {
            return new StreakMilestone(100, "Hundred Hero", currentStreak);
        } else {
            return new StreakMilestone(365, "Year Legend", currentStreak);
        }
    }
    
    @Data
    @AllArgsConstructor
    public static class StreakMilestone {
        private int target;
        private String name;
        private int current;
        
        public int getDaysToGo() {
            return Math.max(0, target - current);
        }
        
        public double getProgressPercentage() {
            return Math.min(100.0, (double) current / target * 100);
        }
    }
}
