package com.codemate.security;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.client.web.HttpSessionOAuth2AuthorizationRequestRepository;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.authentication.logout.LogoutHandler;
import org.springframework.security.web.csrf.CsrfFilter;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private final JwtTokenProvider tokenProvider;
    private final CustomUserDetailsService customUserDetailsService;
    private final RedirectLoopPreventionFilter redirectLoopPreventionFilter;
    private final SecurityContextService securityContextService;
    private final SecurityContextCleanupFilter securityContextCleanupFilter;

    public SecurityConfig(
            JwtTokenProvider tokenProvider, 
            CustomUserDetailsService customUserDetailsService,
            RedirectLoopPreventionFilter redirectLoopPreventionFilter,
            SecurityContextService securityContextService,
            SecurityContextCleanupFilter securityContextCleanupFilter) {
        this.tokenProvider = tokenProvider;
        this.customUserDetailsService = customUserDetailsService;
        this.redirectLoopPreventionFilter = redirectLoopPreventionFilter;
        this.securityContextService = securityContextService;
        this.securityContextCleanupFilter = securityContextCleanupFilter;
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
    public LogoutHandler jwtLogoutHandler() {
        return (request, response, authentication) -> {
            CookieUtils.deleteJwtCookie((HttpServletResponse) response);
            securityContextService.clearContext();
        };
    }

    @Bean
    public SecurityFilterChain filterChain(
            HttpSecurity http,
            OAuth2AuthenticationSuccessHandler oAuth2AuthenticationSuccessHandler,
            OAuth2AuthenticationFailureHandler oAuth2AuthenticationFailureHandler,
            OAuth2UserService<OAuth2UserRequest, OAuth2User> customOAuth2UserService,
            LogoutHandler jwtLogoutHandler) throws Exception {
        
        final String LOGIN_PAGE = "/login";
        
        http
            .cors(cors -> cors.configure(http))
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
                .maximumSessions(1)
                .maxSessionsPreventsLogin(false))
            .csrf(csrf -> csrf
                .ignoringRequestMatchers("/api/**")) // Disable CSRF for API endpoints only
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(AntPathRequestMatcher.antMatcher("/api/auth/**")).permitAll()
                .requestMatchers(AntPathRequestMatcher.antMatcher("/api/oauth2/**")).permitAll()
                .requestMatchers(AntPathRequestMatcher.antMatcher("/api/public/**")).permitAll()
                .requestMatchers(AntPathRequestMatcher.antMatcher("/api/security-test/admin-only")).hasRole("ADMIN")
                .requestMatchers(AntPathRequestMatcher.antMatcher("/api/security-test/user-only")).hasRole("USER")
                .requestMatchers(AntPathRequestMatcher.antMatcher("/api/security-test/**")).authenticated()
                .requestMatchers(AntPathRequestMatcher.antMatcher(LOGIN_PAGE)).permitAll()
                .requestMatchers(AntPathRequestMatcher.antMatcher("/signup")).permitAll()
                .requestMatchers(AntPathRequestMatcher.antMatcher("/css/**")).permitAll()
                .requestMatchers(AntPathRequestMatcher.antMatcher("/js/**")).permitAll()
                .requestMatchers(AntPathRequestMatcher.antMatcher("/images/**")).permitAll()
                .requestMatchers(AntPathRequestMatcher.antMatcher("/api/**")).authenticated()
                .requestMatchers(AntPathRequestMatcher.antMatcher("/dashboard")).authenticated()
                .requestMatchers(AntPathRequestMatcher.antMatcher("/")).authenticated()
                .anyRequest().authenticated())
            .formLogin(form -> form
                .loginPage(LOGIN_PAGE)
                .loginProcessingUrl("/login")
                .defaultSuccessUrl("/dashboard", true)
                .failureUrl(LOGIN_PAGE + "?error=true")
                .usernameParameter("email")
                .passwordParameter("password")
                .permitAll())
            .logout(logout -> logout
                .logoutUrl("/logout")
                .addLogoutHandler(jwtLogoutHandler)
                .logoutSuccessUrl(LOGIN_PAGE + "?logout=true")
                .invalidateHttpSession(true)
                .deleteCookies("JSESSIONID")
                .permitAll())
            .oauth2Login(oauth2 -> oauth2
                .loginPage(LOGIN_PAGE)
                .defaultSuccessUrl("/dashboard", true)
                .failureUrl(LOGIN_PAGE + "?error=true")
                .authorizationEndpoint(authorization -> authorization
                    .baseUri("/oauth2/authorize")
                    .authorizationRequestRepository(new HttpSessionOAuth2AuthorizationRequestRepository()))
                .redirectionEndpoint(redirection -> redirection
                    .baseUri("/oauth2/callback/*"))
                .userInfoEndpoint(userInfo -> userInfo
                    .userService(customOAuth2UserService))
                .successHandler(oAuth2AuthenticationSuccessHandler)
                .failureHandler(oAuth2AuthenticationFailureHandler))
            // Add filters in the correct order
            .addFilterBefore(redirectLoopPreventionFilter, CsrfFilter.class)
            .addFilterBefore(jwtAuthenticationFilter(), UsernamePasswordAuthenticationFilter.class)
            .addFilterAfter(securityContextCleanupFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
} 