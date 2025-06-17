package com.codemate.payload.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AIAssistanceRequest {
    
    @NotBlank(message = "Query cannot be empty")
    private String query;
    
    private String context;
    
    private String language = "java";
} 