package com.neuralhealer.backend.integration.gmail;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/**
 * Service for sending direct emails (not through the notification queue).
 * Used for password resets, email verification, and other immediate email
 * needs.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DirectEmailService {

    private final GmailSmtpService gmailSmtpService;

    /**
     * Send a password reset email with a reset link.
     *
     * @param email      User's email address
     * @param resetToken Password reset token to include in the link
     * @throws EmailSendException if email fails to send
     */
    public void sendPasswordResetEmail(String email, String resetToken) {
        try {
            String subject = "Reset Your Password - NeuralHealer";
            String resetLink = buildResetLink(resetToken);
            String htmlBody = renderPasswordResetTemplate(resetLink, email);

            boolean success = gmailSmtpService.sendEmail(email, subject, htmlBody);

            if (!success) {
                throw new EmailSendException("Failed to send password reset email to: " + email);
            }

            log.info("Password reset email sent to: {}", email);

        } catch (Exception e) {
            log.error("Error sending password reset email to {}: {}", email, e.getMessage());
            throw new EmailSendException("Failed to send password reset email", e);
        }
    }

    /**
     * Send an email verification email with a 6-digit code.
     *
     * @param email            User's email address
     * @param verificationCode 6-digit verification code
     * @throws EmailSendException if email fails to send
     */
    public void sendVerificationEmail(String email, String verificationCode) {
        try {
            String subject = "Verify Your Email - NeuralHealer";
            String htmlBody = renderVerificationTemplate(verificationCode, email);

            boolean success = gmailSmtpService.sendEmail(email, subject, htmlBody);

            if (!success) {
                throw new EmailSendException("Failed to send verification email to: " + email);
            }

            log.info("Verification email sent to: {}", email);

        } catch (Exception e) {
            log.error("Error sending verification email to {}: {}", email, e.getMessage());
            throw new EmailSendException("Failed to send verification email", e);
        }
    }

    /**
     * Build the password reset link.
     */
    private String buildResetLink(String resetToken) {
        // TODO: Update with actual frontend URL
        String baseUrl = "https://neuralhealer.com";
        return baseUrl + "/reset-password?token=" + resetToken;
    }

    /**
     * Render the password reset email HTML template.
     */
    private String renderPasswordResetTemplate(String resetLink, String email) {
        String template = loadTemplate("password-reset.html");

        return template
                .replace("{RESET_LINK}", resetLink)
                .replace("{USER_EMAIL}", email);
    }

    /**
     * Render the email verification HTML template.
     */
    private String renderVerificationTemplate(String verificationCode, String email) {
        String template = loadTemplate("email-verification.html");

        return template
                .replace("{VERIFICATION_CODE}", verificationCode)
                .replace("{USER_EMAIL}", email);
    }

    /**
     * Load HTML template from resources/templates/emails/
     */
    private String loadTemplate(String templateName) {
        try (InputStream is = getClass().getClassLoader().getResourceAsStream("templates/emails/" + templateName)) {
            if (is == null) {
                log.warn("Template {} not found, using fallback", templateName);
                return getFallbackTemplate(templateName);
            }
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            log.error("Error loading template {}: {}", templateName, e.getMessage());
            return getFallbackTemplate(templateName);
        }
    }

    /**
     * Provide a fallback HTML template if file is not found.
     */
    private String getFallbackTemplate(String templateName) {
        if (templateName.equals("password-reset.html")) {
            return """
                    <!DOCTYPE html>
                    <html>
                    <body style="font-family: Arial, sans-serif; padding: 20px; background-color: #f4f4f4;">
                        <div style="max-width: 600px; margin: 0 auto; background-color: white; padding: 30px; border-radius: 8px;">
                            <h2 style="color: #333;">Reset Your Password</h2>
                            <p>You requested to reset your password for {USER_EMAIL}.</p>
                            <p>Click the button below to reset your password:</p>
                            <a href="{RESET_LINK}" style="display: inline-block; padding: 12px 24px; background-color: #4CAF50; color: white; text-decoration: none; border-radius: 4px; margin: 20px 0;">Reset Password</a>
                            <p style="color: #666; font-size: 14px;">This link will expire in 1 hour.</p>
                            <p style="color: #999; fontSize: 12px; margin-top: 30px;">If you didn't request this, please ignore this email.</p>
                        </div>
                    </body>
                    </html>
                    """;
        } else if (templateName.equals("email-verification.html")) {
            return """
                    <!DOCTYPE html>
                    <html>
                    <body style="font-family: Arial, sans-serif; padding: 20px; background-color: #f4f4f4;">
                        <div style="max-width: 600px; margin: 0 auto; background-color: white; padding: 30px; border-radius: 8px;">
                            <h2 style="color: #333;">Verify Your Email</h2>
                            <p>Thank you for signing up! Please use the code below to verify your email address:</p>
                            <div style="font-size: 32px; font-weight: bold; color: #4CAF50; padding: 20px; background-color: #f9f9f9; text-align: center; border-radius: 4px; margin: 20px 0;">{VERIFICATION_CODE}</div>
                            <p style="color: #666; font-size: 14px;">Enter this code in the verification page to complete your registration.</p>
                            <p style="color: #999; font-size: 12px; margin-top: 30px;">If you didn't create an account, please ignore this email.</p>
                        </div>
                    </body>
                    </html>
                    """;
        }
        return "<html><body><p>Email template not available</p></body></html>";
    }

    /**
     * Custom exception for email sending failures.
     */
    public static class EmailSendException extends RuntimeException {
        public EmailSendException(String message) {
            super(message);
        }

        public EmailSendException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
