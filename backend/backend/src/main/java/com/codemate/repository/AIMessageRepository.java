package com.codemate.repository;

import com.codemate.model.AIConversation;
import com.codemate.model.AIMessage;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AIMessageRepository extends JpaRepository<AIMessage, Long> {

    // Find messages by conversation
    List<AIMessage> findByConversationOrderByCreatedAtAsc(AIConversation conversation);
    
    Page<AIMessage> findByConversationOrderByCreatedAtAsc(AIConversation conversation, Pageable pageable);

    // Find recent messages in conversation
    @Query("SELECT m FROM AIMessage m WHERE m.conversation = :conversation " +
           "ORDER BY m.createdAt DESC")
    List<AIMessage> findRecentMessages(@Param("conversation") AIConversation conversation, 
                                     Pageable pageable);

    // Find messages by type
    List<AIMessage> findByConversationAndMessageTypeOrderByCreatedAtAsc(
            AIConversation conversation, AIMessage.MessageType messageType);

    // Count messages in conversation
    long countByConversation(AIConversation conversation);

    // Count messages by type
    long countByConversationAndMessageType(AIConversation conversation, AIMessage.MessageType messageType);

    // Find messages with code snippets
    @Query("SELECT m FROM AIMessage m WHERE m.conversation = :conversation " +
           "AND m.codeSnippet IS NOT NULL AND m.codeSnippet != '' " +
           "ORDER BY m.createdAt ASC")
    List<AIMessage> findMessagesWithCode(@Param("conversation") AIConversation conversation);

    // Search messages by content
    @Query("SELECT m FROM AIMessage m WHERE m.conversation = :conversation " +
           "AND LOWER(m.content) LIKE LOWER(CONCAT('%', :searchTerm, '%')) " +
           "ORDER BY m.createdAt ASC")
    List<AIMessage> searchMessagesByContent(@Param("conversation") AIConversation conversation,
                                          @Param("searchTerm") String searchTerm);

    // Find messages by programming language
    List<AIMessage> findByConversationAndProgrammingLanguageOrderByCreatedAtAsc(
            AIConversation conversation, String programmingLanguage);

    // Get last N messages from conversation
    @Query("SELECT m FROM AIMessage m WHERE m.conversation = :conversation " +
           "ORDER BY m.createdAt DESC")
    List<AIMessage> findLastNMessages(@Param("conversation") AIConversation conversation,
                                    Pageable pageable);

    // Find messages created after specific time
    List<AIMessage> findByConversationAndCreatedAtAfterOrderByCreatedAtAsc(
            AIConversation conversation, LocalDateTime after);

    // Clean up old messages (for maintenance)
    @Query("SELECT m FROM AIMessage m JOIN m.conversation c " +
           "WHERE c.isActive = false AND m.createdAt < :cutoffDate")
    List<AIMessage> findOldMessagesInInactiveConversations(@Param("cutoffDate") LocalDateTime cutoffDate);

    // Get message statistics
    @Query("SELECT m.messageType, COUNT(m) FROM AIMessage m " +
           "WHERE m.conversation = :conversation " +
           "GROUP BY m.messageType")
    List<Object[]> getMessageStatsByConversation(@Param("conversation") AIConversation conversation);

    // Find latest assistant message in conversation
    @Query("SELECT m FROM AIMessage m WHERE m.conversation = :conversation " +
           "AND m.messageType = 'ASSISTANT' ORDER BY m.createdAt DESC")
    List<AIMessage> findLatestAssistantMessage(@Param("conversation") AIConversation conversation,
                                             Pageable pageable);

    // Find user messages with context data
    @Query("SELECT m FROM AIMessage m WHERE m.conversation = :conversation " +
           "AND m.messageType = 'USER' AND m.contextData IS NOT NULL " +
           "ORDER BY m.createdAt ASC")
    List<AIMessage> findUserMessagesWithContext(@Param("conversation") AIConversation conversation);

    // Analytics methods
    @Query("SELECT COUNT(m) FROM AIMessage m WHERE m.conversation.user.id = :userId AND m.createdAt >= :startDate")
    long countByConversationUserIdAndCreatedAtAfter(@Param("userId") Long userId, @Param("startDate") LocalDateTime startDate);
    
    @Query("SELECT COUNT(m) FROM AIMessage m WHERE m.createdAt >= :startDate")
    long countByCreatedAtAfter(@Param("startDate") LocalDateTime startDate);
    
    @Query("SELECT COUNT(m) FROM AIMessage m WHERE m.conversation.user.id = :userId AND m.createdAt BETWEEN :startDate AND :endDate")
    long countByConversationUserIdAndCreatedAtBetween(@Param("userId") Long userId, @Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);
    
    List<AIMessage> findByConversationIdOrderByCreatedAt(Long conversationId);
} 