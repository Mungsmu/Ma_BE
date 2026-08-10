package com.example.demo.user.controller;

import com.example.demo.common.dto.ApiResponse;
import com.example.demo.user.dto.MemberResponse;
import com.example.demo.user.dto.SignupRequest;
import com.example.demo.user.dto.UpdateMemberRequest;
import com.example.demo.user.service.MemberService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
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

    /** 내 정보 조회 (로그인 필요) */
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<MemberResponse>> me(@AuthenticationPrincipal UserDetails userDetails) {
        MemberResponse response = memberService.getMyInfo(userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.ok("내 정보 조회 성공", response));
    }

    /** 내 정보 수정 (로그인 필요, 아이디/비밀번호/이메일은 변경 불가) */
    @PatchMapping("/me")
    public ResponseEntity<ApiResponse<MemberResponse>> updateMe(@AuthenticationPrincipal UserDetails userDetails,
                                                                 @Valid @RequestBody UpdateMemberRequest request) {
        MemberResponse response = memberService.updateMyInfo(userDetails.getUsername(), request);
        return ResponseEntity.ok(ApiResponse.ok("내 정보가 수정되었습니다.", response));
    }
}
