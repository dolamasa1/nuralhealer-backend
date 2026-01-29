-- ================================================================
-- CRITICAL FIX: Recreate create_system_notification() function
-- ================================================================
-- This updates the database function to read the channels column
-- Run this script to fix the notification email issue

-- Drop and recreate the function with the correct logic
CREATE OR REPLACE FUNCTION create_system_notification(
    p_user_id UUID,
    p_template_key VARCHAR(100),
    p_placeholders JSONB DEFAULT '{}'::jsonb,
    p_source VARCHAR(50) DEFAULT 'system'
) RETURNS UUID AS $$
DECLARE
    v_notification_id UUID;
    v_msg RECORD;
    v_send_email BOOLEAN := FALSE;
    v_delivery_status JSONB;
    v_language VARCHAR(10);
    v_recipient_context VARCHAR(50);
    v_key TEXT;
    v_value TEXT;
BEGIN
    -- Get user info
    SELECT 
        COALESCE(NULLIF(language, ''), 'en'),
        CASE 
            WHEN EXISTS (SELECT 1 FROM doctor_profiles WHERE user_id = p_user_id) THEN 'doctor'
            ELSE 'patient'
        END
    INTO v_language, v_recipient_context
    FROM users WHERE id = p_user_id;
    
    RAISE NOTICE 'NOTIFICATION DEBUG: Searching for template %, lang %, context %', p_template_key, v_language, v_recipient_context;
    
    -- Get template with channels
    WITH template_search AS (
        SELECT 
            nmt.title, 
            nmt.message, 
            nmt.default_priority,
            COALESCE(nmt.channels, '{"email": false, "push": false, "sse": true}'::jsonb) as channels
        FROM notification_message_templates nmt
        WHERE LOWER(nmt.template_key) = LOWER(p_template_key)
          AND nmt.language_code = COALESCE(v_language, 'en')
          AND LOWER(nmt.recipient_context) = LOWER(v_recipient_context)
        UNION ALL
        SELECT 
            nmt.title, 
            nmt.message, 
            nmt.default_priority,
            COALESCE(nmt.channels, '{"email": false, "push": false, "sse": true}'::jsonb) as channels
        FROM notification_message_templates nmt
        WHERE LOWER(nmt.template_key) = LOWER(p_template_key)
          AND nmt.language_code = 'en'
          AND LOWER(nmt.recipient_context) = LOWER(v_recipient_context)
        UNION ALL
        SELECT 
            nmt.title, 
            nmt.message, 
            nmt.default_priority,
            COALESCE(nmt.channels, '{"email": false, "push": false, "sse": true}'::jsonb) as channels
        FROM notification_message_templates nmt
        WHERE LOWER(nmt.template_key) = LOWER(p_template_key)
          AND nmt.language_code = 'en'
        LIMIT 1
    )
    SELECT * INTO v_msg FROM template_search LIMIT 1;
    
    -- Absolute fallback
    IF v_msg.title IS NULL THEN
        RAISE NOTICE 'NOTIFICATION DEBUG: Template NOT found. using absolute fallback.';
        v_msg.title := INITCAP(REPLACE(p_template_key, '_', ' '));
        v_msg.message := 'Notification: ' || p_template_key;
        v_msg.default_priority := 'normal';
        v_msg.channels := '{"email": false, "push": false, "sse": true}'::jsonb;
    ELSE
        RAISE NOTICE 'NOTIFICATION DEBUG: Template FOUND. Channels: %', v_msg.channels;
    END IF;
    
    -- Replace placeholders
    IF p_placeholders IS NOT NULL THEN
        FOR v_key, v_value IN SELECT * FROM jsonb_each_text(p_placeholders) LOOP
            v_msg.title := REPLACE(v_msg.title, '{' || v_key || '}', COALESCE(v_value, ''));
            v_msg.message := REPLACE(v_msg.message, '{' || v_key || '}', COALESCE(v_value, ''));
        END LOOP;
    END IF;
    
    -- Set email flag based on template channels
    v_send_email := COALESCE((v_msg.channels->>'email')::boolean, false);
    
    RAISE NOTICE 'NOTIFICATION DEBUG: Calculated email flag: %', v_send_email;
    
    -- Build delivery_status JSONB from channels
    v_delivery_status := jsonb_build_object(
        'sse', COALESCE((v_msg.channels->>'sse')::boolean, true),
        'email', v_send_email,
        'push', COALESCE((v_msg.channels->>'push')::boolean, false),
        'sms', COALESCE((v_msg.channels->>'sms')::boolean, false),
        'whatsapp', COALESCE((v_msg.channels->>'whatsapp')::boolean, false)
    );
    
    -- Insert notification
    INSERT INTO notifications (
        user_id, 
        type, 
        title, 
        message, 
        payload, 
        priority, 
        source, 
        send_email, 
        delivery_status,
        sent_at
    ) VALUES (
        p_user_id,
        p_template_key,
        v_msg.title,
        v_msg.message,
        p_placeholders,
        COALESCE(v_msg.default_priority, 'normal'),
        p_source,
        v_send_email,
        v_delivery_status,
        NOW()
    ) RETURNING id INTO v_notification_id;
    
    RAISE NOTICE 'NOTIFICATION DEBUG: Notification record created with ID % and send_email=%', v_notification_id, v_send_email;
    
    RETURN v_notification_id;
    
EXCEPTION WHEN OTHERS THEN
    RAISE WARNING 'NOTIFICATION ERROR: Failed to create % notification for user %: %', p_template_key, p_user_id, SQLERRM;
    RETURN NULL;
END;
$$ LANGUAGE plpgsql;

-- Test the function to verify it works
DO $$
DECLARE
    test_user_id UUID;
    test_notif_id UUID;
    test_send_email BOOLEAN;
    test_delivery_status JSONB;
BEGIN
    -- Get a test user
    SELECT id INTO test_user_id FROM users LIMIT 1;
    
    IF test_user_id IS NOT NULL THEN
        RAISE NOTICE '========================================';
        RAISE NOTICE 'Testing create_system_notification()';
        RAISE NOTICE '========================================';
        
        -- Create a test notification
        test_notif_id := create_system_notification(
            test_user_id,
            'USER_WELCOME',
            jsonb_build_object('userName', 'Test User')
        );
        
        -- Check the result
        IF test_notif_id IS NOT NULL THEN
            RAISE NOTICE 'Test notification created: %', test_notif_id;
            
            -- Get the notification details
            SELECT send_email, delivery_status 
            INTO test_send_email, test_delivery_status
            FROM notifications 
            WHERE id = test_notif_id;
            
            RAISE NOTICE 'send_email: %', test_send_email;
            RAISE NOTICE 'delivery_status: %', test_delivery_status;
            
            IF test_send_email = TRUE THEN
                RAISE NOTICE '✅ SUCCESS: Email flag is TRUE!';
            ELSE
                RAISE WARNING '❌ FAILED: Email flag is still FALSE!';
            END IF;
        ELSE
            RAISE WARNING 'Test notification creation failed!';
        END IF;
        
        RAISE NOTICE '========================================';
    END IF;
END $$;
