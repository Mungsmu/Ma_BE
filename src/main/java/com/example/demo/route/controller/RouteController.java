package com.example.demo.route.controller;

import com.example.demo.common.dto.ApiResponse;
import com.example.demo.route.dto.RouteResponse;
import com.example.demo.route.service.RouteService;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/routes")
@Validated
public class RouteController {

    private final RouteService routeService;

    public RouteController(RouteService routeService) {
        this.routeService = routeService;
    }

    /** 출발지-목적지 간 터널 회피 경로 탐색 */
    @GetMapping
    public ResponseEntity<ApiResponse<RouteResponse>> findRoute(
            @RequestParam @NotNull @DecimalMin("-90") @DecimalMax("90") Double originLat,
            @RequestParam @NotNull @DecimalMin("-180") @DecimalMax("180") Double originLng,
            @RequestParam @NotNull @DecimalMin("-90") @DecimalMax("90") Double destLat,
            @RequestParam @NotNull @DecimalMin("-180") @DecimalMax("180") Double destLng) {
        RouteResponse response = routeService.findRoute(originLat, originLng, destLat, destLng);
        String message = response.tunnelAvoided()
                ? "터널을 피한 경로를 찾았습니다."
                : "터널을 피할 수 없어 최단 경로를 안내합니다.";
        return ResponseEntity.ok(ApiResponse.ok(message, response));
    }
}
