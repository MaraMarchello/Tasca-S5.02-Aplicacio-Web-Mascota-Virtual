package com.codemate.security;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.SerializationUtils;

import java.io.Serializable;
import java.util.Base64;
import java.util.Optional;

/**
 * Utility class for handling cookies
 */
@Slf4j
@Component
public class CookieUtils {

    public static final String JWT_COOKIE_NAME = "jwt";
    
    private final String cookieDomain;
    private final String cookiePath;
    private final boolean cookieSecure;
    private final boolean cookieHttpOnly;
    private final String cookieSameSite;
    private final int cookieMaxAge;
    
    /**
     * Constructor that initializes configuration values
     * Package-private to hide it from public API while allowing Spring to use it for injection
     */
    CookieUtils(
            @Value("${app.jwt.cookie.domain:localhost}") String domain,
            @Value("${app.jwt.cookie.path:/}") String path,
            @Value("${app.jwt.cookie.secure:true}") boolean secure,
            @Value("${app.jwt.cookie.http-only:true}") boolean httpOnly,
            @Value("${app.jwt.cookie.same-site:strict}") String sameSite,
            @Value("${app.jwt.cookie.max-age:86400}") int maxAge) {
        this.cookieDomain = domain;
        this.cookiePath = path;
        this.cookieSecure = secure;
        this.cookieHttpOnly = httpOnly;
        this.cookieSameSite = sameSite;
        this.cookieMaxAge = maxAge;

        log.info("Initialized cookie settings: domain={}, path={}, secure={}, httpOnly={}, sameSite={}, maxAge={}",
                this.cookieDomain, this.cookiePath, this.cookieSecure, this.cookieHttpOnly, this.cookieSameSite, this.cookieMaxAge);
    }
    
    /**
     * Creates a secure cookie with the JWT token
     * 
     * @param token the JWT token
     * @param maxAge the maximum age of the cookie in seconds
     * @return the created cookie
     */
    public Cookie createJwtCookie(String token, int maxAge) {
        Cookie cookie = new Cookie(JWT_COOKIE_NAME, token);
        cookie.setPath(cookiePath);
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
    public Cookie createJwtCookie(String token) {
        return createJwtCookie(token, cookieMaxAge);
    }
    
    /**
     * Adds a JWT cookie to the response
     * 
     * @param response the HTTP response
     * @param token the JWT token
     */
    public void addJwtCookie(HttpServletResponse response, String token) {
        Cookie cookie = createJwtCookie(token);
        
        // Set SameSite attribute (not directly supported by Java Cookie API)
        if (cookieSameSite != null && !cookieSameSite.isEmpty()) {
            StringBuilder sameSiteHeader = new StringBuilder();
            sameSiteHeader.append(String.format("%s=%s; Path=%s", JWT_COOKIE_NAME, token, cookie.getPath()));
            
            if (cookieHttpOnly) {
                sameSiteHeader.append("; HttpOnly");
            }
            
            if (cookieSecure) {
                sameSiteHeader.append("; Secure");
            }
            
            sameSiteHeader.append(String.format("; SameSite=%s; Max-Age=%d", cookieSameSite, cookie.getMaxAge()));
            
            if (cookie.getDomain() != null) {
                sameSiteHeader.append("; Domain=").append(cookie.getDomain());
            }
            
            response.addHeader("Set-Cookie", sameSiteHeader.toString());
        } else {
            response.addCookie(cookie);
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
    public void addJwtCookie(HttpServletResponse response, String token, int maxAge) {
        Cookie cookie = createJwtCookie(token, maxAge);
        
        // Set SameSite attribute (not directly supported by Java Cookie API)
        if (cookieSameSite != null && !cookieSameSite.isEmpty()) {
            StringBuilder sameSiteHeader = new StringBuilder();
            sameSiteHeader.append(String.format("%s=%s; Path=%s", JWT_COOKIE_NAME, token, cookie.getPath()));
            
            if (cookieHttpOnly) {
                sameSiteHeader.append("; HttpOnly");
            }
            
            if (cookieSecure) {
                sameSiteHeader.append("; Secure");
            }
            
            sameSiteHeader.append(String.format("; SameSite=%s; Max-Age=%d", cookieSameSite, cookie.getMaxAge()));
            
            if (cookie.getDomain() != null) {
                sameSiteHeader.append("; Domain=").append(cookie.getDomain());
            }
            
            response.addHeader("Set-Cookie", sameSiteHeader.toString());
        } else {
            response.addCookie(cookie);
        }
        
        log.debug("Added JWT cookie to response with max age: {}", maxAge);
    }
    
    /**
     * Deletes the JWT cookie
     * 
     * @param response the HTTP response
     */
    public void deleteJwtCookie(HttpServletResponse response) {
        Cookie cookie = new Cookie(JWT_COOKIE_NAME, "");
        cookie.setPath(cookiePath);
        cookie.setHttpOnly(cookieHttpOnly);
        cookie.setSecure(cookieSecure);
        cookie.setMaxAge(0); // Delete immediately
        
        // Set domain if provided
        if (cookieDomain != null && !cookieDomain.equals("localhost")) {
            cookie.setDomain(cookieDomain);
        }
        
        // Set SameSite attribute (not directly supported by Java Cookie API)
        if (cookieSameSite != null && !cookieSameSite.isEmpty()) {
            StringBuilder sameSiteHeader = new StringBuilder();
            sameSiteHeader.append(String.format("%s=; Path=%s", JWT_COOKIE_NAME, cookie.getPath()));
            
            if (cookieHttpOnly) {
                sameSiteHeader.append("; HttpOnly");
            }
            
            if (cookieSecure) {
                sameSiteHeader.append("; Secure");
            }
            
            sameSiteHeader.append(String.format("; SameSite=%s; Max-Age=0", cookieSameSite));
            
            if (cookie.getDomain() != null) {
                sameSiteHeader.append("; Domain=").append(cookie.getDomain());
            }
            
            response.addHeader("Set-Cookie", sameSiteHeader.toString());
        } else {
            response.addCookie(cookie);
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
    public static String serialize(Serializable object) {
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
        byte[] bytes = Base64.getUrlDecoder().decode(cookie.getValue());
        return cls.cast(SerializationUtils.deserialize(bytes));
    }
} 