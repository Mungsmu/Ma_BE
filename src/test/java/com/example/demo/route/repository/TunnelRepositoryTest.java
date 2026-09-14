package com.example.demo.route.repository;

import com.example.demo.route.domain.Tunnel;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class TunnelRepositoryTest {

    @Test
    void CSV에서_터널_목록을_읽어온다() {
        TunnelRepository repository = new TunnelRepository();
        repository.load();

        List<Tunnel> tunnels = repository.findAll();

        assertThat(tunnels).isNotEmpty();
        Tunnel first = tunnels.get(0);
        assertThat(first.id()).isEqualTo("t1");
        assertThat(first.name()).isEqualTo("광치터널");
        assertThat(first.startLat()).isEqualTo(38.14672);
        assertThat(first.startLng()).isEqualTo(128.093106);
    }

    @Test
    void 컬럼이_부족한_줄은_건너뛰고_null을_반환한다() {
        TunnelRepository repository = new TunnelRepository();

        Tunnel result = repository.parseLine("t1,이름만있음", 2);

        assertThat(result).isNull();
    }

    @Test
    void 좌표가_숫자가_아닌_줄은_건너뛰고_null을_반환한다() {
        TunnelRepository repository = new TunnelRepository();

        Tunnel result = repository.parseLine("t1,이름,도로명,이상함,128.0,38.0,128.0", 2);

        assertThat(result).isNull();
    }
}
