package com.example.demo.notification;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/** 데모/개발용 SMS 발송 구현 — 실제로 문자를 보내지 않고 콘솔(로그)에 출력만 한다. */
@Component
public class ConsoleSmsSender implements SmsSender {

    private static final Logger log = LoggerFactory.getLogger(ConsoleSmsSender.class);

    @Override
    public void send(String phone, String message) {
        log.info("[SMS 발송(콘솔)] to={} message={}", phone, message);
    }
}
