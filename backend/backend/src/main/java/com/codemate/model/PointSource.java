package com.codemate.model;

public enum PointSource {
    STACK_TRACE_RESOLVED(50L),
    AI_CHAT_USAGE(10L),
    DAILY_LOGIN(20L),
    ACHIEVEMENT_COMPLETED(0L), // Variable based on achievement
    ITEM_PURCHASE(0L), // Negative transaction
    ADMIN_GRANT(0L), // Variable based on admin decision
    GIT_SCENARIO_COMPLETED(0L); // Variable based on scenario difficulty
    
    private final Long basePoints;
    
    PointSource(Long basePoints) {
        this.basePoints = basePoints;
    }
    
    public Long getBasePoints() {
        return basePoints;
    }
} 