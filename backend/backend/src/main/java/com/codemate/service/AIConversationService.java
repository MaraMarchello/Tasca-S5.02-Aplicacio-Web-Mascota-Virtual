package com.codemate.service;

import com.codemate.exception.ResourceNotFoundException;
import com.codemate.model.AIConversation;
import com.codemate.model.AIMessage;
import com.codemate.model.User;
import com.codemate.repository.AIConversationRepository;
import com.codemate.repository.AIMessageRepository;
import com.codemate.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class AIConversationService {

    private final AIConversationRepository conversationRepository;
    private final AIMessageRepository messageRepository;
    private final UserRepository userRepository;

    // Maximum number of messages to keep in memory for context
    private static final int MAX_CONTEXT_MESSAGES = 20;
    private static final int MAX_CONVERSATIONS_PER_USER = 100;

    /**
     * Get or create a conversation for the user and context type
     */
    public AIConversation getOrCreateConversation(Long userId, AIConversation.ContextType contextType, 
                                                String programmingLanguage) {
        log.debug("Getting or creating conversation for user: {}, context: {}, language: {}", 
                 userId, contextType, programmingLanguage);
        
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        // Try to find an existing active conversation
        Optional<AIConversation> existingConversation = 
                conversationRepository.findFirstByUserAndContextTypeAndIsActiveTrueOrderByUpdatedAtDesc(
                        user, contextType);

        if (existingConversation.isPresent()) {
            AIConversation conversation = existingConversation.get();
            // Update programming language if different
            if (programmingLanguage != null && !programmingLanguage.equals(conversation.getProgrammingLanguage())) {
                conversation.setProgrammingLanguage(programmingLanguage);
                conversation = conversationRepository.save(conversation);
            }
            log.debug("Found existing conversation: {}", conversation.getId());
            return conversation;
        }

        // Create new conversation
        AIConversation newConversation = new AIConversation();
        newConversation.setUser(user);
        newConversation.setContextType(contextType);
        newConversation.setProgrammingLanguage(programmingLanguage != null ? programmingLanguage : "java");
        
        // Check if user has too many conversations
        long userConversationCount = conversationRepository.countByUserAndIsActiveTrue(user);
        if (userConversationCount >= MAX_CONVERSATIONS_PER_USER) {
            // Archive oldest conversation
            List<AIConversation> oldestConversations = conversationRepository
                    .findByUserAndIsActiveTrueOrderByUpdatedAtDesc(user);
            if (!oldestConversations.isEmpty()) {
                AIConversation oldest = oldestConversations.get(oldestConversations.size() - 1);
                oldest.setIsActive(false);
                conversationRepository.save(oldest);
                log.info("Archived oldest conversation {} for user {}", oldest.getId(), userId);
            }
        }

        newConversation = conversationRepository.save(newConversation);
        log.info("Created new conversation: {} for user: {}", newConversation.getId(), userId);
        return newConversation;
    }

    /**
     * Add a message to a conversation
     */
    public AIMessage addMessage(Long conversationId, Long userId, AIMessage.MessageType messageType, 
                              String content, String codeSnippet, Map<String, Object> contextData) {
        log.debug("Adding message to conversation: {}, type: {}", conversationId, messageType);
        
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));
        
        AIConversation conversation = conversationRepository.findByIdAndUser(conversationId, user)
                .orElseThrow(() -> new ResourceNotFoundException("Conversation", "id", conversationId));

        AIMessage message = new AIMessage();
        message.setConversation(conversation);
        message.setMessageType(messageType);
        message.setContent(content);
        message.setCodeSnippet(codeSnippet);
        message.setContextData(contextData);
        message.setProgrammingLanguage(conversation.getProgrammingLanguage());

        message = messageRepository.save(message);
        conversation.addMessage(message);
        conversationRepository.save(conversation);

        log.debug("Added message: {} to conversation: {}", message.getId(), conversationId);
        return message;
    }

    /**
     * Get conversation history with context for AI
     */
    public List<AIMessage> getConversationContext(Long conversationId, Long userId) {
        log.debug("Getting conversation context for conversation: {}", conversationId);
        
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));
        
        AIConversation conversation = conversationRepository.findByIdAndUser(conversationId, user)
                .orElseThrow(() -> new ResourceNotFoundException("Conversation", "id", conversationId));

        // Get recent messages for context
        Pageable pageable = PageRequest.of(0, MAX_CONTEXT_MESSAGES);
        List<AIMessage> recentMessages = messageRepository.findLastNMessages(conversation, pageable);
        
        // Reverse to get chronological order
        recentMessages.sort((m1, m2) -> m1.getCreatedAt().compareTo(m2.getCreatedAt()));
        
        log.debug("Retrieved {} messages for context", recentMessages.size());
        return recentMessages;
    }

    /**
     * Get user's conversations
     */
    @Transactional(readOnly = true)
    public Page<AIConversation> getUserConversations(Long userId, Pageable pageable) {
        log.debug("Getting conversations for user: {}", userId);
        
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));
        
        return conversationRepository.findByUserAndIsActiveTrueOrderByUpdatedAtDesc(user, pageable);
    }

    /**
     * Get conversation by ID
     */
    @Transactional(readOnly = true)
    public AIConversation getConversation(Long conversationId, Long userId) {
        log.debug("Getting conversation: {} for user: {}", conversationId, userId);
        
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));
        
        return conversationRepository.findByIdAndUser(conversationId, user)
                .orElseThrow(() -> new ResourceNotFoundException("Conversation", "id", conversationId));
    }

    /**
     * Get conversation messages
     */
    @Transactional(readOnly = true)
    public List<AIMessage> getConversationMessages(Long conversationId, Long userId) {
        log.debug("Getting messages for conversation: {}", conversationId);
        
        AIConversation conversation = getConversation(conversationId, userId);
        return messageRepository.findByConversationOrderByCreatedAtAsc(conversation);
    }

    /**
     * Archive a conversation
     */
    public void archiveConversation(Long conversationId, Long userId) {
        log.debug("Archiving conversation: {} for user: {}", conversationId, userId);
        
        AIConversation conversation = getConversation(conversationId, userId);
        conversation.setIsActive(false);
        conversationRepository.save(conversation);
        
        log.info("Archived conversation: {}", conversationId);
    }

    /**
     * Delete a conversation and all its messages
     */
    public void deleteConversation(Long conversationId, Long userId) {
        log.debug("Deleting conversation: {} for user: {}", conversationId, userId);
        
        AIConversation conversation = getConversation(conversationId, userId);
        conversationRepository.delete(conversation);
        
        log.info("Deleted conversation: {}", conversationId);
    }

    /**
     * Search conversations
     */
    @Transactional(readOnly = true)
    public List<AIConversation> searchConversations(Long userId, String searchTerm) {
        log.debug("Searching conversations for user: {} with term: {}", userId, searchTerm);
        
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));
        
        return conversationRepository.searchConversations(user, searchTerm);
    }

    /**
     * Get conversation statistics
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getConversationStats(Long userId) {
        log.debug("Getting conversation stats for user: {}", userId);
        
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));
        
        Map<String, Object> stats = new HashMap<>();
        
        // Total conversations
        long totalConversations = conversationRepository.countByUserAndIsActiveTrue(user);
        stats.put("totalConversations", totalConversations);
        
        // Conversations by type
        List<Object[]> typeStats = conversationRepository.getConversationStatsByUser(user);
        Map<String, Long> conversationsByType = new HashMap<>();
        for (Object[] stat : typeStats) {
            conversationsByType.put(stat[0].toString(), (Long) stat[1]);
        }
        stats.put("conversationsByType", conversationsByType);
        
        // Recent activity
        LocalDateTime weekAgo = LocalDateTime.now().minusWeeks(1);
        List<AIConversation> recentConversations = conversationRepository.findRecentConversations(user, weekAgo);
        stats.put("recentConversations", recentConversations.size());
        
        return stats;
    }

    /**
     * Clean up old inactive conversations (maintenance task)
     */
    @Transactional
    public void cleanupOldConversations() {
        log.info("Starting cleanup of old inactive conversations");
        
        LocalDateTime cutoffDate = LocalDateTime.now().minusMonths(6);
        List<AIConversation> oldConversations = conversationRepository.findOldInactiveConversations(cutoffDate);
        
        for (AIConversation conversation : oldConversations) {
            conversationRepository.delete(conversation);
        }
        
        log.info("Cleaned up {} old conversations", oldConversations.size());
    }

    /**
     * Update conversation title
     */
    public AIConversation updateConversationTitle(Long conversationId, Long userId, String newTitle) {
        log.debug("Updating title for conversation: {} to: {}", conversationId, newTitle);
        
        AIConversation conversation = getConversation(conversationId, userId);
        conversation.setTitle(newTitle);
        conversation = conversationRepository.save(conversation);
        
        log.info("Updated conversation title: {}", conversationId);
        return conversation;
    }
} 