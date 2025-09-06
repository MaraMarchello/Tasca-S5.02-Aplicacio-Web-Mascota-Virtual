package com.codemate.scenario;

import lombok.Data;

@Data
public class ValidationRuleDefinition {
    public enum RuleType {
        BRANCH_EXISTS,
        CURRENT_BRANCH,
        COMMIT_MESSAGE,
        MERGE_COMPLETED,
        FILE_EXISTS,
        FILE_CONTENT
    }

    private RuleType type;
    private String target;
    // condition can be a substring, regex, or literal depending on the rule
    private String condition;
    private String errorMessage;
}


