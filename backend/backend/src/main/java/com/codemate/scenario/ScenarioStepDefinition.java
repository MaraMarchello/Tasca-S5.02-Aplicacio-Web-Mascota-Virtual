package com.codemate.scenario;

import lombok.Data;

import java.util.List;

@Data
public class ScenarioStepDefinition {
    private int stepNumber;
    private String title;
    private String instructions;
    // Acceptable command patterns (regex or token strings)
    private List<String> acceptableCommands;
    private List<ValidationRuleDefinition> stateAssertions;
    private String guidance;
    private String hint;
    private Integer points;
}


