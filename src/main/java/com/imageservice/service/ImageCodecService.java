package com.imageservice.service;

import com.imageservice.model.ImageData;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Iterator;
import java.util.Objects;

@Service
public class ImageCodecService {

    /**
     * Decodes image bytes into ImageData.
     */
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

    /**
     * Encodes ImageData to bytes using default quality.
     */
    public byte[] encodeImage(ImageData imageData, String format) throws IOException {
        return encodeImageWithQuality(imageData, format, 1.0f);
    }

    /**
     * Encodes ImageData to bytes with specified quality level.
     * Quality is a float from 0.0 to 1.0 (1.0 = best quality, largest file).
     * For formats that don't support quality (like PNG), quality is ignored.
     */
    public byte[] encodeImageWithQuality(ImageData imageData, String format, float quality) throws IOException {
        Objects.requireNonNull(imageData, "imageData is required");
        String outputFormat = normalizeFormat(format);
        BufferedImage bufferedImage = new BufferedImage(imageData.width(), imageData.height(), BufferedImage.TYPE_INT_ARGB);
        bufferedImage.setRGB(0, 0, imageData.width(), imageData.height(), imageData.pixels(), 0, imageData.width());

        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName(outputFormat);
            if (!writers.hasNext()) {
                // Fallback to default write for unsupported formats
                boolean written = ImageIO.write(bufferedImage, outputFormat, outputStream);
                if (!written) {
                    throw new IOException("No ImageIO writer available for format: " + outputFormat);
                }
                return outputStream.toByteArray();
            }

            ImageWriter writer = writers.next();
            try (ImageOutputStream ios = ImageIO.createImageOutputStream(outputStream)) {
                writer.setOutput(ios);
                ImageWriteParam param = writer.getDefaultWriteParam();

                if (param.canWriteCompressed()) {
                    param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
                    String[] compressionTypes = param.getCompressionTypes();
                    if (compressionTypes != null && compressionTypes.length > 0) {
                        param.setCompressionType(compressionTypes[0]);
                    }
                    float clampedQuality = Math.max(0.0f, Math.min(1.0f, quality));
                    param.setCompressionQuality(clampedQuality);
                }

                writer.write(null, new javax.imageio.IIOImage(bufferedImage, null, null), param);
            } finally {
                writer.dispose();
            }
            return outputStream.toByteArray();
        }
    }

    /**
     * Gets the current size of encoded image for a given quality.
     */
    public long getEncodedSize(ImageData imageData, String format, float quality) throws IOException {
        return encodeImageWithQuality(imageData, format, quality).length;
    }

    private String normalizeFormat(String format) {
        if (format == null || format.isBlank()) {
            return "png";
        }
        String lower = format.toLowerCase();
        return lower.equals("jpeg") ? "jpg" : lower;
    }
}