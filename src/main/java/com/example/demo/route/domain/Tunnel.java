package com.example.demo.route.domain;

public record Tunnel(
        String id,
        String name,
        double startLat,
        double startLng,
        double endLat,
        double endLng
) {
}
