package com.imageservice.service;

import com.imageservice.config.ImageProcessingConfig;
import com.imageservice.model.ImageData;
import com.imageservice.model.TextWatermarkRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ImageTextServiceTest {

    private ImageProcessingConfig imageProcessingConfig;
    private ImageTextService imageTextService;

    private Executor executor;
    private ImageData testImageData;

    @BeforeEach
    void setUp() {
        executor = Executors.newFixedThreadPool(2);
        int[] pixels = new int[10000];
        testImageData = new ImageData(100, 100, pixels, "png");
        
        imageProcessingConfig = mock(ImageProcessingConfig.class);
        imageTextService = new ImageTextService(executor, imageProcessingConfig);
    }

    @Test
    void getAvailableFonts_ReturnsFonts() {
        List<String> fonts = imageTextService.getAvailableFonts();

        assertNotNull(fonts);
        assertFalse(fonts.isEmpty());
        // Should contain at least some standard fonts
        assertTrue(fonts.contains("SansSerif") || fonts.contains("Dialog"));
    }

    @Test
    void getAvailableFonts_Cached() {
        List<String> fonts1 = imageTextService.getAvailableFonts();
        List<String> fonts2 = imageTextService.getAvailableFonts();

        // Should return the same list (cached)
        assertSame(fonts1, fonts2);
    }

    @Test
    void addText_Success() {
        TextWatermarkRequest request = new TextWatermarkRequest(
            "Hello World", "SansSerif", 24, "white", 10, 10, 100
        );

        ImageData result = imageTextService.addText(testImageData, request);

        assertNotNull(result);
        assertEquals(100, result.width());
        assertEquals(100, result.height());
    }

    @Test
    void addText_NullSourceImage() {
        TextWatermarkRequest request = new TextWatermarkRequest(
            "Hello", "SansSerif", 24, "white", 10, 10, 100
        );

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            imageTextService.addText(null, request);
        });
        assertEquals("Source image is required", exception.getMessage());
    }

    @Test
    void addText_NullRequest() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            imageTextService.addText(testImageData, null);
        });
        assertEquals("Text is required", exception.getMessage());
    }

    @Test
    void addText_EmptyText() {
        TextWatermarkRequest request = new TextWatermarkRequest(
            "", "SansSerif", 24, "white", 10, 10, 100
        );

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            imageTextService.addText(testImageData, request);
        });
        assertEquals("Text is required", exception.getMessage());
    }

    @Test
    void addText_BlankText() {
        TextWatermarkRequest request = new TextWatermarkRequest(
            "   ", "SansSerif", 24, "white", 10, 10, 100
        );

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            imageTextService.addText(testImageData, request);
        });
        assertEquals("Text is required", exception.getMessage());
    }

    @Test
    void addText_NamedColorWhite() {
        TextWatermarkRequest request = new TextWatermarkRequest(
            "Test", "SansSerif", 24, "white", 10, 10, 100
        );

        ImageData result = imageTextService.addText(testImageData, request);
        assertNotNull(result);
    }

    @Test
    void addText_NamedColorRed() {
        TextWatermarkRequest request = new TextWatermarkRequest(
            "Test", "SansSerif", 24, "red", 10, 10, 100
        );

        ImageData result = imageTextService.addText(testImageData, request);
        assertNotNull(result);
    }

    @Test
    void addText_HexColor() {
        TextWatermarkRequest request = new TextWatermarkRequest(
            "Test", "SansSerif", 24, "#FF0000", 10, 10, 100
        );

        ImageData result = imageTextService.addText(testImageData, request);
        assertNotNull(result);
    }

    @Test
    void addText_ShortHexColor() {
        TextWatermarkRequest request = new TextWatermarkRequest(
            "Test", "SansSerif", 24, "#F00", 10, 10, 100
        );

        ImageData result = imageTextService.addText(testImageData, request);
        assertNotNull(result);
    }

    @Test
    void addText_WithTransparency() {
        TextWatermarkRequest request = new TextWatermarkRequest(
            "Test", "SansSerif", 24, "white", 10, 10, 50
        );

        ImageData result = imageTextService.addText(testImageData, request);
        assertNotNull(result);
    }

    @Test
    void addText_LongTextWrapping() {
        TextWatermarkRequest request = new TextWatermarkRequest(
            "This is a very long text that should wrap to multiple lines when rendered on the image", 
            "SansSerif", 24, "white", 10, 10, 100
        );

        ImageData result = imageTextService.addText(testImageData, request);
        assertNotNull(result);
    }

    @Test
    void getRecommendedFontSize() {
        int recommended = imageTextService.getRecommendedFontSize(1920, 1080);

        assertTrue(recommended >= 12);
        assertTrue(recommended <= 72);
    }

    @Test
    void getRecommendedFontSize_SmallImage() {
        int recommended = imageTextService.getRecommendedFontSize(100, 100);

        assertTrue(recommended >= 12);
        assertTrue(recommended <= 72);
    }

    @Test
    void getRecommendedFontSize_LargeImage() {
        int recommended = imageTextService.getRecommendedFontSize(4000, 3000);

        assertTrue(recommended >= 12);
        assertTrue(recommended <= 72);
    }
}
