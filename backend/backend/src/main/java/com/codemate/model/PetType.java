package com.codemate.model;

public enum PetType {
    DUKE_JAVA("☕", "The classic Java mascot"),
    COFFEE_BEAN("🫘", "An energetic coffee bean"),
    CODEMATE_MASCOT("🤖", "The official CodeMate mascot");
    
    private final String emoji;
    private final String description;
    
    PetType(String emoji, String description) {
        this.emoji = emoji;
        this.description = description;
    }
    
    public String getEmoji() {
        return emoji;
    }
    
    public String getDescription() {
        return description;
    }
} 