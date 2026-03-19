package com.imageservice.controller;

import com.imageservice.model.ImageData;
import com.imageservice.service.ImageCodecService;
import com.imageservice.service.ImageCompressService;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class ImageCompressControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ImageCodecService imageCodecService;

    @MockBean
    private ImageCompressService imageCompressService;

    @MockBean
    private ImageStorageService imageStorageService;

    @MockBean
    private ImageValidationService imageValidationService;

    private MockMultipartFile testFile;
    private ImageData testImageData;
    private byte[] testBytes;

    @BeforeEach
    void setUp() {
        testBytes = new byte[]{0x01, 0x02, 0x03};
        testFile = new MockMultipartFile("file", "test.png", MediaType.IMAGE_PNG_VALUE, testBytes);
        testImageData = new ImageData(100, 100, new int[10000], "png");
    }

    @Test
    void compressImageUpload_Success() throws Exception {
        when(imageCodecService.decodeImage(any(), anyString())).thenReturn(testImageData);
        when(imageCompressService.compressImage(any(), anyLong(), anyString())).thenReturn(testImageData);
        when(imageCodecService.encodeImage(any(), anyString())).thenReturn(testBytes);
        when(imageStorageService.saveImage(any(), any(), any(), any(), any(), any())).thenReturn(null);

        mockMvc.perform(multipart("/api/images/compress/upload")
                .file(testFile)
                .param("targetSizeKB", "50")
                .param("format", "png"))
            .andExpect(status().isOk())
            .andExpect(header().exists("Content-Disposition"))
            .andExpect(content().contentType(MediaType.APPLICATION_OCTET_STREAM));
    }

    @Test
    void compressImageUpload_MissingTargetSizeKB() throws Exception {
        mockMvc.perform(multipart("/api/images/compress/upload")
                .file(testFile)
                .param("format", "png"))
            .andExpect(status().isBadRequest());
    }

    @Test
    void compressImageUpload_InvalidTargetSizeKB() throws Exception {
        mockMvc.perform(multipart("/api/images/compress/upload")
                .file(testFile)
                .param("targetSizeKB", "0")
                .param("format", "png"))
            .andExpect(status().isBadRequest());
    }

    @Test
    void compressImageUpload_TargetSizeKBTooLarge() throws Exception {
        mockMvc.perform(multipart("/api/images/compress/upload")
                .file(testFile)
                .param("targetSizeKB", "60000")
                .param("format", "png"))
            .andExpect(status().isBadRequest());
    }

    @Test
    void compressImageFromUrl_InvalidUrl() throws Exception {
        mockMvc.perform(post("/api/images/compress/url")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .param("imageUrl", "not-a-valid-url")
                .param("targetSizeKB", "50")
                .param("format", "png"))
            .andExpect(status().isInternalServerError());
    }

    @Test
    void compressImageFromUrl_MissingTargetSizeKB() throws Exception {
        mockMvc.perform(post("/api/images/compress/url")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .param("imageUrl", "http://example.com/image.png")
                .param("format", "png"))
            .andExpect(status().isBadRequest());
    }
}
