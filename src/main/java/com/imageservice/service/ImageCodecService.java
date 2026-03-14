package com.imageservice.service;

import com.imageservice.model.ImageData;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Objects;

@Service
public class ImageCodecService {
    public ImageData decodeImage(byte[] bytes, String formatHint) throws IOException {
        try (ByteArrayInputStream inputStream = new ByteArrayInputStream(bytes)) {
            BufferedImage bufferedImage = ImageIO.read(inputStream);
            if (bufferedImage == null) {
                throw new IOException("Unsupported or corrupted image format");
            }
            int width = bufferedImage.getWidth();
            int height = bufferedImage.getHeight();
            int[] pixels = bufferedImage.getRGB(0, 0, width, height, null, 0, width);
            return new ImageData(width, height, pixels, normalizeFormat(formatHint));
        }
    }

    public byte[] encodeImage(ImageData imageData, String format) throws IOException {
        Objects.requireNonNull(imageData, "imageData is required");
        String outputFormat = normalizeFormat(format);
        BufferedImage bufferedImage = new BufferedImage(imageData.width(), imageData.height(), BufferedImage.TYPE_INT_ARGB);
        bufferedImage.setRGB(0, 0, imageData.width(), imageData.height(), imageData.pixels(), 0, imageData.width());

        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            boolean written = ImageIO.write(bufferedImage, outputFormat, outputStream);
            if (!written) {
                throw new IOException("No ImageIO writer available for format: " + outputFormat);
            }
            return outputStream.toByteArray();
        }
    }

    private String normalizeFormat(String format) {
        if (format == null || format.isBlank()) {
            return "png";
        }
        String lower = format.toLowerCase();
        return lower.equals("jpeg") ? "jpg" : lower;
    }
}