package com.imageservice.service;

import com.imageservice.config.ImageProcessingConfig;
import com.imageservice.model.FlipDirection;
import com.imageservice.model.ImageData;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

/**
 * Service for flipping images horizontally, vertically, or both.
 * Uses parallel processing for pixel manipulation.
 * Follows loose coupling by working with ImageData.
 */
@Service
public class ImageFlipService {

    private final Executor executor;
    private final ImageProcessingConfig imageProcessingConfig;

    public ImageFlipService(Executor imageProcessingExecutor,
                             ImageProcessingConfig imageProcessingConfig) {
        this.executor = imageProcessingExecutor;
        this.imageProcessingConfig = imageProcessingConfig;
    }

    /**
     * Flips an image in the specified direction.
     *
     * @param sourceImage Source image data
     * @param direction Flip direction (HORIZONTAL, VERTICAL, or BOTH)
     * @return Flipped ImageData
     */
    public ImageData flipImage(ImageData sourceImage, FlipDirection direction) {
        if (sourceImage == null) {
            throw new IllegalArgumentException("Source image is required");
        }
        if (direction == null) {
            direction = FlipDirection.HORIZONTAL;
        }

        int width = sourceImage.width();
        int height = sourceImage.height();
        int[] sourcePixels = sourceImage.pixels();
        int[] resultPixels = new int[width * height];

        int chunkSize = Math.max(1, imageProcessingConfig.getChunkSize());
        int rowsPerChunk = Math.max(1, chunkSize / width);

        List<CompletableFuture<Void>> futures = new ArrayList<>();

        switch (direction) {
            case HORIZONTAL:
                // Flip each row left-right
                for (int startRow = 0; startRow < height; startRow += rowsPerChunk) {
                    int endRow = Math.min(height, startRow + rowsPerChunk);
                    int startRowFinal = startRow;
                    int endRowFinal = endRow;
                    futures.add(CompletableFuture.runAsync(() -> {
                        for (int y = startRowFinal; y < endRowFinal; y++) {
                            int rowOffset = y * width;
                            for (int x = 0; x < width; x++) {
                                int sourceX = width - 1 - x;
                                resultPixels[rowOffset + x] = sourcePixels[rowOffset + sourceX];
                            }
                        }
                    }, executor));
                }
                break;

            case VERTICAL:
                // Flip rows top-bottom
                for (int startRow = 0; startRow < height; startRow += rowsPerChunk) {
                    int endRow = Math.min(height, startRow + rowsPerChunk);
                    int startRowFinal = startRow;
                    int endRowFinal = endRow;
                    futures.add(CompletableFuture.runAsync(() -> {
                        for (int y = startRowFinal; y < endRowFinal; y++) {
                            int sourceY = height - 1 - y;
                            int sourceRowOffset = sourceY * width;
                            int targetRowOffset = y * width;
                            System.arraycopy(sourcePixels, sourceRowOffset, resultPixels, targetRowOffset, width);
                        }
                    }, executor));
                }
                break;

            case BOTH:
                // Flip both horizontally and vertically (180 degree rotation)
                for (int startRow = 0; startRow < height; startRow += rowsPerChunk) {
                    int endRow = Math.min(height, startRow + rowsPerChunk);
                    int startRowFinal = startRow;
                    int endRowFinal = endRow;
                    futures.add(CompletableFuture.runAsync(() -> {
                        for (int y = startRowFinal; y < endRowFinal; y++) {
                            int sourceY = height - 1 - y;
                            int sourceRowOffset = sourceY * width;
                            int targetRowOffset = y * width;
                            for (int x = 0; x < width; x++) {
                                int sourceX = width - 1 - x;
                                resultPixels[targetRowOffset + x] = sourcePixels[sourceRowOffset + sourceX];
                            }
                        }
                    }, executor));
                }
                break;
        }

        CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new)).join();
        return new ImageData(width, height, resultPixels, sourceImage.format());
    }

    /**
     * Convenience method for horizontal flip.
     */
    public ImageData flipHorizontal(ImageData sourceImage) {
        return flipImage(sourceImage, FlipDirection.HORIZONTAL);
    }

    /**
     * Convenience method for vertical flip.
     */
    public ImageData flipVertical(ImageData sourceImage) {
        return flipImage(sourceImage, FlipDirection.VERTICAL);
    }
}
