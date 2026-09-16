package com.example.demo.tour.dto;

/** TourAPI areaBasedList2 + detailCommon2 결과를 합친 관광지 응답. */
public record TourAttraction(String contentId, String title, String sigunguName, String address,
                              String imageUrl, String mapX, String mapY, String overview) {
}
