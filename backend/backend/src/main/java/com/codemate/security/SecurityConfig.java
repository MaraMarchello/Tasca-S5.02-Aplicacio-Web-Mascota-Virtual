package com.codemate.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.web.cors.CorsConfigurationSource;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private final JwtTokenProvider tokenProvider;
    private final CustomUserDetailsService customUserDetailsService;
    private final SecurityContextService securityContextService;
    private final CorsConfigurationSource corsConfigurationSource;

    public SecurityConfig(
            JwtTokenProvider tokenProvider, 
            CustomUserDetailsService customUserDetailsService,
            SecurityContextService securityContextService,
            CorsConfigurationSource corsConfigurationSource) {
        this.tokenProvider = tokenProvider;
        this.customUserDetailsService = customUserDetailsService;
        this.securityContextService = securityContextService;
        this.corsConfigurationSource = corsConfigurationSource;
    }

    @Bean
    public JwtAuthenticationFilter jwtAuthenticationFilter() {
        return new JwtAuthenticationFilter(tokenProvider, customUserDetailsService, securityContextService);
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authenticationConfiguration) throws Exception {
        return authenticationConfiguration.getAuthenticationManager();
    }
    


    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        
        http
            .cors(cors -> cors.configurationSource(corsConfigurationSource))
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .csrf(csrf -> csrf.disable()) // Disable CSRF for REST API
            .authorizeHttpRequests(auth -> auth
                // Public API endpoints
                .requestMatchers(AntPathRequestMatcher.antMatcher("/api/auth/**")).permitAll()
                .requestMatchers(AntPathRequestMatcher.antMatcher("/api/health/**")).permitAll()
                .requestMatchers(AntPathRequestMatcher.antMatcher("/api/public/**")).permitAll()
                .requestMatchers(AntPathRequestMatcher.antMatcher("/api/v1/test/**")).permitAll()
                
                // Git scenarios public access for demo mode
                .requestMatchers(AntPathRequestMatcher.antMatcher("/api/v1/git/scenarios")).permitAll()
                .requestMatchers(AntPathRequestMatcher.antMatcher("/api/v1/git/scenarios/**")).permitAll()
                
                // Admin-only endpoints
                .requestMatchers(AntPathRequestMatcher.antMatcher("/api/admin/**")).hasRole("ADMIN")
                .requestMatchers(AntPathRequestMatcher.antMatcher("/api/security-test/admin-only")).hasRole("ADMIN")
                
                // User-specific pet system endpoints (require USER role)
                .requestMatchers(AntPathRequestMatcher.antMatcher("/api/pets/**")).hasRole("USER")
                .requestMatchers(AntPathRequestMatcher.antMatcher("/api/shop/**")).hasRole("USER")
                .requestMatchers(AntPathRequestMatcher.antMatcher("/api/achievements/**")).hasRole("USER")
                .requestMatchers(AntPathRequestMatcher.antMatcher("/api/points/**")).hasRole("USER")
                
                // Other protected endpoints
                .requestMatchers(AntPathRequestMatcher.antMatcher("/api/security-test/user-only")).hasRole("USER")
                .requestMatchers(AntPathRequestMatcher.antMatcher("/api/security-test/**")).authenticated()
                .requestMatchers(AntPathRequestMatcher.antMatcher("/api/**")).authenticated()
                
                // Deny all other requests for security
                .anyRequest().denyAll())

            // Add JWT authentication filter
            .addFilterBefore(jwtAuthenticationFilter(), UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
} 