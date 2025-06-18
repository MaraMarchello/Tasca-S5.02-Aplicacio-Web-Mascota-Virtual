package com.codemate.service;

import com.codemate.model.TransactionType;
import com.codemate.model.PointSource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class PointAwardHelper {
    
    private final PointTransactionService pointTransactionService;
    private final AchievementService achievementService;
    private final PetService petService;
    
    public PointAwardHelper(PointTransactionService pointTransactionService,
                           AchievementService achievementService,
                           PetService petService) {
        this.pointTransactionService = pointTransactionService;
        this.achievementService = achievementService;
        this.petService = petService;
    }
    
    /**
     * Award points for stack trace resolution and track achievements
     */
    public void awardStackTracePoints(Long userId, String stackTraceId) {
        // Award points
        pointTransactionService.awardStackTracePoints(userId, stackTraceId);
        
        // Track achievement
        achievementService.trackStackTraceResolution(userId);
        
        // Update pet's total points
        petService.updateTotalPointsEarned(userId);
    }
    
    /**
     * Award points for AI chat usage and track achievements
     */
    public void awardAIChatPoints(Long userId, String chatSessionId) {
        // Award points
        pointTransactionService.awardAIChatPoints(userId, chatSessionId);
        
        // Track achievement
        achievementService.trackAIUsage(userId);
        
        // Update pet's total points
        petService.updateTotalPointsEarned(userId);
    }
    
    /**
     * Award daily login points (only once per day)
     */
    public void awardDailyLoginPoints(Long userId) {
        // Award points (returns null if already awarded today)
        var transaction = pointTransactionService.awardDailyLoginPoints(userId);
        
        if (transaction != null) {
            // Update pet's total points only if points were actually awarded
            petService.updateTotalPointsEarned(userId);
        }
    }
    
    /**
     * Award admin-granted points
     */
    public void awardAdminPoints(Long userId, Long amount, String reason) {
        // Award points
        pointTransactionService.createTransaction(userId, 
            TransactionType.EARNED,
            PointSource.ADMIN_GRANT,
            amount, reason);
        
        // Update pet's total points
        petService.updateTotalPointsEarned(userId);
    }
}