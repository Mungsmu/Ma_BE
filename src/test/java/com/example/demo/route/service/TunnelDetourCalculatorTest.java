package com.example.demo.route.service;

import com.example.demo.route.domain.Tunnel;
import com.example.demo.route.dto.Coordinate;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class TunnelDetourCalculatorTest {

    private final TunnelDetourCalculator calculator = new TunnelDetourCalculator();
    private final TunnelAvoidanceChecker checker = new TunnelAvoidanceChecker();

    // 광치터널 실좌표 기준
    private final Tunnel tunnel = new Tunnel("t1", "광치터널", 38.14672, 128.093106, 38.144841, 128.086935);

    @Test
    void 좌우_두_개의_경유지_후보를_반환하고_둘_다_터널에서_충분히_떨어져_있다() {
        List<Coordinate> waypoints = calculator.candidateWaypoints(tunnel);

        assertThat(waypoints).hasSize(2);
        for (Coordinate waypoint : waypoints) {
            boolean nearTunnel = checker.passesThroughTunnel(List.of(waypoint), List.of(tunnel), 30);
            assertThat(nearTunnel).isFalse();
        }
    }

    @Test
    void 좌우_후보는_서로_반대_방향으로_떨어져_있다() {
        List<Coordinate> waypoints = calculator.candidateWaypoints(tunnel);
        Coordinate side1 = waypoints.get(0);
        Coordinate side2 = waypoints.get(1);

        double midLat = (tunnel.startLat() + tunnel.endLat()) / 2;
        double midLng = (tunnel.startLng() + tunnel.endLng()) / 2;

        // 두 후보가 터널 중앙을 기준으로 서로 반대쪽에 있어야 한다 (부호가 반대)
        double side1LatDiff = side1.lat() - midLat;
        double side2LatDiff = side2.lat() - midLat;
        assertThat(side1LatDiff * side2LatDiff).isLessThan(0);
        assertThat(midLng).isNotEqualTo(0); // 참고용 — 실제 검증은 위 부호 비교
    }

    @Test
    void 시작_끝_좌표가_동일한_터널이어도_예외없이_후보를_만든다() {
        Tunnel degenerate = new Tunnel("t999", "가상고속도로터널", 37.5, 128.0, 37.5, 128.0);

        List<Coordinate> waypoints = calculator.candidateWaypoints(degenerate);

        assertThat(waypoints).hasSize(2);
        assertThat(waypoints.get(0)).isNotEqualTo(waypoints.get(1));
    }
}
