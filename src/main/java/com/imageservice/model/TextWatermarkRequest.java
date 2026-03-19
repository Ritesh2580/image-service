package com.imageservice.model;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Request model for text watermark/overlay operation.
 * Allows adding custom text at specified position with font styling.
 */
public record TextWatermarkRequest(
    @NotBlank String text,
    String fontFamily,
    @Min(8) @Max(200) Integer fontSize,
    @NotBlank String color,  // Hex color like "#FF0000" or "white"
    @NotNull @Min(0) Integer x,
    @NotNull @Min(0) Integer y,
    @Min(0) @Max(100) Integer alphaPercent
) {
    public TextWatermarkRequest {
        if (fontFamily == null || fontFamily.isBlank()) {
            fontFamily = "SansSerif";
        }
        if (fontSize == null) {
            fontSize = 24;
        }
        if (alphaPercent == null) {
            alphaPercent = 100;
        }
    }

    /**
     * Converts alpha percentage to 0.0-1.0 factor.
     */
    public float getAlphaFactor() {
        return alphaPercent / 100.0f;
    }

    /**
     * Parses hex color string to RGB components.
     * Supports: "#RRGGBB", "RRGGBB", "#RGB", "white", "black", etc.
     */
    public int[] parseColor() {
        String c = color.trim().toLowerCase();
        
        // Named colors
        switch (c) {
            case "white": return new int[]{255, 255, 255};
            case "black": return new int[]{0, 0, 0};
            case "red": return new int[]{255, 0, 0};
            case "green": return new int[]{0, 255, 0};
            case "blue": return new int[]{0, 0, 255};
            case "yellow": return new int[]{255, 255, 0};
            case "cyan": return new int[]{0, 255, 255};
            case "magenta": return new int[]{255, 0, 255};
            case "gray": case "grey": return new int[]{128, 128, 128};
            case "orange": return new int[]{255, 165, 0};
            case "pink": return new int[]{255, 192, 203};
        }
        
        // Hex parsing
        String hex = c.startsWith("#") ? c.substring(1) : c;
        
        if (hex.length() == 3) {
            // Short form #RGB -> #RRGGBB
            hex = "" + hex.charAt(0) + hex.charAt(0) + 
                  hex.charAt(1) + hex.charAt(1) + 
                  hex.charAt(2) + hex.charAt(2);
        }
        
        if (hex.length() != 6) {
            return new int[]{255, 255, 255}; // Default white
        }
        
        try {
            int r = Integer.parseInt(hex.substring(0, 2), 16);
            int g = Integer.parseInt(hex.substring(2, 4), 16);
            int b = Integer.parseInt(hex.substring(4, 6), 16);
            return new int[]{r, g, b};
        } catch (NumberFormatException e) {
            return new int[]{255, 255, 255}; // Default white on parse error
        }
    }
}
