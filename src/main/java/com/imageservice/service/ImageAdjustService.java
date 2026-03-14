package com.imageservice.service;

import com.imageservice.config.ImageProcessingConfig;
import com.imageservice.model.ImageData;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

@Service
public class ImageAdjustService {
    private final Executor executor;
    private final ImageProcessingConfig imageProcessingConfig;

    public ImageAdjustService(Executor imageProcessingExecutor, ImageProcessingConfig imageProcessingConfig) {
        this.executor = imageProcessingExecutor;
        this.imageProcessingConfig = imageProcessingConfig;
    }

    public ImageData adjustImage(ImageData sourceImage, int brightness, int contrast, int luminosity) {
        if (sourceImage == null) {
            throw new IllegalArgumentException("Source image is required");
        }

        int width = sourceImage.width();
        int height = sourceImage.height();
        int[] resultPixels = new int[width * height];

        double brightnessOffset = (brightness / 100.0) * 255.0;
        double contrastFactor = (259.0 * (contrast + 255.0)) / (255.0 * (259.0 - contrast));
        double luminosityFactor = 1.0 + (luminosity / 100.0);

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

                        int adjustedRed = applyAdjustments(red, brightnessOffset, contrastFactor, luminosityFactor);
                        int adjustedGreen = applyAdjustments(green, brightnessOffset, contrastFactor, luminosityFactor);
                        int adjustedBlue = applyAdjustments(blue, brightnessOffset, contrastFactor, luminosityFactor);

                        resultPixels[rowOffset + x] = ImageData.toArgb(alpha, adjustedRed, adjustedGreen, adjustedBlue);
                    }
                }
            }, executor));
        }

        CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new)).join();
        return new ImageData(width, height, resultPixels, sourceImage.format());
    }

    private int applyAdjustments(int value, double brightnessOffset, double contrastFactor, double luminosityFactor) {
        double adjusted = value + brightnessOffset;
        adjusted = contrastFactor * (adjusted - 128.0) + 128.0;
        adjusted = adjusted * luminosityFactor;
        return clampToByte(adjusted);
    }

    private int clampToByte(double value) {
        return (int) Math.max(0, Math.min(255, Math.round(value)));
    }
}