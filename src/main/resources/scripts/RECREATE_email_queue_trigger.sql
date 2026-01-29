-- ================================================================
-- FIX: Recreate Email Queue Trigger
-- ================================================================
-- This recreates the trigger that automatically queues email jobs
-- when a notification is created with send_email = TRUE

-- Drop and recreate the trigger function
CREATE OR REPLACE FUNCTION trigger_queue_email_job()
RETURNS TRIGGER AS $$
BEGIN
    RAISE NOTICE 'TRIGGER DEBUG: trg_auto_queue_email fired for notification %', NEW.id;
    RAISE NOTICE 'TRIGGER DEBUG: send_email = %', NEW.send_email;
    
    IF NEW.send_email = TRUE THEN
        RAISE NOTICE 'TRIGGER DEBUG: Creating email job in message_queues';
        
        INSERT INTO message_queues (
            job_type, status, payload, created_at
        ) VALUES (
            'EMAIL_NOTIFICATION',
            'pending',
            jsonb_build_object(
                'notificationId', NEW.id,
                'userId', NEW.user_id,
                'templateKey', NEW.type,
                'userName', COALESCE(NEW.payload->>'userName', 'User'),
                'doctorName', COALESCE(NEW.payload->>'doctorName', 'Doctor'),
                'title', NEW.title,
                'body', NEW.message
            ),
            NOW()
        );
        
        RAISE NOTICE 'TRIGGER DEBUG: Email job created successfully';
    ELSE
        RAISE NOTICE 'TRIGGER DEBUG: send_email is FALSE, skipping email queue';
    END IF;
    
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Drop and recreate the trigger
DROP TRIGGER IF EXISTS trg_auto_queue_email ON notifications;

CREATE TRIGGER trg_auto_queue_email
AFTER INSERT ON notifications
FOR EACH ROW
EXECUTE FUNCTION trigger_queue_email_job();

-- Verify the trigger exists
SELECT 
    trigger_name,
    event_manipulation,
    event_object_table,
    action_statement
FROM information_schema.triggers
WHERE trigger_name = 'trg_auto_queue_email';

-- Test the trigger
DO $$
DECLARE
    test_user_id UUID;
    test_notif_id UUID;
    queue_count INTEGER;
BEGIN
    RAISE NOTICE '========================================';
    RAISE NOTICE 'Testing Email Queue Trigger';
    RAISE NOTICE '========================================';
    
    -- Get a test user
    SELECT id INTO test_user_id FROM users LIMIT 1;
    
    IF test_user_id IS NOT NULL THEN
        -- Count existing queue jobs before test
        SELECT COUNT(*) INTO queue_count FROM message_queues WHERE job_type = 'EMAIL_NOTIFICATION';
        RAISE NOTICE 'Email jobs before test: %', queue_count;
        
        -- Create a test notification (this should trigger the email queue)
        test_notif_id := create_system_notification(
            test_user_id,
            'USER_WELCOME',
            jsonb_build_object('userName', 'Trigger Test User')
        );
        
        IF test_notif_id IS NOT NULL THEN
            RAISE NOTICE 'Test notification created: %', test_notif_id;
            
            -- Count queue jobs after test
            SELECT COUNT(*) INTO queue_count FROM message_queues WHERE job_type = 'EMAIL_NOTIFICATION';
            RAISE NOTICE 'Email jobs after test: %', queue_count;
            
            -- Check if a job was created for this notification
            IF EXISTS (
                SELECT 1 FROM message_queues 
                WHERE job_type = 'EMAIL_NOTIFICATION' 
                AND payload->>'notificationId' = test_notif_id::text
            ) THEN
                RAISE NOTICE '✅ SUCCESS: Email job was created in message_queues!';
            ELSE
                RAISE WARNING '❌ FAILED: No email job found in message_queues!';
            END IF;
        ELSE
            RAISE WARNING 'Test notification creation failed!';
        END IF;
    END IF;
    
    RAISE NOTICE '========================================';
END $$;
