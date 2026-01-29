-- ================================================================
-- MANUAL FIX: Update notification template channels
-- ================================================================
-- Run this script to immediately fix the email channel configuration
-- This will force-update all templates that should send emails

-- Update USER_WELCOME templates (signup notification)
UPDATE notification_message_templates 
SET channels = '{"email": true, "sse": true}'::jsonb
WHERE template_key = 'USER_WELCOME';

-- Update ENGAGEMENT_STARTED templates
UPDATE notification_message_templates 
SET channels = '{"email": true, "sse": true}'::jsonb
WHERE template_key = 'ENGAGEMENT_STARTED';

-- Update ENGAGEMENT_CANCELLED templates
UPDATE notification_message_templates 
SET channels = '{"email": true, "sse": true}'::jsonb
WHERE template_key = 'ENGAGEMENT_CANCELLED';

-- Update USER_REENGAGE_ACTIVE templates (3 days inactive)
UPDATE notification_message_templates 
SET channels = '{"email": true, "sse": true}'::jsonb
WHERE template_key = 'USER_REENGAGE_ACTIVE';

-- Update USER_INACTIVITY_WARNING templates (14 days inactive)
UPDATE notification_message_templates 
SET channels = '{"email": true, "sse": true}'::jsonb
WHERE template_key = 'USER_INACTIVITY_WARNING';

-- Verify the updates
SELECT 
    template_key,
    language_code,
    recipient_context,
    channels,
    (channels->>'email')::boolean as email_enabled
FROM notification_message_templates
WHERE template_key IN ('USER_WELCOME', 'ENGAGEMENT_STARTED', 'ENGAGEMENT_CANCELLED', 
                       'USER_REENGAGE_ACTIVE', 'USER_INACTIVITY_WARNING')
ORDER BY template_key, language_code;

-- Expected result: All rows should show email_enabled = true
