package com.codemate.repository;

import com.codemate.model.GitRepository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface GitRepositoryRepository extends JpaRepository<GitRepository, Long> {
    
    Optional<GitRepository> findByUserIdAndScenarioIdAndIsActive(Long userId, String scenarioId, Boolean isActive);
    
    List<GitRepository> findByUserIdAndIsActive(Long userId, Boolean isActive);
    
    List<GitRepository> findByScenarioId(String scenarioId);
    
    @Query("SELECT gr FROM GitRepository gr WHERE gr.userId = :userId AND gr.isActive = true ORDER BY gr.updatedAt DESC")
    List<GitRepository> findActiveRepositoriesByUserOrderByUpdatedDesc(@Param("userId") Long userId);
    
    @Query("SELECT COUNT(gr) FROM GitRepository gr WHERE gr.userId = :userId AND gr.scenarioId = :scenarioId")
    Long countByUserIdAndScenarioId(@Param("userId") Long userId, @Param("scenarioId") String scenarioId);
    
    void deleteByUserIdAndScenarioId(Long userId, String scenarioId);
} 