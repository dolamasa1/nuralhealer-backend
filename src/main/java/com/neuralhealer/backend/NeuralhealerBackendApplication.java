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
		System.out.println("🌱 Starting NeuralHealer Backend...");
		Dotenv dotenv = Dotenv.configure().ignoreIfMissing().load();

		dotenv.entries().forEach(entry -> {
			String key = entry.getKey();
			String value = entry.getValue();

			// Precedence: System Property > .env > Environment (usually)
			// But for developer convenience we often want .env to definitely be set
			// if it's there.
			if (System.getProperty(key) == null) {
				System.setProperty(key, value);
			}

			// Core Profile Logic
			if ("SPRING_PROFILES_ACTIVE".equals(key)) {
				System.setProperty("spring.profiles.active", value);
				System.out.println("🚀 Active Profile set from .env: " + value);
			}
		});

		// Fallback for profile
		if (System.getProperty("spring.profiles.active") == null) {
			System.setProperty("spring.profiles.active", "dev");
			System.out.println("⚠️ No profile found in .env, defaulting to: dev");
		}

		SpringApplication.run(NeuralhealerBackendApplication.class, args);
	}

}
