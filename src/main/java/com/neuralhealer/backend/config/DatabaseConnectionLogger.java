package com.neuralhealer.backend.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.util.Arrays;

@Component
public class DatabaseConnectionLogger implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(DatabaseConnectionLogger.class);

    private final Environment environment;

    @Value("${spring.datasource.url}")
    private String dbUrl;

    public DatabaseConnectionLogger(Environment environment) {
        this.environment = environment;
    }

    @Override
    public void run(String... args) throws Exception {
        String[] activeProfiles = environment.getActiveProfiles();
        String profile = (activeProfiles.length > 0) ? activeProfiles[0] : "default";

        String dbType = "UNKNOWN";
        if (dbUrl.contains("localhost") || dbUrl.contains("127.0.0.1")) {
            dbType = "LOCAL";
        } else if (dbUrl.contains("supabase")) {
            dbType = "GLOBAL (Supabase)";
        }

        logger.info("===========================================================");
        logger.info("DATABASE CONNECTION STATUS");
        logger.info("-----------------------------------------------------------");
        logger.info("Active Profile: {}", Arrays.toString(activeProfiles));
        logger.info("Database Type : {}", dbType);
        logger.info("Database URL  : {}", dbUrl);
        logger.info("===========================================================");
    }
}
