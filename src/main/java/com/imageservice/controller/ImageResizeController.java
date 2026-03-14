package com.imageservice.controller;

import com.imageservice.model.ImageData;
import com.imageservice.model.ResizeRequest;
import com.imageservice.service.ImageCodecService;
import com.imageservice.service.ImageResizeService;
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
@Tag(name = "Image Resize", description = "Operations for resizing images")
public class ImageResizeController {
    private final ImageCodecService imageCodecService;
    private final ImageResizeService imageResizeService;
    private final ImageStorageService imageStorageService;
    private final ImageValidationService imageValidationService;

    public ImageResizeController(ImageCodecService imageCodecService,
                                 ImageResizeService imageResizeService,
                                 ImageStorageService imageStorageService,
                                 ImageValidationService imageValidationService) {
        this.imageCodecService = imageCodecService;
        this.imageResizeService = imageResizeService;
        this.imageStorageService = imageStorageService;
        this.imageValidationService = imageValidationService;
    }

    @Operation(summary = "Resize an uploaded image file")
    @PostMapping(value = "/resize/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<byte[]> resizeImageUpload(
            @Parameter(description = "Image file to resize") @RequestParam("file") MultipartFile file,
            @Valid ResizeRequest resizeRequest,
            @Parameter(description = "Output image format (e.g., png, jpg)") @RequestParam(value = "format", required = false, defaultValue = "png") String format) throws IOException {
        byte[] originalBytes = file.getBytes();
        ImageData sourceImage = imageCodecService.decodeImage(originalBytes, format);
        imageValidationService.validateDimensions(sourceImage.width(), sourceImage.height());
        imageValidationService.validateDimensions(resizeRequest.width(), resizeRequest.height());

        ImageData resizedImage = imageResizeService.resizeImage(sourceImage, resizeRequest.width(), resizeRequest.height());
        byte[] outputBytes = imageCodecService.encodeImage(resizedImage, format);

        imageStorageService.saveImage(resizedImage, outputBytes, file.getOriginalFilename(),
            file.getContentType(), null, "RESIZE");

        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"resized." + format + "\"")
            .contentType(MediaType.APPLICATION_OCTET_STREAM)
            .body(outputBytes);
    }

    @Operation(summary = "Resize an image from a URL")
    @PostMapping(value = "/resize/url", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public ResponseEntity<byte[]> resizeImageFromUrl(
            @Parameter(description = "URL of the image to resize") @RequestParam("imageUrl") String imageUrl,
            @Valid ResizeRequest resizeRequest,
            @Parameter(description = "Output image format (e.g., png, jpg)") @RequestParam(value = "format", required = false, defaultValue = "png") String format) throws IOException {
        byte[] originalBytes = downloadImage(imageUrl);
        ImageData sourceImage = imageCodecService.decodeImage(originalBytes, format);
        imageValidationService.validateDimensions(sourceImage.width(), sourceImage.height());
        imageValidationService.validateDimensions(resizeRequest.width(), resizeRequest.height());

        ImageData resizedImage = imageResizeService.resizeImage(sourceImage, resizeRequest.width(), resizeRequest.height());
        byte[] outputBytes = imageCodecService.encodeImage(resizedImage, format);

        imageStorageService.saveImage(resizedImage, outputBytes, null,
            "image/" + format, imageUrl, "RESIZE");

        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"resized." + format + "\"")
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