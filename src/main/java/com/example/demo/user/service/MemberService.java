package com.example.demo.user.service;

import com.example.demo.user.domain.Guardian;
import com.example.demo.user.domain.Member;
import com.example.demo.user.dto.MemberResponse;
import com.example.demo.user.dto.SignupRequest;
import com.example.demo.user.dto.UpdateMemberRequest;
import com.example.demo.user.repository.MemberRepository;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MemberService {

    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;

    public MemberService(MemberRepository memberRepository, PasswordEncoder passwordEncoder) {
        this.memberRepository = memberRepository;
        this.passwordEncoder = passwordEncoder;
    }

    /** 아이디 사용 가능 여부 (true = 사용 가능) */
    @Transactional(readOnly = true)
    public boolean isUsernameAvailable(String username) {
        return !memberRepository.existsByUsername(username);
    }

    /**
     * 회원가입 처리.
     * 1) 아이디 중복 확인
     * 2) 비밀번호 해시 후 저장
     * (SMS 인증은 추후 추가 예정)
     */
    @Transactional
    public Long signup(SignupRequest request) {
        if (!request.password().equals(request.passwordConfirm())) {
            throw new IllegalArgumentException("비밀번호가 일치하지 않습니다.");
        }

        if (memberRepository.existsByUsername(request.username())) {
            throw new DuplicateUsernameException("이미 사용 중인 아이디입니다.");
        }

        if (memberRepository.existsByPhone(request.phone())) {
            throw new DuplicatePhoneException("이미 가입된 전화번호입니다.");
        }

        Guardian guardian = new Guardian(
                request.guardianName(),
                request.guardianPhone(),
                request.guardianRelation()
        );

        Member member = Member.create(
                request.username(),
                passwordEncoder.encode(request.password()),
                request.name(),
                request.phone(),
                request.email(),
                guardian
        );

        return memberRepository.save(member).getId();
    }

    /** 내 정보 조회 */
    @Transactional(readOnly = true)
    public MemberResponse getMyInfo(String username) {
        return MemberResponse.from(findByUsername(username));
    }

    /** 내 정보 수정. 아이디/비밀번호/이메일은 대상 아님 (요청 DTO 자체에 필드 없음). */
    @Transactional
    public MemberResponse updateMyInfo(String username, UpdateMemberRequest request) {
        Member member = findByUsername(username);
        Guardian guardian = new Guardian(request.guardianName(), request.guardianPhone(), request.guardianRelation());
        member.updateProfile(request.name(), request.phone(), guardian, request.guardianAlertEnabled());
        return MemberResponse.from(member);
    }

    private Member findByUsername(String username) {
        return memberRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("존재하지 않는 아이디입니다: " + username));
    }
}
