package com.example.demo.notification;

import com.example.demo.user.domain.Guardian;
import com.example.demo.user.domain.Member;
import org.springframework.stereotype.Service;

/** 길안내 시작/터널 진입·통과 시점에 보호자에게 SMS 알림을 보낸다 (마이페이지 토글로 켜고 끌 수 있음). */
@Service
public class GuardianNotificationService {

    private final SmsSender smsSender;

    public GuardianNotificationService(SmsSender smsSender) {
        this.smsSender = smsSender;
    }

    public void notifyRouteStart(Member member, boolean tunnelAvoided) {
        String routeNote = tunnelAvoided ? "터널을 피한 경로" : "터널이 포함된 경로(회피 불가)";
        send(member, member.getName() + "님이 길 안내를 시작했습니다. (" + routeNote + ")");
    }

    public void notifyTunnelEntered(Member member) {
        send(member, member.getName() + "님이 터널 구간에 진입했습니다.");
    }

    public void notifyTunnelPassed(Member member) {
        send(member, member.getName() + "님이 터널 구간을 통과했습니다.");
    }

    private void send(Member member, String message) {
        if (!member.isGuardianAlertEnabled()) {
            return;
        }
        Guardian guardian = member.getGuardian();
        if (guardian == null || guardian.getPhone() == null || guardian.getPhone().isBlank()) {
            return;
        }
        smsSender.send(guardian.getPhone(), message);
    }
}
