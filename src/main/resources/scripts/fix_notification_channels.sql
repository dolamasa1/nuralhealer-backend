-- ================================================================
-- FIX: Add channels column and update template data
-- ================================================================
-- Purpose: Fix the notification email channel issue
-- This script ensures the channels column exists and has correct data

-- STEP 1: Add channels column if it doesn't exist
ALTER TABLE notification_message_templates 
ADD COLUMN IF NOT EXISTS channels JSONB DEFAULT '{"email": false, "push": false, "sse": true}'::jsonb;

-- STEP 2: Update existing templates that should send emails

-- USER_WELCOME templates
UPDATE notification_message_templates 
SET channels = '{"email": true, "sse": true}'::jsonb
WHERE template_key = 'USER_WELCOME';

-- ENGAGEMENT_STARTED templates  
UPDATE notification_message_templates 
SET channels = '{"email": true, "sse": true}'::jsonb
WHERE template_key = 'ENGAGEMENT_STARTED';

-- ENGAGEMENT_CANCELLED templates
UPDATE notification_message_templates 
SET channels = '{"email": true, "sse": true}'::jsonb
WHERE template_key = 'ENGAGEMENT_CANCELLED';

-- USER_REENGAGE_ACTIVE templates
UPDATE notification_message_templates 
SET channels = '{"email": true, "sse": true}'::jsonb
WHERE template_key = 'USER_REENGAGE_ACTIVE';

-- USER_INACTIVITY_WARNING templates
UPDATE notification_message_templates 
SET channels = '{"email": true, "sse": true}'::jsonb
WHERE template_key = 'USER_INACTIVITY_WARNING';

-- STEP 3: Verify the fix
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

-- Expected: All listed templates should show email_enabled = true
