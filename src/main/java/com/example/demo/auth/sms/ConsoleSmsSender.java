package com.example.demo.auth.sms;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * 개발/데모용 SMS 발송 구현체.
 * 실제로 문자를 보내지 않고 콘솔 로그로 대체한다.
 */
@Component
public class ConsoleSmsSender implements SmsSender {

    private static final Logger log = LoggerFactory.getLogger(ConsoleSmsSender.class);

    @Override
    public void send(String phone, String message) {
        log.info("[SMS 발송] to={} | message={}", phone, message);
    }
}
