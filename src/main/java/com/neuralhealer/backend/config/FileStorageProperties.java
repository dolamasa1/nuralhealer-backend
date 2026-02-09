package com.neuralhealer.backend.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * Configuration properties for file storage and image processing.
 * Centralizes all file-related limits and paths.
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "file")
public class FileStorageProperties {

    /**
     * Base path where all files will be stored.
     */
    private Storage storage = new Storage();

    /**
     * Maximum file size allowed in MegaBytes.
     */
    private int maxSizeMb = 10;

    /**
     * Backend root URL for public file links.
     */
    private String backendRootUrl = "http://localhost:8080";

    /**
     * List of allowed image extensions.
     */
    private List<String> allowedFormats = List.of("jpg", "jpeg", "png", "webp");

    private Image image = new Image();
    private Thumbnail thumbnail = new Thumbnail();

    @Data
    public static class Storage {
        private String basePath;
        private String profilePictures = "doctors/profiles";
    }

    @Data
    public static class Image {
        private int minDimension = 512;
        private int maxDimension = 2048;
        private int compressionQuality = 85;
    }

    @Data
    public static class Thumbnail {
        private int size = 256;
    }
}
