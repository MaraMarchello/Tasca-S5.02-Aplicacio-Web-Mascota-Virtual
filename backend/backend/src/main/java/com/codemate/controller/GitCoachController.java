package com.codemate.controller;

import com.codemate.model.GitRepository;
import com.codemate.model.GitScenario;
import com.codemate.model.GitUserProgress;
import com.codemate.security.CurrentUser;
import com.codemate.security.UserPrincipal;
import com.codemate.service.GitScenarioService;
import com.codemate.service.GitSimulatorService;
import com.codemate.service.GitDashboardService;
import com.codemate.payload.response.GitDashboardResponse;
import com.codemate.service.AchievementService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@RestController
@RequestMapping("/api/v1/git")
@RequiredArgsConstructor
public class GitCoachController {

    private final GitSimulatorService gitSimulatorService;
    private final GitScenarioService gitScenarioService;
    private final GitDashboardService gitDashboardService;
    private final AchievementService achievementService;

    // Scenario endpoints

    @GetMapping("/scenarios")
    public ResponseEntity<List<GitScenario>> getAllScenarios(@CurrentUser(required = false) UserPrincipal currentUser) {
        log.info("Fetching all active Git scenarios for user: {}", currentUser != null ? currentUser.getId() : "anonymous");
        
        // Track Git Coach page visit for authenticated users
        if (currentUser != null) {
            try {
                achievementService.trackGitCoachVisit(currentUser.getId());
            } catch (Exception e) {
                log.warn("Failed to track Git Coach visit for user: {}", currentUser.getId(), e);
            }
        }
        
        List<GitScenario> scenarios = gitScenarioService.getAllActiveScenarios();
        return ResponseEntity.ok(scenarios);
    }

    @GetMapping("/scenarios/level/{level}")
    public ResponseEntity<List<GitScenario>> getScenariosByLevel(@PathVariable GitScenario.GitScenarioLevel level) {
        log.info("Fetching scenarios for level: {}", level);
        List<GitScenario> scenarios = gitScenarioService.getScenariosByLevel(level);
        return ResponseEntity.ok(scenarios);
    }

    @GetMapping("/scenarios/category/{category}")
    public ResponseEntity<List<GitScenario>> getScenariosByCategory(@PathVariable GitScenario.GitScenarioCategory category) {
        log.info("Fetching scenarios for category: {}", category);
        List<GitScenario> scenarios = gitScenarioService.getScenariosByCategory(category);
        return ResponseEntity.ok(scenarios);
    }

    @GetMapping("/scenarios/{scenarioId}")
    public ResponseEntity<GitScenario> getScenario(@PathVariable String scenarioId) {
        log.info("Fetching scenario: {}", scenarioId);
        GitScenario scenario = gitScenarioService.getScenarioById(scenarioId);
        return ResponseEntity.ok(scenario);
    }

    // User progress endpoints

    @PostMapping("/scenarios/{scenarioId}/start")
    public ResponseEntity<GitUserProgress> startScenario(
            @PathVariable String scenarioId,
            @CurrentUser UserPrincipal currentUser) {
        log.info("Starting scenario: {} for user: {}", scenarioId, currentUser.getId());
        
        // Track scenario start achievement
        try {
            achievementService.trackGitScenarioStart(currentUser.getId());
        } catch (Exception e) {
            log.warn("Failed to track scenario start for user: {}", currentUser.getId(), e);
        }
        
        GitUserProgress progress = gitScenarioService.startScenario(currentUser.getId(), scenarioId);
        return ResponseEntity.ok(progress);
    }

    @GetMapping("/progress")
    public ResponseEntity<List<GitUserProgress>> getUserProgress(@CurrentUser UserPrincipal currentUser) {
        log.info("Fetching Git progress for user: {}", currentUser.getId());
        List<GitUserProgress> progress = gitScenarioService.getUserProgress(currentUser.getId());
        return ResponseEntity.ok(progress);
    }

    @GetMapping("/progress/{scenarioId}")
    public ResponseEntity<GitUserProgress> getUserProgressForScenario(
            @PathVariable String scenarioId,
            @CurrentUser UserPrincipal currentUser) {
        log.info("Fetching progress for user: {} scenario: {}", currentUser.getId(), scenarioId);
        Optional<GitUserProgress> progress = gitScenarioService.getUserProgressForScenario(currentUser.getId(), scenarioId);
        return progress.map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/progress/completed")
    public ResponseEntity<List<GitUserProgress>> getCompletedScenarios(@CurrentUser UserPrincipal currentUser) {
        log.info("Fetching completed scenarios for user: {}", currentUser.getId());
        List<GitUserProgress> completed = gitScenarioService.getCompletedScenarios(currentUser.getId());
        return ResponseEntity.ok(completed);
    }

    @GetMapping("/progress/in-progress")
    public ResponseEntity<List<GitUserProgress>> getInProgressScenarios(@CurrentUser UserPrincipal currentUser) {
        log.info("Fetching in-progress scenarios for user: {}", currentUser.getId());
        List<GitUserProgress> inProgress = gitScenarioService.getInProgressScenarios(currentUser.getId());
        return ResponseEntity.ok(inProgress);
    }

    @GetMapping("/stats")
    public ResponseEntity<GitScenarioService.GitUserStats> getUserStats(@CurrentUser UserPrincipal currentUser) {
        log.info("Fetching Git stats for user: {}", currentUser.getId());
        GitScenarioService.GitUserStats stats = gitScenarioService.getUserStats(currentUser.getId());
        return ResponseEntity.ok(stats);
    }

    @GetMapping("/dashboard")
    public ResponseEntity<GitDashboardResponse> getDashboardData(@CurrentUser UserPrincipal currentUser) {
        log.info("Fetching Git dashboard data for user: {}", currentUser.getId());
        GitDashboardResponse dashboardData = gitDashboardService.getDashboardData(currentUser.getId());
        return ResponseEntity.ok(dashboardData);
    }

    // Repository simulation endpoints

    @PostMapping("/repository/create")
    public ResponseEntity<GitRepository> createRepository(
            @RequestBody CreateRepositoryRequest request,
            @CurrentUser UserPrincipal currentUser) {
        log.info("Creating virtual repository for user: {} scenario: {}", currentUser.getId(), request.getScenarioId());
        GitRepository repository = gitSimulatorService.createVirtualRepository(
            currentUser.getId(), 
            request.getScenarioId(), 
            request.getRepositoryName()
        );
        return ResponseEntity.ok(repository);
    }

    @GetMapping("/repository/{repositoryId}/state")
    public ResponseEntity<GitSimulatorService.GitRepositoryState> getRepositoryState(@PathVariable Long repositoryId) {
        log.info("Fetching repository state for repository: {}", repositoryId);
        GitSimulatorService.GitRepositoryState state = gitSimulatorService.getRepositoryState(repositoryId);
        return ResponseEntity.ok(state);
    }

    @PostMapping("/repository/{repositoryId}/execute")
    public ResponseEntity<GitSimulatorService.GitCommandResult> executeCommand(
            @PathVariable Long repositoryId,
            @RequestBody ExecuteCommandRequest request,
            @CurrentUser UserPrincipal currentUser) {
        log.info("Executing command in repository: {} command: {}", repositoryId, request.getCommand());
        GitSimulatorService.GitCommandResult result = gitSimulatorService.executeGitCommand(
            repositoryId, 
            request.getCommand(), 
            currentUser.getId()
        );
        
        // Track command execution for achievements
        gitScenarioService.trackCommandExecution(currentUser.getId(), request.getCommand());
        
        // Check if this command completes a scenario step
        if (request.getScenarioId() != null && request.getStepNumber() != null) {
            boolean stepCompleted = gitScenarioService.checkScenarioStepCompletion(
                request.getScenarioId(), 
                request.getStepNumber(), 
                request.getCommand(), 
                result.getOutput()
            );
            
            if (stepCompleted) {
                gitScenarioService.updateProgress(
                    currentUser.getId(), 
                    request.getScenarioId(), 
                    request.getStepNumber() + 1, 
                    true
                );
            }
        }
        
        return ResponseEntity.ok(result);
    }

    @PostMapping("/repository/{repositoryId}/conflict/{conflictType}")
    public ResponseEntity<Void> generateConflict(
            @PathVariable Long repositoryId,
            @PathVariable String conflictType) {
        log.info("Generating conflict in repository: {} type: {}", repositoryId, conflictType);
        gitSimulatorService.generateConflictScenario(repositoryId, conflictType);
        return ResponseEntity.ok().build();
    }

    // Guidance and hints

    @GetMapping("/scenarios/{scenarioId}/guidance/{stepNumber}")
    public ResponseEntity<String> getStepGuidance(
            @PathVariable String scenarioId,
            @PathVariable int stepNumber) {
        log.info("Getting guidance for scenario: {} step: {}", scenarioId, stepNumber);
        String guidance = gitScenarioService.getNextStepGuidance(scenarioId, stepNumber);
        return ResponseEntity.ok(guidance);
    }

    @PostMapping("/scenarios/{scenarioId}/hint/{stepNumber}")
    public ResponseEntity<String> getStepHint(
            @PathVariable String scenarioId,
            @PathVariable int stepNumber,
            @CurrentUser UserPrincipal currentUser) {
        log.info("Getting hint for user: {} scenario: {} step: {}", currentUser.getId(), scenarioId, stepNumber);
        String hint = gitScenarioService.getStepHint(currentUser.getId(), scenarioId, stepNumber);
        return ResponseEntity.ok(hint);
    }

    // Request DTOs

    public static class CreateRepositoryRequest {
        private String scenarioId;
        private String repositoryName;

        public String getScenarioId() { return scenarioId; }
        public void setScenarioId(String scenarioId) { this.scenarioId = scenarioId; }
        public String getRepositoryName() { return repositoryName; }
        public void setRepositoryName(String repositoryName) { this.repositoryName = repositoryName; }
    }

    public static class ExecuteCommandRequest {
        private String command;
        private String scenarioId;
        private Integer stepNumber;

        public String getCommand() { return command; }
        public void setCommand(String command) { this.command = command; }
        public String getScenarioId() { return scenarioId; }
        public void setScenarioId(String scenarioId) { this.scenarioId = scenarioId; }
        public Integer getStepNumber() { return stepNumber; }
        public void setStepNumber(Integer stepNumber) { this.stepNumber = stepNumber; }
    }
} 