package com.codemate.security;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.http.MediaType;
import com.codemate.payload.AuthResponse;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;

@Slf4j
@Component
public class OAuth2AuthenticationSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final JwtTokenProvider tokenProvider;
    private final ObjectMapper objectMapper;
    private final SecurityContextService securityContextService;

    public OAuth2AuthenticationSuccessHandler(
            JwtTokenProvider tokenProvider, 
            ObjectMapper objectMapper,
            SecurityContextService securityContextService) {
        this.tokenProvider = tokenProvider;
        this.objectMapper = objectMapper;
        this.securityContextService = securityContextService;
        // Set the default target URL for successful authentication
        setDefaultTargetUrl("/dashboard");
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication)
            throws IOException, ServletException {
        
        // Check for redirect loops
        if (RedirectLoopPreventer.isRedirectLoop(request)) {
            log.warn("Detected potential redirect loop for user: {}", authentication.getName());
            RedirectLoopPreventer.handleRedirectLoop(request, response);
            return;
        }
        
        // Ensure the authentication is set in the security context
        securityContextService.setAuthentication(authentication);
        
        // Clear any authentication attributes that might have been set
        super.clearAuthenticationAttributes(request);
        
        if (response.isCommitted()) {
            log.debug("Response has already been committed. Unable to redirect to dashboard");
            return;
        }

        // Generate JWT token
        String jwt = tokenProvider.generateToken(authentication);

        // Check if the client accepts JSON
        String acceptHeader = request.getHeader("Accept");
        if (acceptHeader != null && acceptHeader.contains(MediaType.APPLICATION_JSON_VALUE)) {
            // Return JWT token for API clients
            log.debug("Returning JWT token for API client");
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.getWriter().write(objectMapper.writeValueAsString(new AuthResponse(jwt)));
            
            // Reset redirect count for API clients
            RedirectLoopPreventer.resetRedirectCount(request);
        } else {
            // For browser clients, set the JWT token in a secure cookie
            log.debug("Setting JWT in cookie and redirecting to dashboard for browser client");
            
            // Add JWT to cookie
            CookieUtils.addJwtCookie(response, jwt);
            
            // Perform the redirect to dashboard
            getRedirectStrategy().sendRedirect(request, response, getDefaultTargetUrl());
        }
    }
} 