package com.imageservice.service;

import com.imageservice.config.ImageProcessingConfig;
import com.imageservice.model.ImageData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ImageConvertServiceTest {

    private ImageProcessingConfig imageProcessingConfig;
    private ImageCodecService imageCodecService;
    private ImageConvertService imageConvertService;

    private Executor executor;
    private ImageData testImageData;
    private ImageData transparentImageData;

    @BeforeEach
    void setUp() {
        executor = Executors.newFixedThreadPool(2);
        int[] pixels = new int[10000];
        testImageData = new ImageData(100, 100, pixels, "png");
        
        // Create image with some transparent pixels
        int[] transparentPixels = new int[10000];
        transparentPixels[0] = 0x80FF0000; // Semi-transparent red
        transparentPixels[1] = 0x00FFFFFF; // Fully transparent
        transparentImageData = new ImageData(100, 100, transparentPixels, "png");
        
        imageProcessingConfig = mock(ImageProcessingConfig.class);
        imageCodecService = mock(ImageCodecService.class);
        when(imageProcessingConfig.getChunkSize()).thenReturn(10000);
        imageConvertService = new ImageConvertService(executor, imageProcessingConfig, imageCodecService);
    }

    @Test
    void convertImage_PngToPng() {
        ImageData result = imageConvertService.convertImage(testImageData, "png");

        assertNotNull(result);
        assertEquals(100, result.width());
        assertEquals(100, result.height());
    }

    @Test
    void convertImage_PngToJpg_FlattensAlpha() {
        ImageData result = imageConvertService.convertImage(transparentImageData, "jpg");

        assertNotNull(result);
        assertEquals(100, result.width());
        assertEquals(100, result.height());
        // All pixels should be opaque after flattening
        for (int pixel : result.pixels()) {
            assertEquals(255, (pixel >> 24) & 0xFF, "All pixels should be opaque after flattening for JPG");
        }
    }

    @Test
    void convertImage_NullSourceImage() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            imageConvertService.convertImage(null, "png");
        });
        assertEquals("Source image is required", exception.getMessage());
    }

    @Test
    void convertImage_UnsupportedFormat() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            imageConvertService.convertImage(testImageData, "tiff");
        });
        assertTrue(exception.getMessage().contains("Unsupported target format"));
    }

    @Test
    void isSupportedFormat_ValidFormats() {
        assertTrue(imageConvertService.isSupportedFormat("png"));
        assertTrue(imageConvertService.isSupportedFormat("jpg"));
        assertTrue(imageConvertService.isSupportedFormat("jpeg"));
        assertTrue(imageConvertService.isSupportedFormat("webp"));
        assertTrue(imageConvertService.isSupportedFormat("avif"));
        assertTrue(imageConvertService.isSupportedFormat("gif"));
        assertTrue(imageConvertService.isSupportedFormat("bmp"));
    }

    @Test
    void isSupportedFormat_InvalidFormats() {
        assertFalse(imageConvertService.isSupportedFormat("tiff"));
        assertFalse(imageConvertService.isSupportedFormat("svg"));
        assertFalse(imageConvertService.isSupportedFormat(null));
    }

    @Test
    void isSupportedFormat_EmptyStringDefaultsToPng() {
        // Empty string normalizes to "png" which is supported
        assertTrue(imageConvertService.isSupportedFormat(""));
    }

    @Test
    void getSupportedFormats() {
        Set<String> formats = imageConvertService.getSupportedFormats();

        assertNotNull(formats);
        assertTrue(formats.contains("png"));
        assertTrue(formats.contains("jpg"));
        assertTrue(formats.contains("webp"));
        assertTrue(formats.contains("avif"));
        assertTrue(formats.contains("gif"));
        assertTrue(formats.contains("bmp"));
    }

    @Test
    void convertImage_WebpOutput() {
        ImageData result = imageConvertService.convertImage(testImageData, "webp");

        assertNotNull(result);
        assertEquals(100, result.width());
        assertEquals(100, result.height());
    }

    @Test
    void convertImage_AvifOutput() {
        ImageData result = imageConvertService.convertImage(testImageData, "avif");

        assertNotNull(result);
        assertEquals(100, result.width());
        assertEquals(100, result.height());
    }

    @Test
    void convertImage_GifOutput() {
        ImageData result = imageConvertService.convertImage(testImageData, "gif");

        assertNotNull(result);
        assertEquals(100, result.width());
        assertEquals(100, result.height());
    }

    @Test
    void convertImage_BmpOutput() {
        ImageData result = imageConvertService.convertImage(testImageData, "bmp");

        assertNotNull(result);
        assertEquals(100, result.width());
        assertEquals(100, result.height());
    }
}
