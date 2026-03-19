package com.imageservice.service;

import com.imageservice.config.ImageProcessingConfig;
import com.imageservice.model.ImageData;
import com.imageservice.model.WatermarkPosition;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ImageOverlayServiceTest {

    private ImageProcessingConfig imageProcessingConfig;
    private ImageOverlayService imageOverlayService;

    private Executor executor;
    private ImageData baseImage;
    private ImageData overlayImage;
    private ImageData smallOverlayImage;

    @BeforeEach
    void setUp() {
        executor = Executors.newFixedThreadPool(2);
        int[] basePixels = new int[10000];
        basePixels[0] = 0xFF0000FF; // Blue pixel at top-left
        baseImage = new ImageData(100, 100, basePixels, "png");

        int[] overlayPixels = new int[10000];
        overlayPixels[0] = 0xFFFF0000; // Red pixel at top-left
        overlayImage = new ImageData(100, 100, overlayPixels, "png");

        // Small overlay image (50x50)
        int[] smallOverlayPixels = new int[2500];
        smallOverlayImage = new ImageData(50, 50, smallOverlayPixels, "png");

        imageProcessingConfig = mock(ImageProcessingConfig.class);
        when(imageProcessingConfig.getChunkSize()).thenReturn(10000);
        imageOverlayService = new ImageOverlayService(executor, imageProcessingConfig);
    }

    @Test
    void overlayImage_Success() {
        ImageData result = imageOverlayService.overlayImage(baseImage, overlayImage, 0.5f);

        assertNotNull(result);
        assertEquals(100, result.width());
        assertEquals(100, result.height());
    }

    @Test
    void overlayImage_NullBaseImage() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            imageOverlayService.overlayImage(null, overlayImage, 0.5f);
        });
        assertEquals("Base and overlay images are required", exception.getMessage());
    }

    @Test
    void overlayImage_NullOverlayImage() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            imageOverlayService.overlayImage(baseImage, null, 0.5f);
        });
        assertEquals("Base and overlay images are required", exception.getMessage());
    }

    @Test
    void overlayImage_LargerOverlayGetsScaled() {
        // Create a larger overlay image (200x200)
        int[] largeOverlayPixels = new int[40000];
        ImageData largeOverlay = new ImageData(200, 200, largeOverlayPixels, "png");

        ImageData result = imageOverlayService.overlayImage(baseImage, largeOverlay, 0.5f);

        assertNotNull(result);
        assertEquals(100, result.width());
        assertEquals(100, result.height());
    }

    @Test
    void applyWatermark_BottomRight() {
        ImageData result = imageOverlayService.applyWatermark(baseImage, smallOverlayImage, WatermarkPosition.BOTTOM_RIGHT, 0.5f, 10);

        assertNotNull(result);
        assertEquals(100, result.width());
        assertEquals(100, result.height());
    }

    @Test
    void applyWatermark_TopLeft() {
        ImageData result = imageOverlayService.applyWatermark(baseImage, smallOverlayImage, WatermarkPosition.TOP_LEFT, 0.5f, 10);

        assertNotNull(result);
        assertEquals(100, result.width());
        assertEquals(100, result.height());
    }

    @Test
    void applyWatermark_TopRight() {
        ImageData result = imageOverlayService.applyWatermark(baseImage, smallOverlayImage, WatermarkPosition.TOP_RIGHT, 0.5f, 10);

        assertNotNull(result);
        assertEquals(100, result.width());
        assertEquals(100, result.height());
    }

    @Test
    void applyWatermark_BottomLeft() {
        ImageData result = imageOverlayService.applyWatermark(baseImage, smallOverlayImage, WatermarkPosition.BOTTOM_LEFT, 0.5f, 10);

        assertNotNull(result);
        assertEquals(100, result.width());
        assertEquals(100, result.height());
    }

    @Test
    void applyWatermark_Center() {
        ImageData result = imageOverlayService.applyWatermark(baseImage, smallOverlayImage, WatermarkPosition.CENTER, 0.5f, 10);

        assertNotNull(result);
        assertEquals(100, result.width());
        assertEquals(100, result.height());
    }

    @Test
    void applyWatermark_DefaultPadding() {
        ImageData result = imageOverlayService.applyWatermark(baseImage, smallOverlayImage, WatermarkPosition.BOTTOM_RIGHT, 0.5f);

        assertNotNull(result);
        assertEquals(100, result.width());
        assertEquals(100, result.height());
    }

    @Test
    void applyWatermark_NullBaseImage() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            imageOverlayService.applyWatermark(null, smallOverlayImage, WatermarkPosition.BOTTOM_RIGHT, 0.5f, 10);
        });
        assertEquals("Base image and watermark are required", exception.getMessage());
    }

    @Test
    void applyWatermark_NullWatermarkImage() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            imageOverlayService.applyWatermark(baseImage, null, WatermarkPosition.BOTTOM_RIGHT, 0.5f, 10);
        });
        assertEquals("Base image and watermark are required", exception.getMessage());
    }

    @Test
    void applyWatermark_LargeWatermarkGetsScaled() {
        // Create a watermark larger than 50% of base
        int[] largeWatermarkPixels = new int[10000];
        ImageData largeWatermark = new ImageData(100, 100, largeWatermarkPixels, "png");

        ImageData result = imageOverlayService.applyWatermark(baseImage, largeWatermark, WatermarkPosition.BOTTOM_RIGHT, 0.5f, 10);

        assertNotNull(result);
        assertEquals(100, result.width());
        assertEquals(100, result.height());
    }

    @Test
    void scaleToFit_SmallerImage() {
        ImageData result = imageOverlayService.scaleToFit(smallOverlayImage, 200, 200);

        // Image is already smaller, should return same dimensions
        assertEquals(50, result.width());
        assertEquals(50, result.height());
    }

    @Test
    void scaleToFit_LargerImage() {
        ImageData result = imageOverlayService.scaleToFit(overlayImage, 50, 50);

        // Image should be scaled down to fit
        assertTrue(result.width() <= 50);
        assertTrue(result.height() <= 50);
    }
}
