package com.imageservice.model;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * Request object for image crop operation.
 */
public record CropRequest(
    @NotNull @Min(0) Integer x,
    @NotNull @Min(0) Integer y,
    @NotNull @Min(1) @Max(10000) Integer width,
    @NotNull @Min(1) @Max(10000) Integer height
) {
    /**
     * Validates that the crop region is within the image bounds.
     */
    public boolean isValidForImage(int imageWidth, int imageHeight) {
        return x >= 0 && y >= 0 
            && width > 0 && height > 0
            && x + width <= imageWidth 
            && y + height <= imageHeight;
    }
}