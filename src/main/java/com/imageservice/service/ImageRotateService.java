package com.imageservice.service;

import com.imageservice.config.ImageProcessingConfig;
import com.imageservice.model.ImageData;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

@Service
public class ImageRotateService {
    private final Executor executor;
    private final ImageProcessingConfig imageProcessingConfig;

    public ImageRotateService(Executor imageProcessingExecutor, ImageProcessingConfig imageProcessingConfig) {
        this.executor = imageProcessingExecutor;
        this.imageProcessingConfig = imageProcessingConfig;
    }

    public ImageData rotateImage(ImageData sourceImage, double angleDegrees) {
        if (sourceImage == null) {
            throw new IllegalArgumentException("Source image is required");
        }
        double angle = angleDegrees % 360.0;
        if (angle < 0) {
            angle += 360.0;
        }

        double radians = Math.toRadians(angle);
        double cos = Math.cos(radians);
        double sin = Math.sin(radians);

        int srcWidth = sourceImage.width();
        int srcHeight = sourceImage.height();

        int newWidth = (int) Math.ceil(Math.abs(srcWidth * cos) + Math.abs(srcHeight * sin));
        int newHeight = (int) Math.ceil(Math.abs(srcWidth * sin) + Math.abs(srcHeight * cos));

        int[] resultPixels = new int[newWidth * newHeight];
        int srcCenterX = srcWidth / 2;
        int srcCenterY = srcHeight / 2;
        int newCenterX = newWidth / 2;
        int newCenterY = newHeight / 2;

        int chunkSize = Math.max(1, imageProcessingConfig.getChunkSize());
        int rowsPerChunk = Math.max(1, chunkSize / newWidth);

        List<CompletableFuture<Void>> futures = new ArrayList<>();
        for (int startRow = 0; startRow < newHeight; startRow += rowsPerChunk) {
            int endRow = Math.min(newHeight, startRow + rowsPerChunk);
            int startRowFinal = startRow;
            int endRowFinal = endRow;
            futures.add(CompletableFuture.runAsync(() -> {
                for (int y = startRowFinal; y < endRowFinal; y++) {
                    for (int x = 0; x < newWidth; x++) {
                        int srcX = (int) Math.round((x - newCenterX) * cos + (y - newCenterY) * sin) + srcCenterX;
                        int srcY = (int) Math.round(-(x - newCenterX) * sin + (y - newCenterY) * cos) + srcCenterY;
                        if (srcX >= 0 && srcX < srcWidth && srcY >= 0 && srcY < srcHeight) {
                            resultPixels[y * newWidth + x] = sourceImage.pixels()[srcY * srcWidth + srcX];
                        } else {
                            resultPixels[y * newWidth + x] = 0x00000000;
                        }
                    }
                }
            }, executor));
        }

        CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new)).join();
        return new ImageData(newWidth, newHeight, resultPixels, sourceImage.format());
    }
}