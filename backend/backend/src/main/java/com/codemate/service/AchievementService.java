package com.codemate.service;

import com.codemate.model.Achievement;
import com.codemate.model.UserAchievement;
import com.codemate.model.TransactionType;
import com.codemate.model.PointSource;
import com.codemate.repository.AchievementRepository;
import com.codemate.repository.UserAchievementRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;

@Service
@Transactional
public class AchievementService {
    
    private final AchievementRepository achievementRepository;
    private final UserAchievementRepository userAchievementRepository;
    private final PointTransactionService pointTransactionService;
    
    public AchievementService(AchievementRepository achievementRepository,
                             UserAchievementRepository userAchievementRepository,
                             PointTransactionService pointTransactionService) {
        this.achievementRepository = achievementRepository;
        this.userAchievementRepository = userAchievementRepository;
        this.pointTransactionService = pointTransactionService;
    }
    
    /**
     * Progress an achievement for a user
     */
    public void progressAchievement(Long userId, String achievementCode, int progressIncrement) {
        Achievement achievement = achievementRepository.findByCodeAndActiveTrue(achievementCode)
            .orElse(null);
            
        if (achievement == null) {
            return; // Achievement doesn't exist or is inactive
        }
        
        UserAchievement userAchievement = userAchievementRepository
            .findByUserIdAndAchievementId(userId, achievement.getId())
            .orElseGet(() -> {
                UserAchievement newUA = new UserAchievement();
                newUA.setUserId(userId);
                newUA.setAchievement(achievement);
                return newUA;
            });
        
        if (userAchievement.getCompleted()) {
            return; // Already completed
        }
        
        // Increment progress
        userAchievement.setCurrentProgress(userAchievement.getCurrentProgress() + progressIncrement);
        
        // Check if completed
        if (userAchievement.getCurrentProgress() >= achievement.getTargetValue()) {
            userAchievement.setCompleted(true);
            userAchievement.setCompletedAt(new Date());
            
            // Award points for completing achievement
            pointTransactionService.createTransaction(userId, TransactionType.EARNED,
                PointSource.ACHIEVEMENT_COMPLETED, achievement.getPointsReward(),
                "Completed achievement: " + achievement.getName(),
                achievement.getCode());
        }
        
        userAchievementRepository.save(userAchievement);
    }
    
    /**
     * Get all achievements for a user (completed and in-progress)
     */
    @Transactional(readOnly = true)
    public List<UserAchievement> getUserAchievements(Long userId) {
        return userAchievementRepository.findByUserIdWithAchievement(userId);
    }
    
    /**
     * Get completed achievements for a user
     */
    @Transactional(readOnly = true)
    public List<UserAchievement> getCompletedAchievements(Long userId) {
        return userAchievementRepository.findByUserIdAndCompletedTrue(userId);
    }
    
    /**
     * Get in-progress achievements for a user
     */
    @Transactional(readOnly = true)
    public List<UserAchievement> getInProgressAchievements(Long userId) {
        return userAchievementRepository.findByUserIdAndCompletedFalse(userId);
    }
    
    /**
     * Get all available achievements in the system
     */
    @Transactional(readOnly = true)
    public List<Achievement> getAvailableAchievements() {
        return achievementRepository.findByActiveTrueOrderByName();
    }
    
    /**
     * Get current progress for a specific achievement
     */
    @Transactional(readOnly = true)
    public int getCurrentProgress(Long userId, String achievementCode) {
        return userAchievementRepository.getCurrentProgress(userId, achievementCode)
            .orElse(0);
    }
    
    /**
     * Count completed achievements for a user
     */
    @Transactional(readOnly = true)
    public long getCompletedAchievementCount(Long userId) {
        return userAchievementRepository.countByUserIdAndCompletedTrue(userId);
    }
    
    // Specific achievement tracking methods
    
    /**
     * Track stack trace resolution achievement
     */
    public void trackStackTraceResolution(Long userId) {
        progressAchievement(userId, "PROBLEM_SOLVER", 1);
    }
    
    /**
     * Track AI chat usage achievement
     */
    public void trackAIUsage(Long userId) {
        progressAchievement(userId, "AI_USER", 1);
    }
    
    /**
     * Track pet feeding achievement
     */
    public void trackPetFeeding(Long userId) {
        progressAchievement(userId, "PET_FEEDER", 1);
    }
    
    /**
     * Track shopping achievement
     */
    public void trackShopping(Long userId) {
        progressAchievement(userId, "SHOPPER", 1);
    }
    
    /**
     * Track pet creation achievement
     */
    public void trackPetCreation(Long userId) {
        progressAchievement(userId, "PET_OWNER", 1);
    }
} 