package com.example.demo.tour;

import tools.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.List;

/**
 * 공공데이터포털 TourAPI(KorService2) 클라이언트.
 * 매 호출이 실시간 조회다 — 결과를 정적 데이터로 저장/서빙하지 않는다(캐싱은 호출자인
 * {@link TourAttractionService}가 짧은 TTL로만 담당).
 */
@Component
public class TourApiClient {

    private static final Logger log = LoggerFactory.getLogger(TourApiClient.class);

    private final RestClient restClient;
    private final String serviceKey;

    public TourApiClient(@Value("${app.tour.base-url}") String baseUrl,
                          @Value("${app.tour.api-key}") String serviceKey) {
        this.serviceKey = serviceKey;
        this.restClient = RestClient.builder().baseUrl(baseUrl).build();
    }

    /** areaCd=51 + sigunguCd로 관광지 목록 조회. 실패/resultCode!=0000이면 빈 리스트. */
    public List<JsonNode> areaBasedList(String sigunguCd, int numOfRows) {
        JsonNode root = restClient.get()
                .uri(uriBuilder -> uriBuilder.path("/areaBasedList2")
                        .queryParam("serviceKey", serviceKey)
                        .queryParam("MobileOS", "ETC")
                        .queryParam("MobileApp", "maeumsumgil")
                        .queryParam("_type", "json")
                        .queryParam("areaCode", "51")
                        .queryParam("sigunguCode", sigunguCd)
                        .queryParam("numOfRows", numOfRows)
                        .build())
                .retrieve()
                .body(JsonNode.class);
        return items(root, "areaBasedList2 sigunguCd=" + sigunguCd);
    }

    /** contentid로 overview/대표이미지/좌표 등 상세 보강. 실패 시 null. */
    public JsonNode detailCommon(String contentId, String contentTypeId) {
        JsonNode root = restClient.get()
                .uri(uriBuilder -> {
                    uriBuilder.path("/detailCommon2")
                            .queryParam("serviceKey", serviceKey)
                            .queryParam("MobileOS", "ETC")
                            .queryParam("MobileApp", "maeumsumgil")
                            .queryParam("_type", "json")
                            .queryParam("contentId", contentId)
                            .queryParam("overviewYN", "Y");
                    if (contentTypeId != null && !contentTypeId.isBlank()) {
                        uriBuilder.queryParam("contentTypeId", contentTypeId);
                    }
                    return uriBuilder.build();
                })
                .retrieve()

                .body(JsonNode.class);
        List<JsonNode> items = items(root, "detailCommon2 contentId=" + contentId);
        return items.isEmpty() ? null : items.get(0);
    }

    /** response.header.resultCode 확인 후 response.body.items.item(단건/배열 모두 대응)을 꺼낸다. */
    private List<JsonNode> items(JsonNode root, String context) {
        if (root == null) {
            return List.of();
        }
        JsonNode header = root.path("response").path("header");
        String resultCode = header.path("resultCode").asText();
        if (!"0000".equals(resultCode)) {
            log.warn("TourAPI 실패 [{}] resultCode={}, resultMsg={}", context, resultCode, header.path("resultMsg").asText());
            return List.of();
        }
        JsonNode item = root.path("response").path("body").path("items").path("item");
        if (item.isMissingNode() || item.isNull()) {
            return List.of();
        }
        if (item.isArray()) {
            List<JsonNode> list = new ArrayList<>();
            item.forEach(list::add);
            return list;
        }
        return List.of(item);
    }
}
