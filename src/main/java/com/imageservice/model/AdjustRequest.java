package com.imageservice.model;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record AdjustRequest(
    @NotNull @Min(-100) @Max(100) Integer brightness,
    @NotNull @Min(-100) @Max(100) Integer contrast,
    @NotNull @Min(-100) @Max(100) Integer luminosity
) {
}