ackage com.neuralhealer.backend.shared.config.StaticResourceConfig;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class StaticResourceConfig implements WebMvcConfigurer {

    @Value("${file.storage.base-path}")
    private String storageBasePath;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // Files stored in 'storage' directory will be served under '/api/files/**'
        // Since context-path is '/api', we use '/files/**' here
        String location = "file:" + storageBasePath + "/";
        if (!location.endsWith("/")) {
            location += "/";
        }

        registry.addResourceHandler("/files/**")
                .addResourceLocations(location)
                .setCachePeriod(3600); // 1 hour
    }
}
