package com.example.demo.trip.service;

import com.example.demo.notification.GuardianNotificationService;
import com.example.demo.route.dto.Coordinate;
import com.example.demo.route.dto.RouteResponse;
import com.example.demo.route.domain.Tunnel;
import com.example.demo.route.repository.TunnelRepository;
import com.example.demo.route.service.RouteService;
import com.example.demo.route.service.TunnelAvoidanceChecker;
import com.example.demo.trip.domain.Trip;
import com.example.demo.trip.domain.TripStatus;
import com.example.demo.trip.domain.TunnelState;
import com.example.demo.trip.dto.StartTripRequest;
import com.example.demo.trip.dto.LocationUpdateRequest;
import com.example.demo.trip.dto.TripResponse;
import com.example.demo.trip.repository.TripRepository;
import com.example.demo.user.domain.Member;
import com.example.demo.user.repository.MemberRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class TripService {

    private final TripRepository tripRepository;
    private final MemberRepository memberRepository;
    private final RouteService routeService;
    private final TunnelRepository tunnelRepository;
    private final TunnelAvoidanceChecker avoidanceChecker;
    private final GuardianNotificationService guardianNotificationService;
    private final double tunnelBufferMeters;

    public TripService(TripRepository tripRepository,
                        MemberRepository memberRepository,
                        RouteService routeService,
                        TunnelRepository tunnelRepository,
                        TunnelAvoidanceChecker avoidanceChecker,
                        GuardianNotificationService guardianNotificationService,
                        @Value("${app.route.tunnel-buffer-meters}") double tunnelBufferMeters) {
        this.tripRepository = tripRepository;
        this.memberRepository = memberRepository;
        this.routeService = routeService;
        this.tunnelRepository = tunnelRepository;
        this.avoidanceChecker = avoidanceChecker;
        this.guardianNotificationService = guardianNotificationService;
        this.tunnelBufferMeters = tunnelBufferMeters;
    }

    /** 길안내를 시작한다 — 경로를 탐색해 trip을 만들고, 보호자에게 "안내 시작" 알림을 보낸다. */
    @Transactional
    public TripResponse start(String username, StartTripRequest request) {
        Member member = findMember(username);
        RouteResponse route = routeService.findRoute(
                request.originLat(), request.originLng(), request.destLat(), request.destLng());

        Trip trip = Trip.start(member, request.originLat(), request.originLng(),
                request.destLat(), request.destLng(),
                route.distanceMeters(), route.durationSeconds(), route.tunnelAvoided());
        tripRepository.save(trip);

        guardianNotificationService.notifyRouteStart(member, route.tunnelAvoided());
        return TripResponse.from(trip);
    }

    /**
     * 새 위치를 보고한다. 새 위치가 터널 버퍼 안으로 들어오면 "진입", 버퍼 밖으로 나가면
     * "통과" 알림을 보호자에게 보낸다 (상태가 실제로 바뀔 때만).
     * 판정은 직전 위치를 포함한 "구간"이 아니라 새 위치 "점" 하나만으로 한다 — 직전 위치를
     * 구간에 포함시키면, 막 진입으로 기록된 그 직전 위치 자체가 터널에 가깝기 때문에 실제로는
     * 충분히 멀어진 다음 위치 보고에서도 계속 "근접"으로 판정되어 통과 알림이 한 틱 늦게 나간다.
     * (실시간 위치 보고는 짧은 주기로 들어온다고 가정 — 한 번의 보고 간격 안에서 터널을
     * 그대로 지나쳐버리는 경우까지는 다루지 않는다.)
     */
    @Transactional
    public TripResponse updateLocation(String username, Long tripId, LocationUpdateRequest request) {
        Trip trip = findOwnedActiveTrip(username, tripId);

        List<Coordinate> currentPoint = List.of(new Coordinate(request.lat(), request.lng()));
        List<Tunnel> tunnels = tunnelRepository.findAll();
        boolean nearTunnel = avoidanceChecker.passesThroughTunnel(currentPoint, tunnels, tunnelBufferMeters);

        if (nearTunnel && trip.getTunnelState() == TunnelState.NONE) {
            trip.changeTunnelState(TunnelState.IN_TUNNEL);
            guardianNotificationService.notifyTunnelEntered(trip.getMember());
        } else if (!nearTunnel && trip.getTunnelState() == TunnelState.IN_TUNNEL) {
            trip.changeTunnelState(TunnelState.NONE);
            guardianNotificationService.notifyTunnelPassed(trip.getMember());
        }

        trip.updateLocation(request.lat(), request.lng());
        return TripResponse.from(trip);
    }

    @Transactional
    public TripResponse end(String username, Long tripId) {
        Trip trip = findOwnedActiveTrip(username, tripId);
        trip.end();
        return TripResponse.from(trip);
    }

    private Trip findOwnedActiveTrip(String username, Long tripId) {
        Trip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new TripNotFoundException("존재하지 않는 길안내입니다: " + tripId));
        if (!trip.getMember().getUsername().equals(username)) {
            throw new TripAccessDeniedException("본인의 길안내만 조회/조작할 수 있습니다.");
        }
        if (trip.getStatus() != TripStatus.ACTIVE) {
            throw new TripNotActiveException("이미 종료된 길안내입니다: " + tripId);
        }
        return trip;
    }

    private Member findMember(String username) {
        return memberRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("존재하지 않는 아이디입니다: " + username));
    }
}
