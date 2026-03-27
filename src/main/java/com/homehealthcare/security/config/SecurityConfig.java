package com.homehealthcare.security.config;

import com.homehealthcare.security.tenant.TenantContextFilter;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AnonymousAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
public class SecurityConfig {

    private static final String ACCESS_TOKEN_COOKIE = "hhc_access_token";
    private static final String REFRESH_TOKEN_COOKIE = "hhc_refresh_token";
    private static final String SESSION_ID_COOKIE = "hhc_session_id";

    @Bean
    SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            TenantContextFilter tenantContextFilter,
            WebSecurityProperties webSecurityProperties) throws Exception {
        CookieCsrfTokenRepository csrfTokenRepository = CookieCsrfTokenRepository.withHttpOnlyFalse();
        csrfTokenRepository.setCookieCustomizer(builder -> builder
                .secure(true)
                .sameSite("Lax")
                .path("/"));
        CsrfTokenRequestAttributeHandler csrfTokenRequestHandler = new CsrfTokenRequestAttributeHandler();
        csrfTokenRequestHandler.setCsrfRequestAttributeName("_csrf");

        http
                .cors(Customizer.withDefaults())
                .csrf(csrf -> csrf
                        .csrfTokenRepository(csrfTokenRepository)
                        .csrfTokenRequestHandler(csrfTokenRequestHandler)
                        .ignoringRequestMatchers(this::shouldSkipCsrf))
                .headers(headers -> headers
                        .contentSecurityPolicy(csp -> csp.policyDirectives(webSecurityProperties.getContentSecurityPolicy()))
                        .frameOptions(frameOptions -> frameOptions.deny())
                        .referrerPolicy(referrerPolicy -> referrerPolicy.policy(
                                org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter.ReferrerPolicy.NO_REFERRER))
                        .xssProtection(xss -> xss.disable()))
                .authorizeHttpRequests(authorize -> authorize.anyRequest().permitAll())
                .httpBasic(Customizer.withDefaults())
                .formLogin(formLogin -> formLogin.disable())
                .addFilterAfter(tenantContextFilter, AnonymousAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    CorsConfigurationSource corsConfigurationSource(WebSecurityProperties webSecurityProperties) {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(webSecurityProperties.getAllowedOrigins());
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("Authorization", "Content-Type", "X-CSRF-TOKEN", "X-Session-Id", "X-Refresh-Token"));
        configuration.setExposedHeaders(List.of("Location"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    private boolean shouldSkipCsrf(HttpServletRequest request) {
        if (HttpMethod.GET.matches(request.getMethod())
                || HttpMethod.HEAD.matches(request.getMethod())
                || HttpMethod.OPTIONS.matches(request.getMethod())
                || HttpMethod.TRACE.matches(request.getMethod())) {
            return true;
        }

        String authorizationHeader = request.getHeader("Authorization");
        if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
            return true;
        }

        boolean hasSessionCookies = request.getCookies() != null && java.util.Arrays.stream(request.getCookies())
                .map(jakarta.servlet.http.Cookie::getName)
                .anyMatch(name -> name.equals(ACCESS_TOKEN_COOKIE)
                        || name.equals(REFRESH_TOKEN_COOKIE)
                        || name.equals(SESSION_ID_COOKIE));

        return !hasSessionCookies;
    }
}
