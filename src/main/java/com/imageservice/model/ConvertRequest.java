package com.imageservice.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * Request model for image format conversion operation.
 * Supports: png, jpg, jpeg, webp, avif, gif, bmp
 */
public record ConvertRequest(
    @NotBlank
    @Pattern(regexp = "(?i)(png|jpg|jpeg|webp|avif|gif|bmp)", 
             message = "Format must be one of: png, jpg, jpeg, webp, avif, gif, bmp")
    String targetFormat
) {
    public String getNormalizedFormat() {
        if (targetFormat == null) {
            return "png";
        }
        String lower = targetFormat.toLowerCase();
        return lower.equals("jpeg") ? "jpg" : lower;
    }
}
