package com.imageservice.controller;

import com.imageservice.model.ImageData;
import com.imageservice.service.ImageCodecService;
import com.imageservice.service.ImageConvertService;
import com.imageservice.service.ImageStorageService;
import com.imageservice.service.ImageValidationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Set;

@RestController
@RequestMapping("/api/images")
@Tag(name = "Image Convert", description = "Operations for converting images between formats (PNG↔JPG, JPG→WEBP, WEBP→AVIF, etc.)")
@Validated
public class ImageConvertController {

    private final ImageCodecService imageCodecService;
    private final ImageConvertService imageConvertService;
    private final ImageStorageService imageStorageService;
    private final ImageValidationService imageValidationService;

    public ImageConvertController(ImageCodecService imageCodecService,
                                   ImageConvertService imageConvertService,
                                   ImageStorageService imageStorageService,
                                   ImageValidationService imageValidationService) {
        this.imageCodecService = imageCodecService;
        this.imageConvertService = imageConvertService;
        this.imageStorageService = imageStorageService;
        this.imageValidationService = imageValidationService;
    }

    @Operation(summary = "Convert an uploaded image file to another format")
    @PostMapping(value = "/convert/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<byte[]> convertImageUpload(
            @Parameter(description = "Image file to convert") @RequestParam("file") MultipartFile file,
            @Parameter(description = "Target format (png, jpg, jpeg, webp, avif, gif, bmp)") @RequestParam("targetFormat") @NotBlank @Pattern(regexp = "(?i)(png|jpg|jpeg|webp|avif|gif|bmp)", message = "Format must be one of: png, jpg, jpeg, webp, avif, gif, bmp") String targetFormat) throws IOException {
        String normalizedFormat = targetFormat.toLowerCase().equals("jpeg") ? "jpg" : targetFormat.toLowerCase();
        byte[] originalBytes = file.getBytes();
        
        // Detect source format from filename or default to png
        String sourceFormat = detectFormat(file.getOriginalFilename());
        ImageData sourceImage = imageCodecService.decodeImage(originalBytes, sourceFormat);
        imageValidationService.validateDimensions(sourceImage.width(), sourceImage.height());

        // Process for format compatibility (e.g., flatten alpha for JPG)
        ImageData convertedImage = imageConvertService.convertImage(sourceImage, normalizedFormat);
        byte[] outputBytes = imageCodecService.encodeImage(convertedImage, normalizedFormat);

        imageStorageService.saveImage(convertedImage, outputBytes, file.getOriginalFilename(),
            file.getContentType(), null, "CONVERT");

        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"converted." + normalizedFormat + "\"")
            .contentType(MediaType.APPLICATION_OCTET_STREAM)
            .body(outputBytes);
    }

    @Operation(summary = "Convert an image from a URL to another format")
    @PostMapping(value = "/convert/url", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public ResponseEntity<byte[]> convertImageFromUrl(
            @Parameter(description = "URL of the image to convert") @RequestParam("imageUrl") String imageUrl,
            @Parameter(description = "Target format (png, jpg, jpeg, webp, avif, gif, bmp)") @RequestParam("targetFormat") @NotBlank @Pattern(regexp = "(?i)(png|jpg|jpeg|webp|avif|gif|bmp)", message = "Format must be one of: png, jpg, jpeg, webp, avif, gif, bmp") String targetFormat) throws IOException {
        String normalizedFormat = targetFormat.toLowerCase().equals("jpeg") ? "jpg" : targetFormat.toLowerCase();
        byte[] originalBytes = downloadImage(imageUrl);
        
        // Detect source format from URL or default to png
        String sourceFormat = detectFormat(imageUrl);
        ImageData sourceImage = imageCodecService.decodeImage(originalBytes, sourceFormat);
        imageValidationService.validateDimensions(sourceImage.width(), sourceImage.height());

        // Process for format compatibility (e.g., flatten alpha for JPG)
        ImageData convertedImage = imageConvertService.convertImage(sourceImage, normalizedFormat);
        byte[] outputBytes = imageCodecService.encodeImage(convertedImage, normalizedFormat);

        imageStorageService.saveImage(convertedImage, outputBytes, null,
            "image/" + normalizedFormat, imageUrl, "CONVERT");

        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"converted." + normalizedFormat + "\"")
            .contentType(MediaType.APPLICATION_OCTET_STREAM)
            .body(outputBytes);
    }

    @Operation(summary = "Get list of supported output formats")
    @GetMapping("/convert/formats")
    public ResponseEntity<Set<String>> getSupportedFormats() {
        return ResponseEntity.ok(imageConvertService.getSupportedFormats());
    }

    private byte[] downloadImage(String imageUrl) throws IOException {
        URL url = new URL(imageUrl);
        try (InputStream inputStream = url.openStream()) {
            return inputStream.readAllBytes();
        }
    }

    private String detectFormat(String source) {
        if (source == null) {
            return "png";
        }
        String lower = source.toLowerCase();
        if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) return "jpg";
        if (lower.endsWith(".webp")) return "webp";
        if (lower.endsWith(".avif")) return "avif";
        if (lower.endsWith(".gif")) return "gif";
        if (lower.endsWith(".bmp")) return "bmp";
        if (lower.endsWith(".png")) return "png";
        return "png";
    }
}
