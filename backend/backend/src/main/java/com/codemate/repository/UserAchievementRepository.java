package com.codemate.repository;

import com.codemate.model.UserAchievement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserAchievementRepository extends JpaRepository<UserAchievement, Long> {
    
    // Find user achievement by user and achievement
    Optional<UserAchievement> findByUserIdAndAchievementId(Long userId, Long achievementId);
    
    // Find all achievements for a user
    List<UserAchievement> findByUserId(Long userId);
    
    // Find completed achievements for a user
    List<UserAchievement> findByUserIdAndCompletedTrue(Long userId);
    
    // Find in-progress achievements for a user
    List<UserAchievement> findByUserIdAndCompletedFalse(Long userId);
    
    // Find achievements with full data for a user
    @Query("SELECT ua FROM UserAchievement ua JOIN FETCH ua.achievement WHERE ua.userId = :userId ORDER BY ua.completedAt DESC, ua.createdAt DESC")
    List<UserAchievement> findByUserIdWithAchievement(@Param("userId") Long userId);
    
    // Get current progress for specific achievement
    @Query("SELECT ua.currentProgress FROM UserAchievement ua JOIN ua.achievement a WHERE ua.userId = :userId AND a.code = :achievementCode")
    Optional<Integer> getCurrentProgress(@Param("userId") Long userId, @Param("achievementCode") String achievementCode);
    
    // Count completed achievements for a user
    long countByUserIdAndCompletedTrue(Long userId);
} 