package com.example.demo.auth.service;

import com.example.demo.user.domain.Member;
import com.example.demo.user.repository.MemberRepository;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Spring Security 가 로그인 처리 시 호출하는 사용자 조회 서비스.
 * DB 에서 아이디로 회원을 찾아 UserDetails 로 변환해 반환한다.
 */
@Service
public class MemberUserDetailsService implements UserDetailsService {

    private final MemberRepository memberRepository;

    public MemberUserDetailsService(MemberRepository memberRepository) {
        this.memberRepository = memberRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        Member member = memberRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("존재하지 않는 아이디입니다: " + username));

        return User.builder()
                .username(member.getUsername())
                .password(member.getPassword())   // BCrypt 해시
                .roles("USER")
                .build();
    }
}
