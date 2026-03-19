package com.imageservice.controller;

import com.imageservice.model.FlipDirection;
import com.imageservice.model.ImageData;
import com.imageservice.service.ImageCodecService;
import com.imageservice.service.ImageFlipService;
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
class ImageFlipControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ImageCodecService imageCodecService;

    @MockBean
    private ImageFlipService imageFlipService;

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
    void flipImageUpload_Horizontal_Success() throws Exception {
        when(imageCodecService.decodeImage(any(), anyString())).thenReturn(testImageData);
        when(imageFlipService.flipImage(any(), eq(FlipDirection.HORIZONTAL))).thenReturn(testImageData);
        when(imageCodecService.encodeImage(any(), anyString())).thenReturn(testBytes);
        when(imageStorageService.saveImage(any(), any(), any(), any(), any(), any())).thenReturn(null);

        mockMvc.perform(multipart("/api/images/flip/upload")
                .file(testFile)
                .param("direction", "HORIZONTAL")
                .param("format", "png"))
            .andExpect(status().isOk())
            .andExpect(header().exists("Content-Disposition"))
            .andExpect(content().contentType(MediaType.APPLICATION_OCTET_STREAM));
    }

    @Test
    void flipImageUpload_Vertical_Success() throws Exception {
        when(imageCodecService.decodeImage(any(), anyString())).thenReturn(testImageData);
        when(imageFlipService.flipImage(any(), eq(FlipDirection.VERTICAL))).thenReturn(testImageData);
        when(imageCodecService.encodeImage(any(), anyString())).thenReturn(testBytes);
        when(imageStorageService.saveImage(any(), any(), any(), any(), any(), any())).thenReturn(null);

        mockMvc.perform(multipart("/api/images/flip/upload")
                .file(testFile)
                .param("direction", "VERTICAL")
                .param("format", "png"))
            .andExpect(status().isOk())
            .andExpect(header().exists("Content-Disposition"));
    }

    @Test
    void flipImageUpload_Both_Success() throws Exception {
        when(imageCodecService.decodeImage(any(), anyString())).thenReturn(testImageData);
        when(imageFlipService.flipImage(any(), eq(FlipDirection.BOTH))).thenReturn(testImageData);
        when(imageCodecService.encodeImage(any(), anyString())).thenReturn(testBytes);
        when(imageStorageService.saveImage(any(), any(), any(), any(), any(), any())).thenReturn(null);

        mockMvc.perform(multipart("/api/images/flip/upload")
                .file(testFile)
                .param("direction", "BOTH")
                .param("format", "png"))
            .andExpect(status().isOk())
            .andExpect(header().exists("Content-Disposition"));
    }

    @Test
    void flipImageUpload_MissingDirection() throws Exception {
        mockMvc.perform(multipart("/api/images/flip/upload")
                .file(testFile)
                .param("format", "png"))
            .andExpect(status().isBadRequest());
    }

    @Test
    void flipImageUpload_InvalidDirection() throws Exception {
        mockMvc.perform(multipart("/api/images/flip/upload")
                .file(testFile)
                .param("direction", "DIAGONAL")
                .param("format", "png"))
            .andExpect(status().isBadRequest());
    }

    @Test
    void flipImageFromUrl_MissingDirection() throws Exception {
        mockMvc.perform(post("/api/images/flip/url")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .param("imageUrl", "http://example.com/image.png")
                .param("format", "png"))
            .andExpect(status().isBadRequest());
    }

    @Test
    void getFlipDirections_Success() throws Exception {
        mockMvc.perform(get("/api/images/flip/directions"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isArray())
            .andExpect(jsonPath("$[0]").value("HORIZONTAL"))
            .andExpect(jsonPath("$[1]").value("VERTICAL"))
            .andExpect(jsonPath("$[2]").value("BOTH"));
    }
}
