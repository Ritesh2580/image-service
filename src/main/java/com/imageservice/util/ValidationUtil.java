package com.imageservice.util;

public final class ValidationUtil {
    private ValidationUtil() {
    }

    public static void validateImageDimensions(int width, int height, int maxDimension) {
        if (width <= 0 || height <= 0) {
            throw new IllegalArgumentException("Image dimensions must be positive");
        }
        if (width > maxDimension || height > maxDimension) {
            throw new IllegalArgumentException("Image dimensions exceed max allowed: " + maxDimension);
        }
    }
}