package com.neuralhealer.backend.feature.email.gmail;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
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

    @Value("${frontend.url}")
    private String frontendBaseUrl;

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

            gmailSmtpService.sendEmail(email, subject, htmlBody);
            log.info("Password reset email sent to: {}", email);

        } catch (Exception e) {
            log.error("Error sending password reset email to {}: {}", email, e.getMessage());
            throw new EmailSendException(e.getMessage(), e);
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

            gmailSmtpService.sendEmail(email, subject, htmlBody);
            log.info("Verification email sent to: {}", email);

        } catch (Exception e) {
            log.error("Error sending verification email to {}: {}", email, e.getMessage());
            throw new EmailSendException(e.getMessage(), e);
        }
    }

    /**
     * Send OTP verification email using the OTP.html template.
     *
     * @param email    User's email address
     * @param userName User's first name
     * @param otpCode  6-digit OTP code
     * @throws EmailSendException if email fails to send
     */
    public void sendOtpVerificationEmail(String email, String userName, String otpCode) {
        try {
            String subject = "Verify Your Email - NeuralHealer";
            String htmlBody = renderOtpTemplate(userName, otpCode);

            gmailSmtpService.sendEmail(email, subject, htmlBody);
            log.info("OTP verification email sent to: {}", email);

        } catch (Exception e) {
            log.error("Error sending OTP email to {}: {}", email, e.getMessage());
            throw new EmailSendException(e.getMessage(), e);
        }
    }

    /**
     * Send a special thanks email with custom messages.
     *
     * @param email          Recipient email address
     * @param name           Recipient name
     * @param acknowledgment Personal acknowledgment text
     * @param milestone      Milestone message text
     * @param note           Exclusive note text
     * @throws EmailSendException if email fails to send
     */
    public void sendSpecialThanksEmail(String email, String name, String acknowledgment, String milestone,
            String note) {
        try {
            String subject = "A Moment Just For You - NeuralHealer";
            String htmlBody = renderSpecialThanksTemplate(name, acknowledgment, milestone, note);

            gmailSmtpService.sendEmail(email, subject, htmlBody);
            log.info("Special thanks email sent to: {}", email);

        } catch (Exception e) {
            log.error("Error sending special thanks email to {}: {}", email, e.getMessage());
            throw new EmailSendException(e.getMessage(), e);
        }
    }

    /**
     * Send an engagement request email from a patient to a doctor.
     */
    public void sendEngagementRequestFromPatient(String email, String doctorName, String patientName, String token,
            String accessRule, String message, String engagementCode) {
        try {
            String subject = "New Engagement Request from " + patientName;
            String verificationLink = frontendBaseUrl + "/verify-engagement?token=" + token;
            String htmlBody = renderEngagementRequestTemplate(doctorName, patientName, token, accessRule, message,
                    verificationLink);

            gmailSmtpService.sendEmail(email, subject, htmlBody);
            log.info("Engagement request email sent to doctor: {}", email);

        } catch (Exception e) {
            log.error("Error sending engagement request email to {}: {}", email, e.getMessage());
            throw new EmailSendException(e.getMessage(), e);
        }
    }

    /**
     * Send an engagement activation confirmation to a patient.
     */
    public void sendEngagementActivatedToPatient(String email, String patientName, String doctorName,
            String engagementCode, String accessRule, String activationDate) {
        try {
            String subject = "✅ Engagement Request Accepted - Dr. " + doctorName;
            String dashboardLink = frontendBaseUrl + "/dashboard";
            String htmlBody = renderEngagementActivatedTemplate(patientName, doctorName, engagementCode, accessRule,
                    activationDate, dashboardLink);

            gmailSmtpService.sendEmail(email, subject, htmlBody);
            log.info("Engagement activation email sent to patient: {}", email);

        } catch (Exception e) {
            log.error("Error sending engagement activation email to {}: {}", email, e.getMessage());
            throw new EmailSendException(e.getMessage(), e);
        }
    }

    /**
     * Send a general engagement verification token.
     */
    public void sendEngagementToken(String email, String recipientName, String initiatorName, String token) {
        try {
            String subject = "Engagement Verification Code - NeuralHealer";
            String htmlBody = renderEngagementVerificationTemplate(recipientName, initiatorName, token);

            gmailSmtpService.sendEmail(email, subject, htmlBody);
            log.info("Engagement token email sent to: {}", email);

        } catch (Exception e) {
            log.error("Error sending engagement token email to {}: {}", email, e.getMessage());
            throw new EmailSendException(e.getMessage(), e);
        }
    }

    /**
     * Send an engagement refreshed token notification.
     */
    public void sendEngagementRefreshedToken(String email, String recipientName, String initiatorName, String token,
            int expiryMinutes) {
        try {
            String subject = "New Verification Code - Engagement Refreshed";
            String verificationUrl = frontendBaseUrl + "/verify-engagement?token=" + token;
            String htmlBody = renderEngagementRefreshedTemplate(recipientName, initiatorName, token, expiryMinutes,
                    verificationUrl);

            gmailSmtpService.sendEmail(email, subject, htmlBody);
            log.info("Engagement refreshed token email sent to: {}", email);

        } catch (Exception e) {
            log.error("Error sending engagement refreshed token email to {}: {}", email, e.getMessage());
            throw new EmailSendException(e.getMessage(), e);
        }
    }

    /**
     * Build the password reset link.
     */
    private String buildResetLink(String resetToken) {
        return frontendBaseUrl + "/reset-password?token=" + resetToken;
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
     * Render the special thanks email HTML template.
     */
    private String renderSpecialThanksTemplate(String name, String acknowledgment, String milestone, String note) {
        String template = loadTemplate("special-thanks.html");

        return template
                .replace("{RECIPIENT_NAME}", name)
                .replace("{PERSONAL_ACKNOWLEDGMENT}", acknowledgment)
                .replace("{MILESTONE_MESSAGE}", milestone)
                .replace("{EXCLUSIVE_NOTE}", note);
    }

    private String renderEngagementRequestTemplate(String doctorName, String patientName, String token,
            String accessRule, String message, String verificationLink) {
        String template = loadTemplate("engagement-request-from-patient.html");

        return template
                .replace("{DOCTOR_NAME}", doctorName)
                .replace("{PATIENT_NAME}", patientName)
                .replace("{TOKEN}", token)
                .replace("{ACCESS_RULE}", accessRule)
                .replace("{PATIENT_MESSAGE}", message != null ? message : "No message provided.")
                .replace("{VERIFICATION_LINK}", verificationLink);
    }

    private String renderEngagementActivatedTemplate(String patientName, String doctorName, String engagementCode,
            String accessRule, String activationDate, String dashboardLink) {
        String template = loadTemplate("engagement-activated-by-doctor.html");

        return template
                .replace("{PATIENT_NAME}", patientName)
                .replace("{DOCTOR_NAME}", doctorName)
                .replace("{ENGAGEMENT_CODE}", engagementCode)
                .replace("{ACCESS_RULE}", accessRule)
                .replace("{ACTIVATION_DATE}", activationDate)
                .replace("{DASHBOARD_LINK}", dashboardLink);
    }

    private String renderEngagementVerificationTemplate(String recipientName, String initiatorName, String token) {
        String template = loadTemplate("engagement-started.html"); // Use existing matching structure or
                                                                   // engagment-verification.html

        return template
                .replace("{USER_NAME}", recipientName)
                .replace("{DOCTOR_NAME}", initiatorName)
                .replace("{TOKEN}", token);
    }

    private String renderEngagementRefreshedTemplate(String recipientName, String initiatorName, String token,
            int expiryMinutes, String verificationUrl) {
        String template = loadTemplate("engagement-refreshed.html");

        return template
                .replace("{RECIPIENT_NAME}", recipientName)
                .replace("{INITIATOR_NAME}", initiatorName)
                .replace("{NEW_TOKEN}", token)
                .replace("{EXPIRY_MINUTES}", String.valueOf(expiryMinutes))
                .replace("{VERIFICATION_URL}", verificationUrl);
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
     * Render the OTP email HTML template.
     */
    private String renderOtpTemplate(String userName, String otpCode) {
        String template = loadTemplate("OTP.html");

        return template
                .replace("{USER_NAME}", userName)
                .replace("{OTP_CODE}", otpCode)
                .replace("{EXPIRY_MINUTES}", "15")
                .replace("{SUPPORT_EMAIL}", "support@neuralhealer.com");
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
        } else if (templateName.equals("special-thanks.html")) {
            return """
                    <!DOCTYPE html>
                    <html>
                    <body style="font-family: Arial, sans-serif; padding: 20px; background-color: #1A1625; color: #ECE8F5;">
                        <div style="max-width: 600px; margin: 0 auto; background-color: #241D30; padding: 30px; border-radius: 8px; border: 1px solid #3F3651;">
                            <h2 style="text-align: center; font-weight: 300;">{RECIPIENT_NAME}</h2>
                            <p style="text-align: center; font-style: italic; color: #C5B8D9;">{PERSONAL_ACKNOWLEDGMENT}</p>
                            <p style="text-align: center;">{MILESTONE_MESSAGE}</p>
                            <p style="text-align: center; color: #D8CFE8;">{EXCLUSIVE_NOTE}</p>
                            <div style="margin-top: 30px; text-align: center; border-top: 1px solid #3F3651; padding-top: 20px;">
                                <p>With gratitude & pride,<br/>The Neural Healer Team</p>
                            </div>
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
