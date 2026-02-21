package com.neuralhealer.backend.validator;

import com.neuralhealer.backend.shared.config.FileStorageProperties;
import com.neuralhealer.backend.shared.validator.ImageValidator;
import com.neuralhealer.backend.shared.exception.FileSizeExceededException;
import com.neuralhealer.backend.shared.exception.InvalidImageFormatException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ImageValidatorTest {

    private ImageValidator imageValidator;
    private FileStorageProperties fileProperties;

    @BeforeEach
    void setUp() {
        fileProperties = new FileStorageProperties();
        fileProperties.setMaxSizeMb(5);
        fileProperties.setAllowedFormats(List.of("jpg", "jpeg", "png", "webp"));
        fileProperties.getImage().setMinDimension(10);
        fileProperties.getImage().setMaxDimension(2000);

        imageValidator = new ImageValidator(fileProperties);
    }

    @Test
    void validateImage_Success() throws IOException {
        byte[] imageBytes = createMockImage(100, 100, "jpg");
        MockMultipartFile file = new MockMultipartFile("file", "test.jpg", "image/jpeg", imageBytes);

        assertDoesNotThrow(() -> imageValidator.validateImage(file));
    }

    @Test
    void validateImage_EmptyFile() {
        MockMultipartFile file = new MockMultipartFile("file", "test.jpg", "image/jpeg", new byte[0]);

        assertThrows(InvalidImageFormatException.class, () -> imageValidator.validateImage(file));
    }

    @Test
    void validateImage_SizeExceeded() {
        byte[] largeBytes = new byte[6 * 1024 * 1024]; // 6MB
        MockMultipartFile file = new MockMultipartFile("file", "test.jpg", "image/jpeg", largeBytes);

        assertThrows(FileSizeExceededException.class, () -> imageValidator.validateImage(file));
    }

    @Test
    void validateImage_InvalidFormat() {
        MockMultipartFile file = new MockMultipartFile("file", "test.txt", "text/plain", "hello".getBytes());

        assertThrows(InvalidImageFormatException.class, () -> imageValidator.validateImage(file));
    }

    private byte[] createMockImage(int width, int height, String format) throws IOException {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(image, format, baos);
        return baos.toByteArray();
    }
}
