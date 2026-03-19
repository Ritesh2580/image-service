package com.imageservice.controller;

import com.imageservice.model.ImageData;
import com.imageservice.service.ImageCodecService;
import com.imageservice.service.ImageCompressService;
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
import java.io.InputStream;
import java.net.URL;

@RestController
@RequestMapping("/api/images")
@Tag(name = "Image Compress", description = "Operations for compressing images to target file size")
@Validated
public class ImageCompressController {

    private final ImageCodecService imageCodecService;
    private final ImageCompressService imageCompressService;
    private final ImageStorageService imageStorageService;
    private final ImageValidationService imageValidationService;

    public ImageCompressController(ImageCodecService imageCodecService,
                                    ImageCompressService imageCompressService,
                                    ImageStorageService imageStorageService,
                                    ImageValidationService imageValidationService) {
        this.imageCodecService = imageCodecService;
        this.imageCompressService = imageCompressService;
        this.imageStorageService = imageStorageService;
        this.imageValidationService = imageValidationService;
    }

    @Operation(summary = "Compress an uploaded image file to target size")
    @PostMapping(value = "/compress/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<byte[]> compressImageUpload(
            @Parameter(description = "Image file to compress") @RequestParam("file") MultipartFile file,
            @Parameter(description = "Target file size in KB (1-50000)") @RequestParam("targetSizeKB") @NotNull @Min(1) @Max(50000) Integer targetSizeKB,
            @Parameter(description = "Output image format (e.g., png, jpg, webp)") @RequestParam(value = "format", required = false, defaultValue = "png") String format) throws IOException {
        byte[] originalBytes = file.getBytes();
        ImageData sourceImage = imageCodecService.decodeImage(originalBytes, format);
        imageValidationService.validateDimensions(sourceImage.width(), sourceImage.height());

        long targetSizeBytes = targetSizeKB * 1024L;
        ImageData compressedImage = imageCompressService.compressImage(sourceImage, targetSizeBytes, format);
        byte[] outputBytes = imageCodecService.encodeImage(compressedImage, format);

        imageStorageService.saveImage(compressedImage, outputBytes, file.getOriginalFilename(),
            file.getContentType(), null, "COMPRESS");

        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"compressed." + format + "\"")
            .contentType(MediaType.APPLICATION_OCTET_STREAM)
            .body(outputBytes);
    }

    @Operation(summary = "Compress an image from a URL to target size")
    @PostMapping(value = "/compress/url", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public ResponseEntity<byte[]> compressImageFromUrl(
            @Parameter(description = "URL of the image to compress") @RequestParam("imageUrl") String imageUrl,
            @Parameter(description = "Target file size in KB (1-50000)") @RequestParam("targetSizeKB") @NotNull @Min(1) @Max(50000) Integer targetSizeKB,
            @Parameter(description = "Output image format (e.g., png, jpg, webp)") @RequestParam(value = "format", required = false, defaultValue = "png") String format) throws IOException {
        byte[] originalBytes = downloadImage(imageUrl);
        ImageData sourceImage = imageCodecService.decodeImage(originalBytes, format);
        imageValidationService.validateDimensions(sourceImage.width(), sourceImage.height());

        long targetSizeBytes = targetSizeKB * 1024L;
        ImageData compressedImage = imageCompressService.compressImage(sourceImage, targetSizeBytes, format);
        byte[] outputBytes = imageCodecService.encodeImage(compressedImage, format);

        imageStorageService.saveImage(compressedImage, outputBytes, null,
            "image/" + format, imageUrl, "COMPRESS");

        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"compressed." + format + "\"")
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
