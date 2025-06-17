package com.codemate.security;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import org.springframework.stereotype.Component;
import org.springframework.http.MediaType;
import com.codemate.payload.ApiResponse;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Slf4j
@Component
public class OAuth2AuthenticationFailureHandler extends SimpleUrlAuthenticationFailureHandler {

    private final ObjectMapper objectMapper;
    private final SecurityContextService securityContextService;

    public OAuth2AuthenticationFailureHandler(
            ObjectMapper objectMapper,
            SecurityContextService securityContextService) {
        this.objectMapper = objectMapper;
        this.securityContextService = securityContextService;
        // Set default failure URL
        setDefaultFailureUrl("/login?error=true");
    }

    @Override
    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response, AuthenticationException exception)
            throws IOException, ServletException {
        
        // Check for redirect loops
        if (RedirectLoopPreventer.isRedirectLoop(request)) {
            log.warn("Detected potential redirect loop during authentication failure");
            RedirectLoopPreventer.handleRedirectLoop(request, response);
            return;
        }
        
        // Ensure security context is cleared
        securityContextService.clearContext();
        
        // Get error message
        String errorMessage = exception.getLocalizedMessage();
        if (errorMessage == null) {
            errorMessage = "Authentication failed";
        }

        // Check if the client accepts JSON
        String acceptHeader = request.getHeader("Accept");
        if (acceptHeader != null && acceptHeader.contains(MediaType.APPLICATION_JSON_VALUE)) {
            // Return error response for API clients
            log.debug("Returning JSON error response for API client");
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write(
                objectMapper.writeValueAsString(new ApiResponse(false, errorMessage))
            );
            
            // Reset redirect count for API clients
            RedirectLoopPreventer.resetRedirectCount(request);
        } else {
            // For browser clients, redirect to login page with error message
            log.debug("Redirecting browser client to login page with error message");
            String encodedError = URLEncoder.encode(errorMessage, StandardCharsets.UTF_8);
            String targetUrl = String.format("/login?error=true&message=%s", encodedError);
            getRedirectStrategy().sendRedirect(request, response, targetUrl);
        }
    }
} 