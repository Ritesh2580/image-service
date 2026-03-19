package com.imageservice.controller;

import com.imageservice.model.ImageData;
import com.imageservice.model.WatermarkPosition;
import com.imageservice.service.ImageCodecService;
import com.imageservice.service.ImageOverlayService;
import com.imageservice.service.ImageStorageService;
import com.imageservice.service.ImageValidationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
@RequestMapping("/api/images")
@Tag(name = "Image Overlay & Watermark", description = "Operations for overlaying images and applying watermarks")
@Validated
public class ImageOverlayController {

    private final ImageCodecService imageCodecService;
    private final ImageOverlayService imageOverlayService;
    private final ImageStorageService imageStorageService;
    private final ImageValidationService imageValidationService;

    public ImageOverlayController(ImageCodecService imageCodecService,
                                   ImageOverlayService imageOverlayService,
                                   ImageStorageService imageStorageService,
                                   ImageValidationService imageValidationService) {
        this.imageCodecService = imageCodecService;
        this.imageOverlayService = imageOverlayService;
        this.imageStorageService = imageStorageService;
        this.imageValidationService = imageValidationService;
    }

    // ==================== OVERLAY API ====================

    @Operation(summary = "Overlay one image over another (centered)",
               description = "Combines base image with overlay image using alpha blending. " +
                             "Overlay is centered and scaled to fit if larger than base.")
    @PostMapping(value = "/overlay/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<byte[]> overlayImageUpload(
            @Parameter(description = "Base image file") @RequestParam("baseFile") MultipartFile baseFile,
            @Parameter(description = "Image to overlay on base") @RequestParam("overlayFile") MultipartFile overlayFile,
            @Parameter(description = "Alpha blend factor 0-100 (0=base only, 100=overlay only)") @RequestParam("alphaPercent") @NotNull @Min(0) @Max(100) Integer alphaPercent,
            @Parameter(description = "Output format") @RequestParam(value = "format", required = false, defaultValue = "png") String format) throws IOException {

        float alphaFactor = alphaPercent / 100.0f;
        byte[] baseBytes = baseFile.getBytes();
        byte[] overlayBytes = overlayFile.getBytes();

        ImageData baseImage = imageCodecService.decodeImage(baseBytes, format);
        ImageData overlayImage = imageCodecService.decodeImage(overlayBytes, format);

        imageValidationService.validateDimensions(baseImage.width(), baseImage.height());
        imageValidationService.validateDimensions(overlayImage.width(), overlayImage.height());

        // Apply overlay (centered)
        ImageData resultImage = imageOverlayService.overlayImage(baseImage, overlayImage, alphaFactor);
        byte[] outputBytes = imageCodecService.encodeImage(resultImage, format);

        imageStorageService.saveImage(resultImage, outputBytes, baseFile.getOriginalFilename(),
            baseFile.getContentType(), null, "OVERLAY");

        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"overlaid." + format + "\"")
            .contentType(MediaType.APPLICATION_OCTET_STREAM)
            .body(outputBytes);
    }

    @Operation(summary = "Overlay image from URLs",
               description = "Same as overlay/upload but images sourced from URLs.")
    @PostMapping(value = "/overlay/url", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public ResponseEntity<byte[]> overlayImageFromUrl(
            @Parameter(description = "URL of base image") @RequestParam("baseUrl") String baseUrl,
            @Parameter(description = "URL of overlay image") @RequestParam("overlayUrl") String overlayUrl,
            @Parameter(description = "Alpha blend factor 0-100 (0=base only, 100=overlay only)") @RequestParam("alphaPercent") @NotNull @Min(0) @Max(100) Integer alphaPercent,
            @Parameter(description = "Output format") @RequestParam(value = "format", required = false, defaultValue = "png") String format) throws IOException {

        float alphaFactor = alphaPercent / 100.0f;
        byte[] baseBytes = downloadImage(baseUrl);
        byte[] overlayBytes = downloadImage(overlayUrl);

        ImageData baseImage = imageCodecService.decodeImage(baseBytes, format);
        ImageData overlayImage = imageCodecService.decodeImage(overlayBytes, format);

        imageValidationService.validateDimensions(baseImage.width(), baseImage.height());
        imageValidationService.validateDimensions(overlayImage.width(), overlayImage.height());

        ImageData resultImage = imageOverlayService.overlayImage(baseImage, overlayImage, alphaFactor);
        byte[] outputBytes = imageCodecService.encodeImage(resultImage, format);

        imageStorageService.saveImage(resultImage, outputBytes, null,
            "image/" + format, baseUrl, "OVERLAY");

        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"overlaid." + format + "\"")
            .contentType(MediaType.APPLICATION_OCTET_STREAM)
            .body(outputBytes);
    }

    // ==================== WATERMARK API ====================

    @Operation(summary = "Apply watermark to image at specified position",
               description = "Adds watermark image at one of 5 positions: TOP_LEFT, TOP_RIGHT, BOTTOM_LEFT, BOTTOM_RIGHT, CENTER. " +
                             "Watermark is scaled if larger than 50% of base image.")
    @PostMapping(value = "/watermark/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<byte[]> watermarkImageUpload(
            @Parameter(description = "Base image file") @RequestParam("imageFile") MultipartFile imageFile,
            @Parameter(description = "Watermark image file") @RequestParam("watermarkFile") MultipartFile watermarkFile,
            @Parameter(description = "Position: TOP_LEFT, TOP_RIGHT, BOTTOM_LEFT, BOTTOM_RIGHT, CENTER") @RequestParam("position") @NotNull WatermarkPosition position,
            @Parameter(description = "Alpha blend factor 0-100") @RequestParam("alphaPercent") @NotNull @Min(0) @Max(100) Integer alphaPercent,
            @Parameter(description = "Padding from edges in pixels (0-50)") @RequestParam(value = "padding", required = false, defaultValue = "10") @Min(0) @Max(50) Integer padding,
            @Parameter(description = "Output format") @RequestParam(value = "format", required = false, defaultValue = "png") String format) throws IOException {

        float alphaFactor = alphaPercent / 100.0f;
        int pad = padding != null ? padding : 10;
        byte[] baseBytes = imageFile.getBytes();
        byte[] watermarkBytes = watermarkFile.getBytes();

        ImageData baseImage = imageCodecService.decodeImage(baseBytes, format);
        ImageData watermarkImage = imageCodecService.decodeImage(watermarkBytes, format);

        imageValidationService.validateDimensions(baseImage.width(), baseImage.height());
        imageValidationService.validateDimensions(watermarkImage.width(), watermarkImage.height());

        // Apply watermark at specified position
        ImageData resultImage = imageOverlayService.applyWatermark(baseImage, watermarkImage, position, alphaFactor, pad);
        byte[] outputBytes = imageCodecService.encodeImage(resultImage, format);

        imageStorageService.saveImage(resultImage, outputBytes, imageFile.getOriginalFilename(),
            imageFile.getContentType(), null, "WATERMARK");

        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"watermarked." + format + "\"")
            .contentType(MediaType.APPLICATION_OCTET_STREAM)
            .body(outputBytes);
    }

    @Operation(summary = "Apply watermark from URLs at specified position")
    @PostMapping(value = "/watermark/url", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public ResponseEntity<byte[]> watermarkImageFromUrl(
            @Parameter(description = "URL of base image") @RequestParam("imageUrl") String imageUrl,
            @Parameter(description = "URL of watermark image") @RequestParam("watermarkUrl") String watermarkUrl,
            @Parameter(description = "Position: TOP_LEFT, TOP_RIGHT, BOTTOM_LEFT, BOTTOM_RIGHT, CENTER") @RequestParam("position") @NotNull WatermarkPosition position,
            @Parameter(description = "Alpha blend factor 0-100") @RequestParam("alphaPercent") @NotNull @Min(0) @Max(100) Integer alphaPercent,
            @Parameter(description = "Padding from edges in pixels (0-50)") @RequestParam(value = "padding", required = false, defaultValue = "10") @Min(0) @Max(50) Integer padding,
            @Parameter(description = "Output format") @RequestParam(value = "format", required = false, defaultValue = "png") String format) throws IOException {

        float alphaFactor = alphaPercent / 100.0f;
        int pad = padding != null ? padding : 10;
        byte[] baseBytes = downloadImage(imageUrl);
        byte[] watermarkBytes = downloadImage(watermarkUrl);

        ImageData baseImage = imageCodecService.decodeImage(baseBytes, format);
        ImageData watermarkImage = imageCodecService.decodeImage(watermarkBytes, format);

        imageValidationService.validateDimensions(baseImage.width(), baseImage.height());
        imageValidationService.validateDimensions(watermarkImage.width(), watermarkImage.height());

        ImageData resultImage = imageOverlayService.applyWatermark(baseImage, watermarkImage, position, alphaFactor, pad);
        byte[] outputBytes = imageCodecService.encodeImage(resultImage, format);

        imageStorageService.saveImage(resultImage, outputBytes, null,
            "image/" + format, imageUrl, "WATERMARK");

        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"watermarked." + format + "\"")
            .contentType(MediaType.APPLICATION_OCTET_STREAM)
            .body(outputBytes);
    }

    @Operation(summary = "Get available watermark positions")
    @GetMapping("/watermark/positions")
    public ResponseEntity<WatermarkPosition[]> getWatermarkPositions() {
        return ResponseEntity.ok(WatermarkPosition.values());
    }

    private byte[] downloadImage(String url) throws IOException {
        return new java.net.URL(url).openStream().readAllBytes();
    }
}
