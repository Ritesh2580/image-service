package com.imageservice.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.OperatingSystemMXBean;
import java.util.concurrent.Executor;

@Configuration
public class ImageProcessingConfig {
    
    @Value("${image.processing.thread-pool-size:0}")
    private int configuredThreadPoolSize;
    
    @Value("${image.processing.chunk-size:10000}")
    private int chunkSize;
    
    /**
     * Detects the optimal number of threads based on available processors.
     * Uses CPU-bound formula: cores + 1 for compute-intensive tasks
     */
    public int detectOptimalThreadCount() {
        if (configuredThreadPoolSize > 0) {
            return configuredThreadPoolSize;
        }
        
        int availableProcessors = Runtime.getRuntime().availableProcessors();
        // For CPU-intensive image processing, use cores + 1
        // This accounts for one thread potentially waiting on I/O
        return availableProcessors + 1;
    }
    
    /**
     * Gets system capability information for logging and monitoring
     */
    public SystemCapabilities getSystemCapabilities() {
        Runtime runtime = Runtime.getRuntime();
        OperatingSystemMXBean osBean = ManagementFactory.getOperatingSystemMXBean();
        MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
        
        return new SystemCapabilities(
            runtime.availableProcessors(),
            osBean.getArch(),
            runtime.maxMemory(),
            runtime.totalMemory(),
            runtime.freeMemory(),
            memoryBean.getHeapMemoryUsage().getMax(),
            detectOptimalThreadCount()
        );
    }
    
    @Bean(name = "imageProcessingExecutor")
    public Executor imageProcessingExecutor() {
        int poolSize = detectOptimalThreadCount();
        
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(poolSize);
        executor.setMaxPoolSize(poolSize * 2);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("image-proc-");
        executor.setRejectedExecutionHandler(new java.util.concurrent.ThreadPoolExecutor.CallerRunsPolicy());
        executor.initialize();
        
        return executor;
    }
    
    public int getChunkSize() {
        return chunkSize;
    }
    
    /**
     * Record to hold system capability information
     */
    public record SystemCapabilities(
        int availableProcessors,
        String architecture,
        long maxMemory,
        long totalMemory,
        long freeMemory,
        long heapMaxMemory,
        int optimalThreadCount
    ) {
        @Override
        public String toString() {
            return String.format(
                "SystemCapabilities{processors=%d, arch='%s', maxMemory=%d MB, totalMemory=%d MB, freeMemory=%d MB, heapMax=%d MB, optimalThreads=%d}",
                availableProcessors,
                architecture,
                maxMemory / (1024 * 1024),
                totalMemory / (1024 * 1024),
                freeMemory / (1024 * 1024),
                heapMaxMemory / (1024 * 1024),
                optimalThreadCount
            );
        }
    }
}