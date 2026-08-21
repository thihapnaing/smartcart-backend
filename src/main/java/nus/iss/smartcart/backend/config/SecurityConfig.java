package nus.iss.smartcart.backend.config;

import nus.iss.smartcart.backend.security.JwtAuthenticationFilter;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;

import org.springframework.security.config.http.SessionCreationPolicy;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

//Author: Junior

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    public SecurityConfig(
            JwtAuthenticationFilter jwtAuthenticationFilter
    ) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    @SuppressWarnings("java:S4502") // CSRF is intentionally disabled for stateless JWT auth (no cookie session)
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http
    ) {

        http
                // Safe to disable: this API is stateless (SessionCreationPolicy.STATELESS below)
                // and authenticates every request via a JWT Bearer token read from the
                // Authorization header (JwtAuthenticationFilter), never via a browser-managed
                // session cookie - CSRF only matters when a browser automatically attaches
                // credentials (cookies) to a request, which doesn't happen here.
                .csrf(AbstractHttpConfigurer::disable)

                .cors(cors -> {})

                .sessionManagement(session ->
                        session.sessionCreationPolicy(
                                SessionCreationPolicy.STATELESS
                        )
                )

                .authorizeHttpRequests(auth -> auth

                        // Must come before the /api/auth/** permitAll rule below - Spring
                        // Security matches in declaration order, and this one specific path
                        // needs a real JWT (it changes the caller's own password) even though
                        // everything else under /api/auth/** is intentionally public.
                        .requestMatchers("/api/auth/change-password")
                        .authenticated()

                        // Authentication
                        .requestMatchers("/api/auth/**")
                        .permitAll()

                        // Static images
                        .requestMatchers("/images/**")
                        .permitAll()

                        // Avatar images
                        .requestMatchers("/api/user-profile/with-avatar").permitAll()

                        // Unauthenticated aggregate numbers for the /admin/login screen (see
                        // PublicStatsController) - deliberately separate from /api/admin/**,
                        // which requires an authenticated admin.
                        .requestMatchers("/api/public/**")
                        .permitAll()

                        // Product browsing
                        .requestMatchers(
                                "/api/products/**",
                                "/api/product/**",
                                "/api/categories/**"
                        )
                        .permitAll()

                        .requestMatchers("/internal/tools/**")
                        .permitAll()

                        // Admin-only endpoints. Without this, anyRequest().authenticated()
                        // below would let ANY logged-in user (including customers) call these -
                        // authenticated() only proves who the caller is, not what they're
                        // allowed to do.
                        .requestMatchers("/api/admin/**")
                        .hasRole("ADMIN")


                        // Customer facing endpoints. There's no login UI for these
                        .requestMatchers(
                        		"/api/cart/**",
                                "/api/chat/**",
                                "/api/orders/**",
                                "/api/user-profile/**",
                                "/api/v1/products/**",
                                "/api/home/**"
                        )
                        .permitAll()
                                                            
                        // Everything else requires authentication
                        .anyRequest()
                        .authenticated()
                )

                .addFilterBefore(
                        jwtAuthenticationFilter,
                        UsernamePasswordAuthenticationFilter.class
                );

        return http.build();
    }
}
