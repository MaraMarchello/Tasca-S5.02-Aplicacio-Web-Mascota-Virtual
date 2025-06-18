package com.codemate.repository;

import com.codemate.model.PointTransaction;
import com.codemate.model.TransactionType;
import com.codemate.model.PointSource;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Date;
import java.util.List;

@Repository
public interface PointTransactionRepository extends JpaRepository<PointTransaction, Long> {
    
    // Find transactions by user
    List<PointTransaction> findByUserIdOrderByCreatedAtDesc(Long userId);
    
    // Find transactions by user with pagination
    List<PointTransaction> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);
    
    // Calculate total points by type for a user
    @Query("SELECT SUM(pt.amount) FROM PointTransaction pt WHERE pt.userId = :userId AND pt.type = :type")
    Long sumByUserIdAndType(@Param("userId") Long userId, @Param("type") TransactionType type);
    
    // Find transactions by source
    List<PointTransaction> findByUserIdAndSource(Long userId, PointSource source);
    
    // Check if user has transaction today for specific source (for daily login)
    @Query("SELECT COUNT(pt) > 0 FROM PointTransaction pt WHERE pt.userId = :userId AND pt.source = :source AND pt.createdAt >= :startOfDay AND pt.createdAt < :endOfDay")
    boolean existsByUserIdAndSourceAndCreatedAtBetween(@Param("userId") Long userId, 
                                                      @Param("source") PointSource source, 
                                                      @Param("startOfDay") Date startOfDay, 
                                                      @Param("endOfDay") Date endOfDay);
    
    // Get recent transactions for a user
    List<PointTransaction> findTop10ByUserIdOrderByCreatedAtDesc(Long userId);
    
    // Count transactions by source for achievements
    long countByUserIdAndSource(Long userId, PointSource source);
    
    // Sum all earned points for admin stats
    @Query("SELECT COALESCE(SUM(pt.amount), 0) FROM PointTransaction pt WHERE pt.type = 'EARNED'")
    Long sumEarnedPoints();
    
    // Get recent transactions for admin view with users
    @Query("SELECT pt FROM PointTransaction pt JOIN FETCH pt.user ORDER BY pt.createdAt DESC")
    List<PointTransaction> findRecentTransactionsWithUsers(Pageable pageable);
} 