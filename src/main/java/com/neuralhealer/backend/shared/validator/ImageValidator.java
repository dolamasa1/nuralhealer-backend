ackage com.neuralhealer.backend.shared.validator.ImageValidator;

import com.neuralhealer.backend.shared.config.FileStorageProperties;
import com.neuralhealer.backend.shared.exception.FileSizeExceededException;
import com.neuralhealer.backend.shared.exception.InvalidImageFormatException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.List;

@Component
@RequiredArgsConstructor
public class ImageValidator {

    private final FileStorageProperties fileProperties;

    public void validateImage(MultipartFile file) {
        // Check if file is empty
        if (file.isEmpty()) {
            throw new InvalidImageFormatException("File is empty");
        }

        // Check file size
        long maxSizeBytes = fileProperties.getMaxSizeMb() * 1024L * 1024L;
        if (file.getSize() > maxSizeBytes) {
            throw new FileSizeExceededException("File size exceeds " + fileProperties.getMaxSizeMb() + "MB");
        }

        // Check file format
        String extension = getFileExtension(file.getOriginalFilename());
        List<String> allowedExtensions = fileProperties.getAllowedFormats();
        if (extension == null || !allowedExtensions.contains(extension.toLowerCase())) {
            throw new InvalidImageFormatException("Invalid image format. Allowed formats: " +
                    String.join(", ", allowedExtensions));
        }

        try {
            BufferedImage image = ImageIO.read(file.getInputStream());
            if (image == null) {
                throw new InvalidImageFormatException("Invalid image file or corrupted");
            }

            int width = image.getWidth();
            int height = image.getHeight();

            // Check dimensions
            int minDim = fileProperties.getImage().getMinDimension();
            int maxDim = fileProperties.getImage().getMaxDimension();

            if (width < minDim || height < minDim) {
                throw new InvalidImageFormatException(
                        "Image dimensions too small. Minimum: " + minDim + "x" + minDim);
            }
            if (width > maxDim || height > maxDim) {
                throw new InvalidImageFormatException(
                        "Image dimensions too large. Maximum: " + maxDim + "x" + maxDim);
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
