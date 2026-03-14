package com.imageservice.model;

/**
 * Immutable data class representing an image in memory.
 * Uses primitive int[] for RGB pixel data where each int contains ARGB values.
 */
public record ImageData(
    int width,
    int height,
    int[] pixels,
    String format
) {
    public ImageData {
        if (width <= 0 || height <= 0) {
            throw new IllegalArgumentException("Width and height must be positive");
        }
        if (pixels == null || pixels.length != width * height) {
            throw new IllegalArgumentException("Pixel array must match width * height");
        }
        if (format == null || format.isBlank()) {
            format = "PNG";
        }
    }
    
    /**
     * Gets the ARGB pixel value at the specified coordinates.
     */
    public int getPixel(int x, int y) {
        if (x < 0 || x >= width || y < 0 || y >= height) {
            throw new IndexOutOfBoundsException("Coordinates out of bounds: (" + x + ", " + y + ")");
        }
        return pixels[y * width + x];
    }
    
    /**
     * Extracts red component from ARGB pixel value.
     */
    public static int getRed(int argb) {
        return (argb >> 16) & 0xFF;
    }
    
    /**
     * Extracts green component from ARGB pixel value.
     */
    public static int getGreen(int argb) {
        return (argb >> 8) & 0xFF;
    }
    
    /**
     * Extracts blue component from ARGB pixel value.
     */
    public static int getBlue(int argb) {
        return argb & 0xFF;
    }
    
    /**
     * Extracts alpha component from ARGB pixel value.
     */
    public static int getAlpha(int argb) {
        return (argb >> 24) & 0xFF;
    }
    
    /**
     * Constructs ARGB pixel value from individual components.
     */
    public static int toArgb(int alpha, int red, int green, int blue) {
        return (alpha << 24) | (red << 16) | (green << 8) | blue;
    }
}