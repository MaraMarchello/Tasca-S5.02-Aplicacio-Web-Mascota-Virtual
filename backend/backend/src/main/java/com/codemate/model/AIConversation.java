package com.codemate.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "ai_conversations")
@Data
@EqualsAndHashCode(exclude = {"messages"})
@ToString(exclude = {"messages"})
public class AIConversation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private User user;

    @Column(name = "title")
    private String title;

    @Enumerated(EnumType.STRING)
    @Column(name = "context_type", nullable = false)
    private ContextType contextType = ContextType.GENERAL;

    @Column(name = "programming_language")
    private String programmingLanguage = "java";

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @OneToMany(mappedBy = "conversation", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @OrderBy("createdAt ASC")
    @JsonManagedReference
    private List<AIMessage> messages = new ArrayList<>();

    public enum ContextType {
        GENERAL("general"),
        DEBUG("debug"),
        EXPLAIN("explain"),
        REFACTOR("refactor"),
        GENERATE("generate");

        private final String value;

        ContextType(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }

        public static ContextType fromString(String value) {
            for (ContextType type : ContextType.values()) {
                if (type.value.equalsIgnoreCase(value)) {
                    return type;
                }
            }
            return GENERAL;
        }
    }

    // Helper methods
    public void addMessage(AIMessage message) {
        messages.add(message);
        message.setConversation(this);
        this.updatedAt = LocalDateTime.now();
    }

    public AIMessage getLastMessage() {
        return messages.isEmpty() ? null : messages.get(messages.size() - 1);
    }

    public List<AIMessage> getRecentMessages(int limit) {
        int size = messages.size();
        int fromIndex = Math.max(0, size - limit);
        return messages.subList(fromIndex, size);
    }

    public String generateTitle() {
        if (messages.isEmpty()) {
            return "New " + contextType.getValue() + " conversation";
        }
        
        AIMessage firstUserMessage = messages.stream()
                .filter(msg -> msg.getMessageType() == AIMessage.MessageType.USER)
                .findFirst()
                .orElse(null);
        
        if (firstUserMessage != null) {
            String content = firstUserMessage.getContent();
            if (content.length() > 50) {
                return content.substring(0, 47) + "...";
            }
            return content;
        }
        
        return "AI Conversation - " + programmingLanguage;
    }

    // Lifecycle callbacks
    @PrePersist
    public void prePersist() {
        if (title == null || title.trim().isEmpty()) {
            this.title = generateTitle();
        }
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
} 