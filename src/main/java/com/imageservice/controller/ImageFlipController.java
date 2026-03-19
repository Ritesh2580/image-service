package com.imageservice.controller;

import com.imageservice.model.FlipDirection;
import com.imageservice.model.ImageData;
import com.imageservice.service.ImageCodecService;
import com.imageservice.service.ImageFlipService;
import com.imageservice.service.ImageStorageService;
import com.imageservice.service.ImageValidationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "Image Flip", description = "Operations for flipping images horizontally, vertically, or both")
@Validated
public class ImageFlipController {

    private final ImageCodecService imageCodecService;
    private final ImageFlipService imageFlipService;
    private final ImageStorageService imageStorageService;
    private final ImageValidationService imageValidationService;

    public ImageFlipController(ImageCodecService imageCodecService,
                                ImageFlipService imageFlipService,
                                ImageStorageService imageStorageService,
                                ImageValidationService imageValidationService) {
        this.imageCodecService = imageCodecService;
        this.imageFlipService = imageFlipService;
        this.imageStorageService = imageStorageService;
        this.imageValidationService = imageValidationService;
    }

    @Operation(summary = "Flip an uploaded image",
               description = "Flips image in specified direction. HORIZONTAL flips left-right, VERTICAL flips top-bottom, BOTH flips both directions (180° rotation).")
    @PostMapping(value = "/flip/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<byte[]> flipImageUpload(
            @Parameter(description = "Image file to flip") @RequestParam("file") MultipartFile file,
            @Parameter(description = "Flip direction: HORIZONTAL, VERTICAL, or BOTH") @RequestParam("direction") @NotNull FlipDirection direction,
            @Parameter(description = "Output format") @RequestParam(value = "format", required = false, defaultValue = "png") String format) throws IOException {

        byte[] originalBytes = file.getBytes();
        ImageData sourceImage = imageCodecService.decodeImage(originalBytes, format);
        imageValidationService.validateDimensions(sourceImage.width(), sourceImage.height());

        // Flip the image
        ImageData flippedImage = imageFlipService.flipImage(sourceImage, direction);
        byte[] outputBytes = imageCodecService.encodeImage(flippedImage, format);

        imageStorageService.saveImage(flippedImage, outputBytes, file.getOriginalFilename(),
            file.getContentType(), null, "FLIP");

        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"flipped." + format + "\"")
            .contentType(MediaType.APPLICATION_OCTET_STREAM)
            .body(outputBytes);
    }

    @Operation(summary = "Flip an image from URL")
    @PostMapping(value = "/flip/url", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public ResponseEntity<byte[]> flipImageFromUrl(
            @Parameter(description = "URL of image to flip") @RequestParam("imageUrl") String imageUrl,
            @Parameter(description = "Flip direction: HORIZONTAL, VERTICAL, or BOTH") @RequestParam("direction") @NotNull FlipDirection direction,
            @Parameter(description = "Output format") @RequestParam(value = "format", required = false, defaultValue = "png") String format) throws IOException {

        byte[] originalBytes = downloadImage(imageUrl);
        ImageData sourceImage = imageCodecService.decodeImage(originalBytes, format);
        imageValidationService.validateDimensions(sourceImage.width(), sourceImage.height());

        // Flip the image
        ImageData flippedImage = imageFlipService.flipImage(sourceImage, direction);
        byte[] outputBytes = imageCodecService.encodeImage(flippedImage, format);

        imageStorageService.saveImage(flippedImage, outputBytes, null,
            "image/" + format, imageUrl, "FLIP");

        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"flipped." + format + "\"")
            .contentType(MediaType.APPLICATION_OCTET_STREAM)
            .body(outputBytes);
    }

    @Operation(summary = "Get available flip directions")
    @GetMapping("/flip/directions")
    public ResponseEntity<FlipDirection[]> getFlipDirections() {
        return ResponseEntity.ok(FlipDirection.values());
    }

    private byte[] downloadImage(String imageUrl) throws IOException {
        URL url = new URL(imageUrl);
        try (InputStream inputStream = url.openStream()) {
            return inputStream.readAllBytes();
        }
    }
}
