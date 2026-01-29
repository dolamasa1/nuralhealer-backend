package com.neuralhealer.backend.diagnostic;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Diagnostic controller to investigate and fix notification email channel
 * issues.
 * This is a temporary diagnostic tool - can be removed after the issue is
 * resolved.
 */
@RestController
@RequestMapping("/api/diagnostic/notifications")
public class NotificationChannelDiagnosticController {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    /**
     * Step 1: Check if channels column exists
     */
    @GetMapping("/check-channels-column")
    public Map<String, Object> checkChannelsColumn() {
        Map<String, Object> result = new HashMap<>();

        try {
            String sql = "SELECT column_name, data_type, column_default, is_nullable " +
                    "FROM information_schema.columns " +
                    "WHERE table_name = 'notification_message_templates' " +
                    "AND column_name = 'channels'";

            List<Map<String, Object>> columns = jdbcTemplate.queryForList(sql);

            result.put("exists", !columns.isEmpty());
            result.put("details", columns);
            result.put("message", columns.isEmpty()
                    ? "❌ PROBLEM FOUND: 'channels' column does NOT exist!"
                    : "✅ 'channels' column exists");

        } catch (Exception e) {
            result.put("error", e.getMessage());
        }

        return result;
    }

    /**
     * Step 2: Check USER_WELCOME template data
     */
    @GetMapping("/check-welcome-template")
    public Map<String, Object> checkWelcomeTemplate() {
        Map<String, Object> result = new HashMap<>();

        try {
            String sql = "SELECT template_key, language_code, recipient_context, " +
                    "channels, created_at, updated_at " +
                    "FROM notification_message_templates " +
                    "WHERE template_key = 'USER_WELCOME'";

            List<Map<String, Object>> templates = jdbcTemplate.queryForList(sql);

            result.put("found", !templates.isEmpty());
            result.put("count", templates.size());
            result.put("templates", templates);

        } catch (Exception e) {
            result.put("error", e.getMessage());
            result.put("message", "Error querying templates - channels column might not exist");
        }

        return result;
    }

    /**
     * Step 3: Test JSONB extraction
     */
    @GetMapping("/test-jsonb-extraction")
    public Map<String, Object> testJsonbExtraction() {
        Map<String, Object> result = new HashMap<>();

        try {
            String sql = "SELECT " +
                    "template_key, " +
                    "language_code, " +
                    "channels as raw_channels, " +
                    "channels->>'email' as email_string, " +
                    "(channels->>'email')::boolean as email_boolean, " +
                    "COALESCE((channels->>'email')::boolean, false) as email_with_fallback " +
                    "FROM notification_message_templates " +
                    "WHERE template_key IN ('USER_WELCOME', 'ENGAGEMENT_STARTED', 'USER_REENGAGE_ACTIVE')";

            List<Map<String, Object>> extractions = jdbcTemplate.queryForList(sql);

            result.put("extractions", extractions);
            result.put("message", "JSONB extraction test completed");

        } catch (Exception e) {
            result.put("error", e.getMessage());
            result.put("message", "Error testing JSONB extraction - channels column might not exist");
        }

        return result;
    }

    /**
     * Step 4: Check recent notifications
     */
    @GetMapping("/check-recent-notifications")
    public Map<String, Object> checkRecentNotifications() {
        Map<String, Object> result = new HashMap<>();

        try {
            String sql = "SELECT id, user_id, type, title, send_email, " +
                    "delivery_status, created_at " +
                    "FROM notifications " +
                    "ORDER BY created_at DESC " +
                    "LIMIT 10";

            List<Map<String, Object>> notifications = jdbcTemplate.queryForList(sql);

            long trueCount = notifications.stream()
                    .filter(n -> Boolean.TRUE.equals(n.get("send_email")))
                    .count();

            result.put("notifications", notifications);
            result.put("total", notifications.size());
            result.put("sendEmailTrue", trueCount);
            result.put("sendEmailFalse", notifications.size() - trueCount);
            result.put("message", trueCount == 0
                    ? "⚠️ All recent notifications have send_email=false"
                    : "✅ Some notifications have send_email=true");

        } catch (Exception e) {
            result.put("error", e.getMessage());
        }

        return result;
    }

    /**
     * Step 5: Check email queue
     */
    @GetMapping("/check-email-queue")
    public Map<String, Object> checkEmailQueue() {
        Map<String, Object> result = new HashMap<>();

        try {
            String sql = "SELECT id, job_type, status, " +
                    "payload->>'templateKey' as template, " +
                    "payload->>'userId' as user_id, " +
                    "created_at, processed_at " +
                    "FROM message_queues " +
                    "WHERE job_type = 'EMAIL_NOTIFICATION' " +
                    "ORDER BY created_at DESC " +
                    "LIMIT 10";

            List<Map<String, Object>> jobs = jdbcTemplate.queryForList(sql);

            result.put("jobs", jobs);
            result.put("count", jobs.size());
            result.put("message", jobs.isEmpty()
                    ? "⚠️ No email jobs found - confirms send_email is always false"
                    : "✅ Email jobs exist in queue");

        } catch (Exception e) {
            result.put("error", e.getMessage());
        }

        return result;
    }

    /**
     * FIX: Add channels column and update template data
     */
    @PostMapping("/apply-fix")
    public Map<String, Object> applyFix() {
        Map<String, Object> result = new HashMap<>();
        List<String> steps = new java.util.ArrayList<>();

        try {
            // Step 1: Add channels column if it doesn't exist
            jdbcTemplate.execute(
                    "ALTER TABLE notification_message_templates " +
                            "ADD COLUMN IF NOT EXISTS channels JSONB DEFAULT '{\"email\": false, \"push\": false, \"sse\": true}'::jsonb");
            steps.add("✅ Added channels column (if missing)");

            // Step 2: Update USER_WELCOME templates
            int updated = jdbcTemplate.update(
                    "UPDATE notification_message_templates " +
                            "SET channels = '{\"email\": true, \"sse\": true}'::jsonb " +
                            "WHERE template_key = 'USER_WELCOME'");
            steps.add("✅ Updated USER_WELCOME templates: " + updated + " rows");

            // Step 3: Update ENGAGEMENT_STARTED templates
            updated = jdbcTemplate.update(
                    "UPDATE notification_message_templates " +
                            "SET channels = '{\"email\": true, \"sse\": true}'::jsonb " +
                            "WHERE template_key = 'ENGAGEMENT_STARTED'");
            steps.add("✅ Updated ENGAGEMENT_STARTED templates: " + updated + " rows");

            // Step 4: Update ENGAGEMENT_CANCELLED templates
            updated = jdbcTemplate.update(
                    "UPDATE notification_message_templates " +
                            "SET channels = '{\"email\": true, \"sse\": true}'::jsonb " +
                            "WHERE template_key = 'ENGAGEMENT_CANCELLED'");
            steps.add("✅ Updated ENGAGEMENT_CANCELLED templates: " + updated + " rows");

            // Step 5: Update USER_REENGAGE_ACTIVE templates
            updated = jdbcTemplate.update(
                    "UPDATE notification_message_templates " +
                            "SET channels = '{\"email\": true, \"sse\": true}'::jsonb " +
                            "WHERE template_key = 'USER_REENGAGE_ACTIVE'");
            steps.add("✅ Updated USER_REENGAGE_ACTIVE templates: " + updated + " rows");

            // Step 6: Update USER_INACTIVITY_WARNING templates
            updated = jdbcTemplate.update(
                    "UPDATE notification_message_templates " +
                            "SET channels = '{\"email\": true, \"sse\": true}'::jsonb " +
                            "WHERE template_key = 'USER_INACTIVITY_WARNING'");
            steps.add("✅ Updated USER_INACTIVITY_WARNING templates: " + updated + " rows");

            result.put("success", true);
            result.put("steps", steps);
            result.put("message",
                    "✅ Fix applied successfully! The channels column now exists and templates are updated.");

        } catch (Exception e) {
            result.put("success", false);
            result.put("error", e.getMessage());
            result.put("steps", steps);
        }

        return result;
    }

    /**
     * Verify the fix
     */
    @GetMapping("/verify-fix")
    public Map<String, Object> verifyFix() {
        Map<String, Object> result = new HashMap<>();

        try {
            String sql = "SELECT " +
                    "template_key, " +
                    "language_code, " +
                    "recipient_context, " +
                    "channels, " +
                    "(channels->>'email')::boolean as email_enabled " +
                    "FROM notification_message_templates " +
                    "WHERE template_key IN ('USER_WELCOME', 'ENGAGEMENT_STARTED', 'ENGAGEMENT_CANCELLED', " +
                    "'USER_REENGAGE_ACTIVE', 'USER_INACTIVITY_WARNING') " +
                    "ORDER BY template_key, language_code";

            List<Map<String, Object>> templates = jdbcTemplate.queryForList(sql);

            long emailEnabled = templates.stream()
                    .filter(t -> Boolean.TRUE.equals(t.get("email_enabled")))
                    .count();

            result.put("templates", templates);
            result.put("total", templates.size());
            result.put("emailEnabled", emailEnabled);
            result.put("success", emailEnabled == templates.size());
            result.put("message", emailEnabled == templates.size()
                    ? "✅ All templates correctly configured with email enabled"
                    : "⚠️ Some templates still have email disabled");

        } catch (Exception e) {
            result.put("error", e.getMessage());
        }

        return result;
    }

    /**
     * Run all diagnostics
     */
    @GetMapping("/run-all-diagnostics")
    public Map<String, Object> runAllDiagnostics() {
        Map<String, Object> result = new HashMap<>();

        result.put("1_channels_column", checkChannelsColumn());
        result.put("2_welcome_template", checkWelcomeTemplate());
        result.put("3_jsonb_extraction", testJsonbExtraction());
        result.put("4_recent_notifications", checkRecentNotifications());
        result.put("5_email_queue", checkEmailQueue());

        return result;
    }
}
