package com.codemate.service;

import com.codemate.exception.ResourceNotFoundException;
import com.codemate.model.User;
import com.codemate.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class AdminUserService {
    
    private final UserRepository userRepository;
    private final PointTransactionService pointTransactionService;
    private final PointAwardHelper pointAwardHelper;
    
    public AdminUserService(UserRepository userRepository,
                           PointTransactionService pointTransactionService,
                           PointAwardHelper pointAwardHelper) {
        this.userRepository = userRepository;
        this.pointTransactionService = pointTransactionService;
        this.pointAwardHelper = pointAwardHelper;
    }
    
    /**
     * Get all users
     */
    @Transactional(readOnly = true)
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }
    
    /**
     * Get user by ID
     */
    @Transactional(readOnly = true)
    public User getUserById(Long userId) {
        return userRepository.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));
    }
    
    /**
     * Get user by email
     */
    @Transactional(readOnly = true)
    public User getUserByEmail(String email) {
        return userRepository.findByEmail(email)
            .orElseThrow(() -> new ResourceNotFoundException("User", "email", email));
    }
    
    /**
     * Get user's point balance
     */
    @Transactional(readOnly = true)
    public Long getUserPointBalance(Long userId) {
        // Verify user exists
        if (!userRepository.existsById(userId)) {
            throw new ResourceNotFoundException("User", "id", userId);
        }
        return pointTransactionService.getCurrentPoints(userId);
    }
    
    /**
     * Get user's total points earned
     */
    @Transactional(readOnly = true)
    public Long getUserTotalPointsEarned(Long userId) {
        // Verify user exists
        if (!userRepository.existsById(userId)) {
            throw new ResourceNotFoundException("User", "id", userId);
        }
        return pointTransactionService.getTotalPointsEarned(userId);
    }
    
    /**
     * Grant points to a user (admin action)
     */
    public void grantPointsToUser(Long userId, Long amount, String reason) {
        // Verify user exists
        if (!userRepository.existsById(userId)) {
            throw new ResourceNotFoundException("User", "id", userId);
        }
        
        if (amount <= 0) {
            throw new IllegalArgumentException("Point amount must be positive");
        }
        
        pointAwardHelper.awardAdminPoints(userId, amount, reason);
    }
    
    /**
     * Update user status (enable/disable)
     */
    public void updateUserStatus(Long userId, boolean enabled) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));
        
        user.setEnabled(enabled);
        userRepository.save(user);
    }
    
    /**
     * Get user statistics
     */
    @Transactional(readOnly = true)
    public UserStats getUserStats(Long userId) {
        // Verify user exists
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));
        
        Long currentPoints = pointTransactionService.getCurrentPoints(userId);
        Long totalEarned = pointTransactionService.getTotalPointsEarned(userId);
        Long totalSpent = pointTransactionService.getTotalPointsSpent(userId);
        
        return new UserStats(user.getEmail(), currentPoints, totalEarned, totalSpent);
    }
    
    /**
     * Get system-wide user statistics
     */
    @Transactional(readOnly = true)
    public SystemStats getSystemStats() {
        long totalUsers = userRepository.count();
        // Additional system statistics can be added here
        return new SystemStats(totalUsers);
    }
    
    // Inner classes for statistics
    public static class UserStats {
        private final String email;
        private final Long currentPoints;
        private final Long totalEarned;
        private final Long totalSpent;
        
        public UserStats(String email, Long currentPoints, Long totalEarned, Long totalSpent) {
            this.email = email;
            this.currentPoints = currentPoints;
            this.totalEarned = totalEarned;
            this.totalSpent = totalSpent;
        }
        
        public String getEmail() { return email; }
        public Long getCurrentPoints() { return currentPoints; }
        public Long getTotalEarned() { return totalEarned; }
        public Long getTotalSpent() { return totalSpent; }
    }
    
    public static class SystemStats {
        private final long totalUsers;
        
        public SystemStats(long totalUsers) {
            this.totalUsers = totalUsers;
        }
        
        public long getTotalUsers() { return totalUsers; }
    }
} 