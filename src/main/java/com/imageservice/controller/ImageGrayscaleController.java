package com.imageservice.controller;

import com.imageservice.model.GrayscaleRequest;
import com.imageservice.model.ImageData;
import com.imageservice.service.ImageCodecService;
import com.imageservice.service.ImageGrayscaleService;
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
@Tag(name = "Image Grayscale", description = "Operations for converting images to grayscale")
public class ImageGrayscaleController {
    private final ImageCodecService imageCodecService;
    private final ImageGrayscaleService imageGrayscaleService;
    private final ImageStorageService imageStorageService;
    private final ImageValidationService imageValidationService;

    public ImageGrayscaleController(ImageCodecService imageCodecService,
                                    ImageGrayscaleService imageGrayscaleService,
                                    ImageStorageService imageStorageService,
                                    ImageValidationService imageValidationService) {
        this.imageCodecService = imageCodecService;
        this.imageGrayscaleService = imageGrayscaleService;
        this.imageStorageService = imageStorageService;
        this.imageValidationService = imageValidationService;
    }

    @Operation(summary = "Convert an uploaded image file to grayscale")
    @PostMapping(value = "/grayscale/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<byte[]> grayscaleImageUpload(
            @Parameter(description = "Image file to process") @RequestParam("file") MultipartFile file,
            @Parameter(description = "Output image format (e.g., png, jpg)") @RequestParam(value = "format", required = false, defaultValue = "png") String format) throws IOException {
        byte[] originalBytes = file.getBytes();
        ImageData sourceImage = imageCodecService.decodeImage(originalBytes, format);
        imageValidationService.validateDimensions(sourceImage.width(), sourceImage.height());

        ImageData grayImage = imageGrayscaleService.grayscaleImage(sourceImage, false);
        byte[] outputBytes = imageCodecService.encodeImage(grayImage, format);

        imageStorageService.saveImage(grayImage, outputBytes, file.getOriginalFilename(),
            file.getContentType(), null, "GRAYSCALE");

        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"grayscale." + format + "\"")
            .contentType(MediaType.APPLICATION_OCTET_STREAM)
            .body(outputBytes);
    }

    @Operation(summary = "Convert an image from a URL to grayscale")
    @PostMapping(value = "/grayscale/url", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public ResponseEntity<byte[]> grayscaleImageFromUrl(
            @Parameter(description = "URL of the image to process") @RequestParam("imageUrl") String imageUrl,
            @Valid GrayscaleRequest grayscaleRequest,
            @Parameter(description = "Output image format (e.g., png, jpg)") @RequestParam(value = "format", required = false, defaultValue = "png") String format) throws IOException {
        byte[] originalBytes = downloadImage(imageUrl);
        ImageData sourceImage = imageCodecService.decodeImage(originalBytes, format);
        imageValidationService.validateDimensions(sourceImage.width(), sourceImage.height());

        ImageData grayImage = imageGrayscaleService.grayscaleImage(sourceImage, grayscaleRequest.keepAlpha());
        byte[] outputBytes = imageCodecService.encodeImage(grayImage, format);

        imageStorageService.saveImage(grayImage, outputBytes, null,
            "image/" + format, imageUrl, "GRAYSCALE");

        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"grayscale." + format + "\"")
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