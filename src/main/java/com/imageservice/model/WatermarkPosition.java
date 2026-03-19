package com.imageservice.model;

/**
 * Enum defining watermark placement positions on the base image.
 */
public enum WatermarkPosition {
    TOP_LEFT,
    TOP_RIGHT,
    BOTTOM_LEFT,
    BOTTOM_RIGHT,
    CENTER;

    /**
     * Calculates the top-left coordinates for placing watermark at this position.
     *
     * @param baseWidth Width of the base image
     * @param baseHeight Height of the base image
     * @param watermarkWidth Width of the watermark image
     * @param watermarkHeight Height of the watermark image
     * @param padding Padding from edges (default 10)
     * @return int[2] with [x, y] coordinates
     */
    public int[] calculatePosition(int baseWidth, int baseHeight, 
                                    int watermarkWidth, int watermarkHeight, int padding) {
        int x, y;
        switch (this) {
            case TOP_LEFT:
                x = padding;
                y = padding;
                break;
            case TOP_RIGHT:
                x = baseWidth - watermarkWidth - padding;
                y = padding;
                break;
            case BOTTOM_LEFT:
                x = padding;
                y = baseHeight - watermarkHeight - padding;
                break;
            case BOTTOM_RIGHT:
                x = baseWidth - watermarkWidth - padding;
                y = baseHeight - watermarkHeight - padding;
                break;
            case CENTER:
                x = (baseWidth - watermarkWidth) / 2;
                y = (baseHeight - watermarkHeight) / 2;
                break;
            default:
                x = baseWidth - watermarkWidth - padding;
                y = baseHeight - watermarkHeight - padding;
        }
        // Ensure within bounds
        x = Math.max(0, Math.min(x, baseWidth - 1));
        y = Math.max(0, Math.min(y, baseHeight - 1));
        return new int[]{x, y};
    }
}
