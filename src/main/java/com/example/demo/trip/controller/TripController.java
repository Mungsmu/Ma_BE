package com.example.demo.trip.controller;

import com.example.demo.common.dto.ApiResponse;
import com.example.demo.trip.dto.LocationUpdateRequest;
import com.example.demo.trip.dto.StartTripRequest;
import com.example.demo.trip.dto.TripResponse;
import com.example.demo.trip.service.TripService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/trips")
public class TripController {

    private final TripService tripService;

    public TripController(TripService tripService) {
        this.tripService = tripService;
    }

    /** 길안내 시작 — 경로를 탐색하고, 보호자에게 "안내 시작" SMS를 보낸다. */
    @PostMapping
    public ResponseEntity<ApiResponse<TripResponse>> start(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody StartTripRequest request) {
        TripResponse response = tripService.start(userDetails.getUsername(), request);
        return ResponseEntity.ok(ApiResponse.ok("길안내를 시작했습니다.", response));
    }

    /** 실시간 위치 보고 — 터널 진입/통과가 감지되면 보호자에게 SMS를 보낸다. */
    @PostMapping("/{tripId}/locations")
    public ResponseEntity<ApiResponse<TripResponse>> updateLocation(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long tripId,
            @Valid @RequestBody LocationUpdateRequest request) {
        TripResponse response = tripService.updateLocation(userDetails.getUsername(), tripId, request);
        return ResponseEntity.ok(ApiResponse.ok("위치가 갱신되었습니다.", response));
    }

    /** 길안내 종료 */
    @PostMapping("/{tripId}/end")
    public ResponseEntity<ApiResponse<TripResponse>> end(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long tripId) {
        TripResponse response = tripService.end(userDetails.getUsername(), tripId);
        return ResponseEntity.ok(ApiResponse.ok("길안내를 종료했습니다.", response));
    }
}
