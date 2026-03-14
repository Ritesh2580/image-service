package com.imageservice.controller;

import com.imageservice.model.AdjustRequest;
import com.imageservice.model.ImageData;
import com.imageservice.service.ImageAdjustService;
import com.imageservice.service.ImageCodecService;
import com.imageservice.service.ImageStorageService;
import com.imageservice.service.ImageValidationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

@RestController
@RequestMapping("/api/images")
@Tag(name = "Image Adjustments", description = "Operations for adjusting image brightness, contrast, and luminosity")
public class ImageAdjustController {
    private final ImageAdjustService imageAdjustService;
    private final ImageCodecService imageCodecService;
    private final ImageStorageService imageStorageService;
    private final ImageValidationService imageValidationService;

    public ImageAdjustController(ImageAdjustService imageAdjustService,
                                 ImageCodecService imageCodecService,
                                 ImageStorageService imageStorageService,
                                 ImageValidationService imageValidationService) {
        this.imageAdjustService = imageAdjustService;
        this.imageCodecService = imageCodecService;
        this.imageStorageService = imageStorageService;
        this.imageValidationService = imageValidationService;
    }

    @Operation(summary = "Adjust brightness, contrast, and luminosity of an uploaded image file")
    @PostMapping(value = "/adjust/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<byte[]> adjustImageUpload(
            @Parameter(description = "Image file to process") @RequestParam("file") MultipartFile file,
            @Valid AdjustRequest adjustRequest,
            @Parameter(description = "Output image format (e.g., png, jpg)") @RequestParam(value = "format", required = false, defaultValue = "png") String format) throws IOException {
        byte[] originalBytes = file.getBytes();
        ImageData sourceImage = imageCodecService.decodeImage(originalBytes, format);
        imageValidationService.validateDimensions(sourceImage.width(), sourceImage.height());

        ImageData adjustedImage = imageAdjustService.adjustImage(
            sourceImage,
            adjustRequest.brightness(),
            adjustRequest.contrast(),
            adjustRequest.luminosity()
        );
        byte[] outputBytes = imageCodecService.encodeImage(adjustedImage, format);

        imageStorageService.saveImage(adjustedImage, outputBytes, file.getOriginalFilename(),
            file.getContentType(), null, "ADJUST");

        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"adjusted." + format + "\"")
            .contentType(MediaType.APPLICATION_OCTET_STREAM)
            .body(outputBytes);
    }

    @Operation(summary = "Adjust brightness, contrast, and luminosity of an image from a URL")
    @PostMapping(value = "/adjust/url", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public ResponseEntity<byte[]> adjustImageFromUrl(
            @Parameter(description = "URL of the image to process") @RequestParam("imageUrl") String imageUrl,
            @Valid AdjustRequest adjustRequest,
            @Parameter(description = "Output image format (e.g., png, jpg)") @RequestParam(value = "format", required = false, defaultValue = "png") String format) throws IOException {
        byte[] originalBytes = downloadImage(imageUrl);
        ImageData sourceImage = imageCodecService.decodeImage(originalBytes, format);
        imageValidationService.validateDimensions(sourceImage.width(), sourceImage.height());

        ImageData adjustedImage = imageAdjustService.adjustImage(
            sourceImage,
            adjustRequest.brightness(),
            adjustRequest.contrast(),
            adjustRequest.luminosity()
        );
        byte[] outputBytes = imageCodecService.encodeImage(adjustedImage, format);

        imageStorageService.saveImage(adjustedImage, outputBytes, null,
            "image/" + format, imageUrl, "ADJUST");

        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"adjusted." + format + "\"")
            .contentType(MediaType.APPLICATION_OCTET_STREAM)
            .body(outputBytes);
    }

    private byte[] downloadImage(String imageUrl) throws IOException {
        URL url = new URL(imageUrl);
        try (InputStream inputStream = url.openStream()) {
            return inputStream.readAllBytes();
        }
    }
}