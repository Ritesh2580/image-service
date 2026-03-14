package com.imageservice.model;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record RotateRequest(
    @NotNull @Min(-360) @Max(360) Integer angle
) {
    public double normalizedAngle() {
        double normalized = angle % 360.0;
        if (normalized < 0) {
            normalized += 360.0;
        }
        return normalized;
    }
}