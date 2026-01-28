package com.neuralhealer.backend.integration.gmail.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;

import java.util.Properties;

/**
 * Configuration class for Gmail SMTP integration.
 * Sets up JavaMailSender bean with Gmail-specific settings.
 */
@Configuration
@Slf4j
public class GmailConfig {

    @Value("${spring.mail.host}")
    private String mailHost;

    @Value("${spring.mail.port}")
    private int mailPort;

    @Value("${spring.mail.username}")
    private String mailUsername;

    @Value("${spring.mail.password}")
    private String mailPassword;

    @Bean
    public JavaMailSender javaMailSender() {
        if (mailUsername == null || mailUsername.isBlank() || mailUsername.contains("your-email")) {
            log.error("GMAIL_USERNAME is not configured in .env");
        }

        JavaMailSenderImpl mailSender = new JavaMailSenderImpl();
        mailSender.setHost(mailHost);
        mailSender.setPort(mailPort);
        mailSender.setUsername(mailUsername);

        // Remove spaces from app password if present (Google shows them as 'xxxx xxxx
        // xxxx xxxx')
        String sanitizedPassword = mailPassword != null ? mailPassword.replace(" ", "") : "";
        mailSender.setPassword(sanitizedPassword);

        if (sanitizedPassword.isEmpty()) {
            log.warn("GMAIL_APP_PASSWORD is empty or missing in .env");
        }

        Properties props = mailSender.getJavaMailProperties();
        props.put("mail.transport.protocol", "smtp");
        props.put("mail.smtp.auth", "true");

        // Support both 587 (STARTTLS) and 465 (SSL)
        if (mailPort == 465) {
            props.put("mail.smtp.ssl.enable", "true");
            props.put("mail.smtp.socketFactory.port", "465");
            props.put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
        } else {
            props.put("mail.smtp.starttls.enable", "true");
            props.put("mail.smtp.starttls.required", "true");
        }

        props.put("mail.smtp.connectiontimeout", "5000");
        props.put("mail.smtp.timeout", "5000");
        props.put("mail.smtp.writetimeout", "5000");
        props.put("mail.debug", "false");

        return mailSender;
    }
}
