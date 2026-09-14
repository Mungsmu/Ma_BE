package com.example.demo.route.client;

import com.example.demo.route.dto.Coordinate;
import tools.jackson.databind.JsonNode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.ArrayList;
import java.util.List;

/** 카카오 모빌리티 길찾기(Directions) API — 대안 경로 후보들을 가져온다. */
@Component
public class KakaoDirectionsClient {

    private final RestClient restClient;
    private final String apiKey;

    public KakaoDirectionsClient(
            @Value("${app.kakao.directions-base-url}") String baseUrl,
            @Value("${app.kakao.api-key}") String apiKey) {
        this.restClient = RestClient.builder().baseUrl(baseUrl).build();
        this.apiKey = apiKey;
    }

    public List<RouteCandidate> findRoutes(double originLat, double originLng, double destLat, double destLng) {
        JsonNode body;
        try {
            body = restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .queryParam("origin", originLng + "," + originLat)
                            .queryParam("destination", destLng + "," + destLat)
                            .queryParam("alternatives", true)
                            .build())
                    .header("Authorization", "KakaoAK " + apiKey)
                    .retrieve()
                    .body(JsonNode.class);
        } catch (RestClientException e) {
            throw new KakaoApiException("카카오 길찾기 API 호출에 실패했습니다.", e);
        }

        List<RouteCandidate> candidates = new ArrayList<>();
        if (body == null) {
            return candidates;
        }
        for (JsonNode route : body.path("routes")) {
            if (route.path("result_code").asInt(-1) != 0) {
                continue; // 탐색 실패한 후보는 제외
            }
            JsonNode summary = route.path("summary");
            double distance = summary.path("distance").asDouble();
            double duration = summary.path("duration").asDouble();
            candidates.add(new RouteCandidate(distance, duration, extractPath(route)));
        }
        return candidates;
    }

    private List<Coordinate> extractPath(JsonNode route) {
        List<Coordinate> path = new ArrayList<>();
        for (JsonNode section : route.path("sections")) {
            for (JsonNode road : section.path("roads")) {
                JsonNode vertexes = road.path("vertexes");
                for (int i = 0; i + 1 < vertexes.size(); i += 2) {
                    path.add(new Coordinate(vertexes.get(i + 1).asDouble(), vertexes.get(i).asDouble()));
                }
            }
        }
        return path;
    }

    public record RouteCandidate(double distanceMeters, double durationSeconds, List<Coordinate> path) {
    }
}
