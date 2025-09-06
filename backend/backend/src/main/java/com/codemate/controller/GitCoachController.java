package com.codemate.controller;

import com.codemate.model.GitRepository;
import com.codemate.model.GitScenario;
import com.codemate.model.GitUserProgress;
import com.codemate.security.CurrentUser;
import com.codemate.security.UserPrincipal;
import com.codemate.service.scenario.GitScenarioFacadeService;
import com.codemate.service.scenario.ScenarioProgressService;
import com.codemate.service.scenario.ScenarioValidationService;
import com.codemate.service.git.GitSimulatorFacadeService;
import com.codemate.service.git.GitRepositoryManagementService;
import com.codemate.service.GitDashboardService;
import com.codemate.payload.response.GitDashboardResponse;
import com.codemate.service.AchievementService;
import com.codemate.payload.response.GitExecuteResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@Slf4j
@RestController
@RequestMapping("/api/v1/git")
@RequiredArgsConstructor
public class GitCoachController {

    private final GitSimulatorFacadeService gitSimulatorFacadeService;
    private final GitScenarioFacadeService gitScenarioFacadeService;
    private final GitDashboardService gitDashboardService;
    private final AchievementService achievementService;

    // Scenario endpoints

    @GetMapping("/scenarios")
    public ResponseEntity<List<GitScenario>> getAllScenarios(@CurrentUser UserPrincipal currentUser) {
        log.info("Fetching all active Git scenarios for user: {}", currentUser.getId());
        
        // Track Git Coach page visit
        try {
            achievementService.trackGitCoachVisit(currentUser.getId());
        } catch (Exception e) {
            log.warn("Failed to track Git Coach visit for user: {}", currentUser.getId(), e);
        }
        
        List<GitScenario> scenarios = gitScenarioFacadeService.getAllActiveScenarios();
        return ResponseEntity.ok(scenarios);
    }

    @GetMapping("/scenarios/level/{level}")
    public ResponseEntity<List<GitScenario>> getScenariosByLevel(
            @PathVariable GitScenario.GitScenarioLevel level,
            @CurrentUser UserPrincipal currentUser) {
        log.info("Fetching scenarios for level: {} for user: {}", level, currentUser.getId());
        List<GitScenario> scenarios = gitScenarioFacadeService.getScenariosByLevel(level);
        return ResponseEntity.ok(scenarios);
    }

    @GetMapping("/scenarios/category/{category}")
    public ResponseEntity<List<GitScenario>> getScenariosByCategory(
            @PathVariable GitScenario.GitScenarioCategory category,
            @CurrentUser UserPrincipal currentUser) {
        log.info("Fetching scenarios for category: {} for user: {}", category, currentUser.getId());
        List<GitScenario> scenarios = gitScenarioFacadeService.getScenariosByCategory(category);
        return ResponseEntity.ok(scenarios);
    }

    @GetMapping("/scenarios/{scenarioId}")
    public ResponseEntity<GitScenario> getScenario(
            @PathVariable String scenarioId,
            @CurrentUser UserPrincipal currentUser) {
        log.info("Fetching scenario: {} for user: {}", scenarioId, currentUser.getId());
        GitScenario scenario = gitScenarioFacadeService.getScenarioById(scenarioId);
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
        
        GitUserProgress progress = gitScenarioFacadeService.startScenario(currentUser.getId(), scenarioId);
        return ResponseEntity.ok(progress);
    }

    @GetMapping("/progress")
    public ResponseEntity<List<GitUserProgress>> getUserProgress(@CurrentUser UserPrincipal currentUser) {
        log.info("Fetching Git progress for user: {}", currentUser.getId());
        List<GitUserProgress> progress = gitScenarioFacadeService.getUserProgress(currentUser.getId());
        return ResponseEntity.ok(progress);
    }

    @GetMapping("/progress/{scenarioId}")
    public ResponseEntity<GitUserProgress> getUserProgressForScenario(
            @PathVariable String scenarioId,
            @CurrentUser UserPrincipal currentUser) {
        log.info("Fetching progress for user: {} scenario: {}", currentUser.getId(), scenarioId);
        Optional<GitUserProgress> progress = gitScenarioFacadeService.getUserProgressForScenario(currentUser.getId(), scenarioId);
        return progress.map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/progress/completed")
    public ResponseEntity<List<GitUserProgress>> getCompletedScenarios(@CurrentUser UserPrincipal currentUser) {
        log.info("Fetching completed scenarios for user: {}", currentUser.getId());
        List<GitUserProgress> completed = gitScenarioFacadeService.getCompletedScenarios(currentUser.getId());
        return ResponseEntity.ok(completed);
    }

    @GetMapping("/progress/in-progress")
    public ResponseEntity<List<GitUserProgress>> getInProgressScenarios(@CurrentUser UserPrincipal currentUser) {
        log.info("Fetching in-progress scenarios for user: {}", currentUser.getId());
        List<GitUserProgress> inProgress = gitScenarioFacadeService.getInProgressScenarios(currentUser.getId());
        return ResponseEntity.ok(inProgress);
    }

    @GetMapping("/stats")
    public ResponseEntity<ScenarioProgressService.GitUserStats> getUserStats(@CurrentUser UserPrincipal currentUser) {
        log.info("Fetching Git stats for user: {}", currentUser.getId());
        ScenarioProgressService.GitUserStats stats = gitScenarioFacadeService.getUserStats(currentUser.getId());
        return ResponseEntity.ok(stats);
    }

    @GetMapping("/dashboard")
    public ResponseEntity<GitDashboardResponse> getDashboardData(@CurrentUser UserPrincipal currentUser) {
        log.info("Fetching Git dashboard data for user: {}", currentUser.getId());
        GitDashboardResponse dashboardData = gitDashboardService.getDashboardData(currentUser.getId());
        return ResponseEntity.ok(dashboardData);
    }

    // Repository simulation endpoints

    @PostMapping("/repository/create-demo")
    public ResponseEntity<GitRepository> createDemoRepository(
            @CurrentUser UserPrincipal currentUser) {
        log.info("Creating demo repository for user: {}", currentUser.getId());
        
        try {
            GitRepository repository = gitSimulatorFacadeService.createVirtualRepository(
                currentUser.getId(), 
                "DEMO", // Special scenario ID for demo mode
                "demo-repository-" + System.currentTimeMillis()
            );
            return ResponseEntity.ok(repository);
            
        } catch (Exception e) {
            log.error("Error creating demo repository for user: {}", currentUser.getId(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @PostMapping("/repository/create")
    public ResponseEntity<GitRepository> createRepository(
            @RequestBody CreateRepositoryRequest request,
            @CurrentUser UserPrincipal currentUser) {
        
        // Validate input parameters
        if (request == null || request.getRepositoryName() == null || request.getRepositoryName().trim().isEmpty()) {
            log.warn("Invalid repository creation request: repositoryName={}", 
                    request != null ? request.getRepositoryName() : "null");
            return ResponseEntity.badRequest().build();
        }
        
        log.info("Creating virtual repository for user: {} scenario: {}", currentUser.getId(), request.getScenarioId());
        
        try {
            GitRepository repository = gitSimulatorFacadeService.createVirtualRepository(
                currentUser.getId(), 
                request.getScenarioId(), 
                request.getRepositoryName()
            );
            return ResponseEntity.ok(repository);
            
        } catch (RuntimeException e) {
            log.error("Error creating virtual repository for user: {} scenario: {}", 
                     currentUser.getId(), request.getScenarioId(), e);
            
            if (e.getMessage() != null && e.getMessage().contains("Scenario not found")) {
                return ResponseEntity.notFound().build();
            } else {
                return ResponseEntity.internalServerError().build();
            }
        } catch (Exception e) {
            log.error("Unexpected error creating virtual repository for user: {} scenario: {}", 
                     currentUser.getId(), request.getScenarioId(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/repository/{repositoryId}/state")
    public ResponseEntity<GitRepositoryManagementService.GitRepositoryState> getRepositoryState(@PathVariable Long repositoryId) {
        
        if (repositoryId == null) {
            log.warn("Invalid repository state request: repositoryId is null");
            return ResponseEntity.badRequest().build();
        }
        
        log.info("Fetching repository state for repository: {}", repositoryId);
        
        try {
            GitRepositoryManagementService.GitRepositoryState state = gitSimulatorFacadeService.getRepositoryState(repositoryId);
            return ResponseEntity.ok(state);
            
        } catch (RuntimeException e) {
            log.error("Error fetching repository state for repository: {}", repositoryId, e);
            
            if (e.getMessage() != null && e.getMessage().contains("Repository not found")) {
                return ResponseEntity.notFound().build();
            } else {
                return ResponseEntity.internalServerError().build();
            }
        } catch (Exception e) {
            log.error("Unexpected error fetching repository state for repository: {}", repositoryId, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @PostMapping("/repository/{repositoryId}/execute")
    public ResponseEntity<GitExecuteResponse> executeCommand(
            @PathVariable Long repositoryId,
            @RequestBody ExecuteCommandRequest request,
            @CurrentUser UserPrincipal currentUser) {
        
        // Validate input parameters
        if (repositoryId == null || request == null || request.getCommand() == null || request.getCommand().trim().isEmpty()) {
            log.warn("Invalid command execution request: repositoryId={}, command={}", repositoryId, 
                    request != null ? request.getCommand() : "null");
            return ResponseEntity.badRequest().build();
        }
        
        log.info("Executing command in repository: {} command: {}", repositoryId, request.getCommand());
        
        try {
            GitRepositoryManagementService.GitCommandResult result = gitSimulatorFacadeService.executeGitCommand(
                repositoryId, 
                request.getCommand(), 
                currentUser.getId()
            );
            
            // Track command execution for achievements
            try {
                gitScenarioFacadeService.trackCommandExecution(currentUser.getId(), request.getCommand());
            } catch (Exception e) {
                log.warn("Failed to track command execution for achievements", e);
                // Don't fail the request for achievement tracking failures
            }
            
            // Check if this command completes a scenario step
            boolean stepCompleted = false;
            String tutorMessage = null;
            if (request.getScenarioId() != null && request.getStepNumber() != null) {
                try {
                    ScenarioValidationService.ValidationFeedback feedback = gitScenarioFacadeService.validateStepWithMessage(
                        repositoryId,
                        request.getScenarioId(),
                        request.getStepNumber(),
                        request.getCommand(),
                        result.getOutput()
                    );
                    stepCompleted = feedback.isStepCompleted();
                    
                    if (stepCompleted) {
                        gitScenarioFacadeService.updateProgress(
                            currentUser.getId(), 
                            request.getScenarioId(), 
                            request.getStepNumber() + 1, 
                            true
                        );
                        // Positive reinforcement; if validator included guidance, use it
                        tutorMessage = feedback.getMessage() != null ? feedback.getMessage() : "Great job! You've completed this step.";
                    } else {
                        // Provide specific validator message when available
                        tutorMessage = feedback.getMessage() != null ? feedback.getMessage() : "That command didn't complete the step. Check the instructions or request a hint.";
                    }
                } catch (Exception e) {
                    log.error("Error validating scenario step for user: {} scenario: {} step: {}", 
                             currentUser.getId(), request.getScenarioId(), request.getStepNumber(), e);
                    tutorMessage = "Unable to validate step completion. Please try again or contact support.";
                }
            }
            
            // Always include latest repository state and progress in the response
            GitRepositoryManagementService.GitRepositoryState state = null;
            GitUserProgress progress = null;
            
            try {
                state = gitSimulatorFacadeService.getRepositoryState(repositoryId);
            } catch (Exception e) {
                log.error("Error fetching repository state for repository: {}", repositoryId, e);
                // Continue without state; frontend will handle gracefully
            }
            
            if (request.getScenarioId() != null) {
                try {
                    progress = gitScenarioFacadeService
                        .getUserProgressForScenario(currentUser.getId(), request.getScenarioId())
                        .orElse(null);
                } catch (Exception e) {
                    log.error("Error fetching user progress for user: {} scenario: {}", 
                             currentUser.getId(), request.getScenarioId(), e);
                    // Continue without progress; frontend will handle gracefully
                }
            }

            GitExecuteResponse response = GitExecuteResponse.builder()
                .result(result)
                .stepCompleted(stepCompleted)
                .nextStepNumber(stepCompleted && request.getStepNumber() != null ? request.getStepNumber() + 1 : request.getStepNumber())
                .progress(progress)
                .repositoryState(state)
                .tutorMessage(tutorMessage)
                .build();

            return ResponseEntity.ok(response);
            
        } catch (RuntimeException e) {
            log.error("Error executing git command in repository: {} command: {}", repositoryId, request.getCommand(), e);
            
            if (e.getMessage() != null && e.getMessage().contains("Repository not found")) {
                return ResponseEntity.notFound().build();
            } else if (e.getMessage() != null && e.getMessage().contains("not allowed")) {
                return ResponseEntity.status(403).build();
            } else {
                return ResponseEntity.internalServerError().build();
            }
        } catch (Exception e) {
            log.error("Unexpected error executing git command in repository: {} command: {}", 
                     repositoryId, request.getCommand(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @PostMapping("/repository/{repositoryId}/conflict/{conflictType}")
    public ResponseEntity<Void> generateConflict(
            @PathVariable Long repositoryId,
            @PathVariable String conflictType) {
        log.info("Generating conflict in repository: {} type: {}", repositoryId, conflictType);
        gitSimulatorFacadeService.generateConflictScenario(repositoryId, conflictType);
        return ResponseEntity.ok().build();
    }

    // Guidance and hints

    @GetMapping("/scenarios/{scenarioId}/guidance/{stepNumber}")
    public ResponseEntity<String> getStepGuidance(
            @PathVariable String scenarioId,
            @PathVariable int stepNumber,
            @CurrentUser UserPrincipal currentUser) {
        log.info("Getting guidance for scenario: {} step: {} for user: {}", scenarioId, stepNumber, currentUser.getId());
        String guidance = gitScenarioFacadeService.getNextStepGuidance(scenarioId, stepNumber);
        return ResponseEntity.ok(guidance);
    }

    @PostMapping("/scenarios/{scenarioId}/hint/{stepNumber}")
    public ResponseEntity<String> getStepHint(
            @PathVariable String scenarioId,
            @PathVariable int stepNumber,
            @CurrentUser UserPrincipal currentUser) {
        log.info("Getting hint for user: {} scenario: {} step: {}", currentUser.getId(), scenarioId, stepNumber);
        String hint = gitScenarioFacadeService.getStepHint(currentUser.getId(), scenarioId, stepNumber);
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