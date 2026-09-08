package com.example.demo.auth.sms;

import com.example.demo.auth.sms.dto.SmsSendRequest;
import com.example.demo.common.dto.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/sms")
public class SmsController {

    private static final String GUIDE_MESSAGE_FORMAT = "옥토모 대표번호(1666-3538)로 아래 코드를 %d분 내에 문자로 보내주세요.";

    private final SmsVerificationService smsVerificationService;

    public SmsController(SmsVerificationService smsVerificationService) {
        this.smsVerificationService = smsVerificationService;
    }

    /** 본인 전화번호 인증코드 발급 (공개, 회원가입 전 사용) */
    @PostMapping("/send")
    public ResponseEntity<ApiResponse<Map<String, Object>>> send(@Valid @RequestBody SmsSendRequest request) {
        String code = smsVerificationService.issueCode(request.phone(), SmsVerificationService.Purpose.USER);
        String message = String.format(GUIDE_MESSAGE_FORMAT, smsVerificationService.ttl().toMinutes());
        // text: 옥토모로 실제 보내야 하는 문자열(프리픽스 포함) — 매칭 조회는 이 값 그대로와 비교한다.
        return ResponseEntity.ok(ApiResponse.ok(message,
                Map.of("code", code, "text", SmsVerificationService.Purpose.USER.buildText(code))));
    }

    /** 본인 전화번호 인증 확인 (공개, 최종 소비는 회원가입 시점에 별도 처리) */
    @PostMapping("/verify")
    public ResponseEntity<ApiResponse<Void>> verify(@Valid @RequestBody SmsSendRequest request) {
        boolean ok = smsVerificationService.matches(request.phone(), SmsVerificationService.Purpose.USER);
        if (!ok) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.fail("인증 문자를 확인하지 못했습니다. 번호와 발송 여부를 다시 확인해 주세요."));
        }
        return ResponseEntity.ok(ApiResponse.ok("인증되었습니다."));
    }

    /** 보호자 전화번호 인증코드 발급 (로그인 필요 — 본인 인증을 마친 회원만) */
    @PostMapping("/guardian/send")
    public ResponseEntity<ApiResponse<Map<String, Object>>> guardianSend(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody SmsSendRequest request) {
        String code = smsVerificationService.issueCode(request.phone(), SmsVerificationService.Purpose.GUARDIAN);
        String message = String.format(GUIDE_MESSAGE_FORMAT, smsVerificationService.ttl().toMinutes());
        return ResponseEntity.ok(ApiResponse.ok(message,
                Map.of("code", code, "text", SmsVerificationService.Purpose.GUARDIAN.buildText(code))));
    }

    /** 보호자 전화번호 인증 확인 (로그인 필요, 성공 시 최종 소비) */
    @PostMapping("/guardian/verify")
    public ResponseEntity<ApiResponse<Void>> guardianVerify(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody SmsSendRequest request) {
        boolean ok = smsVerificationService.verifyAndConsume(request.phone(), SmsVerificationService.Purpose.GUARDIAN);
        if (!ok) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.fail("인증 문자를 확인하지 못했습니다. 번호와 발송 여부를 다시 확인해 주세요."));
        }
        return ResponseEntity.ok(ApiResponse.ok("보호자 인증이 완료되었습니다."));
    }

    /** 발급된 보호자 인증코드를 담은 SMS QR 발급 (로그인 필요, guardian/send로 먼저 코드 발급 필요) */
    @PostMapping("/guardian/qr")
    public ResponseEntity<ApiResponse<Map<String, String>>> guardianQr(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody SmsSendRequest request) {
        String qrCode = smsVerificationService.issueGuardianQr(request.phone());
        return ResponseEntity.ok(ApiResponse.ok("QR을 스캔해 문자를 보내면 인증됩니다.", Map.of("qrCode", qrCode)));
    }
}
