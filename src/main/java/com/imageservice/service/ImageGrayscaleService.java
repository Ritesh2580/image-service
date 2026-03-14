package com.imageservice.service;

import com.imageservice.config.ImageProcessingConfig;
import com.imageservice.model.ImageData;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

@Service
public class ImageGrayscaleService {
    private final Executor executor;
    private final ImageProcessingConfig imageProcessingConfig;

    public ImageGrayscaleService(Executor imageProcessingExecutor, ImageProcessingConfig imageProcessingConfig) {
        this.executor = imageProcessingExecutor;
        this.imageProcessingConfig = imageProcessingConfig;
    }

    public ImageData grayscaleImage(ImageData sourceImage, boolean keepAlpha) {
        if (sourceImage == null) {
            throw new IllegalArgumentException("Source image is required");
        }

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
                        int argb = sourceImage.pixels()[rowOffset + x];
                        int alpha = ImageData.getAlpha(argb);
                        int red = ImageData.getRed(argb);
                        int green = ImageData.getGreen(argb);
                        int blue = ImageData.getBlue(argb);
                        int gray = (red + green + blue) / 3;
                        int outAlpha = keepAlpha ? alpha : 0xFF;
                        resultPixels[rowOffset + x] = ImageData.toArgb(outAlpha, gray, gray, gray);
                    }
                }
            }, executor));
        }

        CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new)).join();
        return new ImageData(width, height, resultPixels, sourceImage.format());
    }
}