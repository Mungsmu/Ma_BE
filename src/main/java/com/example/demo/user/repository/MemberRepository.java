package com.example.demo.user.repository;

import com.example.demo.user.domain.Member;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MemberRepository extends JpaRepository<Member, Long> {

    boolean existsByUsername(String username);

    Optional<Member> findByUsername(String username);

    boolean existsByPhone(String phone);

    Optional<Member> findByPhone(String phone);
}
