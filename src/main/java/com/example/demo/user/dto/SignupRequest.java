package com.example.demo.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 회원가입 요청 DTO.
 * SMS 인증은 추후 추가 예정 — verificationCode 필드는 현재 미사용.
 */
public record SignupRequest(

        @NotBlank(message = "이름을 입력해 주세요.")
        String name,

        @NotBlank(message = "전화번호를 입력해 주세요.")
        @Pattern(regexp = "^01[016789]-?\\d{3,4}-?\\d{4}$", message = "전화번호 형식이 올바르지 않습니다.")
        String phone,

        @NotBlank(message = "이메일을 입력해 주세요.")
        @Email(message = "이메일 형식이 올바르지 않습니다.")
        String email,

        @NotBlank(message = "아이디를 입력해 주세요.")
        @Size(min = 4, max = 20, message = "아이디는 4~20자로 입력해 주세요.")
        @Pattern(regexp = "^[a-zA-Z0-9_]+$", message = "아이디는 영문/숫자/밑줄만 사용할 수 있습니다.")
        String username,

        @NotBlank(message = "비밀번호를 입력해 주세요.")
        @Pattern(
                regexp = "^(?=.*[A-Za-z])(?=.*\\d).{8,}$",
                message = "비밀번호는 영문과 숫자를 포함해 8자 이상이어야 합니다."
        )
        String password,

        @NotBlank(message = "비밀번호 확인을 입력해 주세요.")
        String passwordConfirm,

        @NotBlank(message = "보호자 이름을 입력해 주세요.")
        String guardianName,

        @NotBlank(message = "보호자 전화번호를 입력해 주세요.")
        @Pattern(regexp = "^01[016789]-?\\d{3,4}-?\\d{4}$", message = "보호자 전화번호 형식이 올바르지 않습니다.")
        String guardianPhone,

        @NotBlank(message = "보호자와의 관계를 입력해 주세요.")
        String guardianRelation
) {
}
