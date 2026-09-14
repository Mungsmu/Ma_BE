package com.example.demo.route.repository;

import com.example.demo.route.domain.Tunnel;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Repository;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/** 강원도 도로 터널 정보를 CSV에서 읽어 메모리에 올려둔다 (id,name,road,startLat,startLng,endLat,endLng,...). */
@Repository
public class TunnelRepository {

    private static final Logger log = LoggerFactory.getLogger(TunnelRepository.class);
    private static final String CSV_PATH = "강원도_도로터널정보_정리.csv";
    private static final int MIN_COLUMNS = 7;

    private List<Tunnel> tunnels = List.of();

    @PostConstruct
    void load() {
        List<Tunnel> loaded = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                new ClassPathResource(CSV_PATH).getInputStream(), StandardCharsets.UTF_8))) {
            reader.readLine(); // header
            String line;
            int lineNumber = 1;
            while ((line = reader.readLine()) != null) {
                lineNumber++;
                if (line.isBlank()) {
                    continue;
                }
                Tunnel tunnel = parseLine(line, lineNumber);
                if (tunnel != null) {
                    loaded.add(tunnel);
                }
            }
        } catch (IOException e) {
            throw new IllegalStateException("터널 정보 CSV를 읽을 수 없습니다: " + CSV_PATH, e);
        }
        this.tunnels = List.copyOf(loaded);
        log.info("터널 정보 {}건 로드 완료 ({})", tunnels.size(), CSV_PATH);
    }

    Tunnel parseLine(String line, int lineNumber) {
        String[] c = line.split(",");
        if (c.length < MIN_COLUMNS) {
            log.warn("터널 CSV {}번째 줄 컬럼 수 부족({}개) — 건너뜀: {}", lineNumber, c.length, line);
            return null;
        }
        try {
            return new Tunnel(
                    c[0].trim(),
                    c[1].trim(),
                    Double.parseDouble(c[3].trim()),
                    Double.parseDouble(c[4].trim()),
                    Double.parseDouble(c[5].trim()),
                    Double.parseDouble(c[6].trim()));
        } catch (NumberFormatException e) {
            log.warn("터널 CSV {}번째 줄 좌표 파싱 실패 — 건너뜀: {}", lineNumber, line);
            return null;
        }
    }

    public List<Tunnel> findAll() {
        return tunnels;
    }
}
