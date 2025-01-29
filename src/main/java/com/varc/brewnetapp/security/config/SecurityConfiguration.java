package com.varc.brewnetapp.security.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.varc.brewnetapp.security.filter.DaoAuthenticationFilter;
import com.varc.brewnetapp.security.filter.JwtAccessTokenFilter;
import com.varc.brewnetapp.security.filter.JwtRefreshTokenFilter;
import com.varc.brewnetapp.security.provider.ProviderManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

@Configuration
@EnableWebSecurity
public class SecurityConfiguration {
    private final ObjectMapper objectMapper;
    private final ProviderManager providerManager;
    private final AuthenticationEntryPoint authenticationEntryPoint;
    private final AccessDeniedHandler accessDeniedHandler;

    @Autowired
    public SecurityConfiguration(
            ObjectMapper objectMapper,
            ProviderManager providerManager,
            AuthenticationEntryPoint authenticationEntryPoint,
            AccessDeniedHandler accessDeniedHandler
    ) {
        this.objectMapper = objectMapper;
        this.providerManager = providerManager;
        this.authenticationEntryPoint = authenticationEntryPoint;
        this.accessDeniedHandler = accessDeniedHandler;
    }

    @Bean
    public SecurityFilterChain configure(HttpSecurity http) throws Exception {
        http
                // token 기반 인증으로 csrf, session 비활성화
                .csrf(AbstractHttpConfigurer::disable)  // csrf 비활성화
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)) // 세션 비활성화
                .formLogin(AbstractHttpConfigurer::disable)

                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers(new AntPathRequestMatcher("/")).permitAll()
                        .requestMatchers(new AntPathRequestMatcher("/api/v1/check/**")).permitAll()
                        .requestMatchers(new AntPathRequestMatcher("/api/v1/auth/**")).permitAll()
                        .requestMatchers(new AntPathRequestMatcher("/api/v1/email/**")).permitAll()
                        .requestMatchers(new AntPathRequestMatcher("/swagger-ui/**")).permitAll()
                        .requestMatchers(new AntPathRequestMatcher("/v3/api-docs/**")).permitAll()

                        // 가맹점
                        .requestMatchers(new AntPathRequestMatcher("/api/v1/franchise/**")).hasAnyRole("FRANCHISE", "MASTER")

                        // 본사
                        .requestMatchers(new AntPathRequestMatcher("/api/v1/hq/**")).hasAnyRole("MASTER", "GENERAL_ADMIN", "RESPONSIBLE_ADMIN")

                        // 본사책임자
                        .requestMatchers(new AntPathRequestMatcher("/api/v1/super/**")).hasRole("RESPONSIBLE_ADMIN")

                        // 본사책임자
                        .requestMatchers(new AntPathRequestMatcher("/api/v1/responsible/**")).hasAnyRole("MASTER", "RESPONSIBLE_ADMIN")

                        // 마스터
                        .requestMatchers(new AntPathRequestMatcher("/api/v1/master/**")).hasRole("MASTER")

                        // 마스터
                        .requestMatchers(new AntPathRequestMatcher("/api/v1/require-auth/master")).hasRole("MASTER")

                        // 마스터
                        .requestMatchers(new AntPathRequestMatcher("/api/v1/delivery")).hasAnyRole("MASTER", "DELIVERY")

                        .anyRequest().authenticated()
                )

                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint(authenticationEntryPoint) // 401 UNAUTHORIZED
                        .accessDeniedHandler(accessDeniedHandler)           // 403 FORBIDDEN
                )

                .addFilterBefore(new JwtAccessTokenFilter(providerManager), UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(new JwtRefreshTokenFilter(providerManager, objectMapper), UsernamePasswordAuthenticationFilter.class)
                .addFilter(new DaoAuthenticationFilter(providerManager, objectMapper));

        return http.build();
    }

}
