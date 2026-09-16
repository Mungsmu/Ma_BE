package com.example.demo.tour;

/** 강원도(areaCd=51) 시군구 코드. TourAPI 조회 대상 목록이지 관광지 데이터 자체가 아니므로 하드코딩 금지 규칙과 무관하다. */
public enum Sigungu {
    CHUNCHEON("51110", "춘천시"),
    WONJU("51130", "원주시"),
    GANGNEUNG("51150", "강릉시"),
    DONGHAE("51170", "동해시"),
    TAEBAEK("51190", "태백시"),
    SOKCHO("51210", "속초시"),
    SAMCHEOK("51230", "삼척시"),
    HONGCHEON("51720", "홍천군"),
    HOENGSEONG("51730", "횡성군"),
    YEONGWOL("51750", "영월군"),
    PYEONGCHANG("51760", "평창군"),
    JEONGSEON("51770", "정선군"),
    CHEORWON("51780", "철원군"),
    HWACHEON("51790", "화천군"),
    YANGGU("51800", "양구군"),
    INJE("51810", "인제군"),
    GOSEONG("51820", "고성군"),
    YANGYANG("51830", "양양군");

    private final String code;
    private final String displayName;

    Sigungu(String code, String displayName) {
        this.code = code;
        this.displayName = displayName;
    }

    public String code() {
        return code;
    }

    public String displayName() {
        return displayName;
    }
}
