package com.imageservice.service;

import com.imageservice.util.ValidationUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class ImageValidationService {
    private final int maxDimension;

    public ImageValidationService(@Value("${image.processing.max-dimension:10000}") int maxDimension) {
        this.maxDimension = maxDimension;
    }

    public void validateDimensions(int width, int height) {
        ValidationUtil.validateImageDimensions(width, height, maxDimension);
    }

    public int getMaxDimension() {
        return maxDimension;
    }
}