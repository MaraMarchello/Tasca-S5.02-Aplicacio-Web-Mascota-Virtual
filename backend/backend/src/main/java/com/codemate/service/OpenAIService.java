package com.codemate.service;

import com.codemate.exception.AIServiceException;
import com.codemate.payload.response.AIAssistanceResponse;
import com.theokanning.openai.completion.chat.ChatCompletionRequest;
import com.theokanning.openai.completion.chat.ChatMessage;
import com.theokanning.openai.service.OpenAiService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class OpenAIService {

    private final OpenAiService openAiClient;
    
    @Value("${openai.api.model:gpt-3.5-turbo}")
    private String gptModel;
    
    private static final String SYSTEM_ROLE = "system";
    private static final String USER_ROLE = "user";

    private static final String RESPONSE_FORMAT = """
            Please provide your response in the following format:
            
            ANSWER:
            [Your main answer/explanation here]
            
            EXPLANATION:
            [Additional detailed explanation if needed, or 'N/A' if not applicable]
            
            CODE_SNIPPET:
            [Relevant code snippet if applicable, or 'N/A' if not needed]
            
            REFERENCES:
            [Any relevant documentation links or references, or 'N/A' if none]
            """;

    /**
     * Tests the connection to OpenAI API
     * 
     * @return A string indicating whether the connection was successful
     * @throws AIServiceException if the connection fails
     */
    public String testConnection() {
        log.info("Testing connection to OpenAI API");
        List<ChatMessage> messages = new ArrayList<>();
        messages.add(new ChatMessage(USER_ROLE, "Hello, this is a test message."));

        ChatCompletionRequest request = ChatCompletionRequest.builder()
                .model(gptModel)
                .messages(messages)
                .temperature(0.7)
                .build();

        try {
            String response = openAiClient.createChatCompletion(request)
                    .getChoices().getFirst().getMessage().getContent();
            log.info("Successfully connected to OpenAI API");
            return "Connection successful! Response: " + response;
        } catch (Exception e) {
            log.error("Failed to connect to OpenAI API", e);
            String errorMessage = e.getMessage();
            if (errorMessage != null && (errorMessage.contains("timeout") || errorMessage.contains("timed out"))) {
                throw new AIServiceException("Connection to OpenAI API timed out. Please try again later.", e);
            }
            throw new AIServiceException("Failed to connect to OpenAI API: " + errorMessage, e);
        }
    }

    /**
     * Generates code assistance based on the provided query
     * 
     * @param query The user's code-related question
     * @return AIAssistanceResponse containing the answer
     * @throws AIServiceException if the API call fails
     */
    @Cacheable(value = "codeAssistance", key = "#query")
    public AIAssistanceResponse getCodeAssistance(String query) {
        log.info("Generating code assistance for query: {}", query);
        List<ChatMessage> messages = new ArrayList<>();
        messages.add(new ChatMessage(SYSTEM_ROLE, 
            "You are a helpful Java programming assistant. Provide clear, concise, and accurate answers with code examples when appropriate. " + 
            RESPONSE_FORMAT));
        messages.add(new ChatMessage(USER_ROLE, query));

        ChatCompletionRequest request = ChatCompletionRequest.builder()
                .model(gptModel)
                .messages(messages)
                .temperature(0.7)
                .build();

        try {
            String response = openAiClient.createChatCompletion(request)
                    .getChoices().getFirst().getMessage().getContent();
            log.debug("Received response from OpenAI for code assistance");
            return parseResponse(response);
        } catch (Exception e) {
            log.error("Error while getting code assistance for query: {}", query, e);
            String errorMessage = e.getMessage();
            if (errorMessage != null && (errorMessage.contains("timeout") || errorMessage.contains("timed out"))) {
                throw new AIServiceException("Request to OpenAI API timed out. Please try again later.", e);
            }
            throw new AIServiceException("Failed to get code assistance from OpenAI: " + errorMessage, e);
        }
    }

    /**
     * Explains an error based on the provided stack trace
     * 
     * @param stackTrace The error stack trace to explain
     * @return AIAssistanceResponse containing the explanation
     * @throws AIServiceException if the API call fails
     */
    @Cacheable(value = "errorExplanations", key = "#stackTrace")
    public AIAssistanceResponse explainError(String stackTrace) {
        log.info("Generating error explanation for stack trace");
        List<ChatMessage> messages = new ArrayList<>();
        messages.add(new ChatMessage(SYSTEM_ROLE, 
            "You are a Java error analysis expert. Explain the error, its likely causes, and suggest solutions. " + 
            RESPONSE_FORMAT));
        messages.add(new ChatMessage(USER_ROLE, "Please explain this error and how to fix it:\n" + stackTrace));

        ChatCompletionRequest request = ChatCompletionRequest.builder()
                .model(gptModel)
                .messages(messages)
                .temperature(0.7)
                .build();

        try {
            String response = openAiClient.createChatCompletion(request)
                    .getChoices().getFirst().getMessage().getContent();
            log.debug("Received response from OpenAI for error explanation");
            return parseResponse(response);
        } catch (Exception e) {
            log.error("Error while explaining stack trace", e);
            String errorMessage = e.getMessage();
            if (errorMessage != null && (errorMessage.contains("timeout") || errorMessage.contains("timed out"))) {
                throw new AIServiceException("Request to OpenAI API timed out. Please try again later.", e);
            }
            throw new AIServiceException("Failed to get stack trace explanation from OpenAI: " + errorMessage, e);
        }
    }

    /**
     * Explains a Git error based on the provided error message
     * 
     * @param gitError The Git error message to explain
     * @return AIAssistanceResponse containing the explanation
     * @throws AIServiceException if the API call fails
     */
    @Cacheable(value = "gitErrorExplanations", key = "#gitError")
    public AIAssistanceResponse explainGitError(String gitError) {
        log.info("Generating explanation for Git error");
        List<ChatMessage> messages = new ArrayList<>();
        messages.add(new ChatMessage(SYSTEM_ROLE, 
            "You are a Git expert. Explain Git errors and provide step-by-step solutions. " + 
            RESPONSE_FORMAT));
        messages.add(new ChatMessage(USER_ROLE, "Please explain this Git error and how to resolve it:\n" + gitError));

        ChatCompletionRequest request = ChatCompletionRequest.builder()
                .model(gptModel)
                .messages(messages)
                .temperature(0.7)
                .build();

        try {
            String response = openAiClient.createChatCompletion(request)
                    .getChoices().getFirst().getMessage().getContent();
            log.debug("Received response from OpenAI for Git error explanation");
            return parseResponse(response);
        } catch (Exception e) {
            log.error("Error while explaining Git error", e);
            String errorMessage = e.getMessage();
            if (errorMessage != null && (errorMessage.contains("timeout") || errorMessage.contains("timed out"))) {
                throw new AIServiceException("Request to OpenAI API timed out. Please try again later.", e);
            }
            throw new AIServiceException("Failed to get Git error explanation from OpenAI: " + errorMessage, e);
        }
    }

    /**
     * Parses the OpenAI response into structured format
     * 
     * @param response The raw response from OpenAI
     * @return AIAssistanceResponse containing the structured response
     * @throws AIServiceException if parsing fails
     */
    private AIAssistanceResponse parseResponse(String response) {
        try {
            Pattern answerPattern = Pattern.compile("ANSWER:\\s*([\\s\\S]*?)(?=EXPLANATION:|$)");
            Pattern explanationPattern = Pattern.compile("EXPLANATION:\\s*([\\s\\S]*?)(?=CODE_SNIPPET:|$)");
            Pattern codePattern = Pattern.compile("CODE_SNIPPET:\\s*([\\s\\S]*?)(?=REFERENCES:|$)");
            Pattern referencesPattern = Pattern.compile("REFERENCES:\\s*([\\s\\S]*?)$");

            String answer = extractContent(answerPattern.matcher(response));
            String explanation = extractContent(explanationPattern.matcher(response));
            String codeSnippet = extractContent(codePattern.matcher(response));
            String references = extractContent(referencesPattern.matcher(response));

            if (answer.isEmpty()) {
                log.warn("Failed to parse AI response: missing answer section");
                throw new AIServiceException("Failed to parse AI response: missing answer section");
            }

            return AIAssistanceResponse.builder()
                    .answer(answer)
                    .explanation("N/A".equals(explanation.trim()) ? null : explanation)
                    .codeSnippet("N/A".equals(codeSnippet.trim()) ? null : codeSnippet)
                    .references("N/A".equals(references.trim()) ? null : references)
                    .build();
        } catch (AIServiceException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error parsing OpenAI response", e);
            throw new AIServiceException("Failed to parse OpenAI response: " + e.getMessage(), e);
        }
    }

    /**
     * Extracts content from a regex matcher
     * 
     * @param matcher The regex matcher
     * @return The extracted content or empty string if not found
     */
    private String extractContent(Matcher matcher) {
        if (matcher.find()) {
            return matcher.group(1).trim();
        }
        return "";
    }
} 