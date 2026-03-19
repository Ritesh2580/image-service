package com.imageservice.service;

import com.imageservice.config.ImageProcessingConfig;
import com.imageservice.model.ImageData;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

/**
 * Service for converting images between different formats.
 * Handles format-specific processing like transparency flattening for JPG output.
 * Follows loose coupling by delegating encoding to ImageCodecService.
 */
@Service
public class ImageConvertService {

    private final Executor executor;
    private final ImageProcessingConfig imageProcessingConfig;
    private final ImageCodecService imageCodecService;

    // Supported output formats
    private static final Set<String> SUPPORTED_FORMATS = Set.of(
        "png", "jpg", "jpeg", "webp", "avif", "gif", "bmp"
    );

    public ImageConvertService(Executor imageProcessingExecutor,
                                ImageProcessingConfig imageProcessingConfig,
                                ImageCodecService imageCodecService) {
        this.executor = imageProcessingExecutor;
        this.imageProcessingConfig = imageProcessingConfig;
        this.imageCodecService = imageCodecService;
    }

    /**
     * Converts an image to the target format.
     * For formats that don't support transparency (JPG), flattens alpha onto white background.
     *
     * @param sourceImage Source image data
     * @param targetFormat Target format (png, jpg, webp, avif, etc.)
     * @return Converted ImageData (pixels may be modified for format compatibility)
     */
    public ImageData convertImage(ImageData sourceImage, String targetFormat) {
        if (sourceImage == null) {
            throw new IllegalArgumentException("Source image is required");
        }

        String normalizedFormat = normalizeFormat(targetFormat);

        if (!isSupportedFormat(normalizedFormat)) {
            throw new IllegalArgumentException("Unsupported target format: " + targetFormat + 
                ". Supported formats: " + String.join(", ", SUPPORTED_FORMATS));
        }

        // For formats without alpha support (JPG), flatten transparency
        if (requiresAlphaFlattening(normalizedFormat) && hasTransparency(sourceImage)) {
            return flattenAlpha(sourceImage);
        }

        // For formats that support alpha or no alpha present, return as-is
        // The actual encoding to target format happens in ImageCodecService
        return sourceImage;
    }

    /**
     * Checks if a format is supported for output.
     */
    public boolean isSupportedFormat(String format) {
        if (format == null) return false;
        return SUPPORTED_FORMATS.contains(normalizeFormat(format));
    }

    /**
     * Returns the set of supported output formats.
     */
    public Set<String> getSupportedFormats() {
        return SUPPORTED_FORMATS;
    }

    /**
     * Checks if the format requires alpha channel flattening.
     */
    private boolean requiresAlphaFlattening(String format) {
        return format.equals("jpg") || format.equals("jpeg");
    }

    /**
     * Checks if any pixel in the image has non-opaque alpha.
     */
    private boolean hasTransparency(ImageData image) {
        for (int pixel : image.pixels()) {
            if (ImageData.getAlpha(pixel) < 255) {
                return true;
            }
        }
        return false;
    }

    /**
     * Flattens alpha channel onto white background using parallel processing.
     */
    private ImageData flattenAlpha(ImageData sourceImage) {
        int width = sourceImage.width();
        int height = sourceImage.height();
        int[] resultPixels = new int[width * height];

        int chunkSize = Math.max(1, imageProcessingConfig.getChunkSize());
        int rowsPerChunk = Math.max(1, chunkSize / width);

        List<CompletableFuture<Void>> futures = new ArrayList<>();
        for (int startRow = 0; startRow < height; startRow += rowsPerChunk) {
            int endRow = Math.min(height, startRow + rowsPerChunk);
            int startRowFinal = startRow;
            int endRowFinal = endRow;
            futures.add(CompletableFuture.runAsync(() -> {
                for (int y = startRowFinal; y < endRowFinal; y++) {
                    int rowOffset = y * width;
                    for (int x = 0; x < width; x++) {
                        int idx = rowOffset + x;
                        int argb = sourceImage.pixels()[idx];
                        int alpha = ImageData.getAlpha(argb);
                        int red = ImageData.getRed(argb);
                        int green = ImageData.getGreen(argb);
                        int blue = ImageData.getBlue(argb);

                        if (alpha < 255) {
                            // Composite onto white background
                            double a = alpha / 255.0;
                            red = (int) Math.round(red * a + 255 * (1 - a));
                            green = (int) Math.round(green * a + 255 * (1 - a));
                            blue = (int) Math.round(blue * a + 255 * (1 - a));
                        }

                        resultPixels[idx] = ImageData.toArgb(255, red, green, blue);
                    }
                }
            }, executor));
        }

        CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new)).join();
        return new ImageData(width, height, resultPixels, sourceImage.format());
    }

    private String normalizeFormat(String format) {
        if (format == null || format.isBlank()) {
            return "png";
        }
        String lower = format.toLowerCase();
        return lower.equals("jpeg") ? "jpg" : lower;
    }
}
