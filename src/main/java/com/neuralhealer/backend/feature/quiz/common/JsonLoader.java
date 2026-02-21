package com.neuralhealer.backend.feature.quiz.common;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.springframework.util.StreamUtils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

@Component
public class JsonLoader {
    private final ObjectMapper objectMapper = new ObjectMapper();

    public <T> T loadJson(String filePath, Class<T> valueType) throws IOException {
        ClassPathResource resource = new ClassPathResource(filePath);

        if (!resource.exists()) {
            throw new IOException("File not found: " + filePath +
                    ". Make sure the file exists in src/main/resources/" + filePath);
        }

        try (InputStream inputStream = resource.getInputStream()) {
            return objectMapper.readValue(inputStream, valueType);
        }
    }

    public String loadJsonAsString(String filePath) throws IOException {
        ClassPathResource resource = new ClassPathResource(filePath);

        if (!resource.exists()) {
            throw new IOException("File not found: " + filePath);
        }

        try (InputStream inputStream = resource.getInputStream()) {
            return StreamUtils.copyToString(inputStream, StandardCharsets.UTF_8);
        }
    }
}