package com.codemate.scenario;

import lombok.Data;

import java.util.List;

@Data
public class ScenarioDefinition {
    private String version;
    private List<ScenarioStepDefinition> steps;
}


