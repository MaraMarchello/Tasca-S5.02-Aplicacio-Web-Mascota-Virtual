package com.codemate.payload.response;

import com.codemate.model.AIConversation;
import com.codemate.model.AIMessage;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Data
public class AIConversationResponse {
    private Long id;
    private String title;
    private String contextType;
    private String programmingLanguage;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Boolean isActive;
    private List<AIMessageResponse> messages;
    private int messageCount;

    public static AIConversationResponse from(AIConversation conversation) {
        AIConversationResponse response = new AIConversationResponse();
        response.setId(conversation.getId());
        response.setTitle(conversation.getTitle());
        response.setContextType(conversation.getContextType().getValue());
        response.setProgrammingLanguage(conversation.getProgrammingLanguage());
        response.setCreatedAt(conversation.getCreatedAt());
        response.setUpdatedAt(conversation.getUpdatedAt());
        response.setIsActive(conversation.getIsActive());
        
        if (conversation.getMessages() != null) {
            response.setMessages(conversation.getMessages().stream()
                    .map(AIMessageResponse::from)
                    .collect(Collectors.toList()));
            response.setMessageCount(conversation.getMessages().size());
        } else {
            response.setMessageCount(0);
        }
        
        return response;
    }

    public static AIConversationResponse fromWithoutMessages(AIConversation conversation) {
        AIConversationResponse response = new AIConversationResponse();
        response.setId(conversation.getId());
        response.setTitle(conversation.getTitle());
        response.setContextType(conversation.getContextType().getValue());
        response.setProgrammingLanguage(conversation.getProgrammingLanguage());
        response.setCreatedAt(conversation.getCreatedAt());
        response.setUpdatedAt(conversation.getUpdatedAt());
        response.setIsActive(conversation.getIsActive());
        response.setMessageCount(conversation.getMessages() != null ? conversation.getMessages().size() : 0);
        return response;
    }
} 