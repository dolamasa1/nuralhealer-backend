package com.neuralhealer.backend;

import io.github.cdimascio.dotenv.Dotenv;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableCaching
@EnableScheduling
public class NeuralhealerBackendApplication {

	public static void main(String[] args) {
		// Load environment variables from .env file
		Dotenv dotenv = Dotenv.configure().ignoreIfMissing().load();
		System.out.println("🌱 Loading Environment Variables from .env...");

		dotenv.entries().forEach(entry -> {
			String key = entry.getKey();
			String value = entry.getValue();

			// Set as system property if not already set
			if (System.getProperty(key) == null) {
				System.setProperty(key, value);
			}

			// Explicitly map SPRING_PROFILES_ACTIVE to spring.profiles.active for Spring
			// Boot
			if ("SPRING_PROFILES_ACTIVE".equals(key)) {
				System.setProperty("spring.profiles.active", value);
				System.out.println("🚀 Active Profile set to: " + value);
			}
		});

		SpringApplication.run(NeuralhealerBackendApplication.class, args);
	}

}
