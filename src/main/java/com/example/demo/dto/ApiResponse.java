package com.example.demo.dto;

/**
 * 공통 API 응답 포맷.
 *
 * @param success 성공 여부
 * @param message 사용자에게 보여줄 메시지
 * @param data    부가 데이터 (없으면 null)
 */
public record ApiResponse<T>(boolean success, String message, T data) {

    public static <T> ApiResponse<T> ok(String message, T data) {
        return new ApiResponse<>(true, message, data);
    }

    public static ApiResponse<Void> ok(String message) {
        return new ApiResponse<>(true, message, null);
    }

    public static ApiResponse<Void> fail(String message) {
        return new ApiResponse<>(false, message, null);
    }
}
