package com.example.demo.auth.sms;

/**
 * SMS 발송 추상화.
 * 실제 운영에서는 CoolSMS / NHN Cloud / Twilio 등의 구현체로 교체한다.
 */
public interface SmsSender {

    void send(String phone, String message);
}
