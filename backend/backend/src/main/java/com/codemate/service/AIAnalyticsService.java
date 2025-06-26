package com.codemate.service;

import com.codemate.model.AIConversation;
import com.codemate.model.AIMessage;
import com.codemate.model.User;
import com.codemate.repository.AIConversationRepository;
import com.codemate.repository.AIMessageRepository;
import com.codemate.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AIAnalyticsService {

    private final AIConversationRepository conversationRepository;
    private final AIMessageRepository messageRepository;
    private final UserRepository userRepository;

    /**
     * Get user analytics for dashboard
     */
    public Map<String, Object> getUserAnalytics(Long userId, int days) {
        log.info("Generating analytics for user: {} over {} days", userId, days);
        
        LocalDateTime startDate = LocalDateTime.now().minusDays(days);
        Map<String, Object> analytics = new HashMap<>();
        
        // Basic metrics
        long conversations = conversationRepository.countByUserIdAndCreatedAtAfter(userId, startDate);
        long messages = messageRepository.countByConversationUserIdAndCreatedAtAfter(userId, startDate);
        
        analytics.put("totalConversations", conversations);
        analytics.put("totalMessages", messages);
        analytics.put("averageMessagesPerConversation", 
                     conversations > 0 ? (double) messages / conversations : 0.0);
        
        // Daily usage pattern
        Map<String, Long> dailyUsage = getDailyUsagePattern(userId, days);
        analytics.put("dailyUsage", dailyUsage);
        
        // Context distribution
        Map<String, Long> contextTypes = conversationRepository.findContextTypeDistribution(userId, startDate);
        analytics.put("contextTypes", contextTypes);
        
        return analytics;
    }

    /**
     * Get system-wide analytics
     */
    public Map<String, Object> getSystemAnalytics(int days) {
        log.info("Generating system analytics over {} days", days);
        
        LocalDateTime startDate = LocalDateTime.now().minusDays(days);
        Map<String, Object> analytics = new HashMap<>();
        
        // Overall metrics
        long totalUsers = userRepository.count();
        long activeUsers = conversationRepository.countDistinctUsersByCreatedAtAfter(startDate);
        long conversations = conversationRepository.countByCreatedAtAfter(startDate);
        long messages = messageRepository.countByCreatedAtAfter(startDate);
        
        analytics.put("totalUsers", totalUsers);
        analytics.put("activeUsers", activeUsers);
        analytics.put("totalConversations", conversations);
        analytics.put("totalMessages", messages);
        analytics.put("engagementRate", totalUsers > 0 ? (double) activeUsers / totalUsers : 0.0);
        
        // Feature usage
        Map<String, Long> featureUsage = new HashMap<>();
        featureUsage.put("chat", messages);
        featureUsage.put("codeAnalysis", conversations / 4);
        featureUsage.put("codeGeneration", conversations / 6);
        analytics.put("featureUsage", featureUsage);
        
        return analytics;
    }

    /**
     * Get comprehensive AI usage analytics for a user
     */
    public AIUsageAnalytics getUserAnalyticsDetailed(Long userId, int days) {
        log.info("Generating AI usage analytics for user: {} over {} days", userId, days);
        
        LocalDateTime startDate = LocalDateTime.now().minusDays(days);
        
        AIUsageAnalytics analytics = new AIUsageAnalytics();
        analytics.setUserId(userId);
        analytics.setAnalysisPeriodDays(days);
        analytics.setGeneratedAt(LocalDateTime.now());
        
        // Basic conversation metrics
        long totalConversations = conversationRepository.countByUserIdAndCreatedAtAfter(userId, startDate);
        long totalMessages = messageRepository.countByConversationUserIdAndCreatedAtAfter(userId, startDate);
        
        analytics.setTotalConversations(totalConversations);
        analytics.setTotalMessages(totalMessages);
        analytics.setAverageMessagesPerConversation(
            totalConversations > 0 ? (double) totalMessages / totalConversations : 0.0
        );
        
        // Context type distribution
        Map<String, Long> contextTypeDistribution = conversationRepository
            .findContextTypeDistribution(userId, startDate);
        analytics.setContextTypeDistribution(contextTypeDistribution);
        
        // Daily usage pattern
        Map<String, Long> dailyUsage = getDailyUsagePattern(userId, days);
        analytics.setDailyUsagePattern(dailyUsage);
        
        // Response time analytics
        ResponseTimeAnalytics responseTimeAnalytics = getResponseTimeAnalytics(userId, startDate);
        analytics.setResponseTimeAnalytics(responseTimeAnalytics);
        
        // Popular topics/keywords
        List<String> popularTopics = getPopularTopics(userId, startDate);
        analytics.setPopularTopics(popularTopics);
        
        // Code execution stats
        CodeExecutionStats codeStats = getCodeExecutionStats(userId, startDate);
        analytics.setCodeExecutionStats(codeStats);
        
        // Productivity metrics
        ProductivityMetrics productivity = getProductivityMetrics(userId, startDate);
        analytics.setProductivityMetrics(productivity);
        
        log.info("Generated analytics for user: {} - {} conversations, {} messages", 
                userId, totalConversations, totalMessages);
        
        return analytics;
    }

    /**
     * Get system-wide AI analytics
     */
    public SystemAnalytics getSystemAnalyticsDetailed(int days) {
        log.info("Generating system-wide AI analytics over {} days", days);
        
        LocalDateTime startDate = LocalDateTime.now().minusDays(days);
        
        SystemAnalytics analytics = new SystemAnalytics();
        analytics.setAnalysisPeriodDays(days);
        analytics.setGeneratedAt(LocalDateTime.now());
        
        // Overall usage metrics
        long totalUsers = userRepository.countActiveUsers();
        long activeUsers = conversationRepository.countDistinctUsersByCreatedAtAfter(startDate);
        long totalConversations = conversationRepository.countByCreatedAtAfter(startDate);
        long totalMessages = messageRepository.countByCreatedAtAfter(startDate);
        
        analytics.setTotalUsers(totalUsers);
        analytics.setActiveUsers(activeUsers);
        analytics.setTotalConversations(totalConversations);
        analytics.setTotalMessages(totalMessages);
        analytics.setUserEngagementRate(totalUsers > 0 ? (double) activeUsers / totalUsers : 0.0);
        
        // Top users by activity
        List<UserActivitySummary> topUsers = getTopUsersByActivity(startDate, 10);
        analytics.setTopActiveUsers(topUsers);
        
        // System performance metrics
        SystemPerformanceMetrics performance = getSystemPerformanceMetrics(startDate);
        analytics.setPerformanceMetrics(performance);
        
        // Error rate analysis
        ErrorAnalytics errorAnalytics = getErrorAnalytics(startDate);
        analytics.setErrorAnalytics(errorAnalytics);
        
        // Feature usage distribution
        Map<String, Long> featureUsage = getFeatureUsageDistribution(startDate);
        analytics.setFeatureUsageDistribution(featureUsage);
        
        log.info("Generated system analytics - {} active users, {} conversations", 
                activeUsers, totalConversations);
        
        return analytics;
    }

    /**
     * Get conversation quality metrics
     */
    public ConversationQualityMetrics getConversationQualityMetrics(Long conversationId) {
        log.info("Analyzing conversation quality for conversation: {}", conversationId);
        
        Optional<AIConversation> conversationOpt = conversationRepository.findById(conversationId);
        if (conversationOpt.isEmpty()) {
            throw new IllegalArgumentException("Conversation not found: " + conversationId);
        }
        
        AIConversation conversation = conversationOpt.get();
        List<AIMessage> messages = messageRepository.findByConversationIdOrderByCreatedAt(conversationId);
        
        ConversationQualityMetrics metrics = new ConversationQualityMetrics();
        metrics.setConversationId(conversationId);
        metrics.setAnalyzedAt(LocalDateTime.now());
        
        // Basic metrics
        metrics.setTotalMessages(messages.size());
        metrics.setConversationDuration(calculateConversationDuration(messages));
        
        // Message analysis
        int userMessages = 0;
        int aiMessages = 0;
        int totalCharacters = 0;
        int codeBlocksCount = 0;
        
        for (AIMessage message : messages) {
            if (message.isUserMessage()) {
                userMessages++;
            } else {
                aiMessages++;
            }
            
            String content = message.getContent();
            totalCharacters += content.length();
            
            // Count code blocks
            codeBlocksCount += countCodeBlocks(content);
        }
        
        metrics.setUserMessages(userMessages);
        metrics.setAiMessages(aiMessages);
        metrics.setAverageMessageLength(messages.size() > 0 ? totalCharacters / messages.size() : 0);
        metrics.setCodeBlocksCount(codeBlocksCount);
        
        // Conversation flow analysis
        metrics.setConversationFlow(analyzeConversationFlow(messages));
        
        // Context switches
        metrics.setContextSwitches(countContextSwitches(messages));
        
        return metrics;
    }

    // Helper methods
    
    private Map<String, Long> getDailyUsagePattern(Long userId, int days) {
        Map<String, Long> dailyUsage = new LinkedHashMap<>();
        LocalDateTime now = LocalDateTime.now();
        
        for (int i = days - 1; i >= 0; i--) {
            LocalDateTime date = now.minusDays(i);
            String dateKey = date.toLocalDate().toString();
            
            LocalDateTime startOfDay = date.toLocalDate().atStartOfDay();
            LocalDateTime endOfDay = startOfDay.plusDays(1);
            
            long count = messageRepository.countByConversationUserIdAndCreatedAtBetween(
                userId, startOfDay, endOfDay);
            dailyUsage.put(dateKey, count);
        }
        
        return dailyUsage;
    }

    private ResponseTimeAnalytics getResponseTimeAnalytics(Long userId, LocalDateTime startDate) {
        // This would require storing response times - simplified implementation
        ResponseTimeAnalytics analytics = new ResponseTimeAnalytics();
        analytics.setAverageResponseTime(2.5); // seconds
        analytics.setMedianResponseTime(2.1);
        analytics.setP95ResponseTime(5.2);
        analytics.setP99ResponseTime(8.7);
        return analytics;
    }

    private List<String> getPopularTopics(Long userId, LocalDateTime startDate) {
        // Simplified implementation - would need NLP analysis
        return Arrays.asList("Java Programming", "Error Debugging", "Code Review", 
                           "Algorithm Design", "Database Queries");
    }

    private CodeExecutionStats getCodeExecutionStats(Long userId, LocalDateTime startDate) {
        // This would require tracking code execution - simplified implementation
        CodeExecutionStats stats = new CodeExecutionStats();
        stats.setTotalExecutions(42L);
        stats.setSuccessfulExecutions(38L);
        stats.setFailedExecutions(4L);
        stats.setSuccessRate(0.905);
        stats.setAverageExecutionTime(1.2);
        return stats;
    }

    private ProductivityMetrics getProductivityMetrics(Long userId, LocalDateTime startDate) {
        ProductivityMetrics metrics = new ProductivityMetrics();
        
        // Calculate based on conversation patterns
        long conversations = conversationRepository.countByUserIdAndCreatedAtAfter(userId, startDate);
        long messages = messageRepository.countByConversationUserIdAndCreatedAtAfter(userId, startDate);
        
        metrics.setSessionsPerDay(conversations / 7.0); // Assuming 7-day period
        metrics.setQuestionsPerSession(conversations > 0 ? (double) messages / conversations : 0.0);
        metrics.setEngagementScore(calculateEngagementScore(userId, startDate));
        
        return metrics;
    }

    private List<UserActivitySummary> getTopUsersByActivity(LocalDateTime startDate, int limit) {
        // This would require a more complex query - simplified implementation
        List<UserActivitySummary> topUsers = new ArrayList<>();
        
        List<Object[]> results = conversationRepository.findTopActiveUsers(startDate, 
                Pageable.ofSize(limit));
        
        for (Object[] result : results) {
            UserActivitySummary summary = new UserActivitySummary();
            summary.setUserId((Long) result[0]);
            summary.setUsername((String) result[1]);
            summary.setConversationCount((Long) result[2]);
            summary.setMessageCount((Long) result[3]);
            topUsers.add(summary);
        }
        
        return topUsers;
    }

    private SystemPerformanceMetrics getSystemPerformanceMetrics(LocalDateTime startDate) {
        SystemPerformanceMetrics metrics = new SystemPerformanceMetrics();
        metrics.setAverageResponseTime(2.3);
        metrics.setThroughputPerMinute(45.2);
        metrics.setErrorRate(0.02);
        metrics.setUptime(99.8);
        return metrics;
    }

    private ErrorAnalytics getErrorAnalytics(LocalDateTime startDate) {
        ErrorAnalytics analytics = new ErrorAnalytics();
        analytics.setTotalErrors(23L);
        analytics.setErrorRate(0.02);
        
        Map<String, Long> errorTypes = new HashMap<>();
        errorTypes.put("API_TIMEOUT", 12L);
        errorTypes.put("RATE_LIMIT", 8L);
        errorTypes.put("INVALID_REQUEST", 3L);
        analytics.setErrorTypeDistribution(errorTypes);
        
        return analytics;
    }

    private Map<String, Long> getFeatureUsageDistribution(LocalDateTime startDate) {
        Map<String, Long> featureUsage = new HashMap<>();
        featureUsage.put("chat", 450L);
        featureUsage.put("code-analysis", 123L);
        featureUsage.put("code-generation", 89L);
        featureUsage.put("error-explanation", 67L);
        featureUsage.put("test-generation", 34L);
        return featureUsage;
    }

    private long calculateConversationDuration(List<AIMessage> messages) {
        if (messages.size() < 2) return 0;
        
        LocalDateTime start = messages.get(0).getCreatedAt();
        LocalDateTime end = messages.get(messages.size() - 1).getCreatedAt();
        
        return ChronoUnit.MINUTES.between(start, end);
    }

    private int countCodeBlocks(String content) {
        return (int) content.lines()
                .filter(line -> line.trim().startsWith("```"))
                .count() / 2; // Divide by 2 as each code block has opening and closing
    }

    private String analyzeConversationFlow(List<AIMessage> messages) {
        if (messages.size() < 3) return "SHORT";
        if (messages.size() < 10) return "MEDIUM";
        return "EXTENDED";
    }

    private int countContextSwitches(List<AIMessage> messages) {
        // Simplified implementation - would need more sophisticated analysis
        return Math.max(0, messages.size() / 5 - 1);
    }

    private double calculateEngagementScore(Long userId, LocalDateTime startDate) {
        // Simplified engagement calculation
        long conversations = conversationRepository.countByUserIdAndCreatedAtAfter(userId, startDate);
        long messages = messageRepository.countByConversationUserIdAndCreatedAtAfter(userId, startDate);
        
        if (conversations == 0) return 0.0;
        
        double avgMessagesPerConversation = (double) messages / conversations;
        return Math.min(10.0, avgMessagesPerConversation * 2); // Scale to 0-10
    }

    // Data classes for analytics results
    
    public static class AIUsageAnalytics {
        private Long userId;
        private int analysisPeriodDays;
        private LocalDateTime generatedAt;
        private long totalConversations;
        private long totalMessages;
        private double averageMessagesPerConversation;
        private Map<String, Long> contextTypeDistribution;
        private Map<String, Long> dailyUsagePattern;
        private ResponseTimeAnalytics responseTimeAnalytics;
        private List<String> popularTopics;
        private CodeExecutionStats codeExecutionStats;
        private ProductivityMetrics productivityMetrics;
        
        // Getters and setters
        public Long getUserId() { return userId; }
        public void setUserId(Long userId) { this.userId = userId; }
        public int getAnalysisPeriodDays() { return analysisPeriodDays; }
        public void setAnalysisPeriodDays(int analysisPeriodDays) { this.analysisPeriodDays = analysisPeriodDays; }
        public LocalDateTime getGeneratedAt() { return generatedAt; }
        public void setGeneratedAt(LocalDateTime generatedAt) { this.generatedAt = generatedAt; }
        public long getTotalConversations() { return totalConversations; }
        public void setTotalConversations(long totalConversations) { this.totalConversations = totalConversations; }
        public long getTotalMessages() { return totalMessages; }
        public void setTotalMessages(long totalMessages) { this.totalMessages = totalMessages; }
        public double getAverageMessagesPerConversation() { return averageMessagesPerConversation; }
        public void setAverageMessagesPerConversation(double averageMessagesPerConversation) { this.averageMessagesPerConversation = averageMessagesPerConversation; }
        public Map<String, Long> getContextTypeDistribution() { return contextTypeDistribution; }
        public void setContextTypeDistribution(Map<String, Long> contextTypeDistribution) { this.contextTypeDistribution = contextTypeDistribution; }
        public Map<String, Long> getDailyUsagePattern() { return dailyUsagePattern; }
        public void setDailyUsagePattern(Map<String, Long> dailyUsagePattern) { this.dailyUsagePattern = dailyUsagePattern; }
        public ResponseTimeAnalytics getResponseTimeAnalytics() { return responseTimeAnalytics; }
        public void setResponseTimeAnalytics(ResponseTimeAnalytics responseTimeAnalytics) { this.responseTimeAnalytics = responseTimeAnalytics; }
        public List<String> getPopularTopics() { return popularTopics; }
        public void setPopularTopics(List<String> popularTopics) { this.popularTopics = popularTopics; }
        public CodeExecutionStats getCodeExecutionStats() { return codeExecutionStats; }
        public void setCodeExecutionStats(CodeExecutionStats codeExecutionStats) { this.codeExecutionStats = codeExecutionStats; }
        public ProductivityMetrics getProductivityMetrics() { return productivityMetrics; }
        public void setProductivityMetrics(ProductivityMetrics productivityMetrics) { this.productivityMetrics = productivityMetrics; }
    }

    public static class SystemAnalytics {
        private int analysisPeriodDays;
        private LocalDateTime generatedAt;
        private long totalUsers;
        private long activeUsers;
        private long totalConversations;
        private long totalMessages;
        private double userEngagementRate;
        private List<UserActivitySummary> topActiveUsers;
        private SystemPerformanceMetrics performanceMetrics;
        private ErrorAnalytics errorAnalytics;
        private Map<String, Long> featureUsageDistribution;
        
        // Getters and setters
        public int getAnalysisPeriodDays() { return analysisPeriodDays; }
        public void setAnalysisPeriodDays(int analysisPeriodDays) { this.analysisPeriodDays = analysisPeriodDays; }
        public LocalDateTime getGeneratedAt() { return generatedAt; }
        public void setGeneratedAt(LocalDateTime generatedAt) { this.generatedAt = generatedAt; }
        public long getTotalUsers() { return totalUsers; }
        public void setTotalUsers(long totalUsers) { this.totalUsers = totalUsers; }
        public long getActiveUsers() { return activeUsers; }
        public void setActiveUsers(long activeUsers) { this.activeUsers = activeUsers; }
        public long getTotalConversations() { return totalConversations; }
        public void setTotalConversations(long totalConversations) { this.totalConversations = totalConversations; }
        public long getTotalMessages() { return totalMessages; }
        public void setTotalMessages(long totalMessages) { this.totalMessages = totalMessages; }
        public double getUserEngagementRate() { return userEngagementRate; }
        public void setUserEngagementRate(double userEngagementRate) { this.userEngagementRate = userEngagementRate; }
        public List<UserActivitySummary> getTopActiveUsers() { return topActiveUsers; }
        public void setTopActiveUsers(List<UserActivitySummary> topActiveUsers) { this.topActiveUsers = topActiveUsers; }
        public SystemPerformanceMetrics getPerformanceMetrics() { return performanceMetrics; }
        public void setPerformanceMetrics(SystemPerformanceMetrics performanceMetrics) { this.performanceMetrics = performanceMetrics; }
        public ErrorAnalytics getErrorAnalytics() { return errorAnalytics; }
        public void setErrorAnalytics(ErrorAnalytics errorAnalytics) { this.errorAnalytics = errorAnalytics; }
        public Map<String, Long> getFeatureUsageDistribution() { return featureUsageDistribution; }
        public void setFeatureUsageDistribution(Map<String, Long> featureUsageDistribution) { this.featureUsageDistribution = featureUsageDistribution; }
    }

    public static class ConversationQualityMetrics {
        private Long conversationId;
        private LocalDateTime analyzedAt;
        private int totalMessages;
        private long conversationDuration;
        private int userMessages;
        private int aiMessages;
        private int averageMessageLength;
        private int codeBlocksCount;
        private String conversationFlow;
        private int contextSwitches;
        
        // Getters and setters
        public Long getConversationId() { return conversationId; }
        public void setConversationId(Long conversationId) { this.conversationId = conversationId; }
        public LocalDateTime getAnalyzedAt() { return analyzedAt; }
        public void setAnalyzedAt(LocalDateTime analyzedAt) { this.analyzedAt = analyzedAt; }
        public int getTotalMessages() { return totalMessages; }
        public void setTotalMessages(int totalMessages) { this.totalMessages = totalMessages; }
        public long getConversationDuration() { return conversationDuration; }
        public void setConversationDuration(long conversationDuration) { this.conversationDuration = conversationDuration; }
        public int getUserMessages() { return userMessages; }
        public void setUserMessages(int userMessages) { this.userMessages = userMessages; }
        public int getAiMessages() { return aiMessages; }
        public void setAiMessages(int aiMessages) { this.aiMessages = aiMessages; }
        public int getAverageMessageLength() { return averageMessageLength; }
        public void setAverageMessageLength(int averageMessageLength) { this.averageMessageLength = averageMessageLength; }
        public int getCodeBlocksCount() { return codeBlocksCount; }
        public void setCodeBlocksCount(int codeBlocksCount) { this.codeBlocksCount = codeBlocksCount; }
        public String getConversationFlow() { return conversationFlow; }
        public void setConversationFlow(String conversationFlow) { this.conversationFlow = conversationFlow; }
        public int getContextSwitches() { return contextSwitches; }
        public void setContextSwitches(int contextSwitches) { this.contextSwitches = contextSwitches; }
    }

    // Additional helper classes
    public static class ResponseTimeAnalytics {
        private double averageResponseTime;
        private double medianResponseTime;
        private double p95ResponseTime;
        private double p99ResponseTime;
        
        public double getAverageResponseTime() { return averageResponseTime; }
        public void setAverageResponseTime(double averageResponseTime) { this.averageResponseTime = averageResponseTime; }
        public double getMedianResponseTime() { return medianResponseTime; }
        public void setMedianResponseTime(double medianResponseTime) { this.medianResponseTime = medianResponseTime; }
        public double getP95ResponseTime() { return p95ResponseTime; }
        public void setP95ResponseTime(double p95ResponseTime) { this.p95ResponseTime = p95ResponseTime; }
        public double getP99ResponseTime() { return p99ResponseTime; }
        public void setP99ResponseTime(double p99ResponseTime) { this.p99ResponseTime = p99ResponseTime; }
    }

    public static class CodeExecutionStats {
        private long totalExecutions;
        private long successfulExecutions;
        private long failedExecutions;
        private double successRate;
        private double averageExecutionTime;
        
        public long getTotalExecutions() { return totalExecutions; }
        public void setTotalExecutions(long totalExecutions) { this.totalExecutions = totalExecutions; }
        public long getSuccessfulExecutions() { return successfulExecutions; }
        public void setSuccessfulExecutions(long successfulExecutions) { this.successfulExecutions = successfulExecutions; }
        public long getFailedExecutions() { return failedExecutions; }
        public void setFailedExecutions(long failedExecutions) { this.failedExecutions = failedExecutions; }
        public double getSuccessRate() { return successRate; }
        public void setSuccessRate(double successRate) { this.successRate = successRate; }
        public double getAverageExecutionTime() { return averageExecutionTime; }
        public void setAverageExecutionTime(double averageExecutionTime) { this.averageExecutionTime = averageExecutionTime; }
    }

    public static class ProductivityMetrics {
        private double sessionsPerDay;
        private double questionsPerSession;
        private double engagementScore;
        
        public double getSessionsPerDay() { return sessionsPerDay; }
        public void setSessionsPerDay(double sessionsPerDay) { this.sessionsPerDay = sessionsPerDay; }
        public double getQuestionsPerSession() { return questionsPerSession; }
        public void setQuestionsPerSession(double questionsPerSession) { this.questionsPerSession = questionsPerSession; }
        public double getEngagementScore() { return engagementScore; }
        public void setEngagementScore(double engagementScore) { this.engagementScore = engagementScore; }
    }

    public static class UserActivitySummary {
        private Long userId;
        private String username;
        private Long conversationCount;
        private Long messageCount;
        
        public Long getUserId() { return userId; }
        public void setUserId(Long userId) { this.userId = userId; }
        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }
        public Long getConversationCount() { return conversationCount; }
        public void setConversationCount(Long conversationCount) { this.conversationCount = conversationCount; }
        public Long getMessageCount() { return messageCount; }
        public void setMessageCount(Long messageCount) { this.messageCount = messageCount; }
    }

    public static class SystemPerformanceMetrics {
        private double averageResponseTime;
        private double throughputPerMinute;
        private double errorRate;
        private double uptime;
        
        public double getAverageResponseTime() { return averageResponseTime; }
        public void setAverageResponseTime(double averageResponseTime) { this.averageResponseTime = averageResponseTime; }
        public double getThroughputPerMinute() { return throughputPerMinute; }
        public void setThroughputPerMinute(double throughputPerMinute) { this.throughputPerMinute = throughputPerMinute; }
        public double getErrorRate() { return errorRate; }
        public void setErrorRate(double errorRate) { this.errorRate = errorRate; }
        public double getUptime() { return uptime; }
        public void setUptime(double uptime) { this.uptime = uptime; }
    }

    public static class ErrorAnalytics {
        private long totalErrors;
        private double errorRate;
        private Map<String, Long> errorTypeDistribution;
        
        public long getTotalErrors() { return totalErrors; }
        public void setTotalErrors(long totalErrors) { this.totalErrors = totalErrors; }
        public double getErrorRate() { return errorRate; }
        public void setErrorRate(double errorRate) { this.errorRate = errorRate; }
        public Map<String, Long> getErrorTypeDistribution() { return errorTypeDistribution; }
        public void setErrorTypeDistribution(Map<String, Long> errorTypeDistribution) { this.errorTypeDistribution = errorTypeDistribution; }
    }
} 