package com.imageservice.model;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * Request model for image compression operation.
 * Target size is specified in kilobytes.
 */
public record CompressRequest(
    @NotNull @Min(1) @Max(50000) Integer targetSizeKB
) {
    /**
     * Converts target size from KB to bytes.
     */
    public long getTargetSizeBytes() {
        return targetSizeKB * 1024L;
    }
}
