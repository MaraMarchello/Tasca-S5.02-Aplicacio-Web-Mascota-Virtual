package com.codemate.repository;

import com.codemate.model.AIConversation;
import com.codemate.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Repository
public interface AIConversationRepository extends JpaRepository<AIConversation, Long> {

    // Find conversations by user
    Page<AIConversation> findByUserAndIsActiveTrueOrderByUpdatedAtDesc(User user, Pageable pageable);
    
    List<AIConversation> findByUserAndIsActiveTrueOrderByUpdatedAtDesc(User user);

    // Find conversations by user and context type
    Page<AIConversation> findByUserAndContextTypeAndIsActiveTrueOrderByUpdatedAtDesc(
            User user, AIConversation.ContextType contextType, Pageable pageable);

    // Find active conversation by user and context type (for continuing conversations)
    Optional<AIConversation> findFirstByUserAndContextTypeAndIsActiveTrueOrderByUpdatedAtDesc(
            User user, AIConversation.ContextType contextType);

    // Find conversation by ID and user (for security)
    Optional<AIConversation> findByIdAndUser(Long id, User user);

    // Count active conversations by user
    long countByUserAndIsActiveTrue(User user);

    // Find recent conversations
    @Query("SELECT c FROM AIConversation c WHERE c.user = :user AND c.isActive = true " +
           "AND c.updatedAt >= :since ORDER BY c.updatedAt DESC")
    List<AIConversation> findRecentConversations(@Param("user") User user, 
                                                @Param("since") LocalDateTime since);

    // Find conversations with messages count
    @Query("SELECT c FROM AIConversation c LEFT JOIN FETCH c.messages m " +
           "WHERE c.user = :user AND c.isActive = true " +
           "ORDER BY c.updatedAt DESC")
    List<AIConversation> findConversationsWithMessages(@Param("user") User user);

    // Search conversations by title or content
    @Query("SELECT DISTINCT c FROM AIConversation c LEFT JOIN c.messages m " +
           "WHERE c.user = :user AND c.isActive = true " +
           "AND (LOWER(c.title) LIKE LOWER(CONCAT('%', :searchTerm, '%')) " +
           "OR LOWER(m.content) LIKE LOWER(CONCAT('%', :searchTerm, '%'))) " +
           "ORDER BY c.updatedAt DESC")
    List<AIConversation> searchConversations(@Param("user") User user, 
                                           @Param("searchTerm") String searchTerm);

    // Clean up old inactive conversations
    @Query("SELECT c FROM AIConversation c WHERE c.isActive = false " +
           "AND c.updatedAt < :cutoffDate")
    List<AIConversation> findOldInactiveConversations(@Param("cutoffDate") LocalDateTime cutoffDate);

    // Find conversations by programming language
    List<AIConversation> findByUserAndProgrammingLanguageAndIsActiveTrueOrderByUpdatedAtDesc(
            User user, String programmingLanguage);

    // Get conversation statistics
    @Query("SELECT c.contextType, COUNT(c) FROM AIConversation c " +
           "WHERE c.user = :user AND c.isActive = true " +
           "GROUP BY c.contextType")
    List<Object[]> getConversationStatsByUser(@Param("user") User user);

    // Analytics methods
    @Query("SELECT COUNT(c) FROM AIConversation c WHERE c.user.id = :userId AND c.createdAt >= :startDate")
    long countByUserIdAndCreatedAtAfter(@Param("userId") Long userId, @Param("startDate") LocalDateTime startDate);
    
    @Query("SELECT COUNT(c) FROM AIConversation c WHERE c.createdAt >= :startDate")
    long countByCreatedAtAfter(@Param("startDate") LocalDateTime startDate);
    
    @Query("SELECT COUNT(DISTINCT c.user.id) FROM AIConversation c WHERE c.createdAt >= :startDate")
    long countDistinctUsersByCreatedAtAfter(@Param("startDate") LocalDateTime startDate);
    
    @Query("SELECT c.contextType, COUNT(c) FROM AIConversation c WHERE c.user.id = :userId AND c.createdAt >= :startDate GROUP BY c.contextType")
    List<Object[]> findContextTypeDistributionRaw(@Param("userId") Long userId, @Param("startDate") LocalDateTime startDate);
    
    default Map<String, Long> findContextTypeDistribution(Long userId, LocalDateTime startDate) {
        List<Object[]> results = findContextTypeDistributionRaw(userId, startDate);
        Map<String, Long> distribution = new HashMap<>();
        for (Object[] result : results) {
            distribution.put(result[0].toString(), (Long) result[1]);
        }
        return distribution;
    }
    
    @Query("SELECT c.user.id, c.user.name, COUNT(c), " +
           "(SELECT COUNT(m) FROM AIMessage m WHERE m.conversation.user.id = c.user.id AND m.createdAt >= :startDate) " +
           "FROM AIConversation c WHERE c.createdAt >= :startDate " +
           "GROUP BY c.user.id, c.user.name ORDER BY COUNT(c) DESC")
    List<Object[]> findTopActiveUsers(@Param("startDate") LocalDateTime startDate, Pageable pageable);
} 