package com.example.demo.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

/**
 * 보호자 정보.
 * 길찾기 시 이 전화번호로 SMS 알림을 발송한다.
 */
@Embeddable
public class Guardian {

    /** 보호자 이름 */
    @Column(name = "guardian_name", length = 50)
    private String name;

    /** 보호자 전화번호 (SMS 알림 수신) */
    @Column(name = "guardian_phone", length = 20)
    private String phone;

    /** 관계 (예: 부, 모, 자녀, 배우자 등) */
    @Column(name = "guardian_relation", length = 20)
    private String relation;

    protected Guardian() {
    }

    public Guardian(String name, String phone, String relation) {
        this.name = name;
        this.phone = phone;
        this.relation = relation;
    }

    public String getName() {
        return name;
    }

    public String getPhone() {
        return phone;
    }

    public String getRelation() {
        return relation;
    }
}
