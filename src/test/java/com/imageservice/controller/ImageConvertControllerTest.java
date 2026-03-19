package com.imageservice.controller;

import com.imageservice.model.ImageData;
import com.imageservice.service.ImageCodecService;
import com.imageservice.service.ImageConvertService;
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

import java.util.Set;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class ImageConvertControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ImageCodecService imageCodecService;

    @MockBean
    private ImageConvertService imageConvertService;

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
    void convertImageUpload_Success() throws Exception {
        when(imageCodecService.decodeImage(any(), anyString())).thenReturn(testImageData);
        when(imageConvertService.convertImage(any(), anyString())).thenReturn(testImageData);
        when(imageCodecService.encodeImage(any(), anyString())).thenReturn(testBytes);
        when(imageStorageService.saveImage(any(), any(), any(), any(), any(), any())).thenReturn(null);

        mockMvc.perform(multipart("/api/images/convert/upload")
                .file(testFile)
                .param("targetFormat", "jpg"))
            .andExpect(status().isOk())
            .andExpect(header().exists("Content-Disposition"))
            .andExpect(content().contentType(MediaType.APPLICATION_OCTET_STREAM));
    }

    @Test
    void convertImageUpload_MissingTargetFormat() throws Exception {
        mockMvc.perform(multipart("/api/images/convert/upload")
                .file(testFile))
            .andExpect(status().isBadRequest());
    }

    @Test
    void convertImageUpload_InvalidTargetFormat() throws Exception {
        mockMvc.perform(multipart("/api/images/convert/upload")
                .file(testFile)
                .param("targetFormat", "invalid"))
            .andExpect(status().isBadRequest());
    }

    @Test
    void convertImageUpload_JpegToJpg() throws Exception {
        when(imageCodecService.decodeImage(any(), anyString())).thenReturn(testImageData);
        when(imageConvertService.convertImage(any(), anyString())).thenReturn(testImageData);
        when(imageCodecService.encodeImage(any(), anyString())).thenReturn(testBytes);
        when(imageStorageService.saveImage(any(), any(), any(), any(), any(), any())).thenReturn(null);

        mockMvc.perform(multipart("/api/images/convert/upload")
                .file(testFile)
                .param("targetFormat", "jpeg"))
            .andExpect(status().isOk())
            .andExpect(header().string("Content-Disposition", org.hamcrest.Matchers.containsString(".jpg")));
    }

    @Test
    void convertImageFromUrl_MissingTargetFormat() throws Exception {
        mockMvc.perform(post("/api/images/convert/url")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .param("imageUrl", "http://example.com/image.png"))
            .andExpect(status().isBadRequest());
    }

    @Test
    void getSupportedFormats_Success() throws Exception {
        when(imageConvertService.getSupportedFormats()).thenReturn(Set.of("png", "jpg", "webp"));

        mockMvc.perform(get("/api/images/convert/formats"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isArray())
            .andExpect(jsonPath("$[0]").exists());
    }
}
