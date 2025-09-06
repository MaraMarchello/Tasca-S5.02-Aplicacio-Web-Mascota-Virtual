package com.codemate.service.scenario;

import com.codemate.model.GitScenario;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Service responsible for providing hints and guidance for Git scenarios
 * Handles step guidance, hints, and help text extraction
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ScenarioGuidanceService {

    private final ScenarioManagementService scenarioManagementService;
    private final ScenarioProgressService scenarioProgressService;
    private final ObjectMapper objectMapper;

    /**
     * Gets the next step guidance for a scenario
     */
    public String getNextStepGuidance(String scenarioId, int currentStep) {
        log.info("Getting next step guidance for scenario: {} step: {}", scenarioId, currentStep);
        
        GitScenario scenario = scenarioManagementService.getScenarioById(scenarioId);
        return extractStepGuidance(scenario, currentStep);
    }

    /**
     * Provides a hint for the current step
     */
    public String getStepHint(Long userId, String scenarioId, int currentStep) {
        log.info("Getting hint for user: {} scenario: {} step: {}", userId, scenarioId, currentStep);
        
        // Update hint usage count
        scenarioProgressService.incrementHintUsage(userId, scenarioId);
        
        GitScenario scenario = scenarioManagementService.getScenarioById(scenarioId);
        return extractStepHint(scenario, currentStep);
    }

    /**
     * Gets detailed instructions for a specific step
     */
    public String getStepInstructions(String scenarioId, int stepNumber) {
        log.info("Getting instructions for scenario: {} step: {}", scenarioId, stepNumber);
        
        GitScenario scenario = scenarioManagementService.getScenarioById(scenarioId);
        return extractStepInstructions(scenario, stepNumber);
    }

    /**
     * Gets the title of a specific step
     */
    public String getStepTitle(String scenarioId, int stepNumber) {
        log.info("Getting title for scenario: {} step: {}", scenarioId, stepNumber);
        
        GitScenario scenario = scenarioManagementService.getScenarioById(scenarioId);
        return extractStepTitle(scenario, stepNumber);
    }

    /**
     * Gets all acceptable commands for a step
     */
    public String[] getAcceptableCommands(String scenarioId, int stepNumber) {
        log.info("Getting acceptable commands for scenario: {} step: {}", scenarioId, stepNumber);
        
        GitScenario scenario = scenarioManagementService.getScenarioById(scenarioId);
        return extractAcceptableCommands(scenario, stepNumber);
    }

    /**
     * Gets complete step information including title, instructions, guidance, and hints
     */
    public StepInformation getCompleteStepInformation(String scenarioId, int stepNumber) {
        log.info("Getting complete step information for scenario: {} step: {}", scenarioId, stepNumber);
        
        GitScenario scenario = scenarioManagementService.getScenarioById(scenarioId);
        
        return StepInformation.builder()
            .stepNumber(stepNumber)
            .title(extractStepTitle(scenario, stepNumber))
            .instructions(extractStepInstructions(scenario, stepNumber))
            .guidance(extractStepGuidance(scenario, stepNumber))
            .hint(extractStepHint(scenario, stepNumber))
            .acceptableCommands(extractAcceptableCommands(scenario, stepNumber))
            .build();
    }

    /**
     * Checks if a step has custom guidance available
     */
    public boolean hasCustomGuidance(String scenarioId, int stepNumber) {
        try {
            GitScenario scenario = scenarioManagementService.getScenarioById(scenarioId);
            if (scenario.getExpectedCommands() != null) {
                JsonNode expectedCommands = objectMapper.readTree(scenario.getExpectedCommands());
                if (expectedCommands.isArray() && stepNumber < expectedCommands.size()) {
                    JsonNode step = expectedCommands.get(stepNumber);
                    return step.has("guidance") && !step.get("guidance").asText().trim().isEmpty();
                }
            }
        } catch (JsonProcessingException e) {
            log.error("Error checking custom guidance for scenario: {} step: {}", scenarioId, stepNumber, e);
        }
        return false;
    }

    // Private extraction methods

    private String extractStepGuidance(GitScenario scenario, int stepNumber) {
        try {
            if (scenario.getExpectedCommands() != null) {
                JsonNode expectedCommands = objectMapper.readTree(scenario.getExpectedCommands());
                if (expectedCommands.isArray() && stepNumber < expectedCommands.size()) {
                    JsonNode step = expectedCommands.get(stepNumber);
                    return step.has("guidance") ? step.get("guidance").asText() : "Continue with the next step.";
                }
            }
        } catch (JsonProcessingException e) {
            log.error("Error extracting step guidance for scenario: {}", scenario.getScenarioId(), e);
        }
        return "Follow the scenario instructions to proceed.";
    }

    private String extractStepHint(GitScenario scenario, int stepNumber) {
        try {
            if (scenario.getExpectedCommands() != null) {
                JsonNode expectedCommands = objectMapper.readTree(scenario.getExpectedCommands());
                if (expectedCommands.isArray() && stepNumber < expectedCommands.size()) {
                    JsonNode step = expectedCommands.get(stepNumber);
                    return step.has("hint") ? step.get("hint").asText() : "Try using the appropriate git command for this step.";
                }
            }
        } catch (JsonProcessingException e) {
            log.error("Error extracting step hint for scenario: {}", scenario.getScenarioId(), e);
        }
        return "Check the git documentation for help with this command.";
    }

    private String extractStepInstructions(GitScenario scenario, int stepNumber) {
        try {
            if (scenario.getExpectedCommands() != null) {
                JsonNode expectedCommands = objectMapper.readTree(scenario.getExpectedCommands());
                if (expectedCommands.isArray() && stepNumber < expectedCommands.size()) {
                    JsonNode step = expectedCommands.get(stepNumber);
                    return step.has("instructions") ? step.get("instructions").asText() : "Complete this step to proceed.";
                }
            }
        } catch (JsonProcessingException e) {
            log.error("Error extracting step instructions for scenario: {}", scenario.getScenarioId(), e);
        }
        return "Complete this step to proceed with the scenario.";
    }

    private String extractStepTitle(GitScenario scenario, int stepNumber) {
        try {
            if (scenario.getExpectedCommands() != null) {
                JsonNode expectedCommands = objectMapper.readTree(scenario.getExpectedCommands());
                if (expectedCommands.isArray() && stepNumber < expectedCommands.size()) {
                    JsonNode step = expectedCommands.get(stepNumber);
                    return step.has("title") ? step.get("title").asText() : "Step " + (stepNumber + 1);
                }
            }
        } catch (JsonProcessingException e) {
            log.error("Error extracting step title for scenario: {}", scenario.getScenarioId(), e);
        }
        return "Step " + (stepNumber + 1);
    }

    private String[] extractAcceptableCommands(GitScenario scenario, int stepNumber) {
        try {
            if (scenario.getExpectedCommands() != null) {
                JsonNode expectedCommands = objectMapper.readTree(scenario.getExpectedCommands());
                if (expectedCommands.isArray() && stepNumber < expectedCommands.size()) {
                    JsonNode step = expectedCommands.get(stepNumber);
                    if (step.has("acceptableCommands") && step.get("acceptableCommands").isArray()) {
                        JsonNode commandsNode = step.get("acceptableCommands");
                        String[] commands = new String[commandsNode.size()];
                        for (int i = 0; i < commandsNode.size(); i++) {
                            commands[i] = commandsNode.get(i).asText();
                        }
                        return commands;
                    }
                }
            }
        } catch (JsonProcessingException e) {
            log.error("Error extracting acceptable commands for scenario: {}", scenario.getScenarioId(), e);
        }
        return new String[0];
    }

    // Data transfer object for complete step information
    @lombok.Builder
    @lombok.Data
    public static class StepInformation {
        private int stepNumber;
        private String title;
        private String instructions;
        private String guidance;
        private String hint;
        private String[] acceptableCommands;
    }
}
