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
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

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

    /**
     * 모바일 앱(Expo)/배포 도메인에서 호출할 수 있도록 모든 origin을 허용한다 (개발 단계 설정).
     * 쿠키 인증을 쓰지 않고 JWT를 Authorization 헤더로만 주고받으므로 allowCredentials는 false로 둬도 된다
     * — 그래서 "*" origin과 함께 써도 Spring Security 제약에 걸리지 않는다.
     * 배포 전에는 실제 프론트 도메인으로 좁혀야 한다.
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOriginPatterns(List.of("*"));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(false);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http, JwtTokenProvider jwtTokenProvider,
                                           MemberUserDetailsService memberUserDetailsService,
                                           CorsConfigurationSource corsConfigurationSource) throws Exception {
        http
            .cors(cors -> cors.configurationSource(corsConfigurationSource))
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
