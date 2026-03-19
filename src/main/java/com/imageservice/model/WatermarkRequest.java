package com.imageservice.model;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * Request model for watermark operation.
 * Image + watermark image provided via multipart, with position and alpha.
 */
public record WatermarkRequest(
    @NotNull WatermarkPosition position,
    @NotNull @Min(0) @Max(100) Integer alphaPercent,
    @Min(0) @Max(50) Integer padding
) {
    public WatermarkRequest {
        if (padding == null) {
            padding = 10; // Default padding
        }
    }

    /**
     * Converts alpha percentage to 0.0-1.0 factor.
     */
    public float getAlphaFactor() {
        return alphaPercent / 100.0f;
    }
}
