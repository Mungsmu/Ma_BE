package com.example.demo.auth.controller;

import com.example.demo.auth.dto.LoginRequest;
import com.example.demo.auth.jwt.JwtTokenProvider;
import com.example.demo.common.dto.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;

    public AuthController(AuthenticationManager authenticationManager, JwtTokenProvider jwtTokenProvider) {
        this.authenticationManager = authenticationManager;
        this.jwtTokenProvider = jwtTokenProvider;
    }

    /** 로그인. 성공 시 JWT 발급. */
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<Map<String, String>>> login(@Valid @RequestBody LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.username(), request.password()));

        String token = jwtTokenProvider.generateToken(request.username());
        return ResponseEntity.ok(ApiResponse.ok("로그인되었습니다.",
                Map.of("accessToken", token, "tokenType", "Bearer")));
    }
}
