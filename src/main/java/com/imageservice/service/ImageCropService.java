package com.imageservice.service;

import com.imageservice.config.ImageProcessingConfig;
import com.imageservice.model.CropRequest;
import com.imageservice.model.ImageData;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

@Service
public class ImageCropService {
    private final Executor executor;
    private final ImageProcessingConfig imageProcessingConfig;

    public ImageCropService(Executor imageProcessingExecutor, ImageProcessingConfig imageProcessingConfig) {
        this.executor = imageProcessingExecutor;
        this.imageProcessingConfig = imageProcessingConfig;
    }

    public ImageData cropImage(ImageData sourceImage, CropRequest cropRequest) {
        if (sourceImage == null || cropRequest == null) {
            throw new IllegalArgumentException("Source image and crop request are required");
        }
        if (!cropRequest.isValidForImage(sourceImage.width(), sourceImage.height())) {
            throw new IllegalArgumentException("Crop region is outside image bounds");
        }

        int cropWidth = cropRequest.width();
        int cropHeight = cropRequest.height();
        int[] resultPixels = new int[cropWidth * cropHeight];

        int chunkSize = Math.max(1, imageProcessingConfig.getChunkSize());
        int rowsPerChunk = Math.max(1, chunkSize / cropWidth);

        List<CompletableFuture<Void>> futures = new ArrayList<>();
        for (int startRow = 0; startRow < cropHeight; startRow += rowsPerChunk) {
            int endRow = Math.min(cropHeight, startRow + rowsPerChunk);
            int startRowFinal = startRow;
            int endRowFinal = endRow;
            futures.add(CompletableFuture.runAsync(() -> {
                for (int row = startRowFinal; row < endRowFinal; row++) {
                    int sourceRow = cropRequest.y() + row;
                    int sourceStart = sourceRow * sourceImage.width() + cropRequest.x();
                    int targetStart = row * cropWidth;
                    System.arraycopy(sourceImage.pixels(), sourceStart, resultPixels, targetStart, cropWidth);
                }
            }, executor));
        }

        CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new)).join();
        return new ImageData(cropWidth, cropHeight, resultPixels, sourceImage.format());
    }
}