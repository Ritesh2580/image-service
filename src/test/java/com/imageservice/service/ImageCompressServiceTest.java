package com.imageservice.service;

import com.imageservice.config.ImageProcessingConfig;
import com.imageservice.model.ImageData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class ImageCompressServiceTest {

    private ImageProcessingConfig imageProcessingConfig;
    private ImageCodecService imageCodecService;
    private ImageCompressService imageCompressService;

    private Executor executor;
    private ImageData testImageData;
    private byte[] testBytes;

    @BeforeEach
    void setUp() {
        executor = Executors.newFixedThreadPool(2);
        testBytes = new byte[10000];
        int[] pixels = new int[10000];
        testImageData = new ImageData(100, 100, pixels, "png");
        
        imageProcessingConfig = mock(ImageProcessingConfig.class);
        imageCodecService = mock(ImageCodecService.class);
        when(imageProcessingConfig.getChunkSize()).thenReturn(10000);
        imageCompressService = new ImageCompressService(executor, imageProcessingConfig, imageCodecService);
    }

    @Test
    void compressImage_AlreadyFitsTargetSize() throws IOException {
        // Image already smaller than target
        when(imageCodecService.encodeImage(any(), anyString())).thenReturn(new byte[100]);

        ImageData result = imageCompressService.compressImage(testImageData, 10000L, "png");

        assertNotNull(result);
        assertEquals(100, result.width());
        assertEquals(100, result.height());
    }

    @Test
    void compressImage_NullSourceImage() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            imageCompressService.compressImage(null, 5000L, "png");
        });
        assertEquals("Source image is required", exception.getMessage());
    }

    @Test
    void compressImage_InvalidTargetSize() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            imageCompressService.compressImage(testImageData, 0L, "png");
        });
        assertEquals("Target size must be positive", exception.getMessage());
    }

    @Test
    void compressImage_NegativeTargetSize() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            imageCompressService.compressImage(testImageData, -100L, "png");
        });
        assertEquals("Target size must be positive", exception.getMessage());
    }

    @Test
    void compressImage_LossyFormat() throws IOException {
        // For JPG format, should try quality reduction first
        when(imageCodecService.encodeImage(any(), anyString())).thenReturn(new byte[5000]);
        when(imageCodecService.encodeImageWithQuality(any(), anyString(), anyFloat())).thenReturn(new byte[5000]);

        ImageData result = imageCompressService.compressImage(testImageData, 10000L, "jpg");

        assertNotNull(result);
    }

    @Test
    void compressImage_LosslessFormat() throws IOException {
        // For PNG format, should use dimension scaling
        when(imageCodecService.encodeImage(any(), anyString())).thenReturn(new byte[5000]);

        ImageData result = imageCompressService.compressImage(testImageData, 10000L, "png");

        assertNotNull(result);
    }

    @Test
    void compressImage_WebpFormat() throws IOException {
        // WEBP is lossy, should try quality reduction
        when(imageCodecService.encodeImage(any(), anyString())).thenReturn(new byte[5000]);
        when(imageCodecService.encodeImageWithQuality(any(), anyString(), anyFloat())).thenReturn(new byte[5000]);

        ImageData result = imageCompressService.compressImage(testImageData, 10000L, "webp");

        assertNotNull(result);
    }

    @Test
    void compressImage_AvifFormat() throws IOException {
        // AVIF is lossy, should try quality reduction
        when(imageCodecService.encodeImage(any(), anyString())).thenReturn(new byte[5000]);
        when(imageCodecService.encodeImageWithQuality(any(), anyString(), anyFloat())).thenReturn(new byte[5000]);

        ImageData result = imageCompressService.compressImage(testImageData, 10000L, "avif");

        assertNotNull(result);
    }
}
