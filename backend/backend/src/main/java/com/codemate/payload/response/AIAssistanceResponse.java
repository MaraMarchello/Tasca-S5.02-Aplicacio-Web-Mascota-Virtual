package com.codemate.payload.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AIAssistanceResponse {
    
    private String answer;
    private String explanation;
    private String codeSnippet;
    private String references;
} 