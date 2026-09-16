package com.example.demo.trip.domain;

import com.example.demo.user.domain.Member;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

/**
 * 길안내 세션. 출발~도착까지의 경로 요약과, 실시간 위치 추적으로 갱신되는
 * "현재 터널 안에 있는지" 상태를 들고 있다 (터널 진입/통과 알림 전이 판단용).
 */
@Entity
@Table(name = "trip")
public class Trip {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @Column(nullable = false)
    private double originLat;
    @Column(nullable = false)
    private double originLng;
    @Column(nullable = false)
    private double destLat;
    @Column(nullable = false)
    private double destLng;

    @Column(nullable = false)
    private double distanceMeters;
    @Column(nullable = false)
    private double durationSeconds;
    @Column(nullable = false)
    private boolean tunnelAvoided;

    /** 가장 최근에 보고된 위치 (표시/기록용) */
    @Column(nullable = false)
    private double lastLat;
    @Column(nullable = false)
    private double lastLng;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TunnelState tunnelState;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TripStatus status;

    private LocalDateTime startedAt;
    private LocalDateTime endedAt;

    protected Trip() {
    }

    private Trip(Member member, double originLat, double originLng, double destLat, double destLng,
                 double distanceMeters, double durationSeconds, boolean tunnelAvoided) {
        this.member = member;
        this.originLat = originLat;
        this.originLng = originLng;
        this.destLat = destLat;
        this.destLng = destLng;
        this.distanceMeters = distanceMeters;
        this.durationSeconds = durationSeconds;
        this.tunnelAvoided = tunnelAvoided;
        this.lastLat = originLat;
        this.lastLng = originLng;
        this.tunnelState = TunnelState.NONE;
        this.status = TripStatus.ACTIVE;
        this.startedAt = LocalDateTime.now();
    }

    public static Trip start(Member member, double originLat, double originLng, double destLat, double destLng,
                              double distanceMeters, double durationSeconds, boolean tunnelAvoided) {
        return new Trip(member, originLat, originLng, destLat, destLng, distanceMeters, durationSeconds, tunnelAvoided);
    }

    public void updateLocation(double lat, double lng) {
        this.lastLat = lat;
        this.lastLng = lng;
    }

    public void changeTunnelState(TunnelState newState) {
        this.tunnelState = newState;
    }

    public void end() {
        this.status = TripStatus.ENDED;
        this.endedAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public Member getMember() {
        return member;
    }

    public double getOriginLat() {
        return originLat;
    }

    public double getOriginLng() {
        return originLng;
    }

    public double getDestLat() {
        return destLat;
    }

    public double getDestLng() {
        return destLng;
    }

    public double getDistanceMeters() {
        return distanceMeters;
    }

    public double getDurationSeconds() {
        return durationSeconds;
    }

    public boolean isTunnelAvoided() {
        return tunnelAvoided;
    }

    public double getLastLat() {
        return lastLat;
    }

    public double getLastLng() {
        return lastLng;
    }

    public TunnelState getTunnelState() {
        return tunnelState;
    }

    public TripStatus getStatus() {
        return status;
    }

    public LocalDateTime getStartedAt() {
        return startedAt;
    }

    public LocalDateTime getEndedAt() {
        return endedAt;
    }
}
