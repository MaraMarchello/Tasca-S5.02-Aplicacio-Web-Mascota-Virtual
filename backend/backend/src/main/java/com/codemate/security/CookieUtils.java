package com.codemate.security;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.SerializationUtils;

import java.util.Base64;
import java.util.Optional;

/**
 * Utility class for handling cookies
 */
@Slf4j
@Component
public class CookieUtils {

    public static final String JWT_COOKIE_NAME = "jwt";
    
    private static String cookieDomain;
    private static String cookiePath;
    private static boolean cookieSecure;
    private static boolean cookieHttpOnly;
    private static String cookieSameSite;
    private static int cookieMaxAge;
    
    @Value("${app.jwt.cookie.domain:localhost}")
    public void setCookieDomain(String domain) {
        cookieDomain = domain;
    }
    
    @Value("${app.jwt.cookie.path:/}")
    public void setCookiePath(String path) {
        cookiePath = path;
    }
    
    @Value("${app.jwt.cookie.secure:true}")
    public void setCookieSecure(boolean secure) {
        cookieSecure = secure;
    }
    
    @Value("${app.jwt.cookie.http-only:true}")
    public void setCookieHttpOnly(boolean httpOnly) {
        cookieHttpOnly = httpOnly;
    }
    
    @Value("${app.jwt.cookie.same-site:strict}")
    public void setCookieSameSite(String sameSite) {
        cookieSameSite = sameSite;
    }
    
    @Value("${app.jwt.cookie.max-age:86400}")
    public void setCookieMaxAge(int maxAge) {
        cookieMaxAge = maxAge;
    }
    
    private CookieUtils() {
        // Utility class with static methods, private constructor to prevent instantiation
    }
    
    /**
     * Creates a secure cookie with the JWT token
     * 
     * @param token the JWT token
     * @param maxAge the maximum age of the cookie in seconds
     * @return the created cookie
     */
    public static Cookie createJwtCookie(String token, int maxAge) {
        Cookie cookie = new Cookie(JWT_COOKIE_NAME, token);
        cookie.setPath(cookiePath != null ? cookiePath : "/");
        cookie.setHttpOnly(cookieHttpOnly);
        cookie.setSecure(cookieSecure);
        cookie.setMaxAge(maxAge);
        
        // Set domain if provided
        if (cookieDomain != null && !cookieDomain.equals("localhost")) {
            cookie.setDomain(cookieDomain);
        }
        
        return cookie;
    }
    
    /**
     * Creates a secure cookie with the JWT token with default max age
     * 
     * @param token the JWT token
     * @return the created cookie
     */
    public static Cookie createJwtCookie(String token) {
        return createJwtCookie(token, cookieMaxAge);
    }
    
    /**
     * Adds a JWT cookie to the response
     * 
     * @param response the HTTP response
     * @param token the JWT token
     */
    public static void addJwtCookie(HttpServletResponse response, String token) {
        Cookie cookie = createJwtCookie(token);
        response.addCookie(cookie);
        
        // Set SameSite attribute (not directly supported by Java Cookie API)
        if (cookieSameSite != null && !cookieSameSite.isEmpty()) {
            String sameSiteHeader = String.format("%s=%s; Path=%s; HttpOnly=%s; Secure=%s; SameSite=%s; Max-Age=%d", 
                    JWT_COOKIE_NAME, token, cookie.getPath(), 
                    cookie.isHttpOnly(), cookie.getSecure(), 
                    cookieSameSite, cookie.getMaxAge());
            
            if (cookie.getDomain() != null) {
                sameSiteHeader += "; Domain=" + cookie.getDomain();
            }
            
            response.setHeader("Set-Cookie", sameSiteHeader);
        }
        
        log.debug("Added JWT cookie to response");
    }
    
    /**
     * Adds a JWT cookie to the response with specified max age
     * 
     * @param response the HTTP response
     * @param token the JWT token
     * @param maxAge the maximum age of the cookie in seconds
     */
    public static void addJwtCookie(HttpServletResponse response, String token, int maxAge) {
        Cookie cookie = createJwtCookie(token, maxAge);
        response.addCookie(cookie);
        
        // Set SameSite attribute (not directly supported by Java Cookie API)
        if (cookieSameSite != null && !cookieSameSite.isEmpty()) {
            String sameSiteHeader = String.format("%s=%s; Path=%s; HttpOnly=%s; Secure=%s; SameSite=%s; Max-Age=%d", 
                    JWT_COOKIE_NAME, token, cookie.getPath(), 
                    cookie.isHttpOnly(), cookie.getSecure(), 
                    cookieSameSite, cookie.getMaxAge());
            
            if (cookie.getDomain() != null) {
                sameSiteHeader += "; Domain=" + cookie.getDomain();
            }
            
            response.setHeader("Set-Cookie", sameSiteHeader);
        }
        
        log.debug("Added JWT cookie to response with max age: {}", maxAge);
    }
    
    /**
     * Deletes the JWT cookie
     * 
     * @param response the HTTP response
     */
    public static void deleteJwtCookie(HttpServletResponse response) {
        Cookie cookie = new Cookie(JWT_COOKIE_NAME, "");
        cookie.setPath(cookiePath != null ? cookiePath : "/");
        cookie.setHttpOnly(cookieHttpOnly);
        cookie.setSecure(cookieSecure);
        cookie.setMaxAge(0); // Delete immediately
        
        // Set domain if provided
        if (cookieDomain != null && !cookieDomain.equals("localhost")) {
            cookie.setDomain(cookieDomain);
        }
        
        response.addCookie(cookie);
        
        // Set SameSite attribute (not directly supported by Java Cookie API)
        if (cookieSameSite != null && !cookieSameSite.isEmpty()) {
            String sameSiteHeader = String.format("%s=; Path=%s; HttpOnly=%s; Secure=%s; SameSite=%s; Max-Age=0", 
                    JWT_COOKIE_NAME, cookie.getPath(), 
                    cookie.isHttpOnly(), cookie.getSecure(), 
                    cookieSameSite);
            
            if (cookie.getDomain() != null) {
                sameSiteHeader += "; Domain=" + cookie.getDomain();
            }
            
            response.setHeader("Set-Cookie", sameSiteHeader);
        }
        
        log.debug("Deleted JWT cookie from response");
    }
    
    /**
     * Gets a cookie by name
     * 
     * @param request the HTTP request
     * @param name the name of the cookie
     * @return the cookie if found, empty otherwise
     */
    public static Optional<Cookie> getCookie(HttpServletRequest request, String name) {
        Cookie[] cookies = request.getCookies();
        
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if (cookie.getName().equals(name)) {
                    return Optional.of(cookie);
                }
            }
        }
        
        return Optional.empty();
    }
    
    /**
     * Gets the JWT cookie
     * 
     * @param request the HTTP request
     * @return the JWT cookie if found, empty otherwise
     */
    public static Optional<Cookie> getJwtCookie(HttpServletRequest request) {
        return getCookie(request, JWT_COOKIE_NAME);
    }
    
    /**
     * Serializes an object to a cookie value
     * 
     * @param object the object to serialize
     * @return the serialized object as a string
     */
    public static String serialize(Object object) {
        return Base64.getUrlEncoder().encodeToString(SerializationUtils.serialize(object));
    }
    
    /**
     * Deserializes a cookie value to an object
     * 
     * @param <T> the type of the object
     * @param cookie the cookie to deserialize
     * @param cls the class of the object
     * @return the deserialized object
     */
    public static <T> T deserialize(Cookie cookie, Class<T> cls) {
        return cls.cast(SerializationUtils.deserialize(
                Base64.getUrlDecoder().decode(cookie.getValue())
        ));
    }
} 