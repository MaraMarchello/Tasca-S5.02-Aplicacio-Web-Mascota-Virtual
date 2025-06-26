package com.codemate.service;

import com.codemate.exception.AIServiceException;
import com.codemate.model.AIConversation;
import com.codemate.model.User;
import com.codemate.payload.response.AIAssistanceResponse;
import com.codemate.repository.UserRepository;
import com.theokanning.openai.completion.chat.ChatCompletionRequest;
import com.theokanning.openai.completion.chat.ChatMessage;
import com.theokanning.openai.service.OpenAiService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdvancedAIService {

    private final OpenAiService openAiClient;
    private final AIConversationService conversationService;
    private final CodeExecutionService codeExecutionService;
    
    @Value("${openai.api.model:gpt-3.5-turbo}")
    private String gptModel;
    
    private static final String USER_ROLE = "user";
    private static final String SYSTEM_ROLE = "system";

    /**
     * Analyze code quality and provide suggestions
     */
    public CodeAnalysisResult analyzeCode(String code, String language, Long userId) {
        log.info("Analyzing code quality for user: {}", userId);
        
        try {
            // Get or create conversation for context
            AIConversation conversation = conversationService.getOrCreateConversation(
                userId, AIConversation.ContextType.GENERAL, language);
            
            String analysisPrompt = buildCodeAnalysisPrompt(code, language);
            
            List<ChatMessage> messages = Arrays.asList(
                new ChatMessage(SYSTEM_ROLE, getCodeAnalysisSystemPrompt()),
                new ChatMessage(USER_ROLE, analysisPrompt)
            );
            
            ChatCompletionRequest request = ChatCompletionRequest.builder()
                    .model(gptModel)
                    .messages(messages)
                    .temperature(0.3) // Lower temperature for more consistent analysis
                    .build();
            
            String response = openAiClient.createChatCompletion(request)
                    .getChoices().getFirst().getMessage().getContent();
            
            // Save the interaction to conversation history
            conversationService.addMessage(conversation.getId(), userId, 
                com.codemate.model.AIMessage.MessageType.USER, analysisPrompt, code, null);
            conversationService.addMessage(conversation.getId(), userId, 
                com.codemate.model.AIMessage.MessageType.ASSISTANT, response, null, null);
            
            return parseCodeAnalysisResponse(response);
            
        } catch (Exception e) {
            log.error("Error analyzing code", e);
            throw new AIServiceException("Failed to analyze code: " + e.getMessage(), e);
        }
    }

    /**
     * Generate code based on requirements with multiple approaches
     */
    public CodeGenerationResult generateCode(String requirements, String language, 
                                           String context, Long userId) {
        log.info("Generating code for user: {}", userId);
        
        try {
            String generationPrompt = buildCodeGenerationPrompt(requirements, language, context);
            
            List<ChatMessage> messages = Arrays.asList(
                new ChatMessage(SYSTEM_ROLE, getCodeGenerationSystemPrompt()),
                new ChatMessage(USER_ROLE, generationPrompt)
            );
            
            ChatCompletionRequest request = ChatCompletionRequest.builder()
                    .model(gptModel)
                    .messages(messages)
                    .temperature(0.7)
                    .build();
            
            String response = openAiClient.createChatCompletion(request)
                    .getChoices().getFirst().getMessage().getContent();
            
            CodeGenerationResult result = parseCodeGenerationResponse(response);
            
            // Test the generated code if it's Java
            if ("java".equalsIgnoreCase(language) && result.getPrimarySolution() != null) {
                try {
                    var executionResult = codeExecutionService.executeJavaCode(result.getPrimarySolution(), "");
                    if (!executionResult.isSuccess()) {
                        log.warn("Generated code failed execution: {}", executionResult.getError());
                    }
                } catch (Exception e) {
                    log.debug("Code execution test failed (non-critical): {}", e.getMessage());
                }
            }
            
            return result;
            
        } catch (Exception e) {
            log.error("Error generating code", e);
            throw new AIServiceException("Failed to generate code: " + e.getMessage(), e);
        }
    }

    /**
     * Provide intelligent code suggestions based on context
     */
    public List<CodeSuggestion> getCodeSuggestions(String partialCode, String language, 
                                                  String context, Long userId) {
        log.info("Getting code suggestions for user: {}", userId);
        
        try {
            String suggestionPrompt = buildCodeSuggestionPrompt(partialCode, language, context);
            
            List<ChatMessage> messages = Arrays.asList(
                new ChatMessage(SYSTEM_ROLE, getCodeSuggestionSystemPrompt()),
                new ChatMessage(USER_ROLE, suggestionPrompt)
            );
            
            ChatCompletionRequest request = ChatCompletionRequest.builder()
                    .model(gptModel)
                    .messages(messages)
                    .temperature(0.5)
                    .build();
            
            String response = openAiClient.createChatCompletion(request)
                    .getChoices().getFirst().getMessage().getContent();
            
            return parseCodeSuggestions(response);
            
        } catch (Exception e) {
            log.error("Error getting code suggestions", e);
            throw new AIServiceException("Failed to get code suggestions: " + e.getMessage(), e);
        }
    }

    /**
     * Explain code complexity and performance characteristics
     */
    public CodeComplexityAnalysis analyzeComplexity(String code, String language, Long userId) {
        log.info("Analyzing code complexity for user: {}", userId);
        
        try {
            String complexityPrompt = buildComplexityAnalysisPrompt(code, language);
            
            List<ChatMessage> messages = Arrays.asList(
                new ChatMessage(SYSTEM_ROLE, getComplexityAnalysisSystemPrompt()),
                new ChatMessage(USER_ROLE, complexityPrompt)
            );
            
            ChatCompletionRequest request = ChatCompletionRequest.builder()
                    .model(gptModel)
                    .messages(messages)
                    .temperature(0.2)
                    .build();
            
            String response = openAiClient.createChatCompletion(request)
                    .getChoices().getFirst().getMessage().getContent();
            
            return parseComplexityAnalysis(response);
            
        } catch (Exception e) {
            log.error("Error analyzing complexity", e);
            throw new AIServiceException("Failed to analyze complexity: " + e.getMessage(), e);
        }
    }

    /**
     * Generate unit tests for given code
     */
    public TestGenerationResult generateTests(String code, String language, 
                                            String testFramework, Long userId) {
        log.info("Generating tests for user: {}", userId);
        
        try {
            String testPrompt = buildTestGenerationPrompt(code, language, testFramework);
            
            List<ChatMessage> messages = Arrays.asList(
                new ChatMessage(SYSTEM_ROLE, getTestGenerationSystemPrompt()),
                new ChatMessage(USER_ROLE, testPrompt)
            );
            
            ChatCompletionRequest request = ChatCompletionRequest.builder()
                    .model(gptModel)
                    .messages(messages)
                    .temperature(0.4)
                    .build();
            
            String response = openAiClient.createChatCompletion(request)
                    .getChoices().getFirst().getMessage().getContent();
            
            return parseTestGenerationResponse(response);
            
        } catch (Exception e) {
            log.error("Error generating tests", e);
            throw new AIServiceException("Failed to generate tests: " + e.getMessage(), e);
        }
    }

    // System prompts for different AI features
    
    private String getCodeAnalysisSystemPrompt() {
        return """
            You are an expert code reviewer and static analysis tool. Analyze the provided code and return a structured analysis including:
            
            QUALITY_SCORE: [1-10]
            ISSUES: [List of specific issues found]
            SUGGESTIONS: [List of improvement suggestions]
            SECURITY_CONCERNS: [Any security issues identified]
            PERFORMANCE_NOTES: [Performance-related observations]
            BEST_PRACTICES: [Best practice recommendations]
            
            Be specific, actionable, and constructive in your feedback.
            """;
    }

    private String getCodeGenerationSystemPrompt() {
        return """
            You are an expert software developer. Generate clean, efficient, and well-documented code based on requirements.
            
            Provide multiple approaches when applicable and include:
            - Main implementation
            - Alternative approaches
            - Usage examples
            - Documentation comments
            - Error handling
            
            Format your response as:
            PRIMARY_SOLUTION: [Main code implementation]
            ALTERNATIVE_APPROACHES: [Other possible solutions]
            USAGE_EXAMPLES: [How to use the code]
            EXPLANATION: [Why this approach was chosen]
            """;
    }

    private String getCodeSuggestionSystemPrompt() {
        return """
            You are an intelligent code completion assistant. Provide relevant code suggestions based on the partial code and context.
            
            Return suggestions in this format:
            SUGGESTION_1: [Code completion option 1]
            SUGGESTION_2: [Code completion option 2]
            SUGGESTION_3: [Code completion option 3]
            
            Each suggestion should be a logical continuation or improvement of the partial code.
            """;
    }

    private String getComplexityAnalysisSystemPrompt() {
        return """
            You are a performance analysis expert. Analyze the computational complexity and performance characteristics of the provided code.
            
            Provide analysis in this format:
            TIME_COMPLEXITY: [Big O notation for time]
            SPACE_COMPLEXITY: [Big O notation for space]
            PERFORMANCE_RATING: [1-10 scale]
            BOTTLENECKS: [Identified performance bottlenecks]
            OPTIMIZATION_SUGGESTIONS: [How to improve performance]
            SCALABILITY_NOTES: [How the code scales with input size]
            """;
    }

    private String getTestGenerationSystemPrompt() {
        return """
            You are a test automation expert. Generate comprehensive unit tests for the provided code.
            
            Include:
            - Happy path tests
            - Edge cases
            - Error conditions
            - Boundary value tests
            
            Format as:
            TEST_CLASS: [Complete test class code]
            TEST_METHODS: [Individual test methods]
            COVERAGE_NOTES: [What the tests cover]
            ADDITIONAL_TESTS: [Suggestions for integration/performance tests]
            """;
    }

    // Prompt builders
    
    private String buildCodeAnalysisPrompt(String code, String language) {
        return String.format("""
            Please analyze this %s code for quality, issues, and improvements:
            
            ```%s
            %s
            ```
            
            Focus on:
            - Code quality and maintainability
            - Potential bugs or issues
            - Security vulnerabilities
            - Performance considerations
            - Best practice adherence
            """, language, language, code);
    }

    private String buildCodeGenerationPrompt(String requirements, String language, String context) {
        return String.format("""
            Generate %s code for the following requirements:
            
            Requirements: %s
            
            Context: %s
            
            Please provide a complete, working solution with proper error handling and documentation.
            """, language, requirements, context != null ? context : "General purpose application");
    }

    private String buildCodeSuggestionPrompt(String partialCode, String language, String context) {
        return String.format("""
            Complete this %s code snippet with intelligent suggestions:
            
            Partial Code:
            ```%s
            %s
            ```
            
            Context: %s
            
            Provide 3 different completion options that make logical sense.
            """, language, language, partialCode, context != null ? context : "General programming context");
    }

    private String buildComplexityAnalysisPrompt(String code, String language) {
        return String.format("""
            Analyze the computational complexity and performance of this %s code:
            
            ```%s
            %s
            ```
            
            Provide detailed analysis of time/space complexity and performance characteristics.
            """, language, language, code);
    }

    private String buildTestGenerationPrompt(String code, String language, String testFramework) {
        return String.format("""
            Generate comprehensive unit tests for this %s code using %s:
            
            ```%s
            %s
            ```
            
            Include tests for normal cases, edge cases, and error conditions.
            """, language, testFramework != null ? testFramework : "JUnit", language, code);
    }

    // Response parsers
    
    private CodeAnalysisResult parseCodeAnalysisResponse(String response) {
        CodeAnalysisResult result = new CodeAnalysisResult();
        
        result.setQualityScore(extractScore(response, "QUALITY_SCORE"));
        result.setIssues(extractList(response, "ISSUES"));
        result.setSuggestions(extractList(response, "SUGGESTIONS"));
        result.setSecurityConcerns(extractList(response, "SECURITY_CONCERNS"));
        result.setPerformanceNotes(extractList(response, "PERFORMANCE_NOTES"));
        result.setBestPractices(extractList(response, "BEST_PRACTICES"));
        
        return result;
    }

    private CodeGenerationResult parseCodeGenerationResponse(String response) {
        CodeGenerationResult result = new CodeGenerationResult();
        
        result.setPrimarySolution(extractSection(response, "PRIMARY_SOLUTION"));
        result.setAlternativeApproaches(extractList(response, "ALTERNATIVE_APPROACHES"));
        result.setUsageExamples(extractSection(response, "USAGE_EXAMPLES"));
        result.setExplanation(extractSection(response, "EXPLANATION"));
        
        return result;
    }

    private List<CodeSuggestion> parseCodeSuggestions(String response) {
        List<CodeSuggestion> suggestions = new ArrayList<>();
        
        for (int i = 1; i <= 3; i++) {
            String suggestion = extractSection(response, "SUGGESTION_" + i);
            if (suggestion != null && !suggestion.trim().isEmpty()) {
                suggestions.add(new CodeSuggestion(suggestion, "Completion option " + i));
            }
        }
        
        return suggestions;
    }

    private CodeComplexityAnalysis parseComplexityAnalysis(String response) {
        CodeComplexityAnalysis analysis = new CodeComplexityAnalysis();
        
        analysis.setTimeComplexity(extractSection(response, "TIME_COMPLEXITY"));
        analysis.setSpaceComplexity(extractSection(response, "SPACE_COMPLEXITY"));
        analysis.setPerformanceRating(extractScore(response, "PERFORMANCE_RATING"));
        analysis.setBottlenecks(extractList(response, "BOTTLENECKS"));
        analysis.setOptimizationSuggestions(extractList(response, "OPTIMIZATION_SUGGESTIONS"));
        analysis.setScalabilityNotes(extractSection(response, "SCALABILITY_NOTES"));
        
        return analysis;
    }

    private TestGenerationResult parseTestGenerationResponse(String response) {
        TestGenerationResult result = new TestGenerationResult();
        
        result.setTestClass(extractSection(response, "TEST_CLASS"));
        result.setTestMethods(extractList(response, "TEST_METHODS"));
        result.setCoverageNotes(extractSection(response, "COVERAGE_NOTES"));
        result.setAdditionalTests(extractList(response, "ADDITIONAL_TESTS"));
        
        return result;
    }

    // Utility methods for parsing
    
    private int extractScore(String response, String section) {
        String scoreText = extractSection(response, section);
        if (scoreText != null) {
            try {
                // Extract first number found
                Pattern pattern = Pattern.compile("\\d+");
                Matcher matcher = pattern.matcher(scoreText);
                if (matcher.find()) {
                    return Integer.parseInt(matcher.group());
                }
            } catch (NumberFormatException e) {
                log.warn("Failed to parse score from: {}", scoreText);
            }
        }
        return 5; // Default score
    }

    private String extractSection(String response, String section) {
        Pattern pattern = Pattern.compile(section + ":\\s*([\\s\\S]*?)(?=" + 
                                        "\\n[A-Z_]+:|\\n\\n|$)", Pattern.MULTILINE);
        Matcher matcher = pattern.matcher(response);
        if (matcher.find()) {
            return matcher.group(1).trim();
        }
        return null;
    }

    private List<String> extractList(String response, String section) {
        String content = extractSection(response, section);
        if (content == null || content.trim().isEmpty()) {
            return new ArrayList<>();
        }
        
        List<String> items = new ArrayList<>();
        String[] lines = content.split("\n");
        
        for (String line : lines) {
            line = line.trim();
            if (!line.isEmpty()) {
                // Remove bullet points and numbering
                line = line.replaceAll("^[-*•]\\s*", "")
                          .replaceAll("^\\d+\\.\\s*", "");
                if (!line.isEmpty()) {
                    items.add(line);
                }
            }
        }
        
        return items;
    }

    // Result classes
    
    public static class CodeAnalysisResult {
        private int qualityScore;
        private List<String> issues = new ArrayList<>();
        private List<String> suggestions = new ArrayList<>();
        private List<String> securityConcerns = new ArrayList<>();
        private List<String> performanceNotes = new ArrayList<>();
        private List<String> bestPractices = new ArrayList<>();
        
        // Getters and setters
        public int getQualityScore() { return qualityScore; }
        public void setQualityScore(int qualityScore) { this.qualityScore = qualityScore; }
        public List<String> getIssues() { return issues; }
        public void setIssues(List<String> issues) { this.issues = issues; }
        public List<String> getSuggestions() { return suggestions; }
        public void setSuggestions(List<String> suggestions) { this.suggestions = suggestions; }
        public List<String> getSecurityConcerns() { return securityConcerns; }
        public void setSecurityConcerns(List<String> securityConcerns) { this.securityConcerns = securityConcerns; }
        public List<String> getPerformanceNotes() { return performanceNotes; }
        public void setPerformanceNotes(List<String> performanceNotes) { this.performanceNotes = performanceNotes; }
        public List<String> getBestPractices() { return bestPractices; }
        public void setBestPractices(List<String> bestPractices) { this.bestPractices = bestPractices; }
    }

    public static class CodeGenerationResult {
        private String primarySolution;
        private List<String> alternativeApproaches = new ArrayList<>();
        private String usageExamples;
        private String explanation;
        
        // Getters and setters
        public String getPrimarySolution() { return primarySolution; }
        public void setPrimarySolution(String primarySolution) { this.primarySolution = primarySolution; }
        public List<String> getAlternativeApproaches() { return alternativeApproaches; }
        public void setAlternativeApproaches(List<String> alternativeApproaches) { this.alternativeApproaches = alternativeApproaches; }
        public String getUsageExamples() { return usageExamples; }
        public void setUsageExamples(String usageExamples) { this.usageExamples = usageExamples; }
        public String getExplanation() { return explanation; }
        public void setExplanation(String explanation) { this.explanation = explanation; }
    }

    public static class CodeSuggestion {
        private String code;
        private String description;
        
        public CodeSuggestion(String code, String description) {
            this.code = code;
            this.description = description;
        }
        
        // Getters and setters
        public String getCode() { return code; }
        public void setCode(String code) { this.code = code; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
    }

    public static class CodeComplexityAnalysis {
        private String timeComplexity;
        private String spaceComplexity;
        private int performanceRating;
        private List<String> bottlenecks = new ArrayList<>();
        private List<String> optimizationSuggestions = new ArrayList<>();
        private String scalabilityNotes;
        
        // Getters and setters
        public String getTimeComplexity() { return timeComplexity; }
        public void setTimeComplexity(String timeComplexity) { this.timeComplexity = timeComplexity; }
        public String getSpaceComplexity() { return spaceComplexity; }
        public void setSpaceComplexity(String spaceComplexity) { this.spaceComplexity = spaceComplexity; }
        public int getPerformanceRating() { return performanceRating; }
        public void setPerformanceRating(int performanceRating) { this.performanceRating = performanceRating; }
        public List<String> getBottlenecks() { return bottlenecks; }
        public void setBottlenecks(List<String> bottlenecks) { this.bottlenecks = bottlenecks; }
        public List<String> getOptimizationSuggestions() { return optimizationSuggestions; }
        public void setOptimizationSuggestions(List<String> optimizationSuggestions) { this.optimizationSuggestions = optimizationSuggestions; }
        public String getScalabilityNotes() { return scalabilityNotes; }
        public void setScalabilityNotes(String scalabilityNotes) { this.scalabilityNotes = scalabilityNotes; }
    }

    public static class TestGenerationResult {
        private String testClass;
        private List<String> testMethods = new ArrayList<>();
        private String coverageNotes;
        private List<String> additionalTests = new ArrayList<>();
        
        // Getters and setters
        public String getTestClass() { return testClass; }
        public void setTestClass(String testClass) { this.testClass = testClass; }
        public List<String> getTestMethods() { return testMethods; }
        public void setTestMethods(List<String> testMethods) { this.testMethods = testMethods; }
        public String getCoverageNotes() { return coverageNotes; }
        public void setCoverageNotes(String coverageNotes) { this.coverageNotes = coverageNotes; }
        public List<String> getAdditionalTests() { return additionalTests; }
        public void setAdditionalTests(List<String> additionalTests) { this.additionalTests = additionalTests; }
    }
} 