package com.neuralhealer.backend.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Startup task to ensure the notification_message_templates table has the
 * channels column.
 * This is a one-time migration that runs on application startup.
 */
@Component
public class NotificationChannelsMigration implements CommandLineRunner {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Override
    public void run(String... args) {
        try {
            System.out.println("=".repeat(80));
            System.out.println("NOTIFICATION CHANNELS MIGRATION - Starting...");
            System.out.println("=".repeat(80));

            // Step 1: Check if channels column exists
            String checkColumnSql = "SELECT column_name FROM information_schema.columns " +
                    "WHERE table_name = 'notification_message_templates' AND column_name = 'channels'";

            var columns = jdbcTemplate.queryForList(checkColumnSql, String.class);

            if (columns.isEmpty()) {
                System.out.println("❌ PROBLEM DETECTED: 'channels' column does NOT exist!");
                System.out.println("   Applying fix...");

                // Add the column
                jdbcTemplate.execute(
                        "ALTER TABLE notification_message_templates " +
                                "ADD COLUMN channels JSONB DEFAULT '{\"email\": false, \"push\": false, \"sse\": true}'::jsonb");
                System.out.println("✅ Added 'channels' column to notification_message_templates");
            } else {
                System.out.println("✅ 'channels' column already exists");
            }

            // Step 2: Update templates that should send emails
            System.out.println("\nUpdating template channel configurations...");

            int updated = jdbcTemplate.update(
                    "UPDATE notification_message_templates " +
                            "SET channels = '{\"email\": true, \"sse\": true}'::jsonb " +
                            "WHERE template_key = 'USER_WELCOME'");
            if (updated > 0) {
                System.out.println("✅ Updated USER_WELCOME templates: " + updated + " rows");
            }

            updated = jdbcTemplate.update(
                    "UPDATE notification_message_templates " +
                            "SET channels = '{\"email\": true, \"sse\": true}'::jsonb " +
                            "WHERE template_key = 'ENGAGEMENT_STARTED'");
            if (updated > 0) {
                System.out.println("✅ Updated ENGAGEMENT_STARTED templates: " + updated + " rows");
            }

            updated = jdbcTemplate.update(
                    "UPDATE notification_message_templates " +
                            "SET channels = '{\"email\": true, \"sse\": true}'::jsonb " +
                            "WHERE template_key = 'ENGAGEMENT_CANCELLED'");
            if (updated > 0) {
                System.out.println("✅ Updated ENGAGEMENT_CANCELLED templates: " + updated + " rows");
            }

            updated = jdbcTemplate.update(
                    "UPDATE notification_message_templates " +
                            "SET channels = '{\"email\": true, \"sse\": true}'::jsonb " +
                            "WHERE template_key = 'USER_REENGAGE_ACTIVE'");
            if (updated > 0) {
                System.out.println("✅ Updated USER_REENGAGE_ACTIVE templates: " + updated + " rows");
            }

            updated = jdbcTemplate.update(
                    "UPDATE notification_message_templates " +
                            "SET channels = '{\"email\": true, \"sse\": true}'::jsonb " +
                            "WHERE template_key = 'USER_INACTIVITY_WARNING'");
            if (updated > 0) {
                System.out.println("✅ Updated USER_INACTIVITY_WARNING templates: " + updated + " rows");
            }

            // Step 3: Verify the fix
            System.out.println("\nVerifying configuration...");
            String verifySql = "SELECT template_key, language_code, (channels->>'email')::boolean as email_enabled " +
                    "FROM notification_message_templates " +
                    "WHERE template_key IN ('USER_WELCOME', 'ENGAGEMENT_STARTED', 'ENGAGEMENT_CANCELLED', " +
                    "'USER_REENGAGE_ACTIVE', 'USER_INACTIVITY_WARNING') " +
                    "ORDER BY template_key, language_code";

            var results = jdbcTemplate.queryForList(verifySql);
            long emailEnabled = results.stream()
                    .filter(r -> Boolean.TRUE.equals(r.get("email_enabled")))
                    .count();

            System.out.println("   Total templates checked: " + results.size());
            System.out.println("   Email enabled: " + emailEnabled);
            System.out.println("   Email disabled: " + (results.size() - emailEnabled));

            if (emailEnabled == results.size()) {
                System.out.println("\n✅ SUCCESS: All notification templates are correctly configured!");
                System.out.println("   Emails will now be sent for:");
                System.out.println("   - USER_WELCOME (signup)");
                System.out.println("   - ENGAGEMENT_STARTED");
                System.out.println("   - ENGAGEMENT_CANCELLED");
                System.out.println("   - USER_REENGAGE_ACTIVE (3 days inactive)");
                System.out.println("   - USER_INACTIVITY_WARNING (14 days inactive)");
            } else {
                System.out.println("\n⚠️ WARNING: Some templates still have email disabled");
            }

            System.out.println("=".repeat(80));
            System.out.println("NOTIFICATION CHANNELS MIGRATION - Complete");
            System.out.println("=".repeat(80));

        } catch (Exception e) {
            System.err.println("❌ ERROR during notification channels migration: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
