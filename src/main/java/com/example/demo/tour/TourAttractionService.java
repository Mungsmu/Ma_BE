package com.example.demo.tour;

import tools.jackson.databind.JsonNode;
import com.example.demo.tour.dto.TourAttraction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** 강원도 18개 시군구별 추천 관광지를 TourAPI에서 실시간으로 모아 그룹핑한다. */
@Service
public class TourAttractionService {

    private static final Logger log = LoggerFactory.getLogger(TourAttractionService.class);

    private final TourApiClient client;
    private final long cacheTtlSeconds;
    // ponytail: 시군구별 결과를 짧은 TTL로만 재사용하는 단일 맵 — 정적 데이터 대체가 아니라
    // 매 요청마다 18회 API 호출이 나가는 걸 막기 위한 부하 완화용. 인스턴스 재시작 시 소실돼도 무방.
    private final ConcurrentHashMap<String, CacheEntry> cache = new ConcurrentHashMap<>();

    public TourAttractionService(TourApiClient client,
                                  @Value("${app.tour.cache-ttl-seconds:300}") long cacheTtlSeconds) {
        this.client = client;
        this.cacheTtlSeconds = cacheTtlSeconds;
    }

    /** 시군구명 → 추천 관광지 목록. 개별 시군구 조회가 실패해도 나머지는 정상 응답한다. */
    public Map<String, List<TourAttraction>> recommendationsByRegion(int numOfRows) {
        Map<String, List<TourAttraction>> result = new LinkedHashMap<>();
        for (Sigungu sigungu : Sigungu.values()) {
            result.put(sigungu.displayName(), fetch(sigungu, numOfRows));
        }
        return result;
    }

    private List<TourAttraction> fetch(Sigungu sigungu, int numOfRows) {
        String key = sigungu.code() + ":" + numOfRows;
        CacheEntry cached = cache.get(key);
        if (cached != null && cached.isFresh(cacheTtlSeconds)) {
            return cached.data();
        }
        List<TourAttraction> data;
        try {
            data = client.areaBasedList(sigungu.code(), numOfRows).stream()
                    .map(item -> enrich(item, sigungu))
                    .toList();
        } catch (Exception e) {
            log.warn("TourAPI 조회 실패 - sigunguCd={}", sigungu.code(), e);
            data = List.of();
        }
        cache.put(key, new CacheEntry(data, Instant.now()));
        return data;
    }

    private TourAttraction enrich(JsonNode item, Sigungu sigungu) {
        String contentId = textOrNull(item, "contentid");
        String contentTypeId = textOrNull(item, "contenttypeid");
        String overview = null;
        if (contentId != null) {
            try {
                JsonNode detail = client.detailCommon(contentId, contentTypeId);
                if (detail != null) {
                    overview = textOrNull(detail, "overview");
                }
            } catch (Exception e) {
                log.warn("TourAPI 상세조회 실패 - contentId={}", contentId, e);
            }
        }
        return new TourAttraction(
                contentId,
                textOrNull(item, "title"),
                sigungu.displayName(),
                textOrNull(item, "addr1"),
                textOrNull(item, "firstimage"),
                textOrNull(item, "mapx"),
                textOrNull(item, "mapy"),
                overview);
    }

    private static String textOrNull(JsonNode node, String field) {
        JsonNode value = node.path(field);
        return value.isMissingNode() || value.isNull() ? null : value.asText();
    }

    private record CacheEntry(List<TourAttraction> data, Instant fetchedAt) {
        boolean isFresh(long ttlSeconds) {
            return Instant.now().isBefore(fetchedAt.plusSeconds(ttlSeconds));
        }
    }
}
