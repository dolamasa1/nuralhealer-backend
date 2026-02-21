package com.neuralhealer.backend.feature.email.gmail;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

/**
 * Core service for sending emails via Gmail SMTP.
 * Provides low-level email sending capability with HTML support.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class GmailSmtpService {

    private final JavaMailSender mailSender;

    /**
     * Send an email with HTML content.
     *
     * @param to       Recipient email address
     * @param subject  Email subject
     * @param htmlBody HTML content of the email
     * @throws RuntimeException if email fails to send with specific cause
     * 
     * 
     */
    public void sendEmail(String to, String subject, String htmlBody) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlBody, true); // true = HTML content

            mailSender.send(message);
            log.info("Email sent successfully to: {}, subject: {}", to, subject);

        } catch (org.springframework.mail.MailAuthenticationException e) {
            log.error("Authentication failed for Gmail SMTP: {}", e.getMessage());
            throw new RuntimeException(
                    "Email Authentication Failed: Please verify your GMAIL_USERNAME and GMAIL_APP_PASSWORD in .env. Note: App Passwords must be generated in Google Security settings and should not contain spaces.",
                    e);
        } catch (MessagingException e) {
            log.error("Failed to send email to: {}, subject: {}, error: {}", to, subject, e.getMessage());
            throw new RuntimeException("SMTP Error: " + e.getMessage(), e);
        } catch (Exception e) {
            log.error("Unexpected error sending email to: {}, error: {}", to, e.getMessage());
            throw new RuntimeException("Unexpected Email Error: " + e.getMessage(), e);
        }
    }

    /**
     * Send a plain text email.
     *
     * @param to       Recipient email address
     * @param subject  Email subject
     * @param textBody Plain text content of the email
     * @return true if email was sent successfully, false otherwise
     */
    public boolean sendTextEmail(String to, String subject, String textBody) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(textBody, false); // false = plain text

            mailSender.send(message);

            log.info("Plain text email sent successfully to: {}, subject: {}", to, subject);
            return true;

        } catch (MessagingException e) {
            log.error("Failed to send plain text email to: {}, subject: {}, error: {}", to, subject, e.getMessage());
            return false;
        } catch (Exception e) {
            log.error("Unexpected error sending plain text email to: {}, error: {}", to, e.getMessage());
            return false;
        }
    }
}
