package com.imageservice.controller;

import com.imageservice.model.ImageData;
import com.imageservice.model.WatermarkPosition;
import com.imageservice.service.ImageCodecService;
import com.imageservice.service.ImageOverlayService;
import com.imageservice.service.ImageStorageService;
import com.imageservice.service.ImageValidationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class ImageOverlayControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ImageCodecService imageCodecService;

    @MockBean
    private ImageOverlayService imageOverlayService;

    @MockBean
    private ImageStorageService imageStorageService;

    @MockBean
    private ImageValidationService imageValidationService;

    private MockMultipartFile baseFile;
    private MockMultipartFile overlayFile;
    private ImageData testImageData;
    private byte[] testBytes;

    @BeforeEach
    void setUp() {
        testBytes = new byte[]{0x01, 0x02, 0x03};
        baseFile = new MockMultipartFile("baseFile", "base.png", MediaType.IMAGE_PNG_VALUE, testBytes);
        overlayFile = new MockMultipartFile("overlayFile", "overlay.png", MediaType.IMAGE_PNG_VALUE, testBytes);
        testImageData = new ImageData(100, 100, new int[10000], "png");
    }

    @Test
    void overlayImageUpload_Success() throws Exception {
        when(imageCodecService.decodeImage(any(), anyString())).thenReturn(testImageData);
        when(imageOverlayService.overlayImage(any(), any(), anyFloat())).thenReturn(testImageData);
        when(imageCodecService.encodeImage(any(), anyString())).thenReturn(testBytes);
        when(imageStorageService.saveImage(any(), any(), any(), any(), any(), any())).thenReturn(null);

        mockMvc.perform(multipart("/api/images/overlay/upload")
                .file(baseFile)
                .file(overlayFile)
                .param("alphaPercent", "50")
                .param("format", "png"))
            .andExpect(status().isOk())
            .andExpect(header().exists("Content-Disposition"))
            .andExpect(content().contentType(MediaType.APPLICATION_OCTET_STREAM));
    }

    @Test
    void overlayImageUpload_MissingAlphaPercent() throws Exception {
        mockMvc.perform(multipart("/api/images/overlay/upload")
                .file(baseFile)
                .file(overlayFile)
                .param("format", "png"))
            .andExpect(status().isBadRequest());
    }

    @Test
    void overlayImageUpload_InvalidAlphaPercent() throws Exception {
        mockMvc.perform(multipart("/api/images/overlay/upload")
                .file(baseFile)
                .file(overlayFile)
                .param("alphaPercent", "150")
                .param("format", "png"))
            .andExpect(status().isBadRequest());
    }

    @Test
    void watermarkImageUpload_Success() throws Exception {
        MockMultipartFile imageFile = new MockMultipartFile("imageFile", "image.png", MediaType.IMAGE_PNG_VALUE, testBytes);
        MockMultipartFile watermarkFile = new MockMultipartFile("watermarkFile", "watermark.png", MediaType.IMAGE_PNG_VALUE, testBytes);

        when(imageCodecService.decodeImage(any(), anyString())).thenReturn(testImageData);
        when(imageOverlayService.applyWatermark(any(), any(), any(), anyFloat(), anyInt())).thenReturn(testImageData);
        when(imageCodecService.encodeImage(any(), anyString())).thenReturn(testBytes);
        when(imageStorageService.saveImage(any(), any(), any(), any(), any(), any())).thenReturn(null);

        mockMvc.perform(multipart("/api/images/watermark/upload")
                .file(imageFile)
                .file(watermarkFile)
                .param("position", "BOTTOM_RIGHT")
                .param("alphaPercent", "80")
                .param("padding", "10")
                .param("format", "png"))
            .andExpect(status().isOk())
            .andExpect(header().exists("Content-Disposition"))
            .andExpect(content().contentType(MediaType.APPLICATION_OCTET_STREAM));
    }

    @Test
    void watermarkImageUpload_DefaultPadding() throws Exception {
        MockMultipartFile imageFile = new MockMultipartFile("imageFile", "image.png", MediaType.IMAGE_PNG_VALUE, testBytes);
        MockMultipartFile watermarkFile = new MockMultipartFile("watermarkFile", "watermark.png", MediaType.IMAGE_PNG_VALUE, testBytes);

        when(imageCodecService.decodeImage(any(), anyString())).thenReturn(testImageData);
        when(imageOverlayService.applyWatermark(any(), any(), any(), anyFloat(), anyInt())).thenReturn(testImageData);
        when(imageCodecService.encodeImage(any(), anyString())).thenReturn(testBytes);
        when(imageStorageService.saveImage(any(), any(), any(), any(), any(), any())).thenReturn(null);

        mockMvc.perform(multipart("/api/images/watermark/upload")
                .file(imageFile)
                .file(watermarkFile)
                .param("position", "TOP_LEFT")
                .param("alphaPercent", "50")
                .param("format", "png"))
            .andExpect(status().isOk());
    }

    @Test
    void watermarkImageUpload_InvalidPosition() throws Exception {
        MockMultipartFile imageFile = new MockMultipartFile("imageFile", "image.png", MediaType.IMAGE_PNG_VALUE, testBytes);
        MockMultipartFile watermarkFile = new MockMultipartFile("watermarkFile", "watermark.png", MediaType.IMAGE_PNG_VALUE, testBytes);

        mockMvc.perform(multipart("/api/images/watermark/upload")
                .file(imageFile)
                .file(watermarkFile)
                .param("position", "INVALID_POSITION")
                .param("alphaPercent", "50")
                .param("format", "png"))
            .andExpect(status().isBadRequest());
    }

    @Test
    void watermarkImageFromUrl_MissingPosition() throws Exception {
        mockMvc.perform(post("/api/images/watermark/url")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .param("imageUrl", "http://example.com/image.png")
                .param("watermarkUrl", "http://example.com/watermark.png")
                .param("alphaPercent", "60")
                .param("format", "png"))
            .andExpect(status().isBadRequest());
    }

    @Test
    void getWatermarkPositions_Success() throws Exception {
        mockMvc.perform(get("/api/images/watermark/positions"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isArray())
            .andExpect(jsonPath("$[0]").exists());
    }
}
