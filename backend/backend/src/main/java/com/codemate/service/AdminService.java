package com.codemate.service;

import com.codemate.repository.PetRepository;
import com.codemate.repository.PointTransactionRepository;
import com.codemate.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Service
public class AdminService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PetRepository petRepository;

    @Autowired
    private PointTransactionRepository pointTransactionRepository;

    public Map<String, Object> getAdminStats() {
        Map<String, Object> stats = new HashMap<>();
        
        // User statistics
        long totalUsers = userRepository.count();
        long activeUsers = userRepository.countByEnabledTrue();
        
        // Pet statistics
        long totalPets = petRepository.count();
        long happyPets = petRepository.countByHappinessGreaterThan(80);
        
        // Point statistics
        Long totalPointsAwarded = pointTransactionRepository.sumEarnedPoints();
        if (totalPointsAwarded == null) {
            totalPointsAwarded = 0L;
        }
        
        // Recent signups (last 7 days)
        LocalDateTime sevenDaysAgo = LocalDateTime.now().minusDays(7);
        long recentSignups = userRepository.countByCreatedAtAfter(sevenDaysAgo);
        
        stats.put("totalUsers", totalUsers);
        stats.put("activeUsers", activeUsers);
        stats.put("totalPets", totalPets);
        stats.put("happyPets", happyPets);
        stats.put("totalPointsAwarded", totalPointsAwarded);
        stats.put("recentSignups", recentSignups);
        
        return stats;
    }
} 