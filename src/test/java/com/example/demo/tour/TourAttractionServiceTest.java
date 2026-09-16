package com.example.demo.tour;

import com.example.demo.tour.dto.TourAttraction;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TourAttractionServiceTest {

    private final TourApiClient client = mock(TourApiClient.class);
    private final TourAttractionService service = new TourAttractionService(client, 300);
    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void 한_시군구_조회가_실패해도_나머지_17개는_정상_응답한다() throws Exception {
        JsonNode namiseom = mapper.readTree(
                "{\"contentid\":\"1\",\"contenttypeid\":\"12\",\"title\":\"남이섬\",\"addr1\":\"춘천\"}");
        when(client.areaBasedList(anyString(), anyInt())).thenReturn(List.of());
        when(client.areaBasedList(eq(Sigungu.CHUNCHEON.code()), anyInt())).thenReturn(List.of(namiseom));
        when(client.areaBasedList(eq(Sigungu.WONJU.code()), anyInt())).thenThrow(new RuntimeException("timeout"));
        when(client.detailCommon(anyString(), anyString())).thenReturn(null);

        Map<String, List<TourAttraction>> result = service.recommendationsByRegion(5);

        assertThat(result).hasSize(18);
        assertThat(result.get(Sigungu.CHUNCHEON.displayName())).extracting(TourAttraction::title).containsExactly("남이섬");
        assertThat(result.get(Sigungu.WONJU.displayName())).isEmpty();
    }

    @Test
    void 결과는_TTL_동안_캐시되어_같은_시군구를_다시_조회하지_않는다() {
        when(client.areaBasedList(anyString(), anyInt())).thenReturn(List.of());

        service.recommendationsByRegion(5);
        service.recommendationsByRegion(5);

        verify(client, times(Sigungu.values().length)).areaBasedList(anyString(), anyInt());
    }
}
