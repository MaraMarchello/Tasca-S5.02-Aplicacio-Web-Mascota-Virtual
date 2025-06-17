package com.codemate.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;

/**
 * Utility class to prevent redirect loops by tracking the number of redirects in the session
 */
@Slf4j
public class RedirectLoopPreventer {

    private static final String REDIRECT_COUNT_SESSION_ATTR = "REDIRECT_COUNT";
    private static final int DEFAULT_MAX_REDIRECTS = 3;
    
    private RedirectLoopPreventer() {
        // Utility class, no instantiation
    }
    
    /**
     * Checks if there's a redirect loop by counting redirects in the session
     * 
     * @param request the HTTP request
     * @return true if a redirect loop is detected, false otherwise
     */
    public static boolean isRedirectLoop(HttpServletRequest request) {
        return isRedirectLoop(request, DEFAULT_MAX_REDIRECTS);
    }
    
    /**
     * Checks if there's a redirect loop by counting redirects in the session
     * 
     * @param request the HTTP request
     * @param maxRedirects the maximum number of redirects allowed
     * @return true if a redirect loop is detected, false otherwise
     */
    public static boolean isRedirectLoop(HttpServletRequest request, int maxRedirects) {
        HttpSession session = request.getSession(true);
        Integer redirectCount = (Integer) session.getAttribute(REDIRECT_COUNT_SESSION_ATTR);
        
        if (redirectCount == null) {
            redirectCount = 1;
        } else {
            redirectCount++;
        }
        
        session.setAttribute(REDIRECT_COUNT_SESSION_ATTR, redirectCount);
        log.debug("Current redirect count: {}", redirectCount);
        
        return redirectCount > maxRedirects;
    }
    
    /**
     * Resets the redirect count in the session
     * 
     * @param request the HTTP request
     */
    public static void resetRedirectCount(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.removeAttribute(REDIRECT_COUNT_SESSION_ATTR);
            log.debug("Reset redirect count");
        }
    }
    
    /**
     * Handles the redirect loop by sending an error page to the client
     * 
     * @param request the HTTP request
     * @param response the HTTP response
     * @throws IOException if an I/O error occurs
     */
    public static void handleRedirectLoop(HttpServletRequest request, HttpServletResponse response) throws IOException {
        log.warn("Detected potential redirect loop for request: {}", request.getRequestURI());
        response.setContentType("text/html");
        response.getWriter().write("<html><body><h1>Error: Too many redirects</h1>" +
                "<p>We detected a potential redirect loop. Please clear your cookies and try again.</p>" +
                "<p><a href='/login'>Return to login page</a></p></body></html>");
    }
} 