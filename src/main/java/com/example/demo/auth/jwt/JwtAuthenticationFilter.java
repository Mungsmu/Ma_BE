package com.example.demo.auth.jwt;

import com.example.demo.auth.service.MemberUserDetailsService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Authorization: Bearer &lt;token&gt; 헤더를 읽어 유효하면 SecurityContext 에 인증 정보를 채운다.
 * 토큰이 없거나 무효해도 체인은 그대로 진행시키고, 실제 차단은 SecurityConfig 의 authorizeHttpRequests 가 담당한다.
 */
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String HEADER = "Authorization";
    private static final String PREFIX = "Bearer ";

    private final JwtTokenProvider jwtTokenProvider;
    private final MemberUserDetailsService memberUserDetailsService;

    public JwtAuthenticationFilter(JwtTokenProvider jwtTokenProvider,
                                   MemberUserDetailsService memberUserDetailsService) {
        this.jwtTokenProvider = jwtTokenProvider;
        this.memberUserDetailsService = memberUserDetailsService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String token = resolveToken(request);

        if (token != null && jwtTokenProvider.validateToken(token)) {
            UserDetails userDetails = memberUserDetailsService.loadUserByUsername(jwtTokenProvider.getUsername(token));
            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
            authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            SecurityContextHolder.getContext().setAuthentication(authentication);
        }

        filterChain.doFilter(request, response);
    }

    private String resolveToken(HttpServletRequest request) {
        String header = request.getHeader(HEADER);
        if (header != null && header.startsWith(PREFIX)) {
            return header.substring(PREFIX.length());
        }
        return null;
    }
}
