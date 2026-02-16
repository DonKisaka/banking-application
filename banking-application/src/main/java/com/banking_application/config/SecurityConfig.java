package com.banking_application.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfigurationSource;

import java.awt.*;
import java.net.http.HttpHeaders;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {
    private final AuthenticationProvider authenticationProvider;

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    public SecurityConfig(AuthenticationProvider authenticationProvider, JwtAuthenticationFilter jwtAuthenticationFilter) {
        this.authenticationProvider = authenticationProvider;
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    }


    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, CorsConfigurationSource corsConfigurationSource) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .authorizeHttpRequests(authRequests -> authRequests
                        .requestMatchers("/api/v1/auth/**")
                        .permitAll()
                        .requestMatchers( "/webauthn/register/**",     // Passkey registration
                                "/webauthn/authenticate/**")
                        .permitAll()
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")
                        .requestMatchers("/api/manager/**").hasAnyRole("ADMIN", "MANAGER")
                        .anyRequest()
                        .authenticated()
                )

                .webAuthn(wa -> wa
                        .allowedOrigins("http://localhost:8080")
                        .rpName("HDFC Net Banking")
                        .rpId("localhost")
                )

                .oneTimeTokenLogin(ott ->
                        ott.tokenGenerationSuccessHandler((request, response, oneTimeToken) -> {

                            System.out.println("please visit http://localhost:8080/login/ott?token=" + oneTimeToken.getTokenValue());

                            response.setHeader(HttpHeaders.CONTENT_TYPE, MediaType.TEXT_PLAIN_VALUE);
                            response.getWriter().println("You've got the console mail!");
                            response.getWriter().flush();
                        }))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authenticationProvider(authenticationProvider)
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();

    }

}
