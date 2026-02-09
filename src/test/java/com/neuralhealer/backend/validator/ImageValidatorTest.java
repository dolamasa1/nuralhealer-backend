package com.neuralhealer.backend.validator;

import com.neuralhealer.backend.exception.FileSizeExceededException;
import com.neuralhealer.backend.exception.InvalidAspectRatioException;
import com.neuralhealer.backend.exception.InvalidImageFormatException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ImageValidatorTest {

    private ImageValidator imageValidator;

    @BeforeEach
    void setUp() {
        imageValidator = new ImageValidator();
        ReflectionTestUtils.setField(imageValidator, "maxSizeMb", 5);
        ReflectionTestUtils.setField(imageValidator, "allowedFormats", "jpg,jpeg,png,webp");
        ReflectionTestUtils.setField(imageValidator, "minDimension", 10);
        ReflectionTestUtils.setField(imageValidator, "maxDimension", 2000);
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

    @Test
    void validateImage_InvalidAspectRatio() throws IOException {
        byte[] imageBytes = createMockImage(200, 100, "jpg");
        MockMultipartFile file = new MockMultipartFile("file", "test.jpg", "image/jpeg", imageBytes);

        assertThrows(InvalidAspectRatioException.class, () -> imageValidator.validateImage(file));
    }

    private byte[] createMockImage(int width, int height, String format) throws IOException {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(image, format, baos);
        return baos.toByteArray();
    }
}
