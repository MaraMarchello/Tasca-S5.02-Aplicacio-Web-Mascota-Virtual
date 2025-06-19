package com.codemate.repository;

import com.codemate.model.GitScenario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface GitScenarioRepository extends JpaRepository<GitScenario, Long> {
    
    Optional<GitScenario> findByScenarioId(String scenarioId);
    
    List<GitScenario> findByIsActiveOrderByOrderIndex(Boolean isActive);
    
    List<GitScenario> findByLevelAndIsActiveOrderByOrderIndex(GitScenario.GitScenarioLevel level, Boolean isActive);
    
    List<GitScenario> findByCategoryAndIsActiveOrderByOrderIndex(GitScenario.GitScenarioCategory category, Boolean isActive);
    
    @Query("SELECT gs FROM GitScenario gs WHERE gs.level = :level AND gs.category = :category AND gs.isActive = true ORDER BY gs.orderIndex")
    List<GitScenario> findByLevelAndCategoryAndIsActiveOrderByOrderIndex(
        @Param("level") GitScenario.GitScenarioLevel level, 
        @Param("category") GitScenario.GitScenarioCategory category
    );
    
    @Query("SELECT gs FROM GitScenario gs JOIN gs.tags t WHERE t IN :tags AND gs.isActive = true ORDER BY gs.orderIndex")
    List<GitScenario> findByTagsInAndIsActiveOrderByOrderIndex(@Param("tags") List<String> tags);
    
    @Query("SELECT COUNT(gs) FROM GitScenario gs WHERE gs.level = :level AND gs.isActive = true")
    Long countByLevelAndIsActive(@Param("level") GitScenario.GitScenarioLevel level);
} 