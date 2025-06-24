package com.codemate.controller;

import com.codemate.model.Achievement;
import com.codemate.model.UserAchievement;
import com.codemate.payload.DataResponse;
import com.codemate.payload.ApiResponse;
import com.codemate.payload.response.AchievementResponse;
import com.codemate.payload.response.UserAchievementResponse;
import com.codemate.security.CurrentUser;
import com.codemate.security.UserPrincipal;
import com.codemate.service.AchievementService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/api/achievements")
public class AchievementController {
    
    private final AchievementService achievementService;
    
    public AchievementController(AchievementService achievementService) {
        this.achievementService = achievementService;
    }
    
    @GetMapping("/available")
    public ResponseEntity<DataResponse<List<AchievementResponse>>> getAvailableAchievements() {
        
        log.debug("Getting available achievements");
        
        List<Achievement> achievements = achievementService.getAvailableAchievements();
        List<AchievementResponse> response = achievements.stream()
                .map(this::convertToAchievementResponse)
                .collect(Collectors.toList());
        
        return ResponseEntity.ok(DataResponse.success(response));
    }
    
    @GetMapping("/my-achievements")
    public ResponseEntity<DataResponse<List<UserAchievementResponse>>> getMyAchievements(@CurrentUser UserPrincipal userPrincipal) {
        
        log.debug("Getting achievements for user: {}", userPrincipal.getId());
        
        List<UserAchievement> userAchievements = achievementService.getUserAchievements(userPrincipal.getId());
        List<UserAchievementResponse> response = userAchievements.stream()
                .map(this::convertToUserAchievementResponse)
                .collect(Collectors.toList());
        
        return ResponseEntity.ok(DataResponse.success(response));
    }
    
    @GetMapping("/my-achievements/completed")
    public ResponseEntity<DataResponse<List<UserAchievementResponse>>> getCompletedAchievements(@CurrentUser UserPrincipal userPrincipal) {
        
        log.debug("Getting completed achievements for user: {}", userPrincipal.getId());
        
        List<UserAchievement> userAchievements = achievementService.getCompletedAchievements(userPrincipal.getId());
        List<UserAchievementResponse> response = userAchievements.stream()
                .map(this::convertToUserAchievementResponse)
                .collect(Collectors.toList());
        
        return ResponseEntity.ok(DataResponse.success(response));
    }
    
    @GetMapping("/my-achievements/in-progress")
    public ResponseEntity<DataResponse<List<UserAchievementResponse>>> getInProgressAchievements(@CurrentUser UserPrincipal userPrincipal) {
        
        log.debug("Getting in-progress achievements for user: {}", userPrincipal.getId());
        
        List<UserAchievement> userAchievements = achievementService.getInProgressAchievements(userPrincipal.getId());
        List<UserAchievementResponse> response = userAchievements.stream()
                .map(this::convertToUserAchievementResponse)
                .collect(Collectors.toList());
        
        return ResponseEntity.ok(DataResponse.success(response));
    }
    
    @GetMapping("/stats")
    public ResponseEntity<DataResponse<AchievementStats>> getAchievementStats(@CurrentUser UserPrincipal userPrincipal) {
        
        log.debug("Getting achievement statistics for user: {}", userPrincipal.getId());
        
        long completedCount = achievementService.getCompletedAchievementCount(userPrincipal.getId());
        long totalAvailable = achievementService.getAvailableAchievements().size();
        
        AchievementStats stats = new AchievementStats(completedCount, totalAvailable);
        
        return ResponseEntity.ok(DataResponse.success(stats));
    }
    
    // Git Coach specific achievement tracking endpoints
    
    @PostMapping("/track/git-terminal")
    public ResponseEntity<ApiResponse> trackGitTerminalUsage(@CurrentUser UserPrincipal userPrincipal) {
        log.debug("Tracking Git terminal usage for user: {}", userPrincipal.getId());
        
        try {
            achievementService.trackGitTerminalUsage(userPrincipal.getId());
            return ResponseEntity.ok(new ApiResponse(true, "Git terminal usage tracked successfully"));
        } catch (Exception e) {
            log.error("Failed to track Git terminal usage for user: {}", userPrincipal.getId(), e);
            return ResponseEntity.ok(new ApiResponse(true, "Tracking completed")); // Don't fail the request
        }
    }
    
    @PostMapping("/track/git-visualization")
    public ResponseEntity<ApiResponse> trackGitVisualizationUsage(@CurrentUser UserPrincipal userPrincipal) {
        log.debug("Tracking Git visualization usage for user: {}", userPrincipal.getId());
        
        try {
            achievementService.trackGitVisualizationUsage(userPrincipal.getId());
            return ResponseEntity.ok(new ApiResponse(true, "Git visualization usage tracked successfully"));
        } catch (Exception e) {
            log.error("Failed to track Git visualization usage for user: {}", userPrincipal.getId(), e);
            return ResponseEntity.ok(new ApiResponse(true, "Tracking completed")); // Don't fail the request
        }
    }
    
    // Helper methods
    private AchievementResponse convertToAchievementResponse(Achievement achievement) {
        return new AchievementResponse(
                achievement.getId(),
                achievement.getCode(),
                achievement.getName(),
                achievement.getDescription(),
                achievement.getTargetValue(),
                achievement.getPointsReward(),
                achievement.getBadgeImageUrl(),
                achievement.getActive()
        );
    }
    
    private UserAchievementResponse convertToUserAchievementResponse(UserAchievement userAchievement) {
        AchievementResponse achievementResponse = convertToAchievementResponse(userAchievement.getAchievement());
        return new UserAchievementResponse(
                userAchievement.getId(),
                achievementResponse,
                userAchievement.getCurrentProgress(),
                userAchievement.getCompleted(),
                userAchievement.getCompletedAt(),
                userAchievement.getCreatedAt()
        );
    }
    
    // Inner class for statistics
    public static class AchievementStats {
        private final long completedAchievements;
        private final long totalAchievements;
        private final double completionPercentage;
        
        public AchievementStats(long completedAchievements, long totalAchievements) {
            this.completedAchievements = completedAchievements;
            this.totalAchievements = totalAchievements;
            this.completionPercentage = totalAchievements > 0 ? 
                (completedAchievements * 100.0) / totalAchievements : 0.0;
        }
        
        public long getCompletedAchievements() { return completedAchievements; }
        public long getTotalAchievements() { return totalAchievements; }
        public double getCompletionPercentage() { return completionPercentage; }
    }
} 