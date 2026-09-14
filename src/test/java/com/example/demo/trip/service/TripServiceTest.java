package com.example.demo.trip.service;

import com.example.demo.notification.GuardianNotificationService;
import com.example.demo.route.domain.Tunnel;
import com.example.demo.route.dto.Coordinate;
import com.example.demo.route.dto.RouteResponse;
import com.example.demo.route.repository.TunnelRepository;
import com.example.demo.route.service.RouteService;
import com.example.demo.route.service.TunnelAvoidanceChecker;
import com.example.demo.trip.domain.Trip;
import com.example.demo.trip.domain.TripStatus;
import com.example.demo.trip.domain.TunnelState;
import com.example.demo.trip.dto.LocationUpdateRequest;
import com.example.demo.trip.dto.StartTripRequest;
import com.example.demo.trip.dto.TripResponse;
import com.example.demo.trip.repository.TripRepository;
import com.example.demo.user.domain.Guardian;
import com.example.demo.user.domain.Member;
import com.example.demo.user.repository.MemberRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TripServiceTest {

    @Mock
    private TripRepository tripRepository;
    @Mock
    private MemberRepository memberRepository;
    @Mock
    private RouteService routeService;
    @Mock
    private TunnelRepository tunnelRepository;
    @Mock
    private GuardianNotificationService guardianNotificationService;

    private TripService tripService;

    // 광치터널 실좌표 기준
    private final Tunnel tunnel = new Tunnel("t1", "광치터널", 38.14672, 128.093106, 38.144841, 128.086935);

    @BeforeEach
    void setUp() {
        tripService = new TripService(tripRepository, memberRepository, routeService,
                tunnelRepository, new TunnelAvoidanceChecker(), guardianNotificationService, 30);
    }

    private Member newMember(String username) {
        Guardian guardian = new Guardian("보호자", "010-1234-5678", "부");
        return Member.create(username, "encoded-pw", "홍길동", "010-0000-0000", "a@b.com", guardian);
    }

    @Test
    void 길안내를_시작하면_경로를_탐색하고_보호자에게_알림을_보낸다() {
        Member member = newMember("user1");
        when(memberRepository.findByUsername("user1")).thenReturn(Optional.of(member));
        RouteResponse route = new RouteResponse(1000, 100, true, List.of(new Coordinate(38.0, 128.0)));
        when(routeService.findRoute(1, 2, 3, 4)).thenReturn(route);

        TripResponse response = tripService.start("user1", new StartTripRequest(1.0, 2.0, 3.0, 4.0));

        assertThat(response.status()).isEqualTo(TripStatus.ACTIVE);
        assertThat(response.tunnelState()).isEqualTo(TunnelState.NONE);
        assertThat(response.distanceMeters()).isEqualTo(1000);
        assertThat(response.tunnelAvoided()).isTrue();
        verify(guardianNotificationService).notifyRouteStart(member, true);
        verify(tripRepository).save(any(Trip.class));
    }

    @Test
    void 존재하지_않는_회원이면_예외를_던진다() {
        when(memberRepository.findByUsername("nope")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> tripService.start("nope", new StartTripRequest(1.0, 2.0, 3.0, 4.0)))
                .isInstanceOf(UsernameNotFoundException.class);
    }

    @Test
    void 위치가_터널에_근접하면_진입_알림을_보내고_상태가_바뀐다() {
        Member member = newMember("user1");
        Trip trip = Trip.start(member, 38.150000, 128.100000, 38.140000, 128.080000, 1000, 100, false);
        when(tripRepository.findById(1L)).thenReturn(Optional.of(trip));
        when(tunnelRepository.findAll()).thenReturn(List.of(tunnel));

        TripResponse response = tripService.updateLocation("user1", 1L,
                new LocationUpdateRequest(38.14672, 128.093106));

        assertThat(response.tunnelState()).isEqualTo(TunnelState.IN_TUNNEL);
        verify(guardianNotificationService).notifyTunnelEntered(member);
        verify(guardianNotificationService, never()).notifyTunnelPassed(any());
    }

    @Test
    void 터널_진입_후_멀어지면_통과_알림을_보내고_상태가_되돌아온다() {
        Member member = newMember("user1");
        Trip trip = Trip.start(member, 38.14672, 128.093106, 38.140000, 128.080000, 1000, 100, false);
        // 이미 터널 안에 있는 상태로 세팅
        when(tunnelRepository.findAll()).thenReturn(List.of(tunnel));
        when(tripRepository.findById(1L)).thenReturn(Optional.of(trip));
        tripService.updateLocation("user1", 1L, new LocationUpdateRequest(38.144841, 128.086935)); // 진입 유지

        TripResponse response = tripService.updateLocation("user1", 1L,
                new LocationUpdateRequest(38.300000, 128.300000)); // 충분히 멀어짐

        assertThat(response.tunnelState()).isEqualTo(TunnelState.NONE);
        verify(guardianNotificationService).notifyTunnelPassed(member);
    }

    @Test
    void 터널과_계속_먼_상태면_알림을_보내지_않는다() {
        Member member = newMember("user1");
        Trip trip = Trip.start(member, 38.300000, 128.300000, 38.310000, 128.310000, 1000, 100, true);
        when(tripRepository.findById(1L)).thenReturn(Optional.of(trip));
        when(tunnelRepository.findAll()).thenReturn(List.of(tunnel));

        tripService.updateLocation("user1", 1L, new LocationUpdateRequest(38.305000, 128.305000));

        verify(guardianNotificationService, never()).notifyTunnelEntered(any());
        verify(guardianNotificationService, never()).notifyTunnelPassed(any());
    }

    @Test
    void 다른_사람의_trip에_접근하면_예외를_던진다() {
        Member owner = newMember("owner");
        Trip trip = Trip.start(owner, 38.0, 128.0, 38.1, 128.1, 1000, 100, true);
        when(tripRepository.findById(1L)).thenReturn(Optional.of(trip));

        assertThatThrownBy(() -> tripService.updateLocation("intruder", 1L, new LocationUpdateRequest(38.0, 128.0)))
                .isInstanceOf(TripAccessDeniedException.class);
    }

    @Test
    void 종료된_trip에_위치를_보고하면_예외를_던진다() {
        Member member = newMember("user1");
        Trip trip = Trip.start(member, 38.0, 128.0, 38.1, 128.1, 1000, 100, true);
        trip.end();
        when(tripRepository.findById(1L)).thenReturn(Optional.of(trip));

        assertThatThrownBy(() -> tripService.updateLocation("user1", 1L, new LocationUpdateRequest(38.0, 128.0)))
                .isInstanceOf(TripNotActiveException.class);
    }

    @Test
    void trip을_종료하면_상태가_ENDED로_바뀐다() {
        Member member = newMember("user1");
        Trip trip = Trip.start(member, 38.0, 128.0, 38.1, 128.1, 1000, 100, true);
        when(tripRepository.findById(1L)).thenReturn(Optional.of(trip));

        TripResponse response = tripService.end("user1", 1L);

        assertThat(response.status()).isEqualTo(TripStatus.ENDED);
        assertThat(response.endedAt()).isNotNull();
    }

    @Test
    void 존재하지_않는_trip이면_예외를_던진다() {
        when(tripRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> tripService.end("user1", 999L))
                .isInstanceOf(TripNotFoundException.class);
    }
}
