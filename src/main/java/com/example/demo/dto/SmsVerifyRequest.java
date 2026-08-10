package com.example.demo.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/** SMS 인증번호 확인 요청 */
public record SmsVerifyRequest(

        @NotBlank(message = "전화번호를 입력해 주세요.")
        @Pattern(regexp = "^01[016789]-?\\d{3,4}-?\\d{4}$", message = "전화번호 형식이 올바르지 않습니다.")
        String phone,

        @NotBlank(message = "인증번호를 입력해 주세요.")
        String code
) {
}
