package com.codemate.service;

import com.codemate.model.PointTransaction;
import com.codemate.model.TransactionType;
import com.codemate.model.PointSource;
import com.codemate.repository.PointTransactionRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;

@Service
@Transactional
public class PointTransactionService {
    
    private final PointTransactionRepository pointTransactionRepository;
    
    public PointTransactionService(PointTransactionRepository pointTransactionRepository) {
        this.pointTransactionRepository = pointTransactionRepository;
    }
    
    /**
     * Create a new point transaction
     */
    public PointTransaction createTransaction(Long userId, TransactionType type, 
                                            PointSource source, Long amount, String description) {
        return createTransaction(userId, type, source, amount, description, null);
    }
    
    /**
     * Create a new point transaction with reference ID
     */
    public PointTransaction createTransaction(Long userId, TransactionType type, 
                                            PointSource source, Long amount, String description, String referenceId) {
        
        PointTransaction transaction = new PointTransaction();
        transaction.setUserId(userId);
        transaction.setType(type);
        transaction.setSource(source);
        transaction.setAmount(amount);
        transaction.setDescription(description);
        transaction.setReferenceId(referenceId);
        
        return pointTransactionRepository.save(transaction);
    }
    
    /**
     * Get current point balance for a user
     */
    @Transactional(readOnly = true)
    public Long getCurrentPoints(Long userId) {
        Long earned = pointTransactionRepository.sumByUserIdAndType(userId, TransactionType.EARNED);
        Long spent = pointTransactionRepository.sumByUserIdAndType(userId, TransactionType.SPENT);
        return (earned != null ? earned : 0L) - (spent != null ? spent : 0L);
    }
    
    /**
     * Get total points earned by a user
     */
    @Transactional(readOnly = true)
    public Long getTotalPointsEarned(Long userId) {
        Long earned = pointTransactionRepository.sumByUserIdAndType(userId, TransactionType.EARNED);
        return earned != null ? earned : 0L;
    }
    
    /**
     * Get total points spent by a user
     */
    @Transactional(readOnly = true)
    public Long getTotalPointsSpent(Long userId) {
        Long spent = pointTransactionRepository.sumByUserIdAndType(userId, TransactionType.SPENT);
        return spent != null ? spent : 0L;
    }
    
    /**
     * Get transaction history for a user
     */
    @Transactional(readOnly = true)
    public List<PointTransaction> getTransactionHistory(Long userId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return pointTransactionRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);
    }
    
    /**
     * Get recent transactions for a user
     */
    @Transactional(readOnly = true)
    public List<PointTransaction> getRecentTransactions(Long userId) {
        return pointTransactionRepository.findTop10ByUserIdOrderByCreatedAtDesc(userId);
    }
    
    /**
     * Check if user has already been awarded points today for a specific source
     */
    @Transactional(readOnly = true)
    public boolean hasAwardedToday(Long userId, PointSource source) {
        LocalDate today = LocalDate.now();
        Date startOfDay = Date.from(today.atStartOfDay(ZoneId.systemDefault()).toInstant());
        Date endOfDay = Date.from(today.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant());
        
        return pointTransactionRepository.existsByUserIdAndSourceAndCreatedAtBetween(
            userId, source, startOfDay, endOfDay);
    }
    
    /**
     * Award points for stack trace resolution
     */
    public PointTransaction awardStackTracePoints(Long userId, String stackTraceId) {
        return createTransaction(userId, TransactionType.EARNED, PointSource.STACK_TRACE_RESOLVED,
            PointSource.STACK_TRACE_RESOLVED.getBasePoints(),
            "Resolved stack trace error", stackTraceId);
    }
    
    /**
     * Award points for AI chat usage
     */
    public PointTransaction awardAIChatPoints(Long userId, String chatSessionId) {
        return createTransaction(userId, TransactionType.EARNED, PointSource.AI_CHAT_USAGE,
            PointSource.AI_CHAT_USAGE.getBasePoints(),
            "Used AI chat assistance", chatSessionId);
    }
    
    /**
     * Award daily login points (only once per day)
     */
    public PointTransaction awardDailyLoginPoints(Long userId) {
        if (!hasAwardedToday(userId, PointSource.DAILY_LOGIN)) {
            return createTransaction(userId, TransactionType.EARNED, PointSource.DAILY_LOGIN,
                PointSource.DAILY_LOGIN.getBasePoints(),
                "Daily login bonus");
        }
        return null; // Already awarded today
    }
    
    /**
     * Get count of transactions by source (for achievements)
     */
    @Transactional(readOnly = true)
    public long getTransactionCountBySource(Long userId, PointSource source) {
        return pointTransactionRepository.countByUserIdAndSource(userId, source);
    }

    // Admin methods
    /**
     * Get recent transactions for admin view
     */
    @Transactional(readOnly = true)
    public List<PointTransaction> getRecentTransactions(int limit) {
        Pageable pageable = PageRequest.of(0, limit);
        return pointTransactionRepository.findRecentTransactionsWithUsers(pageable);
    }

    /**
     * Award points by admin
     */
    public PointTransaction awardPointsByAdmin(Long userId, Integer amount, String description) {
        return createTransaction(userId, TransactionType.EARNED, PointSource.ADMIN_GRANT,
            amount.longValue(), description);
    }
} 