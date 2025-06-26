package com.codemate.payload.response;

import com.codemate.model.AIMessage;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Map;

@Data
public class AIMessageResponse {
    private Long id;
    private String messageType;
    private String content;
    private String codeSnippet;
    private String programmingLanguage;
    private Map<String, Object> contextData;
    private LocalDateTime createdAt;

    public static AIMessageResponse from(AIMessage message) {
        AIMessageResponse response = new AIMessageResponse();
        response.setId(message.getId());
        response.setMessageType(message.getMessageType().getValue());
        response.setContent(message.getContent());
        response.setCodeSnippet(message.getCodeSnippet());
        response.setProgrammingLanguage(message.getProgrammingLanguage());
        response.setContextData(message.getContextData());
        response.setCreatedAt(message.getCreatedAt());
        return response;
    }
} 