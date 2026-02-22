package com.neuralhealer.backend.shared.service;

import com.neuralhealer.backend.shared.config.FileStorageProperties;
import com.neuralhealer.backend.shared.exception.BadRequestException;
import com.neuralhealer.backend.shared.validator.ImageValidator;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

@Service
public class FileStorageService {

    private static final Logger logger = LoggerFactory.getLogger(FileStorageService.class);

    private final ImageValidator imageValidator;
    private final FileStorageProperties fileProperties;
    private final Path storageLocation;

    public FileStorageService(ImageValidator imageValidator, FileStorageProperties fileProperties) {
        this.imageValidator = imageValidator;
        this.fileProperties = fileProperties;
        this.storageLocation = Paths.get(fileProperties.getStorage().getBasePath()).toAbsolutePath().normalize();
    }

    @PostConstruct
    public void init() {
        try {
            Files.createDirectories(this.storageLocation);
            Files.createDirectories(this.storageLocation.resolve(fileProperties.getStorage().getProfilePictures()));
            logger.info("Storage directories initialized at: {}", this.storageLocation);
        } catch (IOException e) {
            logger.error("Could not initialize storage directory", e);
            throw new RuntimeException("Could not initialize storage directory", e);
        }
    }

    public String saveProfilePicture(MultipartFile file, UUID doctorId) {
        imageValidator.validateImage(file);

        String fileName = "profile.jpg";
        String thumbName = "profile_thumb.jpg";
        String subPath = fileProperties.getStorage().getProfilePictures();
        Path doctorProfilePath = this.storageLocation.resolve(subPath).resolve(doctorId.toString());

        try {
            Files.createDirectories(doctorProfilePath);

            BufferedImage originalImage = ImageIO.read(file.getInputStream());
            BufferedImage squareImage = ensureSquareRatio(originalImage);

            // Save full size
            Path targetPath = doctorProfilePath.resolve(fileName);
            ImageIO.write(squareImage, "jpg", targetPath.toFile());

            // Save thumbnail
            BufferedImage thumbnail = createThumbnail(squareImage, fileProperties.getThumbnail().getSize());
            Path thumbPath = doctorProfilePath.resolve(thumbName);
            ImageIO.write(thumbnail, "jpg", thumbPath.toFile());

            return subPath + "/" + doctorId + "/" + fileName;

        } catch (IOException e) {
            logger.error("Could not save profile picture for doctor: {}", doctorId, e);
            throw new BadRequestException("Could not save profile picture");
        }
    }

    public void deleteProfilePicture(UUID doctorId) {
        Path doctorProfilePath = this.storageLocation.resolve(fileProperties.getStorage().getProfilePictures())
                .resolve(doctorId.toString());
        try {
            if (Files.exists(doctorProfilePath)) {
                // Delete all files in the directory then the directory itself
                Files.walk(doctorProfilePath)
                        .sorted((p1, p2) -> p2.compareTo(p1)) // Delete files before directories
                        .forEach(path -> {
                            try {
                                Files.delete(path);
                            } catch (IOException e) {
                                logger.error("Failed to delete path: {}", path, e);
                            }
                        });
            }
        } catch (IOException e) {
            logger.error("Could not delete profile directory for doctor: {}", doctorId, e);
        }
    }

    public String getPublicUrl(String relativePath, boolean thumbnail) {
        if (!StringUtils.hasText(relativePath)) {
            return null;
        }

        String path = relativePath;
        if (thumbnail && path.endsWith(".jpg")) {
            path = path.replace("profile.jpg", "profile_thumb.jpg");
        }

        return fileProperties.getBackendRootUrl() + "/api/files/" + path;
    }

    private BufferedImage ensureSquareRatio(BufferedImage image) {
        int width = image.getWidth();
        int height = image.getHeight();
        int size = Math.min(width, height);

        int x = (width - size) / 2;
        int y = (height - size) / 2;

        return image.getSubimage(x, y, size, size);
    }

    private BufferedImage createThumbnail(BufferedImage image, int size) {
        Image scaledImage = image.getScaledInstance(size, size, Image.SCALE_SMOOTH);
        BufferedImage thumbnail = new BufferedImage(size, size, BufferedImage.TYPE_INT_RGB);

        Graphics2D g2d = thumbnail.createGraphics();
        g2d.drawImage(scaledImage, 0, 0, null);
        g2d.dispose();

        return thumbnail;
    }
}
