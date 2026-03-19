package com.imageservice.service;

import com.imageservice.config.ImageProcessingConfig;
import com.imageservice.model.FlipDirection;
import com.imageservice.model.ImageData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ImageFlipServiceTest {

    private ImageProcessingConfig imageProcessingConfig;
    private ImageFlipService imageFlipService;

    private Executor executor;
    private ImageData testImageData;

    @BeforeEach
    void setUp() {
        executor = Executors.newFixedThreadPool(2);
        
        // Create test image with identifiable pixels
        int[] pixels = new int[9]; // 3x3 image
        pixels[0] = 0xFFFF0000; // Red (0,0)
        pixels[1] = 0xFF00FF00; // Green (1,0)
        pixels[2] = 0xFF0000FF; // Blue (2,0)
        pixels[3] = 0xFFFFFF00; // Yellow (0,1)
        pixels[4] = 0xFFFFFFFF; // White (1,1) - center
        pixels[5] = 0xFF000000; // Black (2,1)
        pixels[6] = 0xFFFF00FF; // Magenta (0,2)
        pixels[7] = 0xFF00FFFF; // Cyan (1,2)
        pixels[8] = 0xFF808080; // Gray (2,2)
        
        testImageData = new ImageData(3, 3, pixels, "png");
        
        imageProcessingConfig = mock(ImageProcessingConfig.class);
        when(imageProcessingConfig.getChunkSize()).thenReturn(10000);
        imageFlipService = new ImageFlipService(executor, imageProcessingConfig);
    }

    @Test
    void flipImage_Horizontal() {
        ImageData result = imageFlipService.flipImage(testImageData, FlipDirection.HORIZONTAL);

        assertNotNull(result);
        assertEquals(3, result.width());
        assertEquals(3, result.height());
        
        // Check first row is reversed: Blue, Green, Red
        assertEquals(0xFF0000FF, result.pixels()[0]); // Was at (2,0)
        assertEquals(0xFF00FF00, result.pixels()[1]); // Was at (1,0)
        assertEquals(0xFFFF0000, result.pixels()[2]); // Was at (0,0)
    }

    @Test
    void flipImage_Vertical() {
        ImageData result = imageFlipService.flipImage(testImageData, FlipDirection.VERTICAL);

        assertNotNull(result);
        assertEquals(3, result.width());
        assertEquals(3, result.height());
        
        // Check first row is now the last row: Magenta, Cyan, Gray
        assertEquals(0xFFFF00FF, result.pixels()[0]); // Was at (0,2)
        assertEquals(0xFF00FFFF, result.pixels()[1]); // Was at (1,2)
        assertEquals(0xFF808080, result.pixels()[2]); // Was at (2,2)
    }

    @Test
    void flipImage_Both() {
        ImageData result = imageFlipService.flipImage(testImageData, FlipDirection.BOTH);

        assertNotNull(result);
        assertEquals(3, result.width());
        assertEquals(3, result.height());
        
        // Check first pixel is now the last: Gray
        assertEquals(0xFF808080, result.pixels()[0]); // Was at (2,2)
        assertEquals(0xFF00FFFF, result.pixels()[1]); // Was at (1,2)
        assertEquals(0xFFFF00FF, result.pixels()[2]); // Was at (0,2)
    }

    @Test
    void flipImage_NullSourceImage() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            imageFlipService.flipImage(null, FlipDirection.HORIZONTAL);
        });
        assertEquals("Source image is required", exception.getMessage());
    }

    @Test
    void flipImage_NullDirectionDefaultsToHorizontal() {
        ImageData result = imageFlipService.flipImage(testImageData, null);

        assertNotNull(result);
        assertEquals(3, result.width());
        assertEquals(3, result.height());
    }

    @Test
    void flipHorizontal_ConvenienceMethod() {
        ImageData result = imageFlipService.flipHorizontal(testImageData);

        assertNotNull(result);
        assertEquals(3, result.width());
        assertEquals(3, result.height());
        
        // First row should be reversed
        assertEquals(0xFF0000FF, result.pixels()[0]);
        assertEquals(0xFF00FF00, result.pixels()[1]);
        assertEquals(0xFFFF0000, result.pixels()[2]);
    }

    @Test
    void flipVertical_ConvenienceMethod() {
        ImageData result = imageFlipService.flipVertical(testImageData);

        assertNotNull(result);
        assertEquals(3, result.width());
        assertEquals(3, result.height());
        
        // First row should be the last original row
        assertEquals(0xFFFF00FF, result.pixels()[0]);
        assertEquals(0xFF00FFFF, result.pixels()[1]);
        assertEquals(0xFF808080, result.pixels()[2]);
    }

    @Test
    void flipImage_PreservesFormat() {
        ImageData result = imageFlipService.flipImage(testImageData, FlipDirection.HORIZONTAL);

        assertEquals("png", result.format());
    }

    @Test
    void flipImage_LargerImage() {
        // Test with a larger image to exercise parallel processing
        int[] largePixels = new int[10000];
        for (int i = 0; i < 10000; i++) {
            largePixels[i] = i;
        }
        ImageData largeImage = new ImageData(100, 100, largePixels, "png");

        ImageData result = imageFlipService.flipImage(largeImage, FlipDirection.HORIZONTAL);

        assertNotNull(result);
        assertEquals(100, result.width());
        assertEquals(100, result.height());
    }
}
