package com.imageservice.service;

import com.imageservice.config.ImageProcessingConfig;
import com.imageservice.model.ImageData;
import com.imageservice.model.WatermarkPosition;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

/**
 * Service for overlaying images and applying watermarks.
 * Uses alpha blending for compositing with parallel processing.
 * Follows loose coupling by working with ImageData.
 */
@Service
public class ImageOverlayService {

    private final Executor executor;
    private final ImageProcessingConfig imageProcessingConfig;

    public ImageOverlayService(Executor imageProcessingExecutor,
                                ImageProcessingConfig imageProcessingConfig) {
        this.executor = imageProcessingExecutor;
        this.imageProcessingConfig = imageProcessingConfig;
    }

    /**
     * Overlays one image over another with alpha blending.
     * The overlay is centered on the base image.
     * If overlay is larger, it will be scaled to fit base dimensions.
     *
     * @param baseImage Base image (result dimensions)
     * @param overlayImage Image to overlay
     * @param alphaFactor Blending factor (0.0 = base only, 1.0 = overlay only)
     * @return Blended ImageData
     */
    public ImageData overlayImage(ImageData baseImage, ImageData overlayImage, float alphaFactor) {
        if (baseImage == null || overlayImage == null) {
            throw new IllegalArgumentException("Base and overlay images are required");
        }

        // Scale overlay to fit base if needed
        ImageData scaledOverlay = scaleToFit(overlayImage, baseImage.width(), baseImage.height());
        
        // Calculate centered position
        int offsetX = (baseImage.width() - scaledOverlay.width()) / 2;
        int offsetY = (baseImage.height() - scaledOverlay.height()) / 2;

        return blendImages(baseImage, scaledOverlay, offsetX, offsetY, alphaFactor);
    }

    /**
     * Applies a watermark at a specific position.
     * Watermark is placed at the specified corner/center position.
     *
     * @param baseImage Base image
     * @param watermarkImage Watermark image (typically smaller, with transparency)
     * @param position Position constant (TOP_LEFT, TOP_RIGHT, etc.)
     * @param alphaFactor Alpha blending factor
     * @param padding Padding from edges in pixels
     * @return Image with watermark applied
     */
    public ImageData applyWatermark(ImageData baseImage, ImageData watermarkImage,
                                     WatermarkPosition position, float alphaFactor, int padding) {
        if (baseImage == null || watermarkImage == null) {
            throw new IllegalArgumentException("Base image and watermark are required");
        }
        if (position == null) {
            position = WatermarkPosition.BOTTOM_RIGHT;
        }

        // Scale watermark if larger than 50% of base (to prevent covering too much)
        int maxW = baseImage.width() / 2;
        int maxH = baseImage.height() / 2;
        ImageData scaledWatermark = scaleToFit(watermarkImage, maxW, maxH);

        // Calculate position
        int[] pos = position.calculatePosition(
            baseImage.width(), baseImage.height(),
            scaledWatermark.width(), scaledWatermark.height(), padding
        );

        return blendImages(baseImage, scaledWatermark, pos[0], pos[1], alphaFactor);
    }

    /**
     * Applies watermark at default bottom-right position with default padding.
     */
    public ImageData applyWatermark(ImageData baseImage, ImageData watermarkImage,
                                     WatermarkPosition position, float alphaFactor) {
        return applyWatermark(baseImage, watermarkImage, position, alphaFactor, 10);
    }

    /**
     * Scales an image to fit within max dimensions while preserving aspect ratio.
     */
    public ImageData scaleToFit(ImageData source, int maxWidth, int maxHeight) {
        if (source.width() <= maxWidth && source.height() <= maxHeight) {
            return source;
        }

        double ratioW = (double) maxWidth / source.width();
        double ratioH = (double) maxHeight / source.height();
        double ratio = Math.min(ratioW, ratioH);

        int newWidth = Math.max(1, (int) (source.width() * ratio));
        int newHeight = Math.max(1, (int) (source.height() * ratio));

        return scaleImage(source, newWidth, newHeight);
    }

    /**
     * Scales image to exact dimensions using parallel processing.
     */
    private ImageData scaleImage(ImageData source, int targetWidth, int targetHeight) {
        int[] resultPixels = new int[targetWidth * targetHeight];
        double xRatio = (double) source.width() / targetWidth;
        double yRatio = (double) source.height() / targetHeight;

        int chunkSize = Math.max(1, imageProcessingConfig.getChunkSize());
        int rowsPerChunk = Math.max(1, chunkSize / targetWidth);

        List<CompletableFuture<Void>> futures = new ArrayList<>();
        for (int startRow = 0; startRow < targetHeight; startRow += rowsPerChunk) {
            int endRow = Math.min(targetHeight, startRow + rowsPerChunk);
            int startRowFinal = startRow;
            int endRowFinal = endRow;
            futures.add(CompletableFuture.runAsync(() -> {
                for (int y = startRowFinal; y < endRowFinal; y++) {
                    int sourceY = Math.min((int) (y * yRatio), source.height() - 1);
                    for (int x = 0; x < targetWidth; x++) {
                        int sourceX = Math.min((int) (x * xRatio), source.width() - 1);
                        resultPixels[y * targetWidth + x] = source.pixels()[sourceY * source.width() + sourceX];
                    }
                }
            }, executor));
        }

        CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new)).join();
        return new ImageData(targetWidth, targetHeight, resultPixels, source.format());
    }

    /**
     * Blends overlay onto base at specified position using alpha compositing.
     * Uses parallel processing for chunked rows.
     */
    private ImageData blendImages(ImageData base, ImageData overlay, 
                                   int offsetX, int offsetY, float alphaFactor) {
        int width = base.width();
        int height = base.height();
        int[] resultPixels = new int[width * height];

        // Clamp alpha
        float alpha = Math.max(0.0f, Math.min(1.0f, alphaFactor));

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
                        int basePixel = base.pixels()[rowOffset + x];
                        
                        // Check if overlay covers this pixel
                        int overlayX = x - offsetX;
                        int overlayY = y - offsetY;
                        
                        if (overlayX >= 0 && overlayX < overlay.width() &&
                            overlayY >= 0 && overlayY < overlay.height()) {
                            
                            int overlayPixel = overlay.pixels()[overlayY * overlay.width() + overlayX];
                            int overlayAlpha = ImageData.getAlpha(overlayPixel);
                            
                            // Only blend if overlay has some alpha
                            if (overlayAlpha > 0) {
                                // Combine overlay's alpha with user-specified alpha
                                float effectiveAlpha = (overlayAlpha / 255.0f) * alpha;
                                
                                int baseR = ImageData.getRed(basePixel);
                                int baseG = ImageData.getGreen(basePixel);
                                int baseB = ImageData.getBlue(basePixel);
                                
                                int overR = ImageData.getRed(overlayPixel);
                                int overG = ImageData.getGreen(overlayPixel);
                                int overB = ImageData.getBlue(overlayPixel);
                                
                                // Alpha compositing: result = base * (1-a) + overlay * a
                                int r = (int) Math.round(baseR * (1 - effectiveAlpha) + overR * effectiveAlpha);
                                int g = (int) Math.round(baseG * (1 - effectiveAlpha) + overG * effectiveAlpha);
                                int b = (int) Math.round(baseB * (1 - effectiveAlpha) + overB * effectiveAlpha);
                                
                                resultPixels[rowOffset + x] = ImageData.toArgb(255, 
                                    Math.max(0, Math.min(255, r)),
                                    Math.max(0, Math.min(255, g)),
                                    Math.max(0, Math.min(255, b)));
                                continue;
                            }
                        }
                        
                        // No overlay pixel or transparent - keep base
                        resultPixels[rowOffset + x] = basePixel;
                    }
                }
            }, executor));
        }

        CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new)).join();
        return new ImageData(width, height, resultPixels, base.format());
    }
}
