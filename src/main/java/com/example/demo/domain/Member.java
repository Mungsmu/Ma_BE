package com.example.demo.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

/**
 * 회원 엔티티.
 * 아이디/비밀번호와 개인정보, 보호자 정보를 함께 보관한다.
 */
@Entity
@Table(name = "member")
public class Member {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 로그인 아이디 (중복 불가) */
    @Column(nullable = false, unique = true, length = 30)
    private String username;

    /** BCrypt 로 해시된 비밀번호 */
    @Column(nullable = false)
    private String password;

    /** 이름 */
    @Column(nullable = false, length = 50)
    private String name;

    /** 전화번호 */
    @Column(nullable = false, length = 20)
    private String phone;

    /** 이메일 */
    @Column(nullable = false, length = 100)
    private String email;

    /** 보호자 정보 (이름/전화번호/관계) */
    @Embedded
    private Guardian guardian;

    private LocalDateTime createdAt;

    protected Member() {
    }

    private Member(String username, String password, String name, String phone,
                   String email, Guardian guardian) {
        this.username = username;
        this.password = password;
        this.name = name;
        this.phone = phone;
        this.email = email;
        this.guardian = guardian;
        this.createdAt = LocalDateTime.now();
    }

    public static Member create(String username, String encodedPassword, String name,
                                String phone, String email, Guardian guardian) {
        return new Member(username, encodedPassword, name, phone, email, guardian);
    }

    public Long getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public String getName() {
        return name;
    }

    public String getPhone() {
        return phone;
    }

    public String getEmail() {
        return email;
    }

    public Guardian getGuardian() {
        return guardian;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
