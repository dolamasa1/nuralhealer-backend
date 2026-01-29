-- ================================================================
-- DIAGNOSTIC SCRIPT: Notification Email Channel Investigation
-- ================================================================
-- Purpose: Diagnose why send_email flag is always false
-- Run this script to check the notification system configuration

-- 1. CHECK IF CHANNELS COLUMN EXISTS
SELECT 
    column_name, 
    data_type, 
    column_default,
    is_nullable
FROM information_schema.columns 
WHERE table_name = 'notification_message_templates' 
AND column_name = 'channels';

-- Expected: One row showing channels JSONB column
-- If no rows: Column doesn't exist (this is the problem!)

-- 2. CHECK USER_WELCOME TEMPLATE DATA
SELECT 
    template_key, 
    language_code, 
    recipient_context, 
    channels,
    created_at,
    updated_at
FROM notification_message_templates
WHERE template_key = 'USER_WELCOME';

-- Expected: Two rows (en and ar) with channels = {"email": true, "sse": true}

-- 3. TEST JSONB EXTRACTION (verify data type conversion works)
SELECT 
    template_key,
    language_code,
    channels as raw_channels,
    channels->>'email' as email_string,
    (channels->>'email')::boolean as email_boolean,
    COALESCE((channels->>'email')::boolean, false) as email_with_fallback
FROM notification_message_templates
WHERE template_key IN ('USER_WELCOME', 'ENGAGEMENT_STARTED', 'USER_REENGAGE_ACTIVE');

-- Expected: email_boolean should be 'true' for these templates

-- 4. CHECK RECENT NOTIFICATIONS
SELECT 
    id, 
    user_id, 
    type, 
    title,
    send_email, 
    delivery_status,
    created_at
FROM notifications
ORDER BY created_at DESC
LIMIT 10;

-- Expected: send_email should be true for USER_WELCOME, ENGAGEMENT_STARTED, etc.
-- If all false: The problem is confirmed

-- 5. CHECK MESSAGE QUEUE FOR EMAIL JOBS
SELECT 
    id, 
    job_type, 
    status, 
    payload->>'templateKey' as template,
    payload->>'userId' as user_id,
    created_at,
    processed_at
FROM message_queues
WHERE job_type = 'EMAIL_NOTIFICATION'
ORDER BY created_at DESC
LIMIT 10;

-- Expected: Should see EMAIL_NOTIFICATION jobs if send_email was true
-- If no rows: Confirms that send_email is false (no jobs created)

-- 6. CHECK ALL TEMPLATES WITH EMAIL ENABLED
SELECT 
    template_key,
    language_code,
    recipient_context,
    channels,
    channels->>'email' as should_send_email
FROM notification_message_templates
WHERE (channels->>'email')::boolean = true
ORDER BY template_key, language_code;

-- Expected: List of all templates configured to send emails

-- 7. SUMMARY: Count templates by email setting
SELECT 
    CASE 
        WHEN channels IS NULL THEN 'NULL (column missing or not set)'
        WHEN (channels->>'email')::boolean = true THEN 'Email ENABLED'
        ELSE 'Email DISABLED'
    END as email_status,
    COUNT(*) as template_count
FROM notification_message_templates
GROUP BY 
    CASE 
        WHEN channels IS NULL THEN 'NULL (column missing or not set)'
        WHEN (channels->>'email')::boolean = true THEN 'Email ENABLED'
        ELSE 'Email DISABLED'
    END;
