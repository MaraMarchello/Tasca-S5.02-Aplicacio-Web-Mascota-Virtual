package com.codemate.model;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.Map;

@Entity
@Table(name = "ai_messages")
@Data
@EqualsAndHashCode(exclude = {"conversation"})
@ToString(exclude = {"conversation"})
public class AIMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "conversation_id", nullable = false)
    @JsonBackReference
    private AIConversation conversation;

    @Enumerated(EnumType.STRING)
    @Column(name = "message_type", nullable = false)
    private MessageType messageType;

    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(name = "code_snippet", columnDefinition = "TEXT")
    private String codeSnippet;

    @Column(name = "programming_language")
    private String programmingLanguage;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "context_data", columnDefinition = "jsonb")
    private Map<String, Object> contextData;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public enum MessageType {
        USER("user"),
        ASSISTANT("assistant"),
        SYSTEM("system");

        private final String value;

        MessageType(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }

        public static MessageType fromString(String value) {
            for (MessageType type : MessageType.values()) {
                if (type.value.equalsIgnoreCase(value)) {
                    return type;
                }
            }
            return USER;
        }
    }

    // Helper methods
    public boolean isUserMessage() {
        return messageType == MessageType.USER;
    }

    public boolean isAssistantMessage() {
        return messageType == MessageType.ASSISTANT;
    }

    public boolean isSystemMessage() {
        return messageType == MessageType.SYSTEM;
    }

    public boolean hasCodeSnippet() {
        return codeSnippet != null && !codeSnippet.trim().isEmpty();
    }

    public boolean hasContextData() {
        return contextData != null && !contextData.isEmpty();
    }

    // Factory methods for creating messages
    public static AIMessage createUserMessage(String content) {
        AIMessage message = new AIMessage();
        message.setMessageType(MessageType.USER);
        message.setContent(content);
        return message;
    }

    public static AIMessage createAssistantMessage(String content) {
        AIMessage message = new AIMessage();
        message.setMessageType(MessageType.ASSISTANT);
        message.setContent(content);
        return message;
    }

    public static AIMessage createSystemMessage(String content) {
        AIMessage message = new AIMessage();
        message.setMessageType(MessageType.SYSTEM);
        message.setContent(content);
        return message;
    }

    public static AIMessage createUserMessageWithCode(String content, String codeSnippet, String language) {
        AIMessage message = createUserMessage(content);
        message.setCodeSnippet(codeSnippet);
        message.setProgrammingLanguage(language);
        return message;
    }

    public static AIMessage createAssistantMessageWithCode(String content, String codeSnippet, String language) {
        AIMessage message = createAssistantMessage(content);
        message.setCodeSnippet(codeSnippet);
        message.setProgrammingLanguage(language);
        return message;
    }

    // Lifecycle callbacks
    @PrePersist
    public void prePersist() {
        if (programmingLanguage == null && conversation != null) {
            this.programmingLanguage = conversation.getProgrammingLanguage();
        }
    }
} 