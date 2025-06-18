package com.codemate.repository;

import com.codemate.model.Achievement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AchievementRepository extends JpaRepository<Achievement, Long> {
    
    // Find achievement by code
    Optional<Achievement> findByCode(String code);
    
    // Find active achievement by code
    Optional<Achievement> findByCodeAndActiveTrue(String code);
    
    // Find all active achievements
    List<Achievement> findByActiveTrueOrderByName();
    
    // Find achievements by points reward range
    List<Achievement> findByPointsRewardBetweenAndActiveTrue(Long minPoints, Long maxPoints);
} 