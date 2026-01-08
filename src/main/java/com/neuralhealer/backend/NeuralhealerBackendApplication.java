package com.neuralhealer.backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

@SpringBootApplication
@EnableCaching
public class NeuralhealerBackendApplication {

	public static void main(String[] args) {
		SpringApplication.run(NeuralhealerBackendApplication.class, args);
	}

}
