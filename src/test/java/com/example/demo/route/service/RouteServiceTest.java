package com.example.demo.route.service;

import com.example.demo.route.client.KakaoApiException;
import com.example.demo.route.client.KakaoDirectionsClient;
import com.example.demo.route.client.KakaoDirectionsClient.RouteCandidate;
import com.example.demo.route.domain.Tunnel;
import com.example.demo.route.dto.Coordinate;
import com.example.demo.route.dto.RouteResponse;
import com.example.demo.route.repository.TunnelRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RouteServiceTest {

    @Mock
    private KakaoDirectionsClient kakaoDirectionsClient;

    @Mock
    private TunnelRepository tunnelRepository;

    // 광치터널 실좌표 기준
    private final Tunnel tunnel = new Tunnel("t1", "광치터널", 38.14672, 128.093106, 38.144841, 128.086935);
    private final double bufferMeters = 30;

    private RouteService routeService;

    @BeforeEach
    void setUp() {
        routeService = new RouteService(kakaoDirectionsClient, tunnelRepository,
                new TunnelAvoidanceChecker(), new TunnelDetourCalculator(), bufferMeters);
    }

    @Test
    void 최단경로가_터널을_지나면_터널을_피하는_다음_경로를_선택한다() {
        RouteCandidate throughTunnel = new RouteCandidate(1000, 100,
                List.of(new Coordinate(38.14672, 128.093106)));
        RouteCandidate detour = new RouteCandidate(1500, 150,
                List.of(new Coordinate(38.300000, 128.300000)));
        when(kakaoDirectionsClient.findRoutes(1, 2, 3, 4)).thenReturn(List.of(throughTunnel, detour));
        when(tunnelRepository.findAll()).thenReturn(List.of(tunnel));

        RouteResponse response = routeService.findRoute(1, 2, 3, 4);

        assertThat(response.tunnelAvoided()).isTrue();
        assertThat(response.distanceMeters()).isEqualTo(1500);
    }

    @Test
    void 모든_후보가_터널을_지나면_최단경로를_그대로_반환하고_tunnelAvoided는_false다() {
        RouteCandidate a = new RouteCandidate(1000, 100,
                List.of(new Coordinate(38.14672, 128.093106)));
        RouteCandidate b = new RouteCandidate(1200, 120,
                List.of(new Coordinate(38.144841, 128.086935)));
        when(kakaoDirectionsClient.findRoutes(1, 2, 3, 4)).thenReturn(List.of(b, a));
        when(tunnelRepository.findAll()).thenReturn(List.of(tunnel));

        RouteResponse response = routeService.findRoute(1, 2, 3, 4);

        assertThat(response.tunnelAvoided()).isFalse();
        assertThat(response.distanceMeters()).isEqualTo(1000); // 그래도 최단경로
    }

    @Test
    void 터널_정보가_없으면_최단경로를_그대로_반환한다() {
        RouteCandidate onlyCandidate = new RouteCandidate(800, 80,
                List.of(new Coordinate(38.14672, 128.093106)));
        when(kakaoDirectionsClient.findRoutes(1, 2, 3, 4)).thenReturn(List.of(onlyCandidate));
        when(tunnelRepository.findAll()).thenReturn(List.of());

        RouteResponse response = routeService.findRoute(1, 2, 3, 4);

        assertThat(response.tunnelAvoided()).isTrue();
        assertThat(response.distanceMeters()).isEqualTo(800);
    }

    @Test
    void 카카오가_경로를_하나도_찾지_못하면_예외를_던진다() {
        when(kakaoDirectionsClient.findRoutes(1, 2, 3, 4)).thenReturn(List.of());

        assertThatThrownBy(() -> routeService.findRoute(1, 2, 3, 4))
                .isInstanceOf(RouteNotFoundException.class);
    }

    @Test
    void 유일한_경로가_터널을_지나도_경유지_우회로_피할_수_있으면_그걸_선택한다() {
        // 카카오가 대안 경로를 하나만 주는 실사용 케이스 (실제 연동 테스트에서 항상 이랬음)
        RouteCandidate onlyCandidate = new RouteCandidate(1000, 100,
                List.of(new Coordinate(38.14672, 128.093106), new Coordinate(38.144841, 128.086935)));
        when(kakaoDirectionsClient.findRoutes(1, 2, 3, 4)).thenReturn(List.of(onlyCandidate));
        when(tunnelRepository.findAll()).thenReturn(List.of(tunnel));

        RouteCandidate detourResult = new RouteCandidate(1800, 200,
                List.of(new Coordinate(38.300000, 128.300000)));
        when(kakaoDirectionsClient.findRoutes(eq(1.0), eq(2.0), eq(3.0), eq(4.0), any(Coordinate.class)))
                .thenReturn(List.of(detourResult));

        RouteResponse response = routeService.findRoute(1, 2, 3, 4);

        assertThat(response.tunnelAvoided()).isTrue();
        assertThat(response.distanceMeters()).isEqualTo(1800);
    }

    @Test
    void 경유지_우회_요청이_실패해도_최단경로로_안전하게_대체된다() {
        RouteCandidate onlyCandidate = new RouteCandidate(1000, 100,
                List.of(new Coordinate(38.14672, 128.093106), new Coordinate(38.144841, 128.086935)));
        when(kakaoDirectionsClient.findRoutes(1, 2, 3, 4)).thenReturn(List.of(onlyCandidate));
        when(tunnelRepository.findAll()).thenReturn(List.of(tunnel));
        when(kakaoDirectionsClient.findRoutes(eq(1.0), eq(2.0), eq(3.0), eq(4.0), any(Coordinate.class)))
                .thenThrow(new KakaoApiException("카카오 길찾기 API 호출에 실패했습니다.", new RuntimeException("boom")));

        RouteResponse response = routeService.findRoute(1, 2, 3, 4);

        assertThat(response.tunnelAvoided()).isFalse();
        assertThat(response.distanceMeters()).isEqualTo(1000);
    }
}
