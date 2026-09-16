package com.example.demo.auth.config;

import com.example.demo.auth.jwt.JwtAuthenticationFilter;
import com.example.demo.auth.jwt.JwtTokenProvider;
import com.example.demo.auth.service.MemberUserDetailsService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * JWT 기반 stateless 인증.
 * 세션/쿠키를 쓰지 않으므로 CSRF 필터가 필요 없다 — 예전에 "/api/**"를 필터 체인에서 통째로
 * 우회시켰던 이유(CSRF가 POST를 막는 문제) 자체가 사라졌다.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http, JwtTokenProvider jwtTokenProvider,
                                           MemberUserDetailsService memberUserDetailsService) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(
                        "/api/auth/login",
                        "/api/members/signup",
                        "/api/members/check-username",
                        "/api/sms/send",
                        "/api/sms/verify",
                        "/api/tour/**"
                ).permitAll()
                // /api/sms/guardian/** 는 명단에 없으므로 anyRequest().authenticated() 로 자동 인증 대상 —
                // 보호자 인증은 본인 인증(로그인)을 마친 회원만 요청 가능해야 하므로 의도적으로 공개하지 않는다.
                .anyRequest().authenticated()
            )
            // 토큰이 없거나 무효할 때 기본값(403) 대신 401을 반환
            .exceptionHandling(ex -> ex.authenticationEntryPoint((request, response, authException) -> {
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.setContentType("application/json;charset=UTF-8");
                response.getWriter().write("{\"success\":false,\"message\":\"인증이 필요합니다.\",\"data\":null}");
            }))
            .addFilterBefore(new JwtAuthenticationFilter(jwtTokenProvider, memberUserDetailsService),
                    UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
