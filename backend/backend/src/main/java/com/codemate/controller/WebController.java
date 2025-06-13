package com.codemate.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import java.security.Principal;

/**
 * Web Controller for handling traditional form-based authentication pages
 * This controller serves HTML pages (not JSON like REST controllers)
 * 
 * @Controller annotation tells Spring this is a web controller that returns views
 * Unlike @RestController, @Controller returns view names that get resolved to HTML templates
 */
@Controller
public class WebController {

    /**
     * Handles GET requests to /login
     * This method serves the login form page
     * 
     * @param error - Optional parameter to show error messages
     * @param logout - Optional parameter to show logout success message
     * @param model - Spring Model object to pass data to the view
     * @return String - name of the template to render (login.html)
     */
    @GetMapping("/login")
    public String loginPage(
            @RequestParam(value = "error", required = false) String error,
            @RequestParam(value = "message", required = false) String message,
            @RequestParam(value = "logout", required = false) String logout,
            Model model) {
        
        // Add error message if login failed
        if (error != null) {
            String errorMessage = message != null ? message : "Invalid username or password!";
            model.addAttribute("error", errorMessage);
        }
        
        // Add success message if user just logged out
        if (logout != null) {
            model.addAttribute("logout", "You have been logged out successfully!");
        }
        
        // Return the name of the template (login.html in templates folder)
        return "login";
    }

    /**
     * Handles GET requests to /signup
     * This method serves the registration form page
     */
    @GetMapping("/signup")
    public String signupPage() {
        return "signup";
    }

    /**
     * Handles GET requests to /dashboard
     * This is the main page users see after successful login
     * 
     * @param principal - Automatically injected by Spring Security, contains user info
     * @param model - Spring Model to pass data to the view
     */
    @GetMapping("/dashboard")
    public String dashboardPage(Principal principal, Model model) {
        // Principal contains the username of the authenticated user
        if (principal != null) {
            model.addAttribute("username", principal.getName());
        }
        return "dashboard";
    }

    /**
     * Root path redirects to dashboard
     * If user is not authenticated, Spring Security will redirect to login
     */
    @GetMapping("/")
    public String home() {
        return "redirect:/dashboard";
    }
} 