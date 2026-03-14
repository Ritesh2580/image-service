package com.imageservice.service;

import com.imageservice.entity.ImageEntity;
import com.imageservice.model.ImageData;
import com.imageservice.repository.ImageRepository;
import org.springframework.stereotype.Service;

@Service
public class ImageStorageService {
    private final ImageRepository imageRepository;

    public ImageStorageService(ImageRepository imageRepository) {
        this.imageRepository = imageRepository;
    }

    public ImageEntity saveImage(ImageData imageData, byte[] imageBytes, String originalFilename,
                                 String contentType, String sourceUrl, String operationType) {
        ImageEntity entity = new ImageEntity();
        entity.setOriginalFilename(originalFilename);
        entity.setContentType(contentType);
        entity.setWidth(imageData.width());
        entity.setHeight(imageData.height());
        entity.setImageData(imageBytes);
        entity.setSourceUrl(sourceUrl);
        entity.setOperationType(operationType);
        return imageRepository.save(entity);
    }
}