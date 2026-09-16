package com.example.demo.route.service;

import com.example.demo.route.domain.Tunnel;
import com.example.demo.route.dto.Coordinate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/**
 * 경로가 터널 구간에 근접하는지 판단하는 기하 계산.
 * 위경도를 평면 미터 좌표로 근사 투영한 뒤, 경로의 각 구간(연속된 두 점을 잇는 선분)과
 * 터널 선분 사이의 최단 거리를 계산한다 — 경로의 점 하나하나가 아니라 "구간"으로 비교해야,
 * 카카오 API가 반환하는 좌표 간격이 넓은 직선 도로에서 터널을 관통하는 경우를 놓치지 않는다.
 */
@Component
public class TunnelAvoidanceChecker {

    private static final double METERS_PER_DEGREE_LAT = 111_320.0;

    /** 경로의 어느 구간이든 버퍼 거리(m) 이내로 터널 구간에 근접하면 true. */
    public boolean passesThroughTunnel(List<Coordinate> path, List<Tunnel> tunnels, double bufferMeters) {
        return findFirstIntersectedTunnel(path, tunnels, bufferMeters).isPresent();
    }

    /** 경로를 따라가면서 버퍼 거리 안으로 처음 근접하는 터널을 찾는다 (출발지 쪽에 가까운 순서). */
    public Optional<Tunnel> findFirstIntersectedTunnel(List<Coordinate> path, List<Tunnel> tunnels, double bufferMeters) {
        if (path.isEmpty() || tunnels.isEmpty()) {
            return Optional.empty();
        }
        for (int i = 0; i < path.size() - 1; i++) {
            Coordinate from = path.get(i);
            Coordinate to = path.get(i + 1);
            for (Tunnel tunnel : tunnels) {
                if (segmentDistanceMeters(from, to, tunnel) <= bufferMeters) {
                    return Optional.of(tunnel);
                }
            }
        }
        // 경로가 점 하나뿐인 경우(구간이 없음)를 대비한 보강 검사
        if (path.size() == 1) {
            for (Tunnel tunnel : tunnels) {
                if (segmentDistanceMeters(path.get(0), path.get(0), tunnel) <= bufferMeters) {
                    return Optional.of(tunnel);
                }
            }
        }
        return Optional.empty();
    }

    private double segmentDistanceMeters(Coordinate from, Coordinate to, Tunnel tunnel) {
        double refLat = Math.toRadians((from.lat() + to.lat() + tunnel.startLat() + tunnel.endLat()) / 4);
        double metersPerDegreeLng = METERS_PER_DEGREE_LAT * Math.cos(refLat);

        Point p1 = project(from, metersPerDegreeLng);
        Point p2 = project(to, metersPerDegreeLng);
        Point p3 = project(new Coordinate(tunnel.startLat(), tunnel.startLng()), metersPerDegreeLng);
        Point p4 = project(new Coordinate(tunnel.endLat(), tunnel.endLng()), metersPerDegreeLng);

        if (segmentsIntersect(p1, p2, p3, p4)) {
            return 0;
        }
        return Math.min(
                Math.min(pointToSegmentDistance(p1, p3, p4), pointToSegmentDistance(p2, p3, p4)),
                Math.min(pointToSegmentDistance(p3, p1, p2), pointToSegmentDistance(p4, p1, p2)));
    }

    private Point project(Coordinate c, double metersPerDegreeLng) {
        return new Point(c.lng() * metersPerDegreeLng, c.lat() * METERS_PER_DEGREE_LAT);
    }

    private boolean segmentsIntersect(Point p1, Point p2, Point p3, Point p4) {
        double d1 = cross(p3, p4, p1);
        double d2 = cross(p3, p4, p2);
        double d3 = cross(p1, p2, p3);
        double d4 = cross(p1, p2, p4);

        if (((d1 > 0 && d2 < 0) || (d1 < 0 && d2 > 0))
                && ((d3 > 0 && d4 < 0) || (d3 < 0 && d4 > 0))) {
            return true;
        }
        return (d1 == 0 && onSegment(p3, p4, p1))
                || (d2 == 0 && onSegment(p3, p4, p2))
                || (d3 == 0 && onSegment(p1, p2, p3))
                || (d4 == 0 && onSegment(p1, p2, p4));
    }

    private double cross(Point a, Point b, Point c) {
        return (b.x() - a.x()) * (c.y() - a.y()) - (b.y() - a.y()) * (c.x() - a.x());
    }

    private boolean onSegment(Point a, Point b, Point p) {
        return Math.min(a.x(), b.x()) <= p.x() && p.x() <= Math.max(a.x(), b.x())
                && Math.min(a.y(), b.y()) <= p.y() && p.y() <= Math.max(a.y(), b.y());
    }

    private double pointToSegmentDistance(Point p, Point a, Point b) {
        double abx = b.x() - a.x();
        double aby = b.y() - a.y();
        double lengthSq = abx * abx + aby * aby;

        double t = lengthSq == 0
                ? 0
                : Math.max(0, Math.min(1, ((p.x() - a.x()) * abx + (p.y() - a.y()) * aby) / lengthSq));

        double closestX = a.x() + t * abx;
        double closestY = a.y() + t * aby;
        double dx = p.x() - closestX;
        double dy = p.y() - closestY;
        return Math.sqrt(dx * dx + dy * dy);
    }

    private record Point(double x, double y) {
    }
}
