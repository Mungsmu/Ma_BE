package com.example.demo.trip.service;

/** 본인 소유가 아닌 trip에 접근하려 할 때 */
public class TripAccessDeniedException extends RuntimeException {

    public TripAccessDeniedException(String message) {
        super(message);
    }
}
