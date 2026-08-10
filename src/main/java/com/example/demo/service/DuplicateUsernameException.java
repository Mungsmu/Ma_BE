package com.example.demo.service;

/** 아이디가 이미 존재할 때 발생 */
public class DuplicateUsernameException extends RuntimeException {
    public DuplicateUsernameException(String message) {
        super(message);
    }
}
