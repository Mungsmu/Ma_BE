package com.example.demo.trip.dto;

import com.example.demo.trip.domain.Trip;
import com.example.demo.trip.domain.TripStatus;
import com.example.demo.trip.domain.TunnelState;

import java.time.LocalDateTime;

public record TripResponse(
        Long id,
        TripStatus status,
        TunnelState tunnelState,
        double distanceMeters,
        double durationSeconds,
        boolean tunnelAvoided,
        double lastLat,
        double lastLng,
        LocalDateTime startedAt,
        LocalDateTime endedAt
) {
    public static TripResponse from(Trip trip) {
        return new TripResponse(
                trip.getId(),
                trip.getStatus(),
                trip.getTunnelState(),
                trip.getDistanceMeters(),
                trip.getDurationSeconds(),
                trip.isTunnelAvoided(),
                trip.getLastLat(),
                trip.getLastLng(),
                trip.getStartedAt(),
                trip.getEndedAt()
        );
    }
}
