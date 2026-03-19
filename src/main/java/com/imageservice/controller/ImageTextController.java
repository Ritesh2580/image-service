package com.imageservice.controller;

import com.imageservice.model.ImageData;
import com.imageservice.model.TextWatermarkRequest;
import com.imageservice.service.ImageCodecService;
import com.imageservice.service.ImageStorageService;
import com.imageservice.service.ImageTextService;
import com.imageservice.service.ImageValidationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
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
import java.util.List;

@RestController
@RequestMapping("/api/images")
@Tag(name = "Text Watermark", description = "Operations for adding text overlays and watermarks to images")
@Validated
public class ImageTextController {

    private final ImageCodecService imageCodecService;
    private final ImageTextService imageTextService;
    private final ImageStorageService imageStorageService;
    private final ImageValidationService imageValidationService;

    public ImageTextController(ImageCodecService imageCodecService,
                                ImageTextService imageTextService,
                                ImageStorageService imageStorageService,
                                ImageValidationService imageValidationService) {
        this.imageCodecService = imageCodecService;
        this.imageTextService = imageTextService;
        this.imageStorageService = imageStorageService;
        this.imageValidationService = imageValidationService;
    }

    @Operation(summary = "Get list of available system fonts",
               description = "Returns all font families available on the server. Use these names in text watermark requests.")
    @GetMapping("/text/fonts")
    public ResponseEntity<List<String>> getAvailableFonts() {
        return ResponseEntity.ok(imageTextService.getAvailableFonts());
    }

    @Operation(summary = "Add text overlay to uploaded image",
               description = "Renders custom text onto image at specified (x,y) position. " +
                             "Supports font family, size, color (hex or named), and transparency.")
    @PostMapping(value = "/text/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<byte[]> addTextUpload(
            @Parameter(description = "Image file to add text to") @RequestParam("file") MultipartFile file,
            @Parameter(description = "Text to render on image") @RequestParam("text") @NotBlank String text,
            @Parameter(description = "Font family name (use /text/fonts to get available)") @RequestParam(value = "fontFamily", required = false, defaultValue = "SansSerif") String fontFamily,
            @Parameter(description = "Font size (8-200)") @RequestParam(value = "fontSize", required = false, defaultValue = "24") @Min(8) @Max(200) Integer fontSize,
            @Parameter(description = "Text color (hex #RRGGBB or named: red, white, black, etc.)") @RequestParam(value = "color", required = false, defaultValue = "white") String color,
            @Parameter(description = "X coordinate (pixels from left)") @RequestParam("x") @NotNull @Min(0) Integer x,
            @Parameter(description = "Y coordinate (pixels from top)") @RequestParam("y") @NotNull @Min(0) Integer y,
            @Parameter(description = "Transparency 0-100 (0=invisible, 100=opaque)") @RequestParam(value = "alphaPercent", required = false, defaultValue = "100") @Min(0) @Max(100) Integer alphaPercent,
            @Parameter(description = "Output format") @RequestParam(value = "format", required = false, defaultValue = "png") String format) throws IOException {

        TextWatermarkRequest textRequest = new TextWatermarkRequest(text, fontFamily, fontSize, color, x, y, alphaPercent);
        byte[] originalBytes = file.getBytes();
        ImageData sourceImage = imageCodecService.decodeImage(originalBytes, format);
        imageValidationService.validateDimensions(sourceImage.width(), sourceImage.height());

        // Add text to image
        ImageData resultImage = imageTextService.addText(sourceImage, textRequest);
        byte[] outputBytes = imageCodecService.encodeImage(resultImage, format);

        imageStorageService.saveImage(resultImage, outputBytes, file.getOriginalFilename(),
            file.getContentType(), null, "TEXT_OVERLAY");

        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"text_overlay." + format + "\"")
            .contentType(MediaType.APPLICATION_OCTET_STREAM)
            .body(outputBytes);
    }

    @Operation(summary = "Add text overlay to image from URL")
    @PostMapping(value = "/text/url", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public ResponseEntity<byte[]> addTextFromUrl(
            @Parameter(description = "URL of image to add text to") @RequestParam("imageUrl") String imageUrl,
            @Parameter(description = "Text to render on image") @RequestParam("text") @NotBlank String text,
            @Parameter(description = "Font family name (use /text/fonts to get available)") @RequestParam(value = "fontFamily", required = false, defaultValue = "SansSerif") String fontFamily,
            @Parameter(description = "Font size (8-200)") @RequestParam(value = "fontSize", required = false, defaultValue = "24") @Min(8) @Max(200) Integer fontSize,
            @Parameter(description = "Text color (hex #RRGGBB or named: red, white, black, etc.)") @RequestParam(value = "color", required = false, defaultValue = "white") String color,
            @Parameter(description = "X coordinate (pixels from left)") @RequestParam("x") @NotNull @Min(0) Integer x,
            @Parameter(description = "Y coordinate (pixels from top)") @RequestParam("y") @NotNull @Min(0) Integer y,
            @Parameter(description = "Transparency 0-100 (0=invisible, 100=opaque)") @RequestParam(value = "alphaPercent", required = false, defaultValue = "100") @Min(0) @Max(100) Integer alphaPercent,
            @Parameter(description = "Output format") @RequestParam(value = "format", required = false, defaultValue = "png") String format) throws IOException {

        TextWatermarkRequest textRequest = new TextWatermarkRequest(text, fontFamily, fontSize, color, x, y, alphaPercent);
        byte[] originalBytes = downloadImage(imageUrl);
        ImageData sourceImage = imageCodecService.decodeImage(originalBytes, format);
        imageValidationService.validateDimensions(sourceImage.width(), sourceImage.height());

        // Add text to image
        ImageData resultImage = imageTextService.addText(sourceImage, textRequest);
        byte[] outputBytes = imageCodecService.encodeImage(resultImage, format);

        imageStorageService.saveImage(resultImage, outputBytes, null,
            "image/" + format, imageUrl, "TEXT_OVERLAY");

        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"text_overlay." + format + "\"")
            .contentType(MediaType.APPLICATION_OCTET_STREAM)
            .body(outputBytes);
    }

    @Operation(summary = "Get recommended font size for image dimensions",
               description = "Calculates suggested font size based on image width/height for readable text.")
    @GetMapping("/text/recommended-size")
    public ResponseEntity<Integer> getRecommendedFontSize(
            @Parameter(description = "Image width in pixels") @RequestParam int width,
            @Parameter(description = "Image height in pixels") @RequestParam int height) {
        return ResponseEntity.ok(imageTextService.getRecommendedFontSize(width, height));
    }

    private byte[] downloadImage(String imageUrl) throws IOException {
        URL url = new URL(imageUrl);
        try (InputStream inputStream = url.openStream()) {
            return inputStream.readAllBytes();
        }
    }
}
