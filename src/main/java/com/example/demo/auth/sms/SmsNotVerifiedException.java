package com.example.demo.auth.sms;

/** SMS 인증이 완료되지 않았거나 인증번호가 틀렸을 때 발생 */
public class SmsNotVerifiedException extends RuntimeException {
    public SmsNotVerifiedException(String message) {
        super(message);
    }
}
