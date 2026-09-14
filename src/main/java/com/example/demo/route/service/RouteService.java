package com.example.demo.route.service;

import com.example.demo.route.client.KakaoDirectionsClient;
import com.example.demo.route.client.KakaoDirectionsClient.RouteCandidate;
import com.example.demo.route.domain.Tunnel;
import com.example.demo.route.dto.RouteResponse;
import com.example.demo.route.repository.TunnelRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;

@Service
public class RouteService {

    private final KakaoDirectionsClient kakaoDirectionsClient;
    private final TunnelRepository tunnelRepository;
    private final TunnelAvoidanceChecker avoidanceChecker;
    private final double tunnelBufferMeters;

    public RouteService(KakaoDirectionsClient kakaoDirectionsClient,
                         TunnelRepository tunnelRepository,
                         TunnelAvoidanceChecker avoidanceChecker,
                         @Value("${app.route.tunnel-buffer-meters}") double tunnelBufferMeters) {
        this.kakaoDirectionsClient = kakaoDirectionsClient;
        this.tunnelRepository = tunnelRepository;
        this.avoidanceChecker = avoidanceChecker;
        this.tunnelBufferMeters = tunnelBufferMeters;
    }

    /** 카카오 대안 경로 후보 중 터널을 지나지 않는 최단 경로를 찾는다. 전부 터널을 지난다면 최단 경로를 그대로 반환한다. */
    public RouteResponse findRoute(double originLat, double originLng, double destLat, double destLng) {
        List<RouteCandidate> candidates = kakaoDirectionsClient.findRoutes(originLat, originLng, destLat, destLng);
        if (candidates.isEmpty()) {
            throw new RouteNotFoundException("경로를 찾을 수 없습니다.");
        }

        List<Tunnel> tunnels = tunnelRepository.findAll();
        List<RouteCandidate> sortedByDistance = candidates.stream()
                .sorted(Comparator.comparingDouble(RouteCandidate::distanceMeters))
                .toList();

        for (RouteCandidate candidate : sortedByDistance) {
            if (!avoidanceChecker.passesThroughTunnel(candidate.path(), tunnels, tunnelBufferMeters)) {
                return toResponse(candidate, true);
            }
        }

        return toResponse(sortedByDistance.get(0), false);
    }

    private RouteResponse toResponse(RouteCandidate candidate, boolean tunnelAvoided) {
        return new RouteResponse(candidate.distanceMeters(), candidate.durationSeconds(), tunnelAvoided, candidate.path());
    }
}
