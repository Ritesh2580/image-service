package com.imageservice.service;

import com.imageservice.config.ImageProcessingConfig;
import com.imageservice.model.ImageData;
import com.imageservice.model.TextWatermarkRequest;
import org.springframework.stereotype.Service;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Executor;

/**
 * Service for adding text overlays and watermarks to images.
 * Uses Graphics2D for text rendering with font styling and alpha blending.
 * Follows loose coupling by working with ImageData.
 */
@Service
public class ImageTextService {

    private final Executor executor;
    private final ImageProcessingConfig imageProcessingConfig;

    // Cache available fonts for performance
    private volatile List<String> cachedFonts;

    public ImageTextService(Executor imageProcessingExecutor,
                             ImageProcessingConfig imageProcessingConfig) {
        this.executor = imageProcessingExecutor;
        this.imageProcessingConfig = imageProcessingConfig;
    }

    /**
     * Returns list of available font family names on the system.
     * Uses GraphicsEnvironment to query system fonts.
     */
    public List<String> getAvailableFonts() {
        if (cachedFonts == null) {
            synchronized (this) {
                if (cachedFonts == null) {
                    GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
                    String[] fonts = ge.getAvailableFontFamilyNames();
                    cachedFonts = Arrays.asList(fonts);
                }
            }
        }
        return cachedFonts;
    }

    /**
     * Adds text to an image at specified position with styling.
     * Supports custom font, size, color, and transparency.
     *
     * @param sourceImage Source image data
     * @param request Text styling and position request
     * @return ImageData with text rendered on it
     */
    public ImageData addText(ImageData sourceImage, TextWatermarkRequest request) {
        if (sourceImage == null) {
            throw new IllegalArgumentException("Source image is required");
        }
        if (request == null || request.text() == null || request.text().isBlank()) {
            throw new IllegalArgumentException("Text is required");
        }

        int width = sourceImage.width();
        int height = sourceImage.height();
        
        // Create BufferedImage from ImageData
        BufferedImage bufferedImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        bufferedImage.setRGB(0, 0, width, height, sourceImage.pixels(), 0, width);

        // Create Graphics2D context
        Graphics2D g2d = bufferedImage.createGraphics();
        
        try {
            // Enable anti-aliasing for smooth text
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

            // Resolve font
            Font font = resolveFont(request.fontFamily(), request.fontSize());
            g2d.setFont(font);

            // Parse color and apply alpha
            int[] rgb = request.parseColor();
            int alpha = (int) (request.getAlphaFactor() * 255);
            Color color = new Color(rgb[0], rgb[1], rgb[2], Math.max(0, Math.min(255, alpha)));
            g2d.setColor(color);

            // Draw text at specified position
            int x = request.x();
            int y = request.y();
            
            // Adjust y for baseline (text is drawn from baseline, not top)
            FontMetrics fm = g2d.getFontMetrics();
            int textHeight = fm.getHeight();
            int baselineY = y + fm.getAscent(); // y is top of text box
            
            // Handle text wrapping for long text (simple wrapping)
            String text = request.text();
            if (text.length() > 0) {
                drawStringWithWrap(g2d, text, x, baselineY, width - x - 10);
            }
            
        } finally {
            g2d.dispose();
        }

        // Extract pixels back to ImageData
        int[] resultPixels = bufferedImage.getRGB(0, 0, width, height, null, 0, width);
        return new ImageData(width, height, resultPixels, sourceImage.format());
    }

    /**
     * Resolves font family name to actual Font, with fallback.
     */
    private Font resolveFont(String familyName, int size) {
        String family = (familyName == null || familyName.isBlank()) ? "SansSerif" : familyName;
        
        // Check if font family exists
        if (cachedFonts == null) {
            getAvailableFonts(); // Initialize cache
        }
        
        if (!cachedFonts.contains(family)) {
            // Try common fallbacks
            if (cachedFonts.contains("Arial")) family = "Arial";
            else if (cachedFonts.contains("Helvetica")) family = "Helvetica";
            else if (cachedFonts.contains("DejaVu Sans")) family = "DejaVu Sans";
            else family = Font.SANS_SERIF;
        }
        
        return new Font(family, Font.PLAIN, size);
    }

    /**
     * Draws string with simple word wrapping within maxWidth.
     */
    private void drawStringWithWrap(Graphics2D g2d, String text, int x, int y, int maxWidth) {
        FontMetrics fm = g2d.getFontMetrics();
        String[] words = text.split("\\s+");
        StringBuilder line = new StringBuilder();
        int lineY = y;

        for (String word : words) {
            String testLine = line.length() == 0 ? word : line + " " + word;
            int testWidth = fm.stringWidth(testLine);

            if (testWidth > maxWidth && line.length() > 0) {
                // Draw current line and start new
                g2d.drawString(line.toString(), x, lineY);
                line = new StringBuilder(word);
                lineY += fm.getHeight();
            } else {
                line = new StringBuilder(testLine);
            }
        }

        // Draw remaining text
        if (line.length() > 0) {
            g2d.drawString(line.toString(), x, lineY);
        }
    }

    /**
     * Gets default font size recommendation based on image dimensions.
     */
    public int getRecommendedFontSize(int imageWidth, int imageHeight) {
        // ~3% of smaller dimension, clamped to reasonable range
        int minDim = Math.min(imageWidth, imageHeight);
        int recommended = minDim / 30;
        return Math.max(12, Math.min(72, recommended));
    }
}
