package com.example.demo.user.service;

/** 전화번호가 이미 가입되어 있을 때 발생 */
public class DuplicatePhoneException extends RuntimeException {
    public DuplicatePhoneException(String message) {
        super(message);
    }
}
