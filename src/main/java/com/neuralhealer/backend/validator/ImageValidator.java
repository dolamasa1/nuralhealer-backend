package com.neuralhealer.backend.validator;

import com.neuralhealer.backend.exception.FileSizeExceededException;
import com.neuralhealer.backend.exception.InvalidAspectRatioException;
import com.neuralhealer.backend.exception.InvalidImageFormatException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

@Component
public class ImageValidator {

    @Value("${file.max-size-mb:5}")
    private int maxSizeMb;

    @Value("${file.allowed-formats:jpg,jpeg,png,webp}")
    private String allowedFormats;

    @Value("${file.image.min-dimension:512}")
    private int minDimension;

    @Value("${file.image.max-dimension:2048}")
    private int maxDimension;

    public void validateImage(MultipartFile file) {
        // Check if file is empty
        if (file.isEmpty()) {
            throw new InvalidImageFormatException("File is empty");
        }

        // Check file size
        long maxSizeBytes = maxSizeMb * 1024L * 1024L;
        if (file.getSize() > maxSizeBytes) {
            throw new FileSizeExceededException("File size exceeds " + maxSizeMb + "MB");
        }

        // Check file format
        String extension = getFileExtension(file.getOriginalFilename());
        List<String> allowedExtensions = Arrays.asList(allowedFormats.split(","));
        if (extension == null || !allowedExtensions.contains(extension.toLowerCase())) {
            throw new InvalidImageFormatException("Invalid image format. Allowed formats: " + allowedFormats);
        }

        try {
            BufferedImage image = ImageIO.read(file.getInputStream());
            if (image == null) {
                throw new InvalidImageFormatException("Invalid image file or corrupted");
            }

            int width = image.getWidth();
            int height = image.getHeight();

            // Check dimensions
            if (width < minDimension || height < minDimension) {
                throw new InvalidImageFormatException(
                        "Image dimensions too small. Minimum: " + minDimension + "x" + minDimension);
            }
            if (width > maxDimension || height > maxDimension) {
                throw new InvalidImageFormatException(
                        "Image dimensions too large. Maximum: " + maxDimension + "x" + maxDimension);
            }

            // Check aspect ratio (tolerance 5%)
            double aspectRatio = (double) width / height;
            if (aspectRatio < 0.95 || aspectRatio > 1.05) {
                throw new InvalidAspectRatioException(
                        "Image must be square (1:1 ratio). Found: " + width + "x" + height);
            }

        } catch (IOException e) {
            throw new InvalidImageFormatException("Could not read image file");
        }
    }

    private String getFileExtension(String fileName) {
        if (fileName == null || fileName.lastIndexOf(".") == -1) {
            return null;
        }
        return fileName.substring(fileName.lastIndexOf(".") + 1);
    }
}
