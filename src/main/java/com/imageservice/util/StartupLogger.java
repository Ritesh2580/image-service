package com.imageservice.util;

import com.imageservice.config.ImageProcessingConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
public class StartupLogger implements ApplicationRunner {
    private static final Logger logger = LoggerFactory.getLogger(StartupLogger.class);
    private final ImageProcessingConfig imageProcessingConfig;

    public StartupLogger(ImageProcessingConfig imageProcessingConfig) {
        this.imageProcessingConfig = imageProcessingConfig;
    }

    @Override
    public void run(ApplicationArguments args) {
        ImageProcessingConfig.SystemCapabilities capabilities = imageProcessingConfig.getSystemCapabilities();
        logger.info("Image processing system capabilities: {}", capabilities);
    }
}