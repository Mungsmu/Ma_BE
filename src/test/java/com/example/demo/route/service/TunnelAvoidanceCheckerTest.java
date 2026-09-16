package com.example.demo.route.service;

import com.example.demo.route.domain.Tunnel;
import com.example.demo.route.dto.Coordinate;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class TunnelAvoidanceCheckerTest {

    private final TunnelAvoidanceChecker checker = new TunnelAvoidanceChecker();

    // 광치터널 실좌표 기준 (start 38.14672,128.093106 / end 38.144841,128.086935)
    private final Tunnel tunnel = new Tunnel("t1", "광치터널", 38.14672, 128.093106, 38.144841, 128.086935);

    @Test
    void 경로가_터널_구간을_그대로_지나가면_true를_반환한다() {
        List<Coordinate> path = List.of(
                new Coordinate(38.150000, 128.100000),
                new Coordinate(38.14672, 128.093106), // 터널 시작점과 동일한 지점
                new Coordinate(38.140000, 128.080000)
        );

        boolean result = checker.passesThroughTunnel(path, List.of(tunnel), 30);

        assertThat(result).isTrue();
    }

    @Test
    void 경로가_터널에서_충분히_멀면_false를_반환한다() {
        List<Coordinate> path = List.of(
                new Coordinate(38.300000, 128.300000),
                new Coordinate(38.310000, 128.310000)
        );

        boolean result = checker.passesThroughTunnel(path, List.of(tunnel), 30);

        assertThat(result).isFalse();
    }

    @Test
    void 터널_목록이_비어있으면_항상_false를_반환한다() {
        List<Coordinate> path = List.of(new Coordinate(38.14672, 128.093106));

        boolean result = checker.passesThroughTunnel(path, List.of(), 30);

        assertThat(result).isFalse();
    }

    @Test
    void 버퍼_거리보다_먼_근접구간은_false를_반환한다() {
        // 터널 시작점에서 위도로 약 0.01도(~1.1km) 떨어진 지점 -> 버퍼(30m) 밖
        List<Coordinate> path = List.of(new Coordinate(38.15672, 128.093106));

        boolean result = checker.passesThroughTunnel(path, List.of(tunnel), 30);

        assertThat(result).isFalse();
    }

    @Test
    void 두_경로점은_각각_터널과_멀어도_그_사이_구간이_터널을_가로지르면_true를_반환한다() {
        // 동서 방향 터널(위도 37.0000 고정, 경도 128.0000~128.0100)을
        // 남북으로 가로지르는 경로 구간. 양 끝점은 터널까지 약 220m 이상 떨어져 있어
        // "점" 단위로만 검사하면 놓치지만, 두 점을 잇는 구간은 터널 중앙을 정확히 관통한다.
        Tunnel eastWestTunnel = new Tunnel("t99", "가상터널", 37.0000, 128.0000, 37.0000, 128.0100);
        List<Coordinate> crossingPath = List.of(
                new Coordinate(37.0020, 128.0050),
                new Coordinate(36.9980, 128.0050)
        );

        boolean result = checker.passesThroughTunnel(crossingPath, List.of(eastWestTunnel), 30);

        assertThat(result).isTrue();
    }

    @Test
    void 경로를_따라_처음_만나는_터널을_찾는다() {
        Tunnel farTunnel = new Tunnel("t2", "먼터널", 38.300000, 128.300000, 38.301000, 128.301000);
        List<Coordinate> path = List.of(
                new Coordinate(38.150000, 128.100000), // 출발
                new Coordinate(38.14672, 128.093106),  // 광치터널 근접 (먼저 만남)
                new Coordinate(38.300000, 128.300000)  // 먼터널 근접 (나중에 만남)
        );

        Optional<Tunnel> result = checker.findFirstIntersectedTunnel(path, List.of(farTunnel, tunnel), 30);

        assertThat(result).contains(tunnel);
    }

    @Test
    void 근접한_터널이_없으면_빈_Optional을_반환한다() {
        List<Coordinate> path = List.of(new Coordinate(38.300000, 128.300000));

        Optional<Tunnel> result = checker.findFirstIntersectedTunnel(path, List.of(tunnel), 30);

        assertThat(result).isEmpty();
    }
}
