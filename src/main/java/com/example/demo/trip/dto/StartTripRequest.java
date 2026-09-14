package com.example.demo.trip.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

public record StartTripRequest(
        @NotNull @DecimalMin("-90") @DecimalMax("90") Double originLat,
        @NotNull @DecimalMin("-180") @DecimalMax("180") Double originLng,
        @NotNull @DecimalMin("-90") @DecimalMax("90") Double destLat,
        @NotNull @DecimalMin("-180") @DecimalMax("180") Double destLng
) {
}
