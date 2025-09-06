package com.codemate.service.scenario;

import com.codemate.model.GitScenario;
import com.codemate.repository.GitScenarioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Service responsible for managing Git scenarios (CRUD operations, querying)
 * Handles scenario retrieval, filtering, and caching
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ScenarioManagementService {

    private final GitScenarioRepository gitScenarioRepository;

    /**
     * Gets all active scenarios ordered by difficulty and order index
     */
    @Cacheable(value = "scenarios", key = "'all-active'")
    public List<GitScenario> getAllActiveScenarios() {
        log.debug("Fetching all active scenarios (cache miss)");
        return gitScenarioRepository.findByIsActiveOrderByOrderIndex(true);
    }

    /**
     * Gets scenarios by difficulty level
     */
    @Cacheable(value = "scenarios", key = "'level-' + #level")
    public List<GitScenario> getScenariosByLevel(GitScenario.GitScenarioLevel level) {
        log.info("Fetching scenarios for level: {}", level);
        return gitScenarioRepository.findByLevelAndIsActiveOrderByOrderIndex(level, true);
    }

    /**
     * Gets scenarios by category
     */
    @Cacheable(value = "scenarios", key = "'category-' + #category")
    public List<GitScenario> getScenariosByCategory(GitScenario.GitScenarioCategory category) {
        log.info("Fetching scenarios for category: {}", category);
        return gitScenarioRepository.findByCategoryAndIsActiveOrderByOrderIndex(category, true);
    }

    /**
     * Gets a specific scenario by ID
     */
    @Cacheable(value = "scenarios", key = "#scenarioId")
    public GitScenario getScenarioById(String scenarioId) {
        log.debug("Fetching scenario with ID: {} (cache miss)", scenarioId);
        return gitScenarioRepository.findByScenarioId(scenarioId)
            .orElseThrow(() -> new RuntimeException("Scenario not found: " + scenarioId));
    }

    /**
     * Gets scenarios by multiple filters
     */
    public List<GitScenario> getScenariosByFilters(GitScenario.GitScenarioLevel level, 
                                                   GitScenario.GitScenarioCategory category, 
                                                   boolean isActive) {
        log.info("Fetching scenarios with filters - level: {}, category: {}, active: {}", level, category, isActive);
        
        if (level != null && category != null && isActive) {
            // Use available repository methods for filtering
            return gitScenarioRepository.findByLevelAndCategoryAndIsActiveOrderByOrderIndex(level, category);
        } else if (level != null) {
            return gitScenarioRepository.findByLevelAndIsActiveOrderByOrderIndex(level, isActive);
        } else if (category != null) {
            return gitScenarioRepository.findByCategoryAndIsActiveOrderByOrderIndex(category, isActive);
        } else {
            return gitScenarioRepository.findByIsActiveOrderByOrderIndex(isActive);
        }
    }

    /**
     * Checks if a scenario exists and is active
     */
    public boolean isScenarioActive(String scenarioId) {
        return gitScenarioRepository.findByScenarioId(scenarioId)
            .map(GitScenario::getIsActive)
            .orElse(false);
    }

    /**
     * Gets the total number of active scenarios
     */
    public long getActiveScenarioCount() {
        return gitScenarioRepository.findByIsActiveOrderByOrderIndex(true).size();
    }

    /**
     * Gets scenarios by difficulty level count
     */
    public long getScenarioCountByLevel(GitScenario.GitScenarioLevel level) {
        return gitScenarioRepository.findByLevelAndIsActiveOrderByOrderIndex(level, true).size();
    }
}
