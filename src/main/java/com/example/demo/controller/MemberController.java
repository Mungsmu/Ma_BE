package com.example.demo.controller;

import com.example.demo.dto.ApiResponse;
import com.example.demo.dto.SignupRequest;
import com.example.demo.service.MemberService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/members")
@Validated
public class MemberController {

    private final MemberService memberService;

    public MemberController(MemberService memberService) {
        this.memberService = memberService;
    }

    /** 아이디 중복확인 */
    @GetMapping("/check-username")
    public ResponseEntity<ApiResponse<Map<String, Boolean>>> checkUsername(
            @RequestParam @NotBlank String username) {
        boolean available = memberService.isUsernameAvailable(username);
        String message = available ? "사용 가능한 아이디입니다." : "이미 사용 중인 아이디입니다.";
        return ResponseEntity.ok(ApiResponse.ok(message, Map.of("available", available)));
    }

    /** 회원가입 */
    @PostMapping("/signup")
    public ResponseEntity<ApiResponse<Map<String, Long>>> signup(
            @Valid @RequestBody SignupRequest request) {
        Long id = memberService.signup(request);
        return ResponseEntity.ok(ApiResponse.ok("회원가입이 완료되었습니다.", Map.of("memberId", id)));
    }
}
