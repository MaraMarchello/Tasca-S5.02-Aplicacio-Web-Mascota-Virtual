package com.codemate.security;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import org.springframework.stereotype.Component;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Component
public class OAuth2AuthenticationFailureHandler extends SimpleUrlAuthenticationFailureHandler {

    public OAuth2AuthenticationFailureHandler() {
        // Set default failure URL
        setDefaultFailureUrl("/login?error=true");
    }

    @Override
    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response, AuthenticationException exception)
            throws IOException, ServletException {
        
        // Get error message and encode it properly
        String errorMessage = exception.getLocalizedMessage();
        if (errorMessage == null) {
            errorMessage = "Authentication failed";
        }
        
        // URL encode the error message
        String encodedError = URLEncoder.encode(errorMessage, StandardCharsets.UTF_8);
        
        // Build the redirect URL with properly encoded parameters
        String targetUrl = String.format("/login?error=true&message=%s", encodedError);
        
        // Perform the redirect
        getRedirectStrategy().sendRedirect(request, response, targetUrl);
    }
} 