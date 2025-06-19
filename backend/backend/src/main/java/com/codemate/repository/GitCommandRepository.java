package com.codemate.repository;

import com.codemate.model.GitCommand;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface GitCommandRepository extends JpaRepository<GitCommand, Long> {
    
    List<GitCommand> findByRepositoryIdOrderByExecutedAtDesc(Long repositoryId);
    
    List<GitCommand> findByUserIdAndRepositoryIdOrderByExecutedAtDesc(Long userId, Long repositoryId);
    
    List<GitCommand> findByUserIdAndScenarioIdOrderByExecutedAtDesc(Long userId, String scenarioId);
    
    @Query("SELECT gc FROM GitCommand gc WHERE gc.userId = :userId AND gc.executedAt BETWEEN :startDate AND :endDate ORDER BY gc.executedAt DESC")
    List<GitCommand> findByUserIdAndExecutedAtBetween(
        @Param("userId") Long userId, 
        @Param("startDate") LocalDateTime startDate, 
        @Param("endDate") LocalDateTime endDate
    );
    
    @Query("SELECT COUNT(gc) FROM GitCommand gc WHERE gc.userId = :userId AND gc.successful = true")
    Long countSuccessfulCommandsByUser(@Param("userId") Long userId);
    
    @Query("SELECT COUNT(gc) FROM GitCommand gc WHERE gc.userId = :userId AND gc.successful = false")
    Long countFailedCommandsByUser(@Param("userId") Long userId);
    
    @Query("SELECT gc.command, COUNT(gc) as count FROM GitCommand gc WHERE gc.userId = :userId GROUP BY gc.command ORDER BY count DESC")
    List<Object[]> findMostUsedCommandsByUser(@Param("userId") Long userId);
    
    void deleteByRepositoryId(Long repositoryId);
} 