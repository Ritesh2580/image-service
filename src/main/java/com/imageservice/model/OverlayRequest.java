package com.imageservice.model;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * Request model for image overlay operation.
 * Two images are provided via multipart: base image and overlay image.
 */
public record OverlayRequest(
    @NotNull @Min(0) @Max(100) Integer alphaPercent
) {
    /**
     * Converts alpha percentage to 0.0-1.0 factor.
     */
    public float getAlphaFactor() {
        return alphaPercent / 100.0f;
    }
}
