package com.neuralhealer.backend.service;

import com.neuralhealer.backend.shared.config.FileStorageProperties;
import com.neuralhealer.backend.shared.service.FileStorageService;
import com.neuralhealer.backend.shared.validator.ImageValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class FileStorageServiceTest {

    private FileStorageService fileStorageService;
    private FileStorageProperties fileProperties;
    private ImageValidator imageValidator;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        imageValidator = mock(ImageValidator.class);
        fileProperties = new FileStorageProperties();
        fileProperties.getStorage().setBasePath(tempDir.toString());
        fileProperties.getStorage().setProfilePictures("doctors/profiles");
        fileProperties.getThumbnail().setSize(256);
        fileProperties.setBackendRootUrl("http://localhost:8080");

        fileStorageService = new FileStorageService(imageValidator, fileProperties);
        fileStorageService.init();
    }

    @Test
    void saveProfilePicture_Success() throws IOException {
        UUID doctorId = UUID.randomUUID();
        byte[] imageBytes = createMockImage(500, 500);
        MockMultipartFile file = new MockMultipartFile("file", "test.jpg", "image/jpeg", imageBytes);

        String relativePath = fileStorageService.saveProfilePicture(file, doctorId);

        assertNotNull(relativePath);
        assertTrue(relativePath.contains(doctorId.toString()));
        assertTrue(
                Files.exists(tempDir.resolve("doctors/profiles").resolve(doctorId.toString()).resolve("profile.jpg")));
        assertTrue(Files
                .exists(tempDir.resolve("doctors/profiles").resolve(doctorId.toString()).resolve("profile_thumb.jpg")));

        verify(imageValidator).validateImage(file);
    }

    @Test
    void deleteProfilePicture_RemovesDirectory() throws IOException {
        UUID doctorId = UUID.randomUUID();
        Path doctorDir = tempDir.resolve("doctors/profiles").resolve(doctorId.toString());
        Files.createDirectories(doctorDir);
        Files.writeString(doctorDir.resolve("test.txt"), "hello");

        fileStorageService.deleteProfilePicture(doctorId);

        assertFalse(Files.exists(doctorDir));
    }

    @Test
    void getPublicUrl_Works() {
        String relativePath = "doctors/profiles/abc/profile.jpg";

        String fullUrl = fileStorageService.getPublicUrl(relativePath, false);
        String thumbUrl = fileStorageService.getPublicUrl(relativePath, true);

        assertEquals("http://localhost:8080/api/files/doctors/profiles/abc/profile.jpg", fullUrl);
        assertEquals("http://localhost:8080/api/files/doctors/profiles/abc/profile_thumb.jpg", thumbUrl);
    }

    private byte[] createMockImage(int width, int height) throws IOException {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(image, "jpg", baos);
        return baos.toByteArray();
    }
}
