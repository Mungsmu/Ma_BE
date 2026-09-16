package com.example.demo.notification;

/** 실제 SMS 발송 수단을 감춘 인터페이스. 운영에서는 CoolSMS 등 실제 발송 연동체로 교체한다. */
public interface SmsSender {

    void send(String phone, String message);
}
