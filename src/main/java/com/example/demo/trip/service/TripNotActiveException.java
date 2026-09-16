package com.example.demo.trip.service;

/** 이미 종료된 trip에 위치 보고/종료를 다시 시도할 때 */
public class TripNotActiveException extends RuntimeException {

    public TripNotActiveException(String message) {
        super(message);
    }
}
