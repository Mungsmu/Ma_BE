package com.example.demo.tour;

import com.example.demo.common.dto.ApiResponse;
import com.example.demo.tour.dto.TourAttraction;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/tour")
public class TourController {

    private final TourAttractionService service;

    public TourController(TourAttractionService service) {
        this.service = service;
    }

    /** 강원도 시군구별 추천 관광지 (TourAPI 실시간 조회, 시군구명으로 그룹핑) */
    @GetMapping("/attractions")
    public ResponseEntity<ApiResponse<Map<String, List<TourAttraction>>>> attractions(
            @RequestParam(defaultValue = "5") int numOfRows) {
        return ResponseEntity.ok(ApiResponse.ok("조회되었습니다.", service.recommendationsByRegion(numOfRows)));
    }
}
