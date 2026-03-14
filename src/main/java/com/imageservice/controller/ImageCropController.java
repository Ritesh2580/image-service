package com.imageservice.controller;

import com.imageservice.model.CropRequest;
import com.imageservice.model.ImageData;
import com.imageservice.service.ImageCodecService;
import com.imageservice.service.ImageCropService;
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
@Tag(name = "Image Crop", description = "Operations for cropping images")
public class ImageCropController {
    private final ImageCodecService imageCodecService;
    private final ImageCropService imageCropService;
    private final ImageStorageService imageStorageService;
    private final ImageValidationService imageValidationService;

    public ImageCropController(ImageCodecService imageCodecService,
                               ImageCropService imageCropService,
                               ImageStorageService imageStorageService,
                               ImageValidationService imageValidationService) {
        this.imageCodecService = imageCodecService;
        this.imageCropService = imageCropService;
        this.imageStorageService = imageStorageService;
        this.imageValidationService = imageValidationService;
    }

    @Operation(summary = "Crop an uploaded image file")
    @PostMapping(value = "/crop/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<byte[]> cropImageUpload(
            @Parameter(description = "Image file to crop") @RequestParam("file") MultipartFile file,
            @Valid CropRequest cropRequest,
            @Parameter(description = "Output image format (e.g., png, jpg)") @RequestParam(value = "format", required = false, defaultValue = "png") String format) throws IOException {
        byte[] originalBytes = file.getBytes();
        ImageData sourceImage = imageCodecService.decodeImage(originalBytes, format);
        imageValidationService.validateDimensions(sourceImage.width(), sourceImage.height());
        ImageData croppedImage = imageCropService.cropImage(sourceImage, cropRequest);
        byte[] outputBytes = imageCodecService.encodeImage(croppedImage, format);

        imageStorageService.saveImage(croppedImage, outputBytes, file.getOriginalFilename(),
            file.getContentType(), null, "CROP");

        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"cropped." + format + "\"")
            .contentType(MediaType.APPLICATION_OCTET_STREAM)
            .body(outputBytes);
    }

    @Operation(summary = "Crop an image from a URL")
    @PostMapping(value = "/crop/url", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public ResponseEntity<byte[]> cropImageFromUrl(
            @Parameter(description = "URL of the image to crop") @RequestParam("imageUrl") String imageUrl,
            @Valid CropRequest cropRequest,
            @Parameter(description = "Output image format (e.g., png, jpg)") @RequestParam(value = "format", required = false, defaultValue = "png") String format) throws IOException {
        byte[] originalBytes = downloadImage(imageUrl);
        ImageData sourceImage = imageCodecService.decodeImage(originalBytes, format);
        imageValidationService.validateDimensions(sourceImage.width(), sourceImage.height());
        ImageData croppedImage = imageCropService.cropImage(sourceImage, cropRequest);
        byte[] outputBytes = imageCodecService.encodeImage(croppedImage, format);

        imageStorageService.saveImage(croppedImage, outputBytes, null,
            "image/" + format, imageUrl, "CROP");

        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"cropped." + format + "\"")
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