package com.example.demo.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

/**
 * 내 정보 수정 요청.
 * 아이디(username)/이메일은 의도적으로 필드에 포함하지 않는다 — 바꿀 수 없는 값이라
 * 요청 바디에 넣어도 무시된다.
 */
public record UpdateMemberRequest(

        @NotBlank(message = "이름을 입력해 주세요.")
        String name,

        @NotBlank(message = "전화번호를 입력해 주세요.")
        @Pattern(regexp = "^01[016789]-?\\d{3,4}-?\\d{4}$", message = "전화번호 형식이 올바르지 않습니다.")
        String phone,

        @NotBlank(message = "보호자 이름을 입력해 주세요.")
        String guardianName,

        @NotBlank(message = "보호자 전화번호를 입력해 주세요.")
        @Pattern(regexp = "^01[016789]-?\\d{3,4}-?\\d{4}$", message = "보호자 전화번호 형식이 올바르지 않습니다.")
        String guardianPhone,

        @NotBlank(message = "보호자와의 관계를 입력해 주세요.")
        String guardianRelation,

        @NotNull(message = "보호자 알림 사용 여부를 선택해 주세요.")
        Boolean guardianAlertEnabled
) {
}
