package com.imageservice.model;

import jakarta.validation.constraints.NotNull;

/**
 * Request model for image flip operation.
 * Supports horizontal, vertical, or both flip directions.
 */
public record FlipRequest(
    @NotNull FlipDirection direction
) {
}
