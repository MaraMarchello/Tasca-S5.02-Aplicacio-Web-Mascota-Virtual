package com.codemate.config;

import com.theokanning.openai.service.OpenAiService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import java.time.Duration;

@Configuration
public class OpenAIConfig {

    private final String openaiApiKey;
    private final Integer timeout;

    public OpenAIConfig(
            @Value("${openai.api.key}") String openaiApiKey,
            @Value("${openai.api.timeout:60}") Integer timeout) {
        this.openaiApiKey = openaiApiKey;
        this.timeout = timeout;
    }

    @Bean
    public OpenAiService openAiService() {
        return new OpenAiService(openaiApiKey, Duration.ofSeconds(timeout));
    }
} 