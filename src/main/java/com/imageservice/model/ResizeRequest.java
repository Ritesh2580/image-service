package com.imageservice.model;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record ResizeRequest(
    @NotNull @Min(1) @Max(10000) Integer width,
    @NotNull @Min(1) @Max(10000) Integer height
) {
    public boolean isValidForImage(int maxDimension) {
        return width > 0 && height > 0 && width <= maxDimension && height <= maxDimension;
    }
}