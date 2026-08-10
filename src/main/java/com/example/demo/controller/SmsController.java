package com.example.demo.controller;

import com.example.demo.dto.ApiResponse;
import com.example.demo.dto.SmsSendRequest;
import com.example.demo.dto.SmsVerifyRequest;
import com.example.demo.sms.SmsVerificationService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/sms")
public class SmsController {

    private final SmsVerificationService smsVerificationService;

    public SmsController(SmsVerificationService smsVerificationService) {
        this.smsVerificationService = smsVerificationService;
    }

    /** 인증번호 발송 */
    @PostMapping("/send")
    public ResponseEntity<ApiResponse<Void>> send(@Valid @RequestBody SmsSendRequest request) {
        smsVerificationService.sendCode(request.phone());
        long minutes = smsVerificationService.ttl().toMinutes();
        return ResponseEntity.ok(ApiResponse.ok("인증번호를 발송했습니다. " + minutes + "분 내에 입력해 주세요."));
    }

    /** 인증번호 확인 (일치 여부만 검증, 최종 소비는 회원가입 시점) */
    @PostMapping("/verify")
    public ResponseEntity<ApiResponse<Void>> verify(@Valid @RequestBody SmsVerifyRequest request) {
        boolean ok = smsVerificationService.matches(request.phone(), request.code());
        if (!ok) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.fail("인증번호가 올바르지 않거나 만료되었습니다."));
        }
        return ResponseEntity.ok(ApiResponse.ok("인증되었습니다."));
    }
}
