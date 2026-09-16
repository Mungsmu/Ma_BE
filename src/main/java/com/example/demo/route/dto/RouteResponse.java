package com.example.demo.route.dto;

import java.util.List;

public record RouteResponse(
        double distanceMeters,
        double durationSeconds,
        boolean tunnelAvoided,
        List<Coordinate> path
) {
}
