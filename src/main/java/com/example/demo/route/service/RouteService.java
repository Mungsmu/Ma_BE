package com.example.demo.route.service;

import com.example.demo.route.client.KakaoApiException;
import com.example.demo.route.client.KakaoDirectionsClient;
import com.example.demo.route.client.KakaoDirectionsClient.RouteCandidate;
import com.example.demo.route.domain.Tunnel;
import com.example.demo.route.dto.Coordinate;
import com.example.demo.route.dto.RouteResponse;
import com.example.demo.route.repository.TunnelRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Service
public class RouteService {

    private static final Logger log = LoggerFactory.getLogger(RouteService.class);

    private final KakaoDirectionsClient kakaoDirectionsClient;
    private final TunnelRepository tunnelRepository;
    private final TunnelAvoidanceChecker avoidanceChecker;
    private final TunnelDetourCalculator detourCalculator;
    private final double tunnelBufferMeters;

    public RouteService(KakaoDirectionsClient kakaoDirectionsClient,
                         TunnelRepository tunnelRepository,
                         TunnelAvoidanceChecker avoidanceChecker,
                         TunnelDetourCalculator detourCalculator,
                         @Value("${app.route.tunnel-buffer-meters}") double tunnelBufferMeters) {
        this.kakaoDirectionsClient = kakaoDirectionsClient;
        this.tunnelRepository = tunnelRepository;
        this.avoidanceChecker = avoidanceChecker;
        this.detourCalculator = detourCalculator;
        this.tunnelBufferMeters = tunnelBufferMeters;
    }

    /**
     * 카카오 대안 경로 후보 중 터널을 지나지 않는 최단 경로를 찾는다.
     * 대안 경로가 전부(또는 유일하게 하나뿐인 경로가) 터널을 지난다면, 그 경로가 처음 만나는
     * 터널을 우회하는 경유지를 강제 지정해 한 번 더 요청해본다 — 카카오 API 자체는 "이 구간을
     * 피해줘" 같은 회피 요청을 지원하지 않기 때문에 쓰는 우회책이다 (TunnelDetourCalculator 참고).
     * 그래도 터널 없는 경로를 못 찾으면 최단 경로를 그대로 반환한다(tunnelAvoided=false).
     */
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

        RouteCandidate shortest = sortedByDistance.get(0);
        Optional<RouteCandidate> detoured = tryDetourAroundTunnel(
                originLat, originLng, destLat, destLng, shortest, tunnels);
        if (detoured.isPresent()) {
            return toResponse(detoured.get(), true);
        }

        return toResponse(shortest, false);
    }

    private Optional<RouteCandidate> tryDetourAroundTunnel(double originLat, double originLng,
                                                            double destLat, double destLng,
                                                            RouteCandidate blockedRoute, List<Tunnel> tunnels) {
        Optional<Tunnel> blockingTunnel =
                avoidanceChecker.findFirstIntersectedTunnel(blockedRoute.path(), tunnels, tunnelBufferMeters);
        if (blockingTunnel.isEmpty()) {
            return Optional.empty();
        }

        RouteCandidate best = null;
        for (Coordinate waypoint : detourCalculator.candidateWaypoints(blockingTunnel.get())) {
            List<RouteCandidate> detourCandidates;
            try {
                detourCandidates = kakaoDirectionsClient.findRoutes(originLat, originLng, destLat, destLng, waypoint);
            } catch (KakaoApiException e) {
                log.warn("터널 우회 경유지 요청 실패 — 이 경유지는 건너뜀: {}", waypoint, e);
                continue;
            }
            for (RouteCandidate candidate : detourCandidates) {
                if (avoidanceChecker.passesThroughTunnel(candidate.path(), tunnels, tunnelBufferMeters)) {
                    continue;
                }
                if (best == null || candidate.distanceMeters() < best.distanceMeters()) {
                    best = candidate;
                }
            }
        }
        return Optional.ofNullable(best);
    }

    private RouteResponse toResponse(RouteCandidate candidate, boolean tunnelAvoided) {
        return new RouteResponse(candidate.distanceMeters(), candidate.durationSeconds(), tunnelAvoided, candidate.path());
    }
}
