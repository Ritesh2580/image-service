package com.imageservice.controller;

import com.imageservice.model.ImageData;
import com.imageservice.model.RotateRequest;
import com.imageservice.service.ImageCodecService;
import com.imageservice.service.ImageRotateService;
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
@Tag(name = "Image Rotate", description = "Operations for rotating images")
public class ImageRotateController {
    private final ImageCodecService imageCodecService;
    private final ImageRotateService imageRotateService;
    private final ImageStorageService imageStorageService;
    private final ImageValidationService imageValidationService;

    public ImageRotateController(ImageCodecService imageCodecService,
                                 ImageRotateService imageRotateService,
                                 ImageStorageService imageStorageService,
                                 ImageValidationService imageValidationService) {
        this.imageCodecService = imageCodecService;
        this.imageRotateService = imageRotateService;
        this.imageStorageService = imageStorageService;
        this.imageValidationService = imageValidationService;
    }

    @Operation(summary = "Rotate an uploaded image file")
    @PostMapping(value = "/rotate/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<byte[]> rotateImageUpload(
            @Parameter(description = "Image file to rotate") @RequestParam("file") MultipartFile file,
            @Valid RotateRequest rotateRequest,
            @Parameter(description = "Output image format (e.g., png, jpg)") @RequestParam(value = "format", required = false, defaultValue = "png") String format) throws IOException {
        byte[] originalBytes = file.getBytes();
        ImageData sourceImage = imageCodecService.decodeImage(originalBytes, format);
        imageValidationService.validateDimensions(sourceImage.width(), sourceImage.height());

        ImageData rotatedImage = imageRotateService.rotateImage(sourceImage, rotateRequest.normalizedAngle());
        byte[] outputBytes = imageCodecService.encodeImage(rotatedImage, format);

        imageStorageService.saveImage(rotatedImage, outputBytes, file.getOriginalFilename(),
            file.getContentType(), null, "ROTATE");

        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"rotated." + format + "\"")
            .contentType(MediaType.APPLICATION_OCTET_STREAM)
            .body(outputBytes);
    }

    @Operation(summary = "Rotate an image from a URL")
    @PostMapping(value = "/rotate/url", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public ResponseEntity<byte[]> rotateImageFromUrl(
            @Parameter(description = "URL of the image to rotate") @RequestParam("imageUrl") String imageUrl,
            @Valid RotateRequest rotateRequest,
            @Parameter(description = "Output image format (e.g., png, jpg)") @RequestParam(value = "format", required = false, defaultValue = "png") String format) throws IOException {
        byte[] originalBytes = downloadImage(imageUrl);
        ImageData sourceImage = imageCodecService.decodeImage(originalBytes, format);
        imageValidationService.validateDimensions(sourceImage.width(), sourceImage.height());

        ImageData rotatedImage = imageRotateService.rotateImage(sourceImage, rotateRequest.normalizedAngle());
        byte[] outputBytes = imageCodecService.encodeImage(rotatedImage, format);

        imageStorageService.saveImage(rotatedImage, outputBytes, null,
            "image/" + format, imageUrl, "ROTATE");

        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"rotated." + format + "\"")
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