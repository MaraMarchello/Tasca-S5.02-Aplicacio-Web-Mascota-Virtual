package com.codemate.repository;

import com.codemate.model.GitUserProgress;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface GitUserProgressRepository extends JpaRepository<GitUserProgress, Long> {
    
    Optional<GitUserProgress> findByUserIdAndScenarioScenarioId(Long userId, String scenarioId);
    
    List<GitUserProgress> findByUserId(Long userId);
    
    List<GitUserProgress> findByUserIdAndStatus(Long userId, GitUserProgress.GitProgressStatus status);
    
    @Query("SELECT gup FROM GitUserProgress gup WHERE gup.userId = :userId AND gup.status = 'COMPLETED' ORDER BY gup.completedAt DESC")
    List<GitUserProgress> findCompletedScenariosByUser(@Param("userId") Long userId);
    
    @Query("SELECT gup FROM GitUserProgress gup WHERE gup.userId = :userId AND gup.status = 'IN_PROGRESS' ORDER BY gup.updatedAt DESC")
    List<GitUserProgress> findInProgressScenariosByUser(@Param("userId") Long userId);
    
    @Query("SELECT COUNT(gup) FROM GitUserProgress gup WHERE gup.userId = :userId AND gup.status = 'COMPLETED'")
    Long countCompletedScenariosByUser(@Param("userId") Long userId);
    
    @Query("SELECT SUM(gup.pointsEarned) FROM GitUserProgress gup WHERE gup.userId = :userId AND gup.status = 'COMPLETED'")
    Long sumPointsEarnedByUser(@Param("userId") Long userId);
    
    @Query("SELECT AVG(gup.commandsExecuted) FROM GitUserProgress gup WHERE gup.userId = :userId AND gup.status = 'COMPLETED'")
    Double averageCommandsExecutedByUser(@Param("userId") Long userId);
} 