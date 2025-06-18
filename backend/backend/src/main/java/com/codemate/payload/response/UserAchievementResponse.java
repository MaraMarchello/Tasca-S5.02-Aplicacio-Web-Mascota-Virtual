package com.codemate.payload.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserAchievementResponse {
    private Long id;
    private AchievementResponse achievement;
    private Integer currentProgress;
    private Boolean completed;
    private Date completedAt;
    private Date createdAt;
    
    // Progress percentage (calculated field)
    public Double getProgressPercentage() {
        if (achievement == null || achievement.getTargetValue() == null || achievement.getTargetValue() == 0) {
            return 0.0;
        }
        return Math.min(100.0, (currentProgress * 100.0) / achievement.getTargetValue());
    }
} 