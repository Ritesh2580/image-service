package com.imageservice.controller;

import com.imageservice.model.ImageData;
import com.imageservice.service.ImageCodecService;
import com.imageservice.service.ImageStorageService;
import com.imageservice.service.ImageTextService;
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

import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class ImageTextControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ImageCodecService imageCodecService;

    @MockBean
    private ImageTextService imageTextService;

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
    void getAvailableFonts_Success() throws Exception {
        when(imageTextService.getAvailableFonts()).thenReturn(List.of("Arial", "SansSerif", "Times New Roman"));

        mockMvc.perform(get("/api/images/text/fonts"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isArray())
            .andExpect(jsonPath("$[0]").value("Arial"));
    }

    @Test
    void addTextUpload_Success() throws Exception {
        when(imageCodecService.decodeImage(any(), anyString())).thenReturn(testImageData);
        when(imageTextService.addText(any(), any())).thenReturn(testImageData);
        when(imageCodecService.encodeImage(any(), anyString())).thenReturn(testBytes);
        when(imageStorageService.saveImage(any(), any(), any(), any(), any(), any())).thenReturn(null);

        mockMvc.perform(multipart("/api/images/text/upload")
                .file(testFile)
                .param("text", "Hello World")
                .param("fontFamily", "Arial")
                .param("fontSize", "24")
                .param("color", "#FF0000")
                .param("x", "100")
                .param("y", "50")
                .param("alphaPercent", "80")
                .param("format", "png"))
            .andExpect(status().isOk())
            .andExpect(header().exists("Content-Disposition"))
            .andExpect(content().contentType(MediaType.APPLICATION_OCTET_STREAM));
    }

    @Test
    void addTextUpload_MinimalParams() throws Exception {
        when(imageCodecService.decodeImage(any(), anyString())).thenReturn(testImageData);
        when(imageTextService.addText(any(), any())).thenReturn(testImageData);
        when(imageCodecService.encodeImage(any(), anyString())).thenReturn(testBytes);
        when(imageStorageService.saveImage(any(), any(), any(), any(), any(), any())).thenReturn(null);

        mockMvc.perform(multipart("/api/images/text/upload")
                .file(testFile)
                .param("text", "Test")
                .param("x", "10")
                .param("y", "10"))
            .andExpect(status().isOk());
    }

    @Test
    void addTextUpload_MissingText() throws Exception {
        mockMvc.perform(multipart("/api/images/text/upload")
                .file(testFile)
                .param("x", "100")
                .param("y", "50"))
            .andExpect(status().isBadRequest());
    }

    @Test
    void addTextUpload_MissingX() throws Exception {
        mockMvc.perform(multipart("/api/images/text/upload")
                .file(testFile)
                .param("text", "Hello")
                .param("y", "50"))
            .andExpect(status().isBadRequest());
    }

    @Test
    void addTextUpload_InvalidFontSize() throws Exception {
        mockMvc.perform(multipart("/api/images/text/upload")
                .file(testFile)
                .param("text", "Hello")
                .param("x", "100")
                .param("y", "50")
                .param("fontSize", "300"))
            .andExpect(status().isBadRequest());
    }

    @Test
    void addTextUpload_InvalidAlphaPercent() throws Exception {
        mockMvc.perform(multipart("/api/images/text/upload")
                .file(testFile)
                .param("text", "Hello")
                .param("x", "100")
                .param("y", "50")
                .param("alphaPercent", "150"))
            .andExpect(status().isBadRequest());
    }

    @Test
    void addTextFromUrl_MissingText() throws Exception {
        mockMvc.perform(post("/api/images/text/url")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .param("imageUrl", "http://example.com/image.png")
                .param("x", "200")
                .param("y", "100")
                .param("format", "png"))
            .andExpect(status().isBadRequest());
    }

    @Test
    void getRecommendedFontSize_Success() throws Exception {
        when(imageTextService.getRecommendedFontSize(1920, 1080)).thenReturn(64);

        mockMvc.perform(get("/api/images/text/recommended-size")
                .param("width", "1920")
                .param("height", "1080"))
            .andExpect(status().isOk())
            .andExpect(content().string("64"));
    }
}
