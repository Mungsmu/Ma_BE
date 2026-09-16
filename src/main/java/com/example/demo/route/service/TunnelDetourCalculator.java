package com.example.demo.route.service;

import com.example.demo.route.domain.Tunnel;
import com.example.demo.route.dto.Coordinate;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 카카오 길찾기 API는 "이 구간을 피해줘" 같은 회피 요청을 지원하지 않는다. 대신 터널 중앙에서
 * 터널 방향에 수직으로 떨어진 지점을 경유지(waypoint)로 강제 지정해서 우회를 유도한다 —
 * 실제로 그쪽에 대체 도로가 있는지는 알 수 없으므로, 좌우 양쪽 후보를 모두 만들어 둘 다
 * 시도해보고 실제로 터널을 피하는 결과만 채택하는 방식으로 쓴다 (RouteService 참고).
 */
@Component
public class TunnelDetourCalculator {

    private static final double METERS_PER_DEGREE_LAT = 111_320.0;
    private static final double MIN_OFFSET_METERS = 300;
    private static final double MAX_OFFSET_METERS = 1500;

    /** 터널 중앙 기준으로 좌/우 양쪽에 하나씩, 총 2개의 우회 경유지 후보를 반환한다. */
    public List<Coordinate> candidateWaypoints(Tunnel tunnel) {
        double midLat = (tunnel.startLat() + tunnel.endLat()) / 2;
        double midLng = (tunnel.startLng() + tunnel.endLng()) / 2;
        double metersPerDegreeLng = METERS_PER_DEGREE_LAT * Math.cos(Math.toRadians(midLat));

        double dx = (tunnel.endLng() - tunnel.startLng()) * metersPerDegreeLng;
        double dy = (tunnel.endLat() - tunnel.startLat()) * METERS_PER_DEGREE_LAT;
        double length = Math.sqrt(dx * dx + dy * dy);

        if (length == 0) {
            // 좌표 데이터가 시작/끝 동일점으로 기록된 터널(예: 일부 고속도로 터널) — 방향을 알 수
            // 없으니 임의로 동서 방향을 기준 삼아 수직(남북)으로 오프셋한다.
            dx = 1;
            dy = 0;
            length = 1;
        }

        double offsetMeters = Math.max(MIN_OFFSET_METERS, Math.min(length, MAX_OFFSET_METERS));
        // 터널 방향 벡터(dx,dy)를 90도 회전한 단위벡터가 수직 방향
        double ux = -dy / length;
        double uy = dx / length;

        double midX = midLng * metersPerDegreeLng;
        double midY = midLat * METERS_PER_DEGREE_LAT;

        Coordinate side1 = toCoordinate(midX + ux * offsetMeters, midY + uy * offsetMeters, metersPerDegreeLng);
        Coordinate side2 = toCoordinate(midX - ux * offsetMeters, midY - uy * offsetMeters, metersPerDegreeLng);
        return List.of(side1, side2);
    }

    private Coordinate toCoordinate(double x, double y, double metersPerDegreeLng) {
        return new Coordinate(y / METERS_PER_DEGREE_LAT, x / metersPerDegreeLng);
    }
}
