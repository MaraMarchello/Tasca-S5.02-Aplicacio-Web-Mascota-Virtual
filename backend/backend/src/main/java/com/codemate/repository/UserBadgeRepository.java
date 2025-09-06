package com.codemate.repository;

import com.codemate.model.UserBadge;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserBadgeRepository extends JpaRepository<UserBadge, Long> {
    
    List<UserBadge> findByUserIdOrderByEarnedAtDesc(Long userId);
    
    List<UserBadge> findByUserIdAndIsVisibleTrueOrderByEarnedAtDesc(Long userId);
    
    Optional<UserBadge> findByUserIdAndBadgeType(Long userId, UserBadge.BadgeType badgeType);
    
    boolean existsByUserIdAndBadgeType(Long userId, UserBadge.BadgeType badgeType);
    
    @Query("SELECT ub FROM UserBadge ub WHERE ub.userId = :userId AND ub.rarity = :rarity ORDER BY ub.earnedAt DESC")
    List<UserBadge> findByUserIdAndRarity(@Param("userId") Long userId, @Param("rarity") UserBadge.BadgeRarity rarity);
    
    @Query("SELECT COUNT(ub) FROM UserBadge ub WHERE ub.userId = :userId")
    Long countByUserId(@Param("userId") Long userId);
    
    @Query("SELECT COUNT(ub) FROM UserBadge ub WHERE ub.userId = :userId AND ub.rarity = :rarity")
    Long countByUserIdAndRarity(@Param("userId") Long userId, @Param("rarity") UserBadge.BadgeRarity rarity);
    
    @Query("SELECT SUM(ub.pointsAwarded) FROM UserBadge ub WHERE ub.userId = :userId")
    Long sumPointsByUserId(@Param("userId") Long userId);
    
    @Query("SELECT ub FROM UserBadge ub WHERE ub.userId = :userId AND ub.earnedAt >= :startDate ORDER BY ub.earnedAt DESC")
    List<UserBadge> findByUserIdAndEarnedAtAfter(@Param("userId") Long userId, @Param("startDate") LocalDateTime startDate);
    
    @Query("SELECT ub FROM UserBadge ub WHERE ub.earnedAt >= :startDate GROUP BY ub.badgeType ORDER BY COUNT(ub) DESC")
    List<UserBadge> findMostEarnedBadgesSince(@Param("startDate") LocalDateTime startDate);
    
    @Query("SELECT DISTINCT ub.userId FROM UserBadge ub WHERE ub.badgeType = :badgeType")
    List<Long> findUserIdsByBadgeType(@Param("badgeType") UserBadge.BadgeType badgeType);
}
