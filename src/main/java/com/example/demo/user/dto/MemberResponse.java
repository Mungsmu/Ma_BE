package com.example.demo.user.dto;

import com.example.demo.user.domain.Member;

import java.time.LocalDateTime;

/** 내 정보 조회 응답 (비밀번호는 절대 포함하지 않는다) */
public record MemberResponse(
        Long id,
        String username,
        String name,
        String phone,
        String email,
        String guardianName,
        String guardianPhone,
        String guardianRelation,
        boolean guardianAlertEnabled,
        LocalDateTime createdAt
) {
    public static MemberResponse from(Member member) {
        return new MemberResponse(
                member.getId(),
                member.getUsername(),
                member.getName(),
                member.getPhone(),
                member.getEmail(),
                member.getGuardian().getName(),
                member.getGuardian().getPhone(),
                member.getGuardian().getRelation(),
                member.isGuardianAlertEnabled(),
                member.getCreatedAt()
        );
    }
}
