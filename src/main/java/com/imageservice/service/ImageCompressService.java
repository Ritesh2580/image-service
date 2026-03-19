package com.imageservice.service;

import com.imageservice.config.ImageProcessingConfig;
import com.imageservice.model.ImageData;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

/**
 * Service for compressing images to a target file size.
 * Uses iterative quality reduction and/or dimension scaling to achieve target size.
 * Follows loose coupling by delegating encoding to ImageCodecService.
 */
@Service
public class ImageCompressService {

    private final Executor executor;
    private final ImageProcessingConfig imageProcessingConfig;
    private final ImageCodecService imageCodecService;

    // Compression algorithm constants
    private static final float MIN_QUALITY = 0.05f;
    private static final float MAX_QUALITY = 1.0f;
    private static final float QUALITY_STEP = 0.05f;
    private static final float MIN_SCALE_FACTOR = 0.1f;
    private static final float SCALE_STEP = 0.1f;

    public ImageCompressService(Executor imageProcessingExecutor,
                                 ImageProcessingConfig imageProcessingConfig,
                                 ImageCodecService imageCodecService) {
        this.executor = imageProcessingExecutor;
        this.imageProcessingConfig = imageProcessingConfig;
        this.imageCodecService = imageCodecService;
    }

    /**
     * Compresses an image to fit within the target size (in bytes).
     * First attempts quality reduction, then dimension scaling if needed.
     *
     * @param sourceImage The source image data
     * @param targetSizeBytes Maximum allowed file size in bytes
     * @param outputFormat Target output format (affects compression strategy)
     * @return Compressed ImageData
     */
    public ImageData compressImage(ImageData sourceImage, long targetSizeBytes, String outputFormat) {
        if (sourceImage == null) {
            throw new IllegalArgumentException("Source image is required");
        }
        if (targetSizeBytes <= 0) {
            throw new IllegalArgumentException("Target size must be positive");
        }

        String format = normalizeFormat(outputFormat);

        try {
            // First, check if original already fits
            byte[] currentBytes = imageCodecService.encodeImage(sourceImage, format);
            if (currentBytes.length <= targetSizeBytes) {
                return sourceImage;
            }

            // For lossy formats (jpg, webp, avif), try quality reduction first
            if (isLossyFormat(format)) {
                ImageData qualityReduced = tryQualityReduction(sourceImage, format, targetSizeBytes);
                byte[] qualityBytes = imageCodecService.encodeImage(qualityReduced, format);
                if (qualityBytes.length <= targetSizeBytes) {
                    return qualityReduced;
                }
                // If quality reduction wasn't enough, continue with scaled version
                return tryDimensionScaling(qualityReduced, format, targetSizeBytes);
            } else {
                // For lossless formats (png), directly use dimension scaling
                return tryDimensionScaling(sourceImage, format, targetSizeBytes);
            }
        } catch (IOException e) {
            throw new IllegalStateException("Compression failed: " + e.getMessage(), e);
        }
    }

    /**
     * Attempts to compress by reducing quality iteratively.
     */
    private ImageData tryQualityReduction(ImageData sourceImage, String format, long targetSizeBytes)
            throws IOException {
        float quality = MAX_QUALITY;

        while (quality >= MIN_QUALITY) {
            byte[] bytes = imageCodecService.encodeImageWithQuality(sourceImage, format, quality);
            if (bytes.length <= targetSizeBytes) {
                return sourceImage;
            }
            quality -= QUALITY_STEP;
        }

        // Return original if we couldn't compress enough via quality alone
        return sourceImage;
    }

    /**
     * Attempts to compress by scaling down dimensions iteratively.
     * Uses parallel processing for pixel scaling.
     */
    private ImageData tryDimensionScaling(ImageData sourceImage, String format, long targetSizeBytes)
            throws IOException {
        float scaleFactor = 1.0f;
        ImageData currentImage = sourceImage;

        while (scaleFactor >= MIN_SCALE_FACTOR) {
            byte[] bytes = imageCodecService.encodeImage(currentImage, format);
            if (bytes.length <= targetSizeBytes) {
                return currentImage;
            }

            // Calculate new dimensions
            scaleFactor -= SCALE_STEP;
            if (scaleFactor < MIN_SCALE_FACTOR) {
                break;
            }

            int newWidth = Math.max(1, (int) (sourceImage.width() * scaleFactor));
            int newHeight = Math.max(1, (int) (sourceImage.height() * scaleFactor));
            currentImage = scaleImage(sourceImage, newWidth, newHeight);
        }

        // Return smallest possible if we still can't fit
        return currentImage;
    }

    /**
     * Scales an image to new dimensions using parallel processing.
     */
    private ImageData scaleImage(ImageData sourceImage, int targetWidth, int targetHeight) {
        if (sourceImage == null) {
            throw new IllegalArgumentException("Source image is required");
        }
        if (targetWidth <= 0 || targetHeight <= 0) {
            throw new IllegalArgumentException("Target dimensions must be positive");
        }

        int[] resultPixels = new int[targetWidth * targetHeight];
        double xRatio = (double) sourceImage.width() / targetWidth;
        double yRatio = (double) sourceImage.height() / targetHeight;

        int chunkSize = Math.max(1, imageProcessingConfig.getChunkSize());
        int rowsPerChunk = Math.max(1, chunkSize / targetWidth);

        List<CompletableFuture<Void>> futures = new ArrayList<>();
        for (int startRow = 0; startRow < targetHeight; startRow += rowsPerChunk) {
            int endRow = Math.min(targetHeight, startRow + rowsPerChunk);
            int startRowFinal = startRow;
            int endRowFinal = endRow;
            futures.add(CompletableFuture.runAsync(() -> {
                for (int y = startRowFinal; y < endRowFinal; y++) {
                    int sourceY = Math.min((int) (y * yRatio), sourceImage.height() - 1);
                    for (int x = 0; x < targetWidth; x++) {
                        int sourceX = Math.min((int) (x * xRatio), sourceImage.width() - 1);
                        int sourceIndex = sourceY * sourceImage.width() + sourceX;
                        resultPixels[y * targetWidth + x] = sourceImage.pixels()[sourceIndex];
                    }
                }
            }, executor));
        }

        CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new)).join();
        return new ImageData(targetWidth, targetHeight, resultPixels, sourceImage.format());
    }

    private boolean isLossyFormat(String format) {
        return format.equals("jpg") || format.equals("jpeg") ||
               format.equals("webp") || format.equals("avif");
    }

    private String normalizeFormat(String format) {
        if (format == null || format.isBlank()) {
            return "png";
        }
        String lower = format.toLowerCase();
        return lower.equals("jpeg") ? "jpg" : lower;
    }
}
