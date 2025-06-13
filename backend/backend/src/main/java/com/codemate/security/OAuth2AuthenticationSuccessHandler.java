package com.codemate.security;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class OAuth2AuthenticationSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {



    public OAuth2AuthenticationSuccessHandler() {
        // Set the default target URL for successful authentication
        setDefaultTargetUrl("/dashboard");
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication)
            throws IOException, ServletException {
        
        // Clear any authentication attributes that might have been set
        clearAuthenticationAttributes(request);
        
        if (response.isCommitted()) {
            logger.debug("Response has already been committed. Unable to redirect to dashboard");
            return;
        }

        // Perform the redirect to dashboard
        getRedirectStrategy().sendRedirect(request, response, "/dashboard");
    }
} 