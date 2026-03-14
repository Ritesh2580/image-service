package com.imageservice.service;

import com.imageservice.config.ImageProcessingConfig;
import com.imageservice.model.ImageData;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

@Service
public class ImageResizeService {
    private final Executor executor;
    private final ImageProcessingConfig imageProcessingConfig;

    public ImageResizeService(Executor imageProcessingExecutor, ImageProcessingConfig imageProcessingConfig) {
        this.executor = imageProcessingExecutor;
        this.imageProcessingConfig = imageProcessingConfig;
    }

    public ImageData resizeImage(ImageData sourceImage, int targetWidth, int targetHeight) {
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
}