-- ================================================================
-- EMAIL VERIFICATION OTP SYSTEM
-- ================================================================
-- Must be created FIRST to ensure Spring Boot SQL init can find it
CREATE TABLE IF NOT EXISTS email_verification_otps (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL,
    otp_code VARCHAR(6) NOT NULL,
    created_at TIMESTAMP DEFAULT NOW(),
    expires_at TIMESTAMP NOT NULL,
    verified_at TIMESTAMP,
    attempts INTEGER DEFAULT 0,
    is_used BOOLEAN DEFAULT false,
    ip_address VARCHAR(45),
    user_agent TEXT,
    CONSTRAINT fk_otp_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);;

CREATE INDEX IF NOT EXISTS idx_otp_user_id ON email_verification_otps(user_id);;
CREATE INDEX IF NOT EXISTS idx_otp_code ON email_verification_otps(otp_code);;
CREATE INDEX IF NOT EXISTS idx_otp_expires_at ON email_verification_otps(expires_at);;

-- ================================================================
-- PG_DUMP EXPORT SECTION (Legacy - tables defined above take precedence)
-- ================================================================
--
-- PostgreSQL database dump
--

\restrict MtVGBaywtJ9cEi5dmvWSbWefiuAyrh0zWxeG9uDcRiR8QE60jxS1vPsevm4NJPi

-- Dumped from database version 17.6
-- Dumped by pg_dump version 18.0

-- Started on 2026-02-14 05:30:27

SET statement_timeout = 0;
SET lock_timeout = 0;
SET idle_in_transaction_session_timeout = 0;
SET transaction_timeout = 0;
SET client_encoding = 'UTF8';
SET standard_conforming_strings = on;
SELECT pg_catalog.set_config('search_path', '', false);
SET check_function_bodies = false;
SET xmloption = content;
SET client_min_messages = warning;
SET row_security = off;

--
-- TOC entry 2 (class 3079 OID 17459)
-- Name: pgcrypto; Type: EXTENSION; Schema: -; Owner: -
--

CREATE EXTENSION IF NOT EXISTS pgcrypto WITH SCHEMA public;


--
-- TOC entry 3904 (class 0 OID 0)
-- Dependencies: 2
-- Name: EXTENSION pgcrypto; Type: COMMENT; Schema: -; Owner: 
--

COMMENT ON EXTENSION pgcrypto IS 'cryptographic functions';


--
-- TOC entry 3 (class 3079 OID 17496)
-- Name: uuid-ossp; Type: EXTENSION; Schema: -; Owner: -
--

CREATE EXTENSION IF NOT EXISTS "uuid-ossp" WITH SCHEMA public;


--
-- TOC entry 3905 (class 0 OID 0)
-- Dependencies: 3
-- Name: EXTENSION "uuid-ossp"; Type: COMMENT; Schema: -; Owner: 
--

COMMENT ON EXTENSION "uuid-ossp" IS 'generate universally unique identifiers (UUIDs)';


--
-- TOC entry 937 (class 1247 OID 17508)
-- Name: chat_sender_type; Type: TYPE; Schema: public; Owner: postgres
--

CREATE TYPE public.chat_sender_type AS ENUM (
    'patient',
    'ai'
);


ALTER TYPE public.chat_sender_type OWNER TO postgres;

--
-- TOC entry 940 (class 1247 OID 17514)
-- Name: engagement_status; Type: TYPE; Schema: public; Owner: postgres
--

CREATE TYPE public.engagement_status AS ENUM (
    'pending',
    'active',
    'ended',
    'archived',
    'cancelled'
);


ALTER TYPE public.engagement_status OWNER TO postgres;

--
-- TOC entry 943 (class 1247 OID 17526)
-- Name: job_status; Type: TYPE; Schema: public; Owner: postgres
--

CREATE TYPE public.job_status AS ENUM (
    'pending',
    'processing',
    'completed',
    'failed',
    'retry'
);


ALTER TYPE public.job_status OWNER TO postgres;

--
-- TOC entry 946 (class 1247 OID 17538)
-- Name: notification_source; Type: TYPE; Schema: public; Owner: postgres
--

CREATE TYPE public.notification_source AS ENUM (
    'engagement',
    'message',
    'system',
    'ai',
    'reminder',
    'admin'
);


ALTER TYPE public.notification_source OWNER TO postgres;

--
-- TOC entry 949 (class 1247 OID 17552)
-- Name: subscription_status; Type: TYPE; Schema: public; Owner: postgres
--

CREATE TYPE public.subscription_status AS ENUM (
    'active',
    'expired',
    'cancelled',
    'pending'
);


ALTER TYPE public.subscription_status OWNER TO postgres;

--
-- TOC entry 952 (class 1247 OID 17562)
-- Name: token_status; Type: TYPE; Schema: public; Owner: postgres
--

CREATE TYPE public.token_status AS ENUM (
    'pending',
    'verified',
    'expired',
    'cancelled'
);


ALTER TYPE public.token_status OWNER TO postgres;

--
-- TOC entry 955 (class 1247 OID 17572)
-- Name: verification_type; Type: TYPE; Schema: public; Owner: postgres
--

CREATE TYPE public.verification_type AS ENUM (
    'start',
    'end'
);


ALTER TYPE public.verification_type OWNER TO postgres;

--
-- TOC entry 304 (class 1255 OID 17577)
-- Name: calculate_doctor_profile_completion(uuid); Type: FUNCTION; Schema: public; Owner: postgres
--

CREATE FUNCTION public.calculate_doctor_profile_completion(p_doctor_id uuid) RETURNS integer
    LANGUAGE plpgsql
    AS $$
DECLARE
    v_doctor doctor_profiles%ROWTYPE;
    v_score INTEGER := 0;
BEGIN
    SELECT * INTO v_doctor FROM doctor_profiles WHERE id = p_doctor_id;
    IF NOT FOUND THEN RETURN 0; END IF;

    -- Basic info (Total 30)
    IF v_doctor.title IS NOT NULL AND v_doctor.title != '' THEN v_score := v_score + 5; END IF;
    IF v_doctor.bio IS NOT NULL AND v_doctor.bio != '' THEN v_score := v_score + 5; END IF;
    IF v_doctor.specialization IS NOT NULL AND v_doctor.specialization != '' THEN v_score := v_score + 5; END IF;
    IF v_doctor.years_of_experience IS NOT NULL THEN v_score := v_score + 5; END IF;
    IF v_doctor.location_city IS NOT NULL AND v_doctor.location_country IS NOT NULL THEN v_score := v_score + 5; END IF;
    IF v_doctor.consultation_fee IS NOT NULL THEN v_score := v_score + 5; END IF;

    -- Visual (20)
    IF v_doctor.profile_picture_path IS NOT NULL THEN v_score := v_score + 20; END IF;

    -- Professional (10)
    IF v_doctor.certificates IS NOT NULL AND jsonb_array_length(v_doctor.certificates) > 0 THEN v_score := v_score + 10; END IF;

    -- Contact (10)
    IF v_doctor.social_media IS NOT NULL AND v_doctor.social_media::text != '{}' THEN v_score := v_score + 10; END IF;

    -- Verification (30)
    IF v_doctor.verification_status = 'verified' THEN 
        v_score := v_score + 30; 
    ELSIF v_doctor.verification_status = 'pending' THEN 
        v_score := v_score + 10; 
    END IF;

    -- Cap at 100
    RETURN LEAST(v_score, 100);
END;
$$;


ALTER FUNCTION public.calculate_doctor_profile_completion(p_doctor_id uuid) OWNER TO postgres;

--
-- TOC entry 305 (class 1255 OID 17578)
-- Name: calculate_profile_completion(uuid); Type: FUNCTION; Schema: public; Owner: postgres
--

CREATE FUNCTION public.calculate_profile_completion(p_doctor_id uuid) RETURNS integer
    LANGUAGE plpgsql
    AS $$
DECLARE
    v_score INTEGER := 0;
    v_doctor RECORD;
BEGIN
    -- Get doctor profile
    SELECT * INTO v_doctor
    FROM doctor_profiles
    WHERE id = p_doctor_id;
    
    IF NOT FOUND THEN
        RETURN 0;
    END IF;
    
    -- Basic info (40 points)
    IF v_doctor.title IS NOT NULL AND v_doctor.title != '' THEN
        v_score := v_score + 10;
    END IF;
    
    IF v_doctor.bio IS NOT NULL AND LENGTH(v_doctor.bio) > 50 THEN
        v_score := v_score + 15;
    END IF;
    
    IF v_doctor.specialization IS NOT NULL THEN
        v_score := v_score + 10;
    END IF;
    
    IF v_doctor.years_of_experience IS NOT NULL AND v_doctor.years_of_experience > 0 THEN
        v_score := v_score + 5;
    END IF;
    
    -- Visual & Professional (30 points)
    IF v_doctor.profile_picture_path IS NOT NULL THEN
        v_score := v_score + 20;
    END IF;
    
    IF v_doctor.certificates IS NOT NULL AND jsonb_array_length(v_doctor.certificates) > 0 THEN
        v_score := v_score + 10;
    END IF;
    
    -- Verification (30 points)
    IF v_doctor.identity_verified = true THEN
        v_score := v_score + 10;
    END IF;
    
    IF v_doctor.license_verified = true THEN
        v_score := v_score + 10;
    END IF;
    
    IF v_doctor.platform_approved = true THEN
        v_score := v_score + 10;
    END IF;
    
    -- Social presence (bonus - not counted in 100, but adds value)
    -- You can add this if you want social media to contribute
    
    RETURN v_score;
END;
$$;


ALTER FUNCTION public.calculate_profile_completion(p_doctor_id uuid) OWNER TO postgres;

--
-- TOC entry 3906 (class 0 OID 0)
-- Dependencies: 305
-- Name: FUNCTION calculate_profile_completion(p_doctor_id uuid); Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON FUNCTION public.calculate_profile_completion(p_doctor_id uuid) IS 'Calculates doctor profile completion percentage (0-100)';


--
-- TOC entry 306 (class 1255 OID 17579)
-- Name: can_doctor_view_ai_session(uuid, uuid, uuid); Type: FUNCTION; Schema: public; Owner: postgres
--

CREATE FUNCTION public.can_doctor_view_ai_session(p_doctor_id uuid, p_patient_id uuid, p_session_id uuid) RETURNS boolean
    LANGUAGE plpgsql
    AS $$
DECLARE
    v_result BOOLEAN;
BEGIN
    SELECT EXISTS (
        SELECT 1
        FROM ai_chat_sessions s
        JOIN doctor_patients dp ON dp.patient_id = s.patient_id
        JOIN engagement_access_rules r ON r.rule_name = dp.relationship_status
        LEFT JOIN engagements e ON e.id = dp.current_engagement_id
        WHERE dp.doctor_id = p_doctor_id
          AND s.id = p_session_id
          AND s.patient_id = p_patient_id
          AND (
              r.can_view_all_history = true
              OR
              (r.can_view_current_only = true 
               AND s.started_at >= e.start_at
               AND (e.end_at IS NULL OR s.started_at <= e.end_at))
          )
    ) INTO v_result;
    
    RETURN COALESCE(v_result, false);
END;
$$;


ALTER FUNCTION public.can_doctor_view_ai_session(p_doctor_id uuid, p_patient_id uuid, p_session_id uuid) OWNER TO postgres;

--
-- TOC entry 307 (class 1255 OID 17580)
-- Name: create_engagement_notification(); Type: FUNCTION; Schema: public; Owner: postgres
--

CREATE FUNCTION public.create_engagement_notification() RETURNS trigger
    LANGUAGE plpgsql
    AS $$
DECLARE
    v_patient_name TEXT;
    v_patient_user_id UUID;
    v_doctor_name TEXT;
    v_doctor_user_id UUID;
    v_template_key VARCHAR(100);
    v_placeholders JSONB;
    v_doctor_context VARCHAR(50);
    v_patient_context VARCHAR(50);
    v_notification RECORD;
BEGIN
    -- Get patient info
    SELECT CONCAT(u.first_name, ' ', u.last_name), u.id
    INTO v_patient_name, v_patient_user_id
    FROM users u
    JOIN patient_profiles p ON u.id = p.user_id
    WHERE p.id = NEW.patient_id;
    
    -- Get doctor info
    SELECT CONCAT(u.first_name, ' ', u.last_name), u.id
    INTO v_doctor_name, v_doctor_user_id
    FROM users u
    JOIN doctor_profiles d ON u.id = d.user_id
    WHERE d.id = NEW.doctor_id;
    
    -- ENGAGEMENT STARTED
    IF NEW.status = 'active' AND (OLD IS NULL OR OLD.status = 'pending') THEN
        v_template_key := 'ENGAGEMENT_STARTED';
        v_placeholders := jsonb_build_object('patientName', v_patient_name);
        
        -- Notify doctor
        SELECT * INTO v_notification
        FROM get_notification_message(v_template_key, v_doctor_user_id, 'doctor', v_placeholders);
        
        INSERT INTO notifications (user_id, type, title, message, payload, priority, source, sent_at)
        VALUES (
            v_doctor_user_id,
            v_template_key,
            v_notification.title,
            v_notification.message,
            jsonb_build_object('engagementId', NEW.id, 'patientName', v_patient_name),
            v_notification.priority,
            'engagement',
            NOW()
        );
    
    -- ENGAGEMENT CANCELLED
    ELSIF NEW.status = 'cancelled' THEN
        v_template_key := 'ENGAGEMENT_CANCELLED';
        
        -- Determine who cancelled and set contexts
        IF NEW.ended_by = v_doctor_user_id THEN
            v_doctor_context := 'initiator';  -- Doctor cancelled
            v_patient_context := 'target';     -- Patient is notified
        ELSE
            v_doctor_context := 'target';      -- Doctor is notified
            v_patient_context := 'initiator';  -- Patient cancelled
        END IF;
        
        -- Notify doctor
        v_placeholders := jsonb_build_object('otherPartyName', v_patient_name);
        SELECT * INTO v_notification
        FROM get_notification_message(v_template_key, v_doctor_user_id, v_doctor_context, v_placeholders);
        
        INSERT INTO notifications (user_id, type, title, message, payload, priority, source, sent_at)
        VALUES (
            v_doctor_user_id,
            v_template_key,
            v_notification.title,
            v_notification.message,
            jsonb_build_object('engagementId', NEW.id, 'otherPartyName', v_patient_name),
            v_notification.priority,
            'engagement',
            NOW()
        );
        
        -- Notify patient
        v_placeholders := jsonb_build_object('otherPartyName', 'Dr. ' || v_doctor_name);
        SELECT * INTO v_notification
        FROM get_notification_message(v_template_key, v_patient_user_id, v_patient_context, v_placeholders);
        
        INSERT INTO notifications (user_id, type, title, message, payload, priority, source, sent_at)
        VALUES (
            v_patient_user_id,
            v_template_key,
            v_notification.title,
            v_notification.message,
            jsonb_build_object('engagementId', NEW.id, 'otherPartyName', 'Dr. ' || v_doctor_name),
            v_notification.priority,
            'engagement',
            NOW()
        );
    END IF;
    
    RETURN NEW;
END;
$$;


ALTER FUNCTION public.create_engagement_notification() OWNER TO postgres;

--
-- TOC entry 308 (class 1255 OID 17581)
-- Name: create_system_notification(uuid, character varying, jsonb, character varying); Type: FUNCTION; Schema: public; Owner: postgres
--

CREATE FUNCTION public.create_system_notification(p_user_id uuid, p_template_key character varying, p_placeholders jsonb DEFAULT '{}'::jsonb, p_source character varying DEFAULT 'system'::character varying) RETURNS uuid
    LANGUAGE plpgsql
    AS $$
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
$$;


ALTER FUNCTION public.create_system_notification(p_user_id uuid, p_template_key character varying, p_placeholders jsonb, p_source character varying) OWNER TO postgres;

--
-- TOC entry 309 (class 1255 OID 17582)
-- Name: generate_engagement_id(); Type: FUNCTION; Schema: public; Owner: postgres
--

CREATE FUNCTION public.generate_engagement_id() RETURNS trigger
    LANGUAGE plpgsql
    AS $$
BEGIN
   IF NEW.engagement_id IS NULL THEN
      NEW.engagement_id := 'ENG-' || EXTRACT(YEAR FROM now()) || '-' || LPAD(nextval('engagement_id_seq')::TEXT, 6, '0');
   END IF;
   RETURN NEW;
END;
$$;


ALTER FUNCTION public.generate_engagement_id() OWNER TO postgres;

--
-- TOC entry 310 (class 1255 OID 17583)
-- Name: get_accessible_ai_chat_sessions(uuid, uuid); Type: FUNCTION; Schema: public; Owner: postgres
--

CREATE FUNCTION public.get_accessible_ai_chat_sessions(p_doctor_id uuid, p_patient_id uuid) RETURNS TABLE(session_id uuid, session_title character varying, started_at timestamp without time zone, message_count integer, is_active boolean)
    LANGUAGE plpgsql
    AS $$
DECLARE
  v_relationship_status VARCHAR(255);
  v_can_view_all_history BOOLEAN;
  v_can_view_current_only BOOLEAN;
  v_engagement_start TIMESTAMP;
  v_engagement_end TIMESTAMP;
BEGIN
  -- Get relationship and rules
  SELECT 
    dp.relationship_status,
    ear.can_view_all_history,
    ear.can_view_current_only,
    e.start_at,
    e.end_at
  INTO 
    v_relationship_status,
    v_can_view_all_history,
    v_can_view_current_only,
    v_engagement_start,
    v_engagement_end
  FROM doctor_patients dp
  JOIN engagement_access_rules ear ON ear.rule_name = dp.relationship_status
  LEFT JOIN engagements e ON e.id = dp.current_engagement_id
  WHERE dp.doctor_id = p_doctor_id 
    AND dp.patient_id = p_patient_id;

  -- No relationship found
  IF v_relationship_status IS NULL THEN
    RETURN;
  END IF;

  -- Full access: return all sessions
  IF v_can_view_all_history THEN
    RETURN QUERY
    SELECT 
      s.id,
      s.session_title,
      s.started_at,
      s.message_count,
      s.is_active
    FROM ai_chat_sessions s
    WHERE s.patient_id = p_patient_id
    ORDER BY s.started_at DESC;
  
  -- Current only: return sessions within engagement period
  ELSIF v_can_view_current_only AND v_engagement_start IS NOT NULL THEN
    RETURN QUERY
    SELECT 
      s.id,
      s.session_title,
      s.started_at,
      s.message_count,
      s.is_active
    FROM ai_chat_sessions s
    WHERE s.patient_id = p_patient_id
      AND s.started_at >= v_engagement_start
      AND (v_engagement_end IS NULL OR s.started_at <= v_engagement_end)
    ORDER BY s.started_at DESC;
  END IF;
END;
$$;


ALTER FUNCTION public.get_accessible_ai_chat_sessions(p_doctor_id uuid, p_patient_id uuid) OWNER TO postgres;

--
-- TOC entry 311 (class 1255 OID 17584)
-- Name: get_accessible_messages(uuid, uuid); Type: FUNCTION; Schema: public; Owner: postgres
--

CREATE FUNCTION public.get_accessible_messages(p_doctor_id uuid, p_patient_id uuid) RETURNS TABLE(message_id uuid, content text, sender_id uuid, sent_at timestamp without time zone, is_system_message boolean)
    LANGUAGE plpgsql
    AS $$
DECLARE
  v_relationship_status VARCHAR(255);
  v_current_engagement_id UUID;
  v_can_view_all_history BOOLEAN;
  v_can_view_current_only BOOLEAN;
  v_engagement_start TIMESTAMP;
  v_engagement_end TIMESTAMP;
BEGIN
  -- Get relationship and access rules in one query
  SELECT 
    dp.relationship_status,
    dp.current_engagement_id,
    ear.can_view_all_history,
    ear.can_view_current_only,
    e.start_at,
    e.end_at
  INTO 
    v_relationship_status,
    v_current_engagement_id,
    v_can_view_all_history,
    v_can_view_current_only,
    v_engagement_start,
    v_engagement_end
  FROM doctor_patients dp
  JOIN engagement_access_rules ear ON ear.rule_name = dp.relationship_status
  LEFT JOIN engagements e ON e.id = dp.current_engagement_id
  WHERE dp.doctor_id = p_doctor_id 
    AND dp.patient_id = p_patient_id;

  -- No relationship found
  IF v_relationship_status IS NULL THEN
    RETURN;
  END IF;

  -- If full access, return all messages
  IF v_can_view_all_history THEN
    RETURN QUERY
    SELECT em.id, em.content, em.sender_id, em.sent_at, em.is_system_message
    FROM engagement_messages em
    JOIN engagements e ON em.engagement_id = e.id
    WHERE e.doctor_id = p_doctor_id
      AND e.patient_id = p_patient_id
    ORDER BY em.sent_at DESC;
  
  -- If current engagement only
  ELSIF v_can_view_current_only AND v_current_engagement_id IS NOT NULL THEN
    RETURN QUERY
    SELECT em.id, em.content, em.sender_id, em.sent_at, em.is_system_message
    FROM engagement_messages em
    WHERE em.engagement_id = v_current_engagement_id
      AND em.created_at >= v_engagement_start
      AND (v_engagement_end IS NULL OR em.created_at <= v_engagement_end)
    ORDER BY em.sent_at DESC;
  END IF;
END;
$$;


ALTER FUNCTION public.get_accessible_messages(p_doctor_id uuid, p_patient_id uuid) OWNER TO postgres;

--
-- TOC entry 312 (class 1255 OID 17585)
-- Name: get_notification_message(character varying, uuid, character varying, jsonb); Type: FUNCTION; Schema: public; Owner: postgres
--

CREATE FUNCTION public.get_notification_message(p_template_key character varying, p_recipient_user_id uuid, p_recipient_context character varying, p_placeholders jsonb DEFAULT '{}'::jsonb) RETURNS TABLE(title text, message text, priority character varying)
    LANGUAGE plpgsql
    AS $$
DECLARE
    v_language VARCHAR(10);
    v_title TEXT;
    v_message TEXT;
    v_priority VARCHAR(20);
    v_placeholder_key TEXT;
    v_placeholder_value TEXT;
BEGIN
    -- Get user's preferred language with fallback
    SELECT COALESCE(NULLIF(language, ''), 'en') INTO v_language
    FROM users
    WHERE id = p_recipient_user_id;
    
    v_language := COALESCE(v_language, 'en');
    
    -- Try to find template (case-insensitive for key and context)
    SELECT 
        nmt.title, nmt.message, nmt.default_priority
    INTO v_title, v_message, v_priority
    FROM notification_message_templates nmt
    WHERE LOWER(nmt.template_key) = LOWER(p_template_key)
      AND nmt.language_code = v_language
      AND LOWER(nmt.recipient_context) = LOWER(p_recipient_context);
    
    -- Fallback 1: English version of same context
    IF v_title IS NULL AND v_language != 'en' THEN
        SELECT 
            nmt.title, nmt.message, nmt.default_priority
        INTO v_title, v_message, v_priority
        FROM notification_message_templates nmt
        WHERE LOWER(nmt.template_key) = LOWER(p_template_key)
          AND nmt.language_code = 'en'
          AND LOWER(nmt.recipient_context) = LOWER(p_recipient_context);
    END IF;

    -- Fallback 2: Any English version of this template key (if context mismatch)
    IF v_title IS NULL THEN
        SELECT 
            nmt.title, nmt.message, nmt.default_priority
        INTO v_title, v_message, v_priority
        FROM notification_message_templates nmt
        WHERE LOWER(nmt.template_key) = LOWER(p_template_key)
          AND nmt.language_code = 'en'
        LIMIT 1;
    END IF;

    -- Absolute Fallback: Hardcoded defaults so the system NEVER returns NULL title
    IF v_title IS NULL THEN
        v_title := INITCAP(REPLACE(p_template_key, '_', ' '));
        v_message := 'New system notification: ' || p_template_key;
        v_priority := 'normal';
    END IF;
    
    -- Replace placeholders
    FOR v_placeholder_key, v_placeholder_value IN
        SELECT key, value FROM jsonb_each_text(p_placeholders)
    LOOP
        v_title := REPLACE(v_title, '{' || v_placeholder_key || '}', COALESCE(v_placeholder_value, ''));
        v_message := REPLACE(v_message, '{' || v_placeholder_key || '}', COALESCE(v_placeholder_value, ''));
    END LOOP;
    
    RETURN QUERY SELECT v_title, v_message, COALESCE(v_priority, 'normal');
END;
$$;


ALTER FUNCTION public.get_notification_message(p_template_key character varying, p_recipient_user_id uuid, p_recipient_context character varying, p_placeholders jsonb) OWNER TO postgres;

--
-- TOC entry 313 (class 1255 OID 17586)
-- Name: notify_access_rule_change(); Type: FUNCTION; Schema: public; Owner: postgres
--

CREATE FUNCTION public.notify_access_rule_change() RETURNS trigger
    LANGUAGE plpgsql
    AS $$
BEGIN
    IF NEW.relationship_status IS DISTINCT FROM OLD.relationship_status THEN
        -- Insert system message into active engagement
        INSERT INTO engagement_messages (
            engagement_id,
            content,
            is_system_message,
            system_message_type,
            sent_at
        )
        SELECT 
            e.id,
            '🔔 Access level changed from "' || COALESCE(OLD.relationship_status, 'None') || 
            '" to "' || NEW.relationship_status || '"',
            true,
            'access_changed',
            NOW()
        FROM engagements e
        WHERE e.id = NEW.current_engagement_id
          AND e.status = 'active';
    END IF;
    
    RETURN NEW;
END;
$$;


ALTER FUNCTION public.notify_access_rule_change() OWNER TO postgres;

--
-- TOC entry 314 (class 1255 OID 17587)
-- Name: trigger_queue_email_job(); Type: FUNCTION; Schema: public; Owner: postgres
--

CREATE FUNCTION public.trigger_queue_email_job() RETURNS trigger
    LANGUAGE plpgsql
    AS $$ BEGIN     IF NEW.send_email = TRUE THEN         INSERT INTO message_queues (             job_type, status, priority, payload, created_at         ) VALUES (             'EMAIL_NOTIFICATION',             'pending',             NEW.priority,             jsonb_build_object(                 'notificationId', NEW.id,                 'userId', NEW.user_id,                 'templateKey', NEW.type,                 'userName', COALESCE(NEW.payload->>'userName', 'User'),                 'doctorName', COALESCE(NEW.payload->>'doctorName', 'Doctor'),                 'otpCode', NEW.payload->>'otpCode',                 'title', NEW.title,                 'body', NEW.message             ),             NOW()         );         PERFORM pg_notify('email_queue', 'new_job');     END IF;     RETURN NEW; END; $$;


ALTER FUNCTION public.trigger_queue_email_job() OWNER TO postgres;

--
-- TOC entry 315 (class 1255 OID 17588)
-- Name: update_doctor_rating(); Type: FUNCTION; Schema: public; Owner: postgres
--

CREATE FUNCTION public.update_doctor_rating() RETURNS trigger
    LANGUAGE plpgsql
    AS $$
DECLARE
    v_avg_rating DECIMAL(3,2);
    v_total_reviews INTEGER;
BEGIN
    -- Determine which doctor_id to update
    IF (TG_OP = 'DELETE') THEN
        -- Calculate for the deleted review's doctor
        SELECT 
            COALESCE(ROUND(AVG(rating)::numeric, 2), 0.00),
            COUNT(*)
        INTO v_avg_rating, v_total_reviews
        FROM doctor_reviews
        WHERE doctor_id = OLD.doctor_id;
        
        -- Update doctor profile
        UPDATE doctor_profiles
        SET 
            rating = v_avg_rating,
            total_reviews = v_total_reviews,
            updated_at = NOW()
        WHERE id = OLD.doctor_id;
        
    ELSE
        -- For INSERT or UPDATE, use NEW
        SELECT 
            COALESCE(ROUND(AVG(rating)::numeric, 2), 0.00),
            COUNT(*)
        INTO v_avg_rating, v_total_reviews
        FROM doctor_reviews
        WHERE doctor_id = NEW.doctor_id;
        
        -- Update doctor profile
        UPDATE doctor_profiles
        SET 
            rating = v_avg_rating,
            total_reviews = v_total_reviews,
            updated_at = NOW()
        WHERE id = NEW.doctor_id;
    END IF;
    
    RETURN NULL;
END;
$$;


ALTER FUNCTION public.update_doctor_rating() OWNER TO postgres;

--
-- TOC entry 3907 (class 0 OID 0)
-- Dependencies: 315
-- Name: FUNCTION update_doctor_rating(); Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON FUNCTION public.update_doctor_rating() IS 'Auto-updates doctor rating and review count when reviews change';


--
-- TOC entry 316 (class 1255 OID 17589)
-- Name: update_profile_completion(); Type: FUNCTION; Schema: public; Owner: postgres
--

CREATE FUNCTION public.update_profile_completion() RETURNS trigger
    LANGUAGE plpgsql
    AS $$
BEGIN
    NEW.profile_completion_percentage := calculate_doctor_profile_completion(NEW.id);
    RETURN NEW;
END;
$$;


ALTER FUNCTION public.update_profile_completion() OWNER TO postgres;

--
-- TOC entry 317 (class 1255 OID 17590)
-- Name: update_relationship_status_on_engagement(); Type: FUNCTION; Schema: public; Owner: postgres
--

CREATE FUNCTION public.update_relationship_status_on_engagement() RETURNS trigger
    LANGUAGE plpgsql
    AS $$
DECLARE
    current_rule RECORD;
    new_status VARCHAR;
    v_access_rule_name TEXT;
BEGIN
    -- When engagement becomes active
    IF NEW.status = 'active' AND (OLD IS NULL OR OLD.status != 'active') THEN
        -- Get the rule name for the message
        SELECT rule_name INTO v_access_rule_name
        FROM engagement_access_rules
        WHERE rule_name = NEW.access_rule_name;
        
        UPDATE doctor_patients
        SET 
            relationship_status = NEW.access_rule_name,
            current_engagement_id = NEW.id,
            relationship_started_at = NEW.start_at,
            is_active = true
        WHERE doctor_id = NEW.doctor_id 
          AND patient_id = NEW.patient_id;
        
        -- Send system message (English only for system messages)
        INSERT INTO engagement_messages (
            engagement_id,
            content,
            is_system_message,
            system_message_type,
            sent_at
        ) VALUES (
            NEW.id,
            '🔔 Engagement started with access level: ' || v_access_rule_name,
            true,
            'engagement_started',
            NOW()
        );
    END IF;
    
    -- When engagement ends
    IF NEW.status = 'ended' AND (OLD IS NULL OR OLD.status = 'active') THEN
        SELECT * INTO current_rule 
        FROM engagement_access_rules 
        WHERE rule_name = NEW.access_rule_name;
        
        IF current_rule.retains_history_access OR current_rule.retains_period_access THEN
            new_status := NEW.access_rule_name;
        ELSE
            new_status := 'NO_ACCESS';
        END IF;
        
        UPDATE doctor_patients
        SET 
            relationship_status = new_status,
            current_engagement_id = NULL,
            relationship_ended_at = NEW.end_at,
            is_active = CASE WHEN new_status = 'NO_ACCESS' THEN false ELSE true END
        WHERE doctor_id = NEW.doctor_id 
          AND patient_id = NEW.patient_id;
        
        INSERT INTO engagement_messages (
            engagement_id,
            content,
            is_system_message,
            system_message_type,
            sent_at
        ) VALUES (
            NEW.id,
            '🔔 Engagement ended. Access updated based on retention policy.',
            true,
            'engagement_ended',
            NOW()
        );
    END IF;
    
    RETURN NEW;
END;
$$;


ALTER FUNCTION public.update_relationship_status_on_engagement() OWNER TO postgres;

--
-- TOC entry 318 (class 1255 OID 17591)
-- Name: update_updated_at_column(); Type: FUNCTION; Schema: public; Owner: postgres
--

CREATE FUNCTION public.update_updated_at_column() RETURNS trigger
    LANGUAGE plpgsql
    AS $$
BEGIN
   NEW.updated_at = now();
   RETURN NEW;
END;
$$;


ALTER FUNCTION public.update_updated_at_column() OWNER TO postgres;

--
-- TOC entry 319 (class 1255 OID 17592)
-- Name: user_welcome_notification(); Type: FUNCTION; Schema: public; Owner: postgres
--

CREATE FUNCTION public.user_welcome_notification() RETURNS trigger
    LANGUAGE plpgsql
    AS $$
BEGIN
    RAISE NOTICE 'TRIGGER: Creating welcome notification for new user %', NEW.id;
    
    -- Direct call to the main notification creation function
    PERFORM create_system_notification(
        NEW.id, 
        'USER_WELCOME', 
        jsonb_build_object('userName', COALESCE(NULLIF(NEW.first_name, ''), 'User'))
    );
    
    RETURN NEW;
END;
$$;


ALTER FUNCTION public.user_welcome_notification() OWNER TO postgres;

--
-- TOC entry 3908 (class 0 OID 0)
-- Dependencies: 319
-- Name: FUNCTION user_welcome_notification(); Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON FUNCTION public.user_welcome_notification() IS 'Creates welcome notification for new users using the centralized notification system';


SET default_tablespace = '';

SET default_table_access_method = heap;

--
-- TOC entry 219 (class 1259 OID 17593)
-- Name: active_service_subscriptions; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.active_service_subscriptions (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    user_id uuid NOT NULL,
    plan_id character varying(100) NOT NULL,
    plan_name character varying(255),
    start_date timestamp without time zone NOT NULL,
    end_date timestamp without time zone,
    status public.subscription_status DEFAULT 'active'::public.subscription_status,
    auto_renew boolean DEFAULT true,
    payment_data jsonb,
    created_at timestamp without time zone DEFAULT now(),
    updated_at timestamp without time zone DEFAULT now()
);


ALTER TABLE public.active_service_subscriptions OWNER TO postgres;

--
-- TOC entry 220 (class 1259 OID 17603)
-- Name: ai_chat_messages; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.ai_chat_messages (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    session_id uuid NOT NULL,
    sender_type public.chat_sender_type NOT NULL,
    sender_id uuid,
    content text NOT NULL,
    content_type character varying(50) DEFAULT 'text'::character varying,
    ai_model character varying(50),
    ai_response_time integer,
    tokens_used integer,
    sentiment_score numeric,
    flagged_for_review boolean DEFAULT false,
    flag_reason character varying(255),
    sent_at timestamp without time zone DEFAULT now(),
    read_at timestamp without time zone,
    created_at timestamp without time zone DEFAULT now()
);


ALTER TABLE public.ai_chat_messages OWNER TO postgres;

--
-- TOC entry 3909 (class 0 OID 0)
-- Dependencies: 220
-- Name: TABLE ai_chat_messages; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON TABLE public.ai_chat_messages IS 'NeuralHealer: Messages exchanged in AI chat sessions';


--
-- TOC entry 221 (class 1259 OID 17613)
-- Name: ai_chat_sessions; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.ai_chat_sessions (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    patient_id uuid NOT NULL,
    session_title character varying(255),
    session_type character varying(50) DEFAULT 'general'::character varying,
    started_at timestamp without time zone DEFAULT now(),
    ended_at timestamp without time zone,
    is_active boolean DEFAULT true,
    message_count integer DEFAULT 0,
    total_duration interval,
    meta jsonb,
    created_at timestamp without time zone DEFAULT now(),
    updated_at timestamp without time zone DEFAULT now()
);


ALTER TABLE public.ai_chat_sessions OWNER TO postgres;

--
-- TOC entry 3910 (class 0 OID 0)
-- Dependencies: 221
-- Name: TABLE ai_chat_sessions; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON TABLE public.ai_chat_sessions IS 'NeuralHealer: AI chatbot sessions for patients';


--
-- TOC entry 222 (class 1259 OID 17625)
-- Name: audit_log; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.audit_log (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    user_id uuid,
    action character varying(100) NOT NULL,
    resource_type character varying(100),
    resource_id character varying(255),
    change_data jsonb,
    ip_address inet,
    user_agent text,
    created_at timestamp without time zone DEFAULT now()
);


ALTER TABLE public.audit_log OWNER TO postgres;

--
-- TOC entry 223 (class 1259 OID 17632)
-- Name: doctor_patients; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.doctor_patients (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    doctor_id uuid NOT NULL,
    patient_id uuid NOT NULL,
    relationship_status character varying(255),
    current_engagement_id uuid,
    added_at timestamp without time zone DEFAULT now(),
    relationship_started_at timestamp without time zone,
    relationship_ended_at timestamp without time zone,
    is_active boolean DEFAULT true,
    created_at timestamp without time zone DEFAULT now(),
    updated_at timestamp without time zone DEFAULT now()
);


ALTER TABLE public.doctor_patients OWNER TO postgres;

--
-- TOC entry 3911 (class 0 OID 0)
-- Dependencies: 223
-- Name: TABLE doctor_patients; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON TABLE public.doctor_patients IS 'NeuralHealer: Relationship mapping between doctors and patients with current access rules';


--
-- TOC entry 224 (class 1259 OID 17640)
-- Name: doctor_profiles; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.doctor_profiles (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    user_id uuid NOT NULL,
    title character varying(100),
    bio text,
    specialities jsonb,
    experience_years integer,
    certificates jsonb,
    location_city character varying(100),
    location_country character varying(100),
    is_verified boolean DEFAULT false,
    verification_data jsonb,
    created_at timestamp without time zone DEFAULT now(),
    updated_at timestamp without time zone DEFAULT now(),
    profile_picture_path character varying(500),
    verification_status character varying(50) DEFAULT 'unverified'::character varying,
    availability_status character varying(50) DEFAULT 'offline'::character varying,
    specialization character varying(100),
    rating numeric(3,2) DEFAULT 0.00,
    total_reviews integer DEFAULT 0,
    profile_completion_percentage integer DEFAULT 0,
    social_media jsonb,
    consultation_fee numeric(10,2),
    years_of_experience integer,
    latitude numeric(10,8),
    longitude numeric(11,8)
);


ALTER TABLE public.doctor_profiles OWNER TO postgres;

--
-- TOC entry 3912 (class 0 OID 0)
-- Dependencies: 224
-- Name: TABLE doctor_profiles; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON TABLE public.doctor_profiles IS 'NeuralHealer: Extended profile information for doctors';


--
-- TOC entry 225 (class 1259 OID 17654)
-- Name: doctor_reviews; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.doctor_reviews (
    id uuid NOT NULL,
    doctor_id uuid,
    patient_id uuid,
    rating integer,
    comment text,
    created_at timestamp without time zone,
    CONSTRAINT doctor_reviews_rating_check CHECK (((rating >= 1) AND (rating <= 5)))
);


ALTER TABLE public.doctor_reviews OWNER TO postgres;

--
-- TOC entry 226 (class 1259 OID 17660)
-- Name: doctor_verification_questions; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.doctor_verification_questions (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    doctor_id uuid NOT NULL,
    question_key character varying(100) NOT NULL,
    answer text,
    verified_at timestamp without time zone,
    created_at timestamp without time zone DEFAULT now(),
    updated_at timestamp without time zone DEFAULT now()
);


ALTER TABLE public.doctor_verification_questions OWNER TO postgres;

--
-- TOC entry 3913 (class 0 OID 0)
-- Dependencies: 226
-- Name: TABLE doctor_verification_questions; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON TABLE public.doctor_verification_questions IS 'Stores verification question responses from doctors';


--
-- TOC entry 246 (class 1259 OID 18192)
-- Name: email_verification_otps; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.email_verification_otps (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    user_id uuid NOT NULL,
    otp_code character varying(6) NOT NULL,
    created_at timestamp without time zone DEFAULT now(),
    expires_at timestamp without time zone NOT NULL,
    verified_at timestamp without time zone,
    attempts integer DEFAULT 0,
    is_used boolean DEFAULT false,
    ip_address inet,
    user_agent text
);


ALTER TABLE public.email_verification_otps OWNER TO postgres;

--
-- TOC entry 227 (class 1259 OID 17668)
-- Name: engagement_access_rules; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.engagement_access_rules (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    rule_name character varying(255) NOT NULL,
    can_view_all_history boolean DEFAULT false,
    can_view_current_only boolean DEFAULT true,
    can_view_patient_profile boolean DEFAULT true,
    can_modify_notes boolean DEFAULT true,
    can_message_patient boolean DEFAULT true,
    retains_period_access boolean DEFAULT false,
    retains_history_access boolean DEFAULT false,
    retains_no_access boolean DEFAULT true,
    description text,
    is_active boolean DEFAULT true,
    created_at timestamp without time zone DEFAULT now(),
    updated_at timestamp without time zone DEFAULT now()
);


ALTER TABLE public.engagement_access_rules OWNER TO postgres;

--
-- TOC entry 3914 (class 0 OID 0)
-- Dependencies: 227
-- Name: TABLE engagement_access_rules; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON TABLE public.engagement_access_rules IS 'NeuralHealer: Defines access control rules for doctor-patient engagements';


--
-- TOC entry 228 (class 1259 OID 17685)
-- Name: engagement_analytics; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.engagement_analytics (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    engagement_id uuid NOT NULL,
    metric_name character varying(100) NOT NULL,
    metric_value numeric,
    recorded_at timestamp without time zone DEFAULT now(),
    created_at timestamp without time zone DEFAULT now()
);


ALTER TABLE public.engagement_analytics OWNER TO postgres;

--
-- TOC entry 229 (class 1259 OID 17693)
-- Name: engagement_events; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.engagement_events (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    engagement_id uuid NOT NULL,
    event_type character varying(100) NOT NULL,
    triggered_at timestamp without time zone DEFAULT now(),
    triggered_by uuid,
    payload jsonb,
    created_at timestamp without time zone DEFAULT now()
);


ALTER TABLE public.engagement_events OWNER TO postgres;

--
-- TOC entry 230 (class 1259 OID 17701)
-- Name: engagement_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.engagement_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.engagement_id_seq OWNER TO postgres;

--
-- TOC entry 231 (class 1259 OID 17702)
-- Name: engagement_messages; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.engagement_messages (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    engagement_id uuid NOT NULL,
    message_uuid uuid DEFAULT gen_random_uuid(),
    sender_id uuid,
    recipient_id uuid,
    content text,
    content_type character varying(50) DEFAULT 'text'::character varying,
    sent_at timestamp without time zone DEFAULT now(),
    delivered_at timestamp without time zone,
    read_at timestamp without time zone,
    is_encrypted boolean DEFAULT true,
    encryption_key_id character varying(255),
    is_system_message boolean DEFAULT false,
    system_message_type character varying(50),
    created_at timestamp without time zone DEFAULT now()
);


ALTER TABLE public.engagement_messages OWNER TO postgres;

--
-- TOC entry 3915 (class 0 OID 0)
-- Dependencies: 231
-- Name: TABLE engagement_messages; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON TABLE public.engagement_messages IS 'NeuralHealer: Messages exchanged during engagements';


--
-- TOC entry 232 (class 1259 OID 17714)
-- Name: engagement_sessions; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.engagement_sessions (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    engagement_id uuid NOT NULL,
    user_id uuid,
    session_token character varying(255) NOT NULL,
    session_start timestamp without time zone DEFAULT now(),
    session_end timestamp without time zone,
    ip_address inet,
    user_agent text,
    device_info jsonb,
    created_at timestamp without time zone DEFAULT now()
);


ALTER TABLE public.engagement_sessions OWNER TO postgres;

--
-- TOC entry 233 (class 1259 OID 17722)
-- Name: engagement_verification_tokens; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.engagement_verification_tokens (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    engagement_id uuid NOT NULL,
    token character varying(255) NOT NULL,
    verification_type public.verification_type,
    qr_code_data text,
    doctor_id uuid,
    patient_id uuid,
    verified_by uuid,
    verified_at timestamp without time zone,
    expires_at timestamp without time zone NOT NULL,
    status public.token_status DEFAULT 'pending'::public.token_status,
    created_at timestamp without time zone DEFAULT now()
);


ALTER TABLE public.engagement_verification_tokens OWNER TO postgres;

--
-- TOC entry 234 (class 1259 OID 17730)
-- Name: engagements; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.engagements (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    engagement_id character varying(100),
    doctor_id uuid NOT NULL,
    patient_id uuid NOT NULL,
    access_rule_name character varying(255) NOT NULL,
    status public.engagement_status DEFAULT 'pending'::public.engagement_status,
    engagement_type character varying(50),
    start_at timestamp without time zone,
    end_at timestamp without time zone,
    ended_by uuid,
    termination_reason text,
    start_verified_at timestamp without time zone,
    end_verified_at timestamp without time zone,
    notes text,
    created_at timestamp without time zone DEFAULT now(),
    updated_at timestamp without time zone DEFAULT now(),
    initiated_by character varying(10) DEFAULT 'doctor'::character varying NOT NULL,
    CONSTRAINT check_initiated_by CHECK (((initiated_by)::text = ANY ((ARRAY['doctor'::character varying, 'patient'::character varying])::text[])))
);


ALTER TABLE public.engagements OWNER TO postgres;

--
-- TOC entry 3916 (class 0 OID 0)
-- Dependencies: 234
-- Name: TABLE engagements; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON TABLE public.engagements IS 'NeuralHealer: Active or historical doctor-patient engagement periods';


--
-- TOC entry 235 (class 1259 OID 17739)
-- Name: message_queues; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.message_queues (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    job_type character varying(100) NOT NULL,
    payload jsonb,
    status public.job_status DEFAULT 'pending'::public.job_status,
    scheduled_at timestamp without time zone DEFAULT now(),
    processed_at timestamp without time zone,
    retry_count integer DEFAULT 0,
    priority character varying(20) DEFAULT 'normal'::character varying,
    error_message text,
    created_at timestamp without time zone DEFAULT now()
);


ALTER TABLE public.message_queues OWNER TO postgres;

--
-- TOC entry 236 (class 1259 OID 17749)
-- Name: notification_message_templates; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.notification_message_templates (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    template_key character varying(100) NOT NULL,
    language_code character varying(10) NOT NULL,
    title text NOT NULL,
    message text NOT NULL,
    recipient_context character varying(50) NOT NULL,
    default_priority character varying(20) DEFAULT 'normal'::character varying,
    notes text,
    created_at timestamp without time zone DEFAULT now(),
    updated_at timestamp without time zone DEFAULT now(),
    channels jsonb DEFAULT '{"sse": false, "push": false, "email": false}'::jsonb
);


ALTER TABLE public.notification_message_templates OWNER TO postgres;

--
-- TOC entry 237 (class 1259 OID 17759)
-- Name: notification_templates; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.notification_templates (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    type character varying(100) NOT NULL,
    title_template text NOT NULL,
    message_template text NOT NULL,
    default_priority character varying(20) DEFAULT 'normal'::character varying,
    default_channels jsonb DEFAULT '["sse"]'::jsonb,
    created_at timestamp without time zone DEFAULT now()
);


ALTER TABLE public.notification_templates OWNER TO postgres;

--
-- TOC entry 238 (class 1259 OID 17768)
-- Name: notifications; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.notifications (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    user_id uuid NOT NULL,
    type character varying(100) NOT NULL,
    title character varying(255),
    message text,
    payload jsonb,
    is_read boolean DEFAULT false,
    sent_at timestamp without time zone DEFAULT now(),
    read_at timestamp without time zone,
    expires_at timestamp without time zone,
    created_at timestamp without time zone DEFAULT now(),
    priority character varying(20) DEFAULT 'normal'::character varying,
    source character varying(50) DEFAULT 'engagement'::character varying,
    delivery_status jsonb DEFAULT '{"sse": false, "push": false, "email": false}'::jsonb,
    metadata jsonb DEFAULT '{}'::jsonb,
    send_email boolean DEFAULT false
);


ALTER TABLE public.notifications OWNER TO postgres;

--
-- TOC entry 239 (class 1259 OID 17782)
-- Name: patient_profiles; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.patient_profiles (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    user_id uuid NOT NULL,
    date_of_birth date,
    gender character varying(20),
    emergency_contact character varying(255),
    primary_health_concerns jsonb,
    medical_history jsonb,
    notes text,
    created_at timestamp without time zone DEFAULT now(),
    updated_at timestamp without time zone DEFAULT now()
);


ALTER TABLE public.patient_profiles OWNER TO postgres;

--
-- TOC entry 3917 (class 0 OID 0)
-- Dependencies: 239
-- Name: TABLE patient_profiles; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON TABLE public.patient_profiles IS 'NeuralHealer: Extended profile information for patients';


--
-- TOC entry 240 (class 1259 OID 17790)
-- Name: platform_analytics; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.platform_analytics (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    analytics_date date NOT NULL,
    total_users integer DEFAULT 0,
    new_users integer DEFAULT 0,
    active_users integer DEFAULT 0,
    total_sessions integer DEFAULT 0,
    new_sessions integer DEFAULT 0,
    total_doctors integer DEFAULT 0,
    verified_doctors integer DEFAULT 0,
    active_doctors integer DEFAULT 0,
    active_engagements integer DEFAULT 0,
    ended_engagements integer DEFAULT 0,
    crm_resources_count integer DEFAULT 0,
    messages_processed integer DEFAULT 0,
    avg_engagement_duration interval,
    created_at timestamp without time zone DEFAULT now()
);


ALTER TABLE public.platform_analytics OWNER TO postgres;

--
-- TOC entry 241 (class 1259 OID 17807)
-- Name: security_authentication_tokens; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.security_authentication_tokens (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    user_id uuid NOT NULL,
    token_type character varying(50) DEFAULT 'session'::character varying,
    token character varying(255) NOT NULL,
    expires_at timestamp without time zone NOT NULL,
    is_revoked boolean DEFAULT false,
    revoked_at timestamp without time zone,
    created_at timestamp without time zone DEFAULT now()
);


ALTER TABLE public.security_authentication_tokens OWNER TO postgres;

--
-- TOC entry 242 (class 1259 OID 17814)
-- Name: system_settings; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.system_settings (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    setting_key character varying(255) NOT NULL,
    setting_value jsonb,
    description text,
    is_public boolean DEFAULT false,
    created_at timestamp without time zone DEFAULT now(),
    updated_at timestamp without time zone DEFAULT now()
);


ALTER TABLE public.system_settings OWNER TO postgres;

--
-- TOC entry 243 (class 1259 OID 17823)
-- Name: url_shortcuts; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.url_shortcuts (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    user_id uuid NOT NULL,
    url text NOT NULL,
    label character varying(255),
    short_code character varying(50) NOT NULL,
    visit_count integer DEFAULT 0,
    expires_at timestamp without time zone,
    created_at timestamp without time zone DEFAULT now()
);


ALTER TABLE public.url_shortcuts OWNER TO postgres;

--
-- TOC entry 244 (class 1259 OID 17831)
-- Name: user_management_metrics; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.user_management_metrics (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    user_id uuid NOT NULL,
    metric_name character varying(100) NOT NULL,
    metric_value numeric,
    recorded_at timestamp without time zone DEFAULT now(),
    period character varying(20) DEFAULT 'daily'::character varying,
    created_at timestamp without time zone DEFAULT now()
);


ALTER TABLE public.user_management_metrics OWNER TO postgres;

--
-- TOC entry 245 (class 1259 OID 17840)
-- Name: users; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.users (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    email character varying(255) NOT NULL,
    password_hash character varying(255) NOT NULL,
    first_name character varying(100) NOT NULL,
    last_name character varying(100) NOT NULL,
    phone character varying(20),
    timezone character varying(50) DEFAULT 'UTC'::character varying,
    email_verified_at timestamp without time zone,
    phone_verified_at timestamp without time zone,
    is_active boolean DEFAULT true,
    last_login_at timestamp without time zone,
    mfa_enabled boolean DEFAULT false,
    created_at timestamp without time zone DEFAULT now(),
    updated_at timestamp without time zone DEFAULT now(),
    deleted_at timestamp without time zone,
    language character varying(10) DEFAULT 'en'::character varying,
    activity_status character varying(20) DEFAULT 'active'::character varying,
    last_activity_check timestamp without time zone DEFAULT now(),
    email_verification_required boolean DEFAULT true,
    failed_verification_attempts integer DEFAULT 0,
    verification_locked_until timestamp without time zone,
    email_verification_sent_at timestamp without time zone
);


ALTER TABLE public.users OWNER TO postgres;

--
-- TOC entry 3918 (class 0 OID 0)
-- Dependencies: 245
-- Name: TABLE users; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON TABLE public.users IS 'NeuralHealer: Base user table for all platform users (doctors, patients, admins)';


--
-- TOC entry 3919 (class 0 OID 0)
-- Dependencies: 245
-- Name: COLUMN users.language; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN public.users.language IS 'User preferred language code (en, ar, etc.)';


--
-- TOC entry 3871 (class 0 OID 17593)
-- Dependencies: 219
-- Data for Name: active_service_subscriptions; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.active_service_subscriptions (id, user_id, plan_id, plan_name, start_date, end_date, status, auto_renew, payment_data, created_at, updated_at) FROM stdin;
\.


--
-- TOC entry 3872 (class 0 OID 17603)
-- Dependencies: 220
-- Data for Name: ai_chat_messages; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.ai_chat_messages (id, session_id, sender_type, sender_id, content, content_type, ai_model, ai_response_time, tokens_used, sentiment_score, flagged_for_review, flag_reason, sent_at, read_at, created_at) FROM stdin;
2c5d9565-c127-4dc2-a996-eef603afd1f5	f43867b7-2e64-4ff2-b487-5fdc714cf23d	patient	\N	I feel sad	\N	\N	\N	\N	\N	f	\N	2026-02-08 03:51:45.727571	\N	2026-02-08 03:51:45.730105
af9773df-27d9-416e-9216-726e9d717e6b	a110acd4-032c-4f0d-978c-1be0de3af14c	patient	\N	I feel sad	\N	\N	\N	\N	\N	f	\N	2026-02-08 03:52:31.873446	\N	2026-02-08 03:52:31.873445
1ee02722-0d81-48be-9480-ca2224084102	4bbb015c-c621-4ed0-a21b-f84092b5b673	patient	\N	I feel sad	\N	\N	\N	\N	\N	f	\N	2026-02-08 03:55:29.641473	\N	2026-02-08 03:55:29.643264
4f1a3900-e465-4037-8c3f-86144071ba73	a0b46180-1476-4887-b378-a0d4959337dd	patient	\N	I feel sad	\N	\N	\N	\N	\N	f	\N	2026-02-08 03:56:27.313617	\N	2026-02-08 03:56:27.315616
2402e672-cf09-454b-b6e7-76c664c3cb9a	a0b46180-1476-4887-b378-a0d4959337dd	ai	\N	الاحباط هو شعور بالحزن والضيق اللي بيحصل بعد أحداث محبطة او مؤلمة. ساعات الاحباط بيكون شديد، وساعات بيبقى خفيف. ممكن تحس انك عايز تستسلم، او تبكي كتير، او تفكر في حاجات كنت نفسك تعملها بطريقة تانية. في ناس بتعاني من الاكتئاب، وبيحسوا انهم غرقانين في حفرة مظلمة ومش شايفين اي مخرج. وفي ناس بتحس انها ملهاش قيمة او ندمانة على نفسها. كمان ممكن متحبش نفسك دلوقتي خالص.\nلو حابب، أشرحلك أكتر أو نربط ده بأمثلة واقعية.	\N	\N	\N	\N	\N	f	\N	2026-02-08 03:56:45.450994	\N	2026-02-08 03:56:45.452524
fb4ce9f4-3267-4c1c-a076-70b596f0787c	37da6543-f366-4a54-ac0d-c67a521b2c23	patient	\N	I feel sad	\N	\N	\N	\N	\N	f	\N	2026-02-08 04:07:22.071377	\N	2026-02-08 04:07:22.073493
625770a1-efae-4264-ab2f-359119c58fab	9735f1ba-bc0e-4daa-b1d0-03de2de3028e	patient	\N	I feel sad	\N	\N	\N	\N	\N	f	\N	2026-02-08 04:07:27.387436	\N	2026-02-08 04:07:27.389537
f63bc7c1-40e7-47c2-8d4e-ee98d7de9cd5	9735f1ba-bc0e-4daa-b1d0-03de2de3028e	ai	\N	الاحباط هو شعور بالحزن والضيق اللي بيحصل بعد أحداث محبطة او مؤلمة. ساعات الاحباط بيكون شديد، وساعات بيبقى خفيف. ممكن تحس انك عايز تستسلم، او تبكي كتير، او تفكر في حاجات كنت نفسك تعملها بطريقة تانية. في ناس بتعاني من الاكتئاب، وبيحسوا انهم غرقانين في حفرة مظلمة ومش شايفين اي مخرج. وفي ناس بتحس انها ملهاش قيمة او ندمانة على نفسها. كمان ممكن متحبش نفسك دلوقتي خالص.\nلو حابب، أشرحلك أكتر أو نربط ده بأمثلة واقعية.	\N	\N	\N	\N	\N	f	\N	2026-02-08 04:07:46.223381	\N	2026-02-08 04:07:46.224904
372e4071-a2af-4124-a8f9-91d6493d1701	add3b367-3a63-48f1-a20e-703374d591fe	patient	\N	I feel sad	\N	\N	\N	\N	\N	f	\N	2026-02-08 11:05:28.166896	\N	2026-02-08 11:05:28.169297
d6e9ce72-e6ab-46b6-89c9-f938e759cf00	add3b367-3a63-48f1-a20e-703374d591fe	ai	\N	الاحباط هو شعور بالحزن والضيق اللي بيحصل بعد أحداث محبطة او مؤلمة. ساعات الاحباط بيكون شديد، وساعات بيبقى خفيف. ممكن تحس انك عايز تستسلم، او تبكي كتير، او تفكر في حاجات كنت نفسك تعملها بطريقة تانية. في ناس بتعاني من الاكتئاب، وبيحسوا انهم غرقانين في حفرة مظلمة ومش شايفين اي مخرج. وفي ناس بتحس انها ملهاش قيمة او ندمانة على نفسها. كمان ممكن متحبش نفسك دلوقتي خالص.\nلو حابب، أشرحلك أكتر أو نربط ده بأمثلة واقعية.	\N	\N	\N	\N	\N	f	\N	2026-02-08 11:05:49.007692	\N	2026-02-08 11:05:49.011487
\.


--
-- TOC entry 3873 (class 0 OID 17613)
-- Dependencies: 221
-- Data for Name: ai_chat_sessions; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.ai_chat_sessions (id, patient_id, session_title, session_type, started_at, ended_at, is_active, message_count, total_duration, meta, created_at, updated_at) FROM stdin;
f43867b7-2e64-4ff2-b487-5fdc714cf23d	1db15fab-11be-4148-9000-4bcdd745338d	I feel sad	general	2026-02-08 03:51:45.706603	\N	t	1	\N	\N	2026-02-08 03:51:45.708729	2026-02-08 03:51:45.77996
a110acd4-032c-4f0d-978c-1be0de3af14c	1db15fab-11be-4148-9000-4bcdd745338d	I feel sad	general	2026-02-08 03:52:31.869658	\N	t	1	\N	\N	2026-02-08 03:52:31.869657	2026-02-08 03:52:31.884592
4bbb015c-c621-4ed0-a21b-f84092b5b673	1db15fab-11be-4148-9000-4bcdd745338d	I feel sad	general	2026-02-08 03:55:29.557375	\N	t	1	\N	\N	2026-02-08 03:55:29.607902	2026-02-08 03:55:29.678773
a0b46180-1476-4887-b378-a0d4959337dd	1db15fab-11be-4148-9000-4bcdd745338d	I feel sad	general	2026-02-08 03:56:27.301539	\N	t	2	\N	\N	2026-02-08 03:56:27.302268	2026-02-08 03:56:45.465361
37da6543-f366-4a54-ac0d-c67a521b2c23	1db15fab-11be-4148-9000-4bcdd745338d	I feel sad	general	2026-02-08 04:07:21.989533	\N	t	1	\N	\N	2026-02-08 04:07:22.032421	2026-02-08 04:07:22.101829
9735f1ba-bc0e-4daa-b1d0-03de2de3028e	1db15fab-11be-4148-9000-4bcdd745338d	I feel sad	general	2026-02-08 04:07:27.371129	\N	t	2	\N	\N	2026-02-08 04:07:27.372658	2026-02-08 04:07:46.24053
add3b367-3a63-48f1-a20e-703374d591fe	1db15fab-11be-4148-9000-4bcdd745338d	I feel sad	general	2026-02-08 11:05:28.075913	\N	t	2	\N	\N	2026-02-08 11:05:28.124828	2026-02-08 11:05:49.03127
\.


--
-- TOC entry 3874 (class 0 OID 17625)
-- Dependencies: 222
-- Data for Name: audit_log; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.audit_log (id, user_id, action, resource_type, resource_id, change_data, ip_address, user_agent, created_at) FROM stdin;
\.


--
-- TOC entry 3875 (class 0 OID 17632)
-- Dependencies: 223
-- Data for Name: doctor_patients; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.doctor_patients (id, doctor_id, patient_id, relationship_status, current_engagement_id, added_at, relationship_started_at, relationship_ended_at, is_active, created_at, updated_at) FROM stdin;
c7e67fa1-9f01-4aa6-8340-3c269fa7bb85	590e9593-7762-4d99-a034-a5f79dff25bd	1db15fab-11be-4148-9000-4bcdd745338d	FULL_ACCESS	f75666c4-a68a-4839-9de3-705270a37572	2026-01-26 14:58:57.439734	2026-01-27 12:44:40.155015	\N	t	\N	2026-02-09 19:39:20.652526
\.


--
-- TOC entry 3876 (class 0 OID 17640)
-- Dependencies: 224
-- Data for Name: doctor_profiles; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.doctor_profiles (id, user_id, title, bio, specialities, experience_years, certificates, location_city, location_country, is_verified, verification_data, created_at, updated_at, profile_picture_path, verification_status, availability_status, specialization, rating, total_reviews, profile_completion_percentage, social_media, consultation_fee, years_of_experience, latitude, longitude) FROM stdin;
590e9593-7762-4d99-a034-a5f79dff25bd	4e1bd976-626a-45a2-84ce-e306b9c23108	\N	\N	\N	\N	\N	\N	\N	f	\N	2026-01-22 15:12:17.684931	2026-02-09 17:51:08.806168	\N	unverified	offline	\N	0.00	0	0	\N	\N	\N	\N	\N
e2d5a089-feab-4c7b-8a9d-d430e5f5c74e	425357c8-7f55-4851-9129-4b97aaad84bd	\N	\N	\N	\N	\N	\N	\N	f	\N	2026-02-09 17:43:48.290671	2026-02-09 17:51:08.806168	\N	unverified	offline	\N	0.00	0	0	\N	\N	\N	\N	\N
38550775-a0ba-4a86-949c-9a73c492a5a4	8ad37331-0cd4-482d-bb84-3403a460e87c	\N	\N	\N	\N	\N	\N	\N	f	\N	2026-02-14 05:19:10.83906	2026-02-14 05:19:10.83906	\N	unverified	offline	\N	0.00	0	0	\N	\N	\N	\N	\N
\.


--
-- TOC entry 3877 (class 0 OID 17654)
-- Dependencies: 225
-- Data for Name: doctor_reviews; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.doctor_reviews (id, doctor_id, patient_id, rating, comment, created_at) FROM stdin;
\.


--
-- TOC entry 3878 (class 0 OID 17660)
-- Dependencies: 226
-- Data for Name: doctor_verification_questions; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.doctor_verification_questions (id, doctor_id, question_key, answer, verified_at, created_at, updated_at) FROM stdin;
\.


--
-- TOC entry 3898 (class 0 OID 18192)
-- Dependencies: 246
-- Data for Name: email_verification_otps; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.email_verification_otps (id, user_id, otp_code, created_at, expires_at, verified_at, attempts, is_used, ip_address, user_agent) FROM stdin;
\.


--
-- TOC entry 3879 (class 0 OID 17668)
-- Dependencies: 227
-- Data for Name: engagement_access_rules; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.engagement_access_rules (id, rule_name, can_view_all_history, can_view_current_only, can_view_patient_profile, can_modify_notes, can_message_patient, retains_period_access, retains_history_access, retains_no_access, description, is_active, created_at, updated_at) FROM stdin;
77d71fc3-9775-4d2e-97ec-2d193775c907	FULL_ACCESS	t	t	t	t	t	t	f	f	Full access to all patient data and history	t	2026-01-22 15:00:13.467252	2026-01-22 15:00:13.467252
e3d27b93-d28e-4421-abfb-b4531632bb52	CURRENT_ENGAGEMENT_ACCESS	f	t	t	t	t	f	f	t	Access only to current engagement period	t	2026-01-22 15:00:13.467252	2026-01-22 15:00:13.467252
2d95c097-d34c-43f7-abe4-cea71ccf32d6	READ_ONLY_ACCESS	t	t	t	f	f	t	f	f	Read-only access to patient data	t	2026-01-22 15:00:13.467252	2026-01-22 15:00:13.467252
fdcdbf2a-3f3a-4c08-9ce6-20c6a883e9fd	LIMITED_ENGAGEMENT_ACCESS	f	t	f	f	t	f	f	t	Limited access during active engagement only	t	2026-01-22 15:00:13.467252	2026-01-22 15:00:13.467252
58f5bef8-f34c-4182-b06a-202fd428c735	NO_ACCESS	f	f	f	f	f	f	f	t	No access to patient data	t	2026-01-22 15:00:13.467252	2026-01-22 15:00:13.467252
70042ef5-fd60-4812-8e7e-39c2db3c20c5	INITIAL_PENDING	f	f	f	f	f	f	f	t	First engagement request sent, not yet verified	t	2026-01-22 15:00:13.467252	2026-01-22 15:00:13.467252
193a5b29-715b-4626-a9b5-508ad49619c9	INITIAL_CANCELLED_PENDING	f	f	f	f	f	f	f	t	First engagement cancelled before activation	t	2026-01-22 15:00:13.467252	2026-01-22 15:00:13.467252
\.


--
-- TOC entry 3880 (class 0 OID 17685)
-- Dependencies: 228
-- Data for Name: engagement_analytics; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.engagement_analytics (id, engagement_id, metric_name, metric_value, recorded_at, created_at) FROM stdin;
\.


--
-- TOC entry 3881 (class 0 OID 17693)
-- Dependencies: 229
-- Data for Name: engagement_events; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.engagement_events (id, engagement_id, event_type, triggered_at, triggered_by, payload, created_at) FROM stdin;
a89e7cfe-e8f6-492e-9956-b60a672cfd3f	76cffb88-8656-4c73-a6d1-dfde555b2631	INITIATED	2026-01-27 16:30:47.06495	4e1bd976-626a-45a2-84ce-e306b9c23108	{"doctorName": "adel", "patientName": "ahmed"}	2026-01-27 16:30:47.06495
23da6775-10ae-4115-9d22-1c8988b5bda7	76cffb88-8656-4c73-a6d1-dfde555b2631	CANCELLED	2026-01-27 16:32:36.335787	66574cc8-3141-4468-8b4f-0007ca0cedd0	{"reason": "Switching doctors", "cancelledBy": "PATIENT"}	2026-01-27 16:32:36.335787
04756b1f-90db-480f-9759-5d52d2ae3ded	f75666c4-a68a-4839-9de3-705270a37572	INITIATED	2026-02-09 19:36:20.87023	4e1bd976-626a-45a2-84ce-e306b9c23108	{"doctorName": "adel", "patientName": "ahmed"}	2026-02-09 19:36:20.87023
36bd4b49-fc12-42ba-b357-65a5bad2417b	f75666c4-a68a-4839-9de3-705270a37572	VERIFIED	2026-02-09 19:39:20.659518	66574cc8-3141-4468-8b4f-0007ca0cedd0	{"role": "PATIENT"}	2026-02-09 19:39:20.659518
\.


--
-- TOC entry 3883 (class 0 OID 17702)
-- Dependencies: 231
-- Data for Name: engagement_messages; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.engagement_messages (id, engagement_id, message_uuid, sender_id, recipient_id, content, content_type, sent_at, delivered_at, read_at, is_encrypted, encryption_key_id, is_system_message, system_message_type, created_at) FROM stdin;
3fa4fcd0-fe7d-4a41-b5ab-0168119cf34f	76cffb88-8656-4c73-a6d1-dfde555b2631	2b7e1519-4888-492d-a671-7c2017fac81f	\N	\N	🚫 Patient abdallah cancelled the pending engagement.\nReason: Switching doctors	text	2026-01-27 16:32:36.416962	\N	\N	t	\N	t	\N	2026-01-27 16:32:36.326928
b39b2263-3021-41c4-bf65-f7f12a542e19	f75666c4-a68a-4839-9de3-705270a37572	ba109ced-1346-45df-8df1-0d6ca81682e6	\N	\N	✅ Engagement activated. Access level: FULL_ACCESS	text	2026-02-09 19:39:20.668548	\N	\N	t	\N	t	\N	2026-02-09 19:39:20.652526
20bb7a07-d3b1-4781-98b1-97067d4bb784	f75666c4-a68a-4839-9de3-705270a37572	aef1f5d1-a83d-46bc-98a5-6f8dbf3b0689	\N	\N	🔔 Access level changed from "READ_ONLY_ACCESS" to "FULL_ACCESS"	text	2026-02-09 19:39:20.652526	\N	\N	t	\N	t	access_changed	2026-02-09 19:39:20.652526
2280b735-02b1-4bfc-a811-3a5a976309a2	f75666c4-a68a-4839-9de3-705270a37572	dd617824-a40e-4f18-94ef-252099e01edb	\N	\N	🔔 Engagement started with access level: FULL_ACCESS	text	2026-02-09 19:39:20.652526	\N	\N	t	\N	t	engagement_started	2026-02-09 19:39:20.652526
\.


--
-- TOC entry 3884 (class 0 OID 17714)
-- Dependencies: 232
-- Data for Name: engagement_sessions; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.engagement_sessions (id, engagement_id, user_id, session_token, session_start, session_end, ip_address, user_agent, device_info, created_at) FROM stdin;
\.


--
-- TOC entry 3885 (class 0 OID 17722)
-- Dependencies: 233
-- Data for Name: engagement_verification_tokens; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.engagement_verification_tokens (id, engagement_id, token, verification_type, qr_code_data, doctor_id, patient_id, verified_by, verified_at, expires_at, status, created_at) FROM stdin;
3c0c4da0-e10f-49b5-bd17-f991027b87f2	76cffb88-8656-4c73-a6d1-dfde555b2631	C4B3EB02	start	neuralhealer://verify/start/C4B3EB02	\N	\N	\N	\N	2026-01-27 16:33:47.066783	pending	2026-01-27 16:30:47.068293
de989da7-9c19-4f74-97fc-088400de3704	f75666c4-a68a-4839-9de3-705270a37572	4884380B	start	neuralhealer://verify/start/4884380B	\N	\N	\N	2026-02-09 19:39:20.659518	2026-02-09 19:39:20.87023	verified	2026-02-09 19:36:20.87249
\.


--
-- TOC entry 3886 (class 0 OID 17730)
-- Dependencies: 234
-- Data for Name: engagements; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.engagements (id, engagement_id, doctor_id, patient_id, access_rule_name, status, engagement_type, start_at, end_at, ended_by, termination_reason, start_verified_at, end_verified_at, notes, created_at, updated_at, initiated_by) FROM stdin;
76cffb88-8656-4c73-a6d1-dfde555b2631	ENG-2026-000008	590e9593-7762-4d99-a034-a5f79dff25bd	1db15fab-11be-4148-9000-4bcdd745338d	FULL_ACCESS	cancelled	\N	\N	2026-01-27 16:32:36.33428	66574cc8-3141-4468-8b4f-0007ca0cedd0	Switching doctors	\N	\N	\N	2026-01-27 16:30:47.05719	2026-01-27 16:32:36.326928	doctor
f75666c4-a68a-4839-9de3-705270a37572	ENG-2026-000009	590e9593-7762-4d99-a034-a5f79dff25bd	1db15fab-11be-4148-9000-4bcdd745338d	FULL_ACCESS	active	\N	2026-02-09 19:39:20.659518	\N	\N	\N	2026-02-09 19:39:20.659518	\N	\N	2026-02-09 19:36:20.825484	2026-02-09 19:39:20.652526	doctor
\.


--
-- TOC entry 3887 (class 0 OID 17739)
-- Dependencies: 235
-- Data for Name: message_queues; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.message_queues (id, job_type, payload, status, scheduled_at, processed_at, retry_count, error_message, created_at) FROM stdin;
472f718e-19fe-4e9d-a333-3b96ab89cbe8	EMAIL_NOTIFICATION	{"body": "Hi ahmed, we're thrilled to have you here! Your journey to better health starts now. Let us know if you need any help getting started.", "title": "Welcome to NeuralHealer! 🎉", "userId": "a03f93bc-f258-46a0-bf71-414d0e9442cf", "userName": "ahmed", "doctorName": "Doctor", "templateKey": "USER_WELCOME", "notificationId": "01de5b8c-76be-4e88-bf63-c8e0ae794c3d"}	completed	2026-01-30 17:17:20.79872	2026-01-30 17:17:20.927721	0	\N	2026-01-30 17:17:20.79872
95003dac-e597-4d0a-ba95-75107b3e15f6	EMAIL_NOTIFICATION	{"body": "Hi Dr. Sarah, we're thrilled to have you here! Your journey to better health starts now. Let us know if you need any help getting started.", "title": "Welcome to NeuralHealer! 🎉", "userId": "425357c8-7f55-4851-9129-4b97aaad84bd", "userName": "Dr. Sarah", "doctorName": "Doctor", "templateKey": "USER_WELCOME", "notificationId": "00bc985e-c736-44eb-8fd3-a23965d07477"}	completed	2026-02-09 17:43:48.162384	2026-02-09 17:43:48.381029	0	\N	2026-02-09 17:43:48.162384
40f3dac7-327b-4626-a1cd-732828a93663	EMAIL_NOTIFICATION	{"body": "Hi Dr. ahmed, we're thrilled to have you here! Your journey to better health starts now. Let us know if you need any help getting started.", "title": "Welcome to NeuralHealer! 🎉", "userId": "d2a5cea5-3fef-4fee-a45a-497003bfcb3c", "userName": "Dr. ahmed", "doctorName": "Doctor", "templateKey": "USER_WELCOME", "notificationId": "c286baea-bf90-4325-9d63-143e518766da"}	completed	2026-02-13 22:34:10.547419	2026-02-13 22:34:11.540392	0	\N	2026-02-13 22:34:10.547419
9a57fdba-56ac-47a0-8ebf-72c4d4f9d42e	EMAIL_NOTIFICATION	{"body": "Hi Dr. Sarah, we're thrilled to have you here! Your journey to better health starts now. Let us know if you need any help getting started.", "title": "Welcome to NeuralHealer! 🎉", "userId": "ef0fe20c-eda9-43fe-9254-b6691e8a7719", "userName": "Dr. Sarah", "doctorName": "Doctor", "templateKey": "USER_WELCOME", "notificationId": "febbe18b-2d2a-421e-8a66-d32674154cbf"}	failed	2026-02-14 00:43:38.215295	2026-02-14 02:22:50.939233	3	Missing recipientEmail	2026-02-14 00:43:38.215295
63182c62-54cc-4b89-b295-47ceaf093f85	EMAIL_NOTIFICATION	{"body": "Hi Dr. Sarah, we're thrilled to have you here! Your journey to better health starts now. Let us know if you need any help getting started.", "title": "Welcome to NeuralHealer! 🎉", "userId": "a45e1f65-69a4-4ede-b80b-d6739651e5a9", "userName": "Dr. Sarah", "doctorName": "Doctor", "templateKey": "USER_WELCOME", "notificationId": "d3eada50-a24b-4061-8b45-b2d77398724c"}	failed	2026-02-14 01:14:20.802325	2026-02-14 02:22:51.193508	3	Missing recipientEmail	2026-02-14 01:14:20.802325
9abbc480-90d2-4d95-9ff7-069ee8a81bc0	EMAIL_NOTIFICATION	{"body": "Hi Dr. Sarah, we're thrilled to have you here! Your journey to better health starts now. Let us know if you need any help getting started.", "title": "Welcome to NeuralHealer! 🎉", "userId": "9b199c08-f6a9-4b57-a595-c6838409e362", "userName": "Dr. Sarah", "doctorName": "Doctor", "templateKey": "USER_WELCOME", "notificationId": "4c09ffb9-a507-4790-b0a7-d196d63d9771"}	completed	2026-02-14 02:29:02.452081	2026-02-14 02:29:06.095	0	\N	2026-02-14 02:29:02.452081
682f11db-9e5e-46ec-8c29-407b749fd4e9	EMAIL_NOTIFICATION	{"body": "Hi John, we're thrilled to have you here! Your journey to better health starts now. Let us know if you need any help getting started.", "title": "Welcome to NeuralHealer! 🎉", "userId": "e72b0166-04b2-4d33-815d-6965dd16a698", "userName": "John", "doctorName": "Doctor", "templateKey": "USER_WELCOME", "notificationId": "906b3920-89f4-4886-ba50-f8974f282f29"}	completed	2026-02-14 02:34:12.544291	2026-02-14 02:34:16.468003	0	\N	2026-02-14 02:34:12.544291
e7aa59f3-2ab2-4494-bd0f-ae1e2d84aced	EMAIL_NOTIFICATION	{"body": "Hi Dr. Sarah, we're thrilled to have you here! Your journey to better health starts now. Let us know if you need any help getting started.", "title": "Welcome to NeuralHealer! 🎉", "userId": "0578f359-d47b-4b47-b3f2-64e985d3f7aa", "userName": "Dr. Sarah", "doctorName": "Doctor", "templateKey": "USER_WELCOME", "notificationId": "312f2e8f-f174-4b79-bb0e-f88da1af8be3"}	completed	2026-02-14 03:02:55.162929	2026-02-14 03:02:58.412605	0	\N	2026-02-14 03:02:55.162929
1a06d77b-08e0-4c97-a62c-4259a58a4f82	EMAIL_NOTIFICATION	{"body": "Hi Dr. Sarah, we're thrilled to have you here! Your journey to better health starts now. Let us know if you need any help getting started.", "title": "Welcome to NeuralHealer! 🎉", "userId": "8144a0bc-2e10-4618-bd0d-9e6c6d6511f9", "otpCode": null, "userName": "Dr. Sarah", "doctorName": "Doctor", "templateKey": "USER_WELCOME", "notificationId": "1926f681-8de0-484b-9181-f6047d019485"}	completed	2026-02-14 03:11:59.031998	2026-02-14 03:12:03.66571	0	\N	2026-02-14 03:11:59.031998
07857dbd-7f09-4f31-ab16-ca4f03b4bc0d	EMAIL_NOTIFICATION	{"body": "Hi Dr. Sarah, we're thrilled to have you here! Your journey to better health starts now. Let us know if you need any help getting started.", "title": "Welcome to NeuralHealer! 🎉", "userId": "93158a34-6d54-4020-b029-6265a31cf450", "otpCode": null, "userName": "Dr. Sarah", "doctorName": "Doctor", "templateKey": "USER_WELCOME", "notificationId": "79d36f40-f9d1-416c-8dd8-3cb7130b5040"}	completed	2026-02-14 03:17:09.17448	2026-02-14 03:17:09.94043	0	\N	2026-02-14 03:17:09.17448
d94471b1-c8d5-4edf-9329-8a898f43145a	EMAIL_NOTIFICATION	{"body": "Hi Dr. Sarah, we're thrilled to have you here! Your journey to better health starts now. Let us know if you need any help getting started.", "title": "Welcome to NeuralHealer! 🎉", "userId": "8ad37331-0cd4-482d-bb84-3403a460e87c", "otpCode": null, "userName": "Dr. Sarah", "doctorName": "Doctor", "templateKey": "USER_WELCOME", "notificationId": "adc8e84b-3cc0-4db0-8f59-d80a86891b5f"}	completed	2026-02-14 03:20:47.066792	2026-02-14 03:20:48.390384	0	\N	2026-02-14 03:20:47.066792
\.


--
-- TOC entry 3888 (class 0 OID 17749)
-- Dependencies: 236
-- Data for Name: notification_message_templates; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.notification_message_templates (id, template_key, language_code, title, message, recipient_context, default_priority, notes, created_at, updated_at, channels) FROM stdin;
d999584b-5718-4ee6-bcf7-bff467d536de	ENGAGEMENT_ENDED	en	Engagement Ended	Your engagement with {patientName} has ended.	doctor	normal	\N	2026-01-27 15:37:34.747301	2026-01-27 15:37:34.747301	{"sse": true, "push": false, "email": false}
299e9f7b-3cba-4b18-9a03-4d5dc0b220f7	ENGAGEMENT_ENDED	ar	انتهت المتابعة	انتهت متابعتك مع المريض {patientName}.	doctor	normal	\N	2026-01-27 15:37:34.747301	2026-01-27 15:37:34.747301	{"sse": true, "push": false, "email": false}
0ee4df84-eeb0-4b7d-bdfd-0ff1b4322797	ENGAGEMENT_ENDED	en	Engagement Ended	Your engagement with Dr. {doctorName} has ended.	patient	normal	\N	2026-01-27 15:37:34.747301	2026-01-27 15:37:34.747301	{"sse": true, "push": false, "email": false}
40f1fa4d-d6cd-4cd4-babb-b6db191c565d	ENGAGEMENT_ENDED	ar	انتهت المتابعة	انتهت متابعتك مع الدكتور {doctorName}.	patient	normal	\N	2026-01-27 15:37:34.747301	2026-01-27 15:37:34.747301	{"sse": true, "push": false, "email": false}
74757050-0403-4e46-9caa-1156ad838500	MESSAGE_RECEIVED	en	New Message	You received a message from {patientName}.	doctor	normal	\N	2026-01-27 15:37:34.747301	2026-01-27 15:37:34.747301	{"sse": true, "push": false, "email": false}
a526cad6-d847-4fd7-bf4f-3cfb36d87bd5	MESSAGE_RECEIVED	ar	رسالة جديدة	لديك رسالة جديدة من {patientName}.	doctor	normal	\N	2026-01-27 15:37:34.747301	2026-01-27 15:37:34.747301	{"sse": true, "push": false, "email": false}
2b705223-cd8d-4b19-8607-5af364ebd3c2	MESSAGE_RECEIVED	en	New Message	You received a message from Dr. {doctorName}.	patient	normal	\N	2026-01-27 15:37:34.747301	2026-01-27 15:37:34.747301	{"sse": true, "push": false, "email": false}
820bb023-ab2e-4812-ad89-835ce9f8c89e	MESSAGE_RECEIVED	ar	رسالة جديدة	لديك رسالة جديدة من الدكتور {doctorName}.	patient	normal	\N	2026-01-27 15:37:34.747301	2026-01-27 15:37:34.747301	{"sse": true, "push": false, "email": false}
2c52a344-5e17-4bd7-9d92-8cd0d8461ff6	SYSTEM_ALERT	en	System Alert	{alertMessage}	doctor	critical	\N	2026-01-27 15:37:34.747301	2026-01-27 15:37:34.747301	{"sse": true, "push": false, "email": false}
88b5211b-65fb-4364-8046-f52a05e4e128	AI_RESPONSE_READY	en	AI Analysis Ready	Your AI health analysis is ready to view.	patient	normal	\N	2026-01-27 15:37:34.747301	2026-01-27 15:37:34.747301	{"sse": true, "push": false, "email": false}
c2404060-71d8-453c-9596-ce2edf139181	AI_RESPONSE_READY	ar	التحليل الذكي جاهز	تحليل صحتك بالذكاء الاصطناعي جاهز للعرض.	patient	normal	\N	2026-01-27 15:37:34.747301	2026-01-27 15:37:34.747301	{"sse": true, "push": false, "email": false}
1a0d6254-32ab-42b4-882d-d11eaa06a017	SYSTEM_ALERT	ar	تنبيه النظام	{alertMessage}	doctor	critical	\N	2026-01-27 15:37:34.747301	2026-01-27 15:37:34.747301	{"sse": true, "push": false, "email": false}
3683f71e-0d92-4341-84f2-1a94aa6c47ab	SYSTEM_ALERT	en	System Alert	{alertMessage}	patient	critical	\N	2026-01-27 15:37:34.747301	2026-01-27 15:37:34.747301	{"sse": true, "push": false, "email": false}
09ed9ff4-8665-45d4-bf04-7eb13396a98d	SYSTEM_ALERT	ar	تنبيه النظام	{alertMessage}	patient	critical	\N	2026-01-27 15:37:34.747301	2026-01-27 15:37:34.747301	{"sse": true, "push": false, "email": false}
c43b30f4-28a2-4363-ad06-d4625ccedfbf	ACCESS_LEVEL_CHANGED	en	Access Updated	Access level changed from "{oldAccess}" to "{newAccess}" for patient {patientName}.	doctor	normal	\N	2026-01-27 15:37:34.747301	2026-01-27 15:37:34.747301	{"sse": true, "push": false, "email": false}
94878a1e-0603-47bf-959c-1a3b33cbce2c	ACCESS_LEVEL_CHANGED	ar	تم تحديث الصلاحيات	تم تغيير مستوى الوصول من "{oldAccess}" إلى "{newAccess}" للمريض {patientName}.	doctor	normal	\N	2026-01-27 15:37:34.747301	2026-01-27 15:37:34.747301	{"sse": true, "push": false, "email": false}
e1fe5648-9951-4a76-9718-2e0d0694fe49	ENGAGEMENT_CANCELLED	en	Engagement Cancelled	You have cancelled the engagement with {otherPartyName}.	initiator	high	\N	2026-01-27 15:37:34.747301	2026-01-27 15:37:34.747301	{"sse": true, "email": true}
b2aa0a60-2c03-4aba-87dc-665a1dacf8cb	USER_WELCOME	ar	مرحباً بك في NeuralHealer! 🎉	مرحباً {userName}، يسعدنا انضمامك! رحلتك نحو صحة أفضل تبدأ الآن. أخبرنا إذا احتجت أي مساعدة للبدء.	patient	normal	\N	2026-01-27 17:06:35.291358	2026-01-27 17:06:35.291358	{"sse": true, "email": true}
0001d2b7-e4b1-46ac-ae22-28bdaf5d46ff	USER_WELCOME	en	Welcome to NeuralHealer! 🎉	Hi {userName}, we're thrilled to have you here! Your journey to better health starts now. Let us know if you need any help getting started.	patient	normal	\N	2026-01-27 17:06:35.291358	2026-01-27 17:06:35.291358	{"sse": true, "email": true}
250fd89e-09b5-4c84-98b2-5fba7ca5d4fd	ENGAGEMENT_STARTED	ar	تم تفعيل المتابعة	المريض {patientName} قام بالتحقق وبدأ المتابعة.	doctor	high	\N	2026-01-27 15:37:34.747301	2026-01-27 15:37:34.747301	{"sse": true, "email": true}
f29bbc1a-8033-4440-ab72-642e06c3bfd4	ENGAGEMENT_STARTED	en	Engagement Activated	Patient {patientName} has verified and started the engagement.	doctor	high	\N	2026-01-27 15:37:34.747301	2026-01-27 15:37:34.747301	{"sse": true, "email": true}
abc0df86-f98f-4691-886a-b483720afbcc	ENGAGEMENT_CANCELLED	en	Engagement Cancelled	{otherPartyName} has cancelled the engagement.	target	high	\N	2026-01-27 15:37:34.747301	2026-01-27 15:37:34.747301	{"sse": true, "email": true}
056f9d30-a184-44e8-80cc-23a4d849cc74	ENGAGEMENT_CANCELLED	ar	تم إلغاء المتابعة	لقد قمت بإلغاء المتابعة مع {otherPartyName}.	initiator	high	\N	2026-01-27 15:37:34.747301	2026-01-27 15:37:34.747301	{"sse": true, "email": true}
5d0c934b-f66a-4a55-85d3-ebc5d143d021	ENGAGEMENT_CANCELLED	ar	تم إلغاء المتابعة	{otherPartyName} قام بإلغاء المتابعة.	target	high	\N	2026-01-27 15:37:34.747301	2026-01-27 15:37:34.747301	{"sse": true, "email": true}
72fad524-e8cf-4b94-ac90-9c0d822aa2ac	USER_REENGAGE_ACTIVE	ar	نفتقدك! 👋	مرحباً {userName}، مضى 3 أيام منذ زيارتك الأخيرة. كيف حالك اليوم؟ تحقق من حالتك الصحية معنا!	patient	normal	\N	2026-01-27 17:06:35.291358	2026-01-27 17:06:35.291358	{"sse": true, "email": true}
e1f24e9d-e909-4dc3-8ecd-3e6efaa13128	USER_REENGAGE_ACTIVE	en	We miss you! 👋	Hey {userName}, it's been 3 days since your last visit. How are you feeling today? Check in with your health companion!	patient	normal	\N	2026-01-27 17:06:35.291358	2026-01-27 17:06:35.291358	{"sse": true, "email": true}
7a05789c-03b0-45cd-9759-897e1382bc18	USER_INACTIVITY_WARNING	ar	ابقَ على تواصل مع صحتك	مرحباً {userName}، لاحظنا أنك لم تسجل دخولك لمدة 14 يوماً. صحتك تهمنا - عد وشاهد ما الجديد!	patient	normal	\N	2026-01-27 17:06:35.291358	2026-01-27 17:06:35.291358	{"sse": true, "email": true}
3f6fcbc7-b114-4cc0-b2f4-6fe7ec4ce2f7	USER_INACTIVITY_WARNING	en	Stay Connected with Your Health	Hi {userName}, we noticed you haven't logged in for 14 days. Your health journey matters to us - come back and see what's new!	patient	normal	\N	2026-01-27 17:06:35.291358	2026-01-27 17:06:35.291358	{"sse": true, "email": true}
a1b2c3d4-5e6f-7a8b-9c0d-1e2f3a4b5c6d	EMAIL_VERIFICATION	en	Verify Your Email Address	Your email verification code is: {otpCode}. This code will expire in 15 minutes.	patient	high	\N	2026-02-14 06:05:00.000000	2026-02-14 06:05:00.000000	{"sse": true, "email": true, "push": false, "sms": false, "whatsapp": false}
b2c3d4e5-6f7a-8b9c-0d1e-2f3a4b5c6d7e	EMAIL_VERIFICATION	en	Verify Your Email Address	Your email verification code is: {otpCode}. This code will expire in 15 minutes.	doctor	high	\N	2026-02-14 06:05:00.000000	2026-02-14 06:05:00.000000	{"sse": true, "email": true, "push": false, "sms": false, "whatsapp": false}
c3d4e5f6-7a8b-9c0d-1e2f-3a4b5c6d7e8f	EMAIL_VERIFICATION	ar	تحقق من عنوان بريدك الإلكتروني	رمز التحقق من بريدك الإلكتروني هو: {otpCode}. سينتهي صلاحية هذا الرمز في 15 دقيقة.	patient	high	\N	2026-02-14 06:05:00.000000	2026-02-14 06:05:00.000000	{"sse": true, "email": true, "push": false, "sms": false, "whatsapp": false}
d4e5f6a7-8b9c-0d1e-2f3a-4b5c6d7e8f9a	EMAIL_VERIFICATION	ar	تحقق من عنوان بريدك الإلكتروني	رمز التحقق من بريدك الإلكتروني هو: {otpCode}. سينتهي صلاحية هذا الرمز في 15 دقيقة.	doctor	high	\N	2026-02-14 06:05:00.000000	2026-02-14 06:05:00.000000	{"sse": true, "email": true, "push": false, "sms": false, "whatsapp": false}
\.


--
-- TOC entry 3889 (class 0 OID 17759)
-- Dependencies: 237
-- Data for Name: notification_templates; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.notification_templates (id, type, title_template, message_template, default_priority, default_channels, created_at) FROM stdin;
9fdbe205-7c2f-4272-8693-d79b11914c57	ENGAGEMENT_STARTED	Engagement Activated	Patient {patientName} has verified and started the engagement.	high	["sse"]	2026-01-25 19:22:45.084452
935102fb-6b19-44c9-b8aa-6a03297776d9	ENGAGEMENT_CANCELLED	Engagement Cancelled	{actorName} has cancelled the engagement.	high	["sse"]	2026-01-25 19:22:45.084452
0cb36560-cddb-4f35-a14d-00500f27bb8e	MESSAGE_RECEIVED	New Message	You have a new message from {senderName}.	normal	["sse"]	2026-01-25 19:22:45.084452
2645ea1a-1909-41cf-bf14-bc1b47d9676b	AI_RESPONSE_READY	AI Analysis Ready	Your AI health analysis is ready.	normal	["sse"]	2026-01-25 19:22:45.084452
50dc1df4-2376-49a2-a280-f41e8377bb92	SYSTEM_ALERT	System Alert	{alertMessage}	critical	["sse"]	2026-01-25 19:22:45.084452
\.


--
-- TOC entry 3890 (class 0 OID 17768)
-- Dependencies: 238
-- Data for Name: notifications; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.notifications (id, user_id, type, title, message, payload, is_read, sent_at, read_at, expires_at, created_at, priority, source, delivery_status, metadata, send_email) FROM stdin;
ff792f90-876d-4306-9b45-78bf784c0bc6	66574cc8-3141-4468-8b4f-0007ca0cedd0	ENGAGEMENT_CANCELLED	Engagement Cancelled	Dr. adel has cancelled the engagement.	\N	f	2026-01-22 16:10:03.87737	\N	\N	2026-01-22 16:10:03.970679	normal	engagement	{"sse": true}	{}	f
d852decc-4798-462a-9be0-6197348598fc	66574cc8-3141-4468-8b4f-0007ca0cedd0	ENGAGEMENT_CANCELLED	Engagement Cancelled	Patient abdallah has cancelled the engagement.	\N	f	2026-01-22 16:11:19.934895	\N	\N	2026-01-22 16:11:19.97075	normal	engagement	{"sse": true}	{}	f
e74a3e41-ef91-4e36-a8fc-4e959552b949	66574cc8-3141-4468-8b4f-0007ca0cedd0	ENGAGEMENT_CANCELLED	Engagement Cancelled	Dr. adel has cancelled the engagement.	\N	f	2026-01-22 16:31:14.60723	\N	\N	2026-01-22 16:31:14.642611	normal	engagement	{"sse": true}	{}	f
85b4a7ec-0367-4493-a66a-90ce0a0ed928	66574cc8-3141-4468-8b4f-0007ca0cedd0	ENGAGEMENT_CANCELLED	Engagement Cancelled	Patient abdallah has cancelled the engagement.	{}	f	2026-01-26 15:01:08.212316	\N	\N	2026-01-26 15:01:08.212316	normal	system	{"sse": true}	{}	f
181e40f3-95d9-424f-9fcd-9c4ac0b3515e	66574cc8-3141-4468-8b4f-0007ca0cedd0	ENGAGEMENT_CANCELLED	Engagement Cancelled	Patient abdallah has cancelled the engagement.	{}	f	2026-01-27 12:46:21.018546	\N	\N	2026-01-27 12:46:21.018546	normal	system	{"sse": true}	{}	f
6d3780d5-8ab2-4b2d-8fa5-fc19f455a5ad	66574cc8-3141-4468-8b4f-0007ca0cedd0	ENGAGEMENT_CANCELLED	Engagement Cancelled	Patient abdallah has cancelled the engagement.	{}	f	2026-01-27 12:54:05.061794	\N	\N	2026-01-27 12:54:05.063081	normal	system	{"sse": true}	{}	f
00bc985e-c736-44eb-8fd3-a23965d07477	425357c8-7f55-4851-9129-4b97aaad84bd	USER_WELCOME	Welcome to NeuralHealer! 🎉	Hi Dr. Sarah, we're thrilled to have you here! Your journey to better health starts now. Let us know if you need any help getting started.	{"userName": "Dr. Sarah"}	f	2026-02-09 17:43:48.162384	\N	\N	2026-02-09 17:43:48.162384	normal	system	{"sms": false, "sse": true, "push": false, "email": true, "whatsapp": false}	{}	t
7a394d04-f0b6-4690-adb4-2ad2acc1c30a	4e1bd976-626a-45a2-84ce-e306b9c23108	ENGAGEMENT_STARTED	Engagement Activated	Patient abdallah has verified and started the engagement.	\N	f	2026-01-22 15:14:37.116442	\N	\N	2026-01-22 15:14:37.211164	normal	engagement	{"sse": true}	{}	f
d17e3ad3-75c8-46c5-90b7-e9362baf37be	4e1bd976-626a-45a2-84ce-e306b9c23108	ENGAGEMENT_CANCELLED	Engagement Cancelled	Patient abdallah has cancelled the engagement.	\N	f	2026-01-22 15:15:14.637487	\N	\N	2026-01-22 15:15:14.671461	normal	engagement	{"sse": true}	{}	f
a7acb664-bfaa-4c65-9347-c85b32c5b463	4e1bd976-626a-45a2-84ce-e306b9c23108	ENGAGEMENT_STARTED	Engagement Activated	Patient abdallah has verified and started the engagement.	\N	f	2026-01-22 15:44:59.292285	\N	\N	2026-01-22 15:44:59.365009	normal	engagement	{"sse": true}	{}	f
c831c319-3f17-4c33-b8a0-d85fc95bcaa8	4e1bd976-626a-45a2-84ce-e306b9c23108	ENGAGEMENT_CANCELLED	Engagement Cancelled	Dr. adel has cancelled the engagement.	\N	f	2026-01-22 16:10:03.87737	\N	\N	2026-01-22 16:10:03.967819	normal	engagement	{"sse": true}	{}	f
8b5425e9-758d-454e-9b48-3fbdb9619953	4e1bd976-626a-45a2-84ce-e306b9c23108	ENGAGEMENT_CANCELLED	Engagement Cancelled	Patient abdallah has cancelled the engagement.	\N	f	2026-01-22 16:11:19.934895	\N	\N	2026-01-22 16:11:19.968802	normal	engagement	{"sse": true}	{}	f
b59ed66d-d789-45e5-81f5-6f8dd393d785	4e1bd976-626a-45a2-84ce-e306b9c23108	ENGAGEMENT_STARTED	Engagement Activated	Patient abdallah has verified and started the engagement.	\N	f	2026-01-22 16:30:34.745541	\N	\N	2026-01-22 16:30:34.7793	normal	engagement	{"sse": true}	{}	f
0af476f7-8346-4e16-8543-016b8c51f94d	4e1bd976-626a-45a2-84ce-e306b9c23108	ENGAGEMENT_CANCELLED	Engagement Cancelled	Dr. adel has cancelled the engagement.	\N	f	2026-01-22 16:31:14.60723	\N	\N	2026-01-22 16:31:14.638996	normal	engagement	{"sse": true}	{}	f
6133ffa7-3169-4328-8def-ffc92cfc9d98	4e1bd976-626a-45a2-84ce-e306b9c23108	ENGAGEMENT_CANCELLED	Engagement Cancelled	Patient abdallah has cancelled the engagement.	{}	f	2026-01-26 15:01:08.19927	\N	\N	2026-01-26 15:01:08.199864	normal	system	{"sse": true}	{}	f
3e82836c-7225-42c5-94b9-c4217f15cda3	4e1bd976-626a-45a2-84ce-e306b9c23108	ENGAGEMENT_STARTED	Engagement Activated	Patient abdallah has verified and started the engagement.	{}	f	2026-01-27 12:44:40.209947	\N	\N	2026-01-27 12:44:40.211651	normal	system	{"sse": true}	{}	f
872d376c-d06a-4763-b774-d4b4dd67008b	4e1bd976-626a-45a2-84ce-e306b9c23108	ENGAGEMENT_CANCELLED	Engagement Cancelled	Patient abdallah has cancelled the engagement.	{}	f	2026-01-27 12:46:21.016242	\N	\N	2026-01-27 12:46:21.016386	normal	system	{"sse": true}	{}	f
8eab81cc-493e-4c72-bd14-617408d0d7ae	66574cc8-3141-4468-8b4f-0007ca0cedd0	ENGAGEMENT_CANCELLED	Engagement Cancelled	Patient abdallah has cancelled the engagement.	{}	t	2026-01-27 16:32:36.401867	2026-02-09 19:40:10.448761	\N	2026-01-27 16:32:36.401867	high	engagement	{"sse": true}	{}	f
76f0d49f-2e27-43fb-ae11-f0101ff813f9	66574cc8-3141-4468-8b4f-0007ca0cedd0	ENGAGEMENT_CANCELLED	Engagement Cancelled	You have cancelled the engagement with Dr. ahmed adel.	{"engagementId": "76cffb88-8656-4c73-a6d1-dfde555b2631", "otherPartyName": "Dr. ahmed adel"}	t	2026-01-27 16:32:36.326928	2026-02-09 19:40:11.204971	\N	2026-01-27 16:32:36.326928	high	engagement	{"sse": true}	{}	f
beabf104-ae92-41f2-a02b-4ac007668ca8	4e1bd976-626a-45a2-84ce-e306b9c23108	ENGAGEMENT_STARTED	Engagement Activated	Patient abdallah has verified and started the engagement.	{}	f	2026-01-27 12:53:15.202205	\N	\N	2026-01-27 12:53:15.202718	normal	system	{"sse": true}	{}	f
b2073957-24ab-4c96-b837-a37262221a53	4e1bd976-626a-45a2-84ce-e306b9c23108	ENGAGEMENT_STARTED	Engagement Activated	Patient abdallah has verified and started the engagement.	{}	t	2026-02-09 19:39:20.745479	2026-02-09 19:39:37.1274	\N	2026-02-09 19:39:20.7457	high	engagement	{"sse": true}	{}	f
8f6b8bd0-077a-4dd8-863b-5be520aa0829	4e1bd976-626a-45a2-84ce-e306b9c23108	ENGAGEMENT_STARTED	Engagement Activated	Patient abdallah ahmed has verified and started the engagement.	{"patientName": "abdallah ahmed", "engagementId": "f75666c4-a68a-4839-9de3-705270a37572"}	t	2026-02-09 19:39:20.652526	2026-02-09 19:39:37.950899	\N	2026-02-09 19:39:20.652526	high	engagement	{"sse": true}	{}	f
855ce789-5e7b-4450-8485-5eccb74f7451	4e1bd976-626a-45a2-84ce-e306b9c23108	ENGAGEMENT_CANCELLED	Engagement Cancelled	Patient abdallah has cancelled the engagement.	{}	t	2026-01-27 16:32:36.371923	2026-02-09 19:39:38.817468	\N	2026-01-27 16:32:36.373432	high	engagement	{"sse": true}	{}	f
ae88cb20-2bf2-4ab5-878a-75156533214d	4e1bd976-626a-45a2-84ce-e306b9c23108	ENGAGEMENT_CANCELLED	Engagement Cancelled	abdallah ahmed has cancelled the engagement.	{"engagementId": "76cffb88-8656-4c73-a6d1-dfde555b2631", "otherPartyName": "abdallah ahmed"}	t	2026-01-27 16:32:36.326928	2026-02-09 19:39:39.910356	\N	2026-01-27 16:32:36.326928	high	engagement	{"sse": true}	{}	f
07b9f5e6-35cb-4c99-8834-0c6ed10529d1	4e1bd976-626a-45a2-84ce-e306b9c23108	ENGAGEMENT_CANCELLED	Engagement Cancelled	Patient abdallah has cancelled the engagement.	{}	t	2026-01-27 12:54:05.05959	2026-02-09 19:39:40.659145	\N	2026-01-27 12:54:05.060783	normal	system	{"sse": true}	{}	f
c728d5ae-2632-40b7-807a-f8e4e43a9517	66574cc8-3141-4468-8b4f-0007ca0cedd0	ENGAGEMENT_STARTED	Engagement Activated	Dr. ahmed has verified and started the engagement.	{}	f	2026-02-14 01:14:41.759281	\N	\N	2026-02-14 01:14:41.761891	high	engagement	{"sse": false}	{}	f
adc8e84b-3cc0-4db0-8f59-d80a86891b5f	8ad37331-0cd4-482d-bb84-3403a460e87c	USER_WELCOME	Welcome to NeuralHealer! 🎉	Hi Dr. Sarah, we're thrilled to have you here! Your journey to better health starts now. Let us know if you need any help getting started.	{"userName": "Dr. Sarah"}	f	2026-02-14 03:20:47.066792	\N	\N	2026-02-14 03:20:47.066792	normal	system	{"sms": false, "sse": true, "push": false, "email": true, "whatsapp": false}	{}	t
\.


--
-- TOC entry 3891 (class 0 OID 17782)
-- Dependencies: 239
-- Data for Name: patient_profiles; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.patient_profiles (id, user_id, date_of_birth, gender, emergency_contact, primary_health_concerns, medical_history, notes, created_at, updated_at) FROM stdin;
1db15fab-11be-4148-9000-4bcdd745338d	66574cc8-3141-4468-8b4f-0007ca0cedd0	\N	\N	\N	\N	\N	\N	2026-01-22 15:12:48.683225	2026-01-22 15:12:48.683225
\.


--
-- TOC entry 3892 (class 0 OID 17790)
-- Dependencies: 240
-- Data for Name: platform_analytics; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.platform_analytics (id, analytics_date, total_users, new_users, active_users, total_sessions, new_sessions, total_doctors, verified_doctors, active_doctors, active_engagements, ended_engagements, crm_resources_count, messages_processed, avg_engagement_duration, created_at) FROM stdin;
\.


--
-- TOC entry 3893 (class 0 OID 17807)
-- Dependencies: 241
-- Data for Name: security_authentication_tokens; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.security_authentication_tokens (id, user_id, token_type, token, expires_at, is_revoked, revoked_at, created_at) FROM stdin;
\.


--
-- TOC entry 3894 (class 0 OID 17814)
-- Dependencies: 242
-- Data for Name: system_settings; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.system_settings (id, setting_key, setting_value, description, is_public, created_at, updated_at) FROM stdin;
94f97a81-280f-4bc1-84ca-6c3c336f1f7e	platform_name	"NeuralHealer"	Platform name	t	2026-01-22 15:00:54.366637	2026-02-09 17:51:08.806168
f368cc47-6577-4bef-9df8-7f7502ce1673	platform_version	"1.0.0"	Current platform version	t	2026-01-22 15:00:54.366637	2026-02-09 17:51:08.806168
1a8e65c9-abea-4085-9930-4e801423340e	database_version	"1.0.0"	Database schema version	f	2026-01-22 15:00:54.366637	2026-02-09 17:51:08.806168
d2342b81-42e6-43e2-8879-7e9cab6a25d6	max_engagement_duration_days	365	Maximum engagement duration in days	f	2026-01-22 15:00:54.366637	2026-02-09 17:51:08.806168
77a006c9-239a-45b9-9def-3d07e9c22551	ai_model_version	"gpt-4"	Default AI model for patient chats	f	2026-01-22 15:00:54.366637	2026-02-09 17:51:08.806168
b6ff6da5-c996-4d9c-ad12-1491df4c3c11	require_2fa_for_engagement	true	Require 2FA verification for engagement start/end	f	2026-01-22 15:00:54.366637	2026-02-09 17:51:08.806168
392d3b09-c183-49b9-a307-52b7896926cb	notification_active_user_threshold_days	3	Days of inactivity before re-engagement notification for active users	f	2026-01-27 17:06:35.291358	2026-02-09 17:51:08.806168
29ad22f6-e118-4a0b-9ce7-47332b2f2e05	notification_inactive_warning_days	14	Days of inactivity before warning notification	f	2026-01-27 17:06:35.291358	2026-02-09 17:51:08.806168
4b3ddb46-7e6e-4c61-af58-ad6195841b43	notification_inactive_status_days	4	Days after warning before user marked as inactive (total 18 days)	f	2026-01-27 17:06:35.291358	2026-02-09 17:51:08.806168
c450b560-c3f7-4c75-92b3-601a05e89e1c	doctor_verification_questions	[{"key": "license_number", "type": "text", "label": "Medical License Number", "helpText": "Your official medical practice license number", "required": true, "placeholder": "e.g., EG123456"}, {"key": "graduation_year", "type": "number", "label": "Year of Graduation", "helpText": "Year you completed your medical degree", "required": true, "placeholder": "e.g., 2015"}, {"key": "medical_school", "type": "text", "label": "Medical School/University", "helpText": "Name of the institution where you obtained your degree", "required": true, "placeholder": "e.g., Cairo University Faculty of Medicine"}, {"key": "specialty_certification", "type": "text", "label": "Board Certification Number", "helpText": "Optional: If you have specialty board certification", "required": false, "placeholder": "e.g., PSY-2020-456"}, {"key": "current_practice", "type": "text", "label": "Current Practice/Clinic", "helpText": "Where you currently practice", "required": false, "placeholder": "e.g., Mind Wellness Clinic"}]	List of verification questions for doctors	f	2026-02-09 00:45:47.930493	2026-02-09 17:51:08.806168
\.


--
-- TOC entry 3895 (class 0 OID 17823)
-- Dependencies: 243
-- Data for Name: url_shortcuts; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.url_shortcuts (id, user_id, url, label, short_code, visit_count, expires_at, created_at) FROM stdin;
\.


--
-- TOC entry 3896 (class 0 OID 17831)
-- Dependencies: 244
-- Data for Name: user_management_metrics; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.user_management_metrics (id, user_id, metric_name, metric_value, recorded_at, period, created_at) FROM stdin;
\.


--
-- TOC entry 3897 (class 0 OID 17840)
-- Dependencies: 245
-- Data for Name: users; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.users (id, email, password_hash, first_name, last_name, phone, timezone, email_verified_at, phone_verified_at, is_active, last_login_at, mfa_enabled, created_at, updated_at, deleted_at, language, activity_status, last_activity_check, email_verification_required, failed_verification_attempts, verification_locked_until, email_verification_sent_at) FROM stdin;
425357c8-7f55-4851-9129-4b97aaad84bd	doctor@example.com	$2a$10$Dp9r0q.VlUak7y.idHmLV.2tUceD7CFGH1zA/0gOvuZsiWU4awg5u	Dr. Sarah	Johnson	\N	UTC	\N	\N	t	\N	f	2026-02-09 17:43:48.274005	2026-02-09 17:43:48.274005	\N	en	active	2026-02-09 17:43:48.250424	t	0	\N	\N
4e1bd976-626a-45a2-84ce-e306b9c23108	doctor@test.com	$2a$10$nJu9zspotzE.NKFcryH.KuOzrHyKNrLC7IN06Pwe5.wOGbP3MMHAy	ahmed	adel	\N	UTC	\N	\N	t	2026-02-09 19:37:49.929387	f	2026-01-22 15:12:17.683385	2026-02-09 19:37:49.826946	\N	en	active	2026-01-27 17:07:46.08543	t	0	\N	\N
66574cc8-3141-4468-8b4f-0007ca0cedd0	patient@test.com	$2a$10$X6uyO7JlEnEoeFhV0xnLw.ArCaVCVenNtseDAKK.x4WpAyGhM76XS	abdallah	ahmed	\N	UTC	\N	\N	t	2026-02-14 01:12:44.949507	f	2026-01-22 15:12:48.680639	2026-02-13 23:14:20.985666	\N	en	active	2026-01-27 17:07:46.08543	t	0	\N	\N
8ad37331-0cd4-482d-bb84-3403a460e87c	anmedadel16@gmail.com	$2a$10$lsV1D0sBx.keg2hx/uhgn.oV5G1LT1dQoRprssO4TFDnWAw.IPcU.	Dr. Sarah	Johnson	\N	UTC	\N	\N	t	\N	f	2026-02-14 05:19:10.838946	2026-02-14 05:19:10.838946	\N	en	active	2026-02-14 05:19:10.838439	t	0	\N	\N
\.


--
-- TOC entry 3920 (class 0 OID 0)
-- Dependencies: 230
-- Name: engagement_id_seq; Type: SEQUENCE SET; Schema: public; Owner: postgres
--

SELECT pg_catalog.setval('public.engagement_id_seq', 12, true);


--
-- TOC entry 3544 (class 2606 OID 17856)
-- Name: active_service_subscriptions active_service_subscriptions_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.active_service_subscriptions
    ADD CONSTRAINT active_service_subscriptions_pkey PRIMARY KEY (id);


--
-- TOC entry 3546 (class 2606 OID 17858)
-- Name: ai_chat_messages ai_chat_messages_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.ai_chat_messages
    ADD CONSTRAINT ai_chat_messages_pkey PRIMARY KEY (id);


--
-- TOC entry 3550 (class 2606 OID 17860)
-- Name: ai_chat_sessions ai_chat_sessions_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.ai_chat_sessions
    ADD CONSTRAINT ai_chat_sessions_pkey PRIMARY KEY (id);


--
-- TOC entry 3555 (class 2606 OID 17862)
-- Name: audit_log audit_log_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.audit_log
    ADD CONSTRAINT audit_log_pkey PRIMARY KEY (id);


--
-- TOC entry 3559 (class 2606 OID 17864)
-- Name: doctor_patients doctor_patients_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.doctor_patients
    ADD CONSTRAINT doctor_patients_pkey PRIMARY KEY (id);


--
-- TOC entry 3567 (class 2606 OID 17866)
-- Name: doctor_profiles doctor_profiles_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.doctor_profiles
    ADD CONSTRAINT doctor_profiles_pkey PRIMARY KEY (id);


--
-- TOC entry 3577 (class 2606 OID 17868)
-- Name: doctor_reviews doctor_reviews_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.doctor_reviews
    ADD CONSTRAINT doctor_reviews_pkey PRIMARY KEY (id);


--
-- TOC entry 3579 (class 2606 OID 17870)
-- Name: doctor_verification_questions doctor_verification_questions_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.doctor_verification_questions
    ADD CONSTRAINT doctor_verification_questions_pkey PRIMARY KEY (id);


--
-- TOC entry 3667 (class 2606 OID 18202)
-- Name: email_verification_otps email_verification_otps_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.email_verification_otps
    ADD CONSTRAINT email_verification_otps_pkey PRIMARY KEY (id);


--
-- TOC entry 3585 (class 2606 OID 17872)
-- Name: engagement_access_rules engagement_access_rules_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.engagement_access_rules
    ADD CONSTRAINT engagement_access_rules_pkey PRIMARY KEY (id);


--
-- TOC entry 3587 (class 2606 OID 17874)
-- Name: engagement_access_rules engagement_access_rules_rule_name_key; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.engagement_access_rules
    ADD CONSTRAINT engagement_access_rules_rule_name_key UNIQUE (rule_name);


--
-- TOC entry 3589 (class 2606 OID 17876)
-- Name: engagement_analytics engagement_analytics_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.engagement_analytics
    ADD CONSTRAINT engagement_analytics_pkey PRIMARY KEY (id);


--
-- TOC entry 3591 (class 2606 OID 17878)
-- Name: engagement_events engagement_events_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.engagement_events
    ADD CONSTRAINT engagement_events_pkey PRIMARY KEY (id);


--
-- TOC entry 3593 (class 2606 OID 17880)
-- Name: engagement_messages engagement_messages_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.engagement_messages
    ADD CONSTRAINT engagement_messages_pkey PRIMARY KEY (id);


--
-- TOC entry 3598 (class 2606 OID 17882)
-- Name: engagement_sessions engagement_sessions_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.engagement_sessions
    ADD CONSTRAINT engagement_sessions_pkey PRIMARY KEY (id);


--
-- TOC entry 3600 (class 2606 OID 17884)
-- Name: engagement_sessions engagement_sessions_session_token_key; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.engagement_sessions
    ADD CONSTRAINT engagement_sessions_session_token_key UNIQUE (session_token);


--
-- TOC entry 3602 (class 2606 OID 17886)
-- Name: engagement_verification_tokens engagement_verification_tokens_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.engagement_verification_tokens
    ADD CONSTRAINT engagement_verification_tokens_pkey PRIMARY KEY (id);


--
-- TOC entry 3604 (class 2606 OID 17888)
-- Name: engagement_verification_tokens engagement_verification_tokens_token_key; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.engagement_verification_tokens
    ADD CONSTRAINT engagement_verification_tokens_token_key UNIQUE (token);


--
-- TOC entry 3606 (class 2606 OID 17890)
-- Name: engagements engagements_engagement_id_key; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.engagements
    ADD CONSTRAINT engagements_engagement_id_key UNIQUE (engagement_id);


--
-- TOC entry 3608 (class 2606 OID 17892)
-- Name: engagements engagements_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.engagements
    ADD CONSTRAINT engagements_pkey PRIMARY KEY (id);


--
-- TOC entry 3617 (class 2606 OID 17894)
-- Name: message_queues message_queues_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.message_queues
    ADD CONSTRAINT message_queues_pkey PRIMARY KEY (id);


--
-- TOC entry 3620 (class 2606 OID 17896)
-- Name: notification_message_templates notification_message_templates_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.notification_message_templates
    ADD CONSTRAINT notification_message_templates_pkey PRIMARY KEY (id);


--
-- TOC entry 3624 (class 2606 OID 17898)
-- Name: notification_templates notification_templates_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.notification_templates
    ADD CONSTRAINT notification_templates_pkey PRIMARY KEY (id);


--
-- TOC entry 3626 (class 2606 OID 17900)
-- Name: notification_templates notification_templates_type_key; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.notification_templates
    ADD CONSTRAINT notification_templates_type_key UNIQUE (type);


--
-- TOC entry 3633 (class 2606 OID 17902)
-- Name: notifications notifications_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.notifications
    ADD CONSTRAINT notifications_pkey PRIMARY KEY (id);


--
-- TOC entry 3636 (class 2606 OID 17904)
-- Name: patient_profiles patient_profiles_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.patient_profiles
    ADD CONSTRAINT patient_profiles_pkey PRIMARY KEY (id);


--
-- TOC entry 3638 (class 2606 OID 17906)
-- Name: platform_analytics platform_analytics_analytics_date_key; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.platform_analytics
    ADD CONSTRAINT platform_analytics_analytics_date_key UNIQUE (analytics_date);


--
-- TOC entry 3640 (class 2606 OID 17908)
-- Name: platform_analytics platform_analytics_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.platform_analytics
    ADD CONSTRAINT platform_analytics_pkey PRIMARY KEY (id);


--
-- TOC entry 3644 (class 2606 OID 17910)
-- Name: security_authentication_tokens security_authentication_tokens_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.security_authentication_tokens
    ADD CONSTRAINT security_authentication_tokens_pkey PRIMARY KEY (id);


--
-- TOC entry 3646 (class 2606 OID 17912)
-- Name: security_authentication_tokens security_authentication_tokens_token_key; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.security_authentication_tokens
    ADD CONSTRAINT security_authentication_tokens_token_key UNIQUE (token);


--
-- TOC entry 3648 (class 2606 OID 17914)
-- Name: system_settings system_settings_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.system_settings
    ADD CONSTRAINT system_settings_pkey PRIMARY KEY (id);


--
-- TOC entry 3650 (class 2606 OID 17916)
-- Name: system_settings system_settings_setting_key_key; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.system_settings
    ADD CONSTRAINT system_settings_setting_key_key UNIQUE (setting_key);


--
-- TOC entry 3565 (class 2606 OID 17918)
-- Name: doctor_patients unique_doctor_patient; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.doctor_patients
    ADD CONSTRAINT unique_doctor_patient UNIQUE (doctor_id, patient_id);


--
-- TOC entry 3583 (class 2606 OID 17920)
-- Name: doctor_verification_questions unique_doctor_question; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.doctor_verification_questions
    ADD CONSTRAINT unique_doctor_question UNIQUE (doctor_id, question_key);


--
-- TOC entry 3622 (class 2606 OID 17922)
-- Name: notification_message_templates unique_template_lang_context; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.notification_message_templates
    ADD CONSTRAINT unique_template_lang_context UNIQUE (template_key, language_code, recipient_context);


--
-- TOC entry 3652 (class 2606 OID 17924)
-- Name: url_shortcuts url_shortcuts_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.url_shortcuts
    ADD CONSTRAINT url_shortcuts_pkey PRIMARY KEY (id);


--
-- TOC entry 3654 (class 2606 OID 17926)
-- Name: url_shortcuts url_shortcuts_short_code_key; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.url_shortcuts
    ADD CONSTRAINT url_shortcuts_short_code_key UNIQUE (short_code);


--
-- TOC entry 3656 (class 2606 OID 17928)
-- Name: user_management_metrics user_management_metrics_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.user_management_metrics
    ADD CONSTRAINT user_management_metrics_pkey PRIMARY KEY (id);


--
-- TOC entry 3663 (class 2606 OID 17930)
-- Name: users users_email_key; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.users
    ADD CONSTRAINT users_email_key UNIQUE (email);


--
-- TOC entry 3665 (class 2606 OID 17932)
-- Name: users users_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.users
    ADD CONSTRAINT users_pkey PRIMARY KEY (id);


--
-- TOC entry 3547 (class 1259 OID 17933)
-- Name: idx_ai_chat_messages_created_at; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_ai_chat_messages_created_at ON public.ai_chat_messages USING btree (created_at);


--
-- TOC entry 3548 (class 1259 OID 17934)
-- Name: idx_ai_chat_messages_session_id; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_ai_chat_messages_session_id ON public.ai_chat_messages USING btree (session_id);


--
-- TOC entry 3551 (class 1259 OID 17935)
-- Name: idx_ai_chat_sessions_is_active; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_ai_chat_sessions_is_active ON public.ai_chat_sessions USING btree (is_active);


--
-- TOC entry 3552 (class 1259 OID 17936)
-- Name: idx_ai_chat_sessions_patient_id; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_ai_chat_sessions_patient_id ON public.ai_chat_sessions USING btree (patient_id);


--
-- TOC entry 3553 (class 1259 OID 17937)
-- Name: idx_ai_chat_sessions_patient_started; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_ai_chat_sessions_patient_started ON public.ai_chat_sessions USING btree (patient_id, started_at);


--
-- TOC entry 3556 (class 1259 OID 17938)
-- Name: idx_audit_log_created_at; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_audit_log_created_at ON public.audit_log USING btree (created_at);


--
-- TOC entry 3557 (class 1259 OID 17939)
-- Name: idx_audit_log_user_id; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_audit_log_user_id ON public.audit_log USING btree (user_id);


--
-- TOC entry 3641 (class 1259 OID 17940)
-- Name: idx_auth_tokens_expires_at; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_auth_tokens_expires_at ON public.security_authentication_tokens USING btree (expires_at);


--
-- TOC entry 3642 (class 1259 OID 17941)
-- Name: idx_auth_tokens_user_id; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_auth_tokens_user_id ON public.security_authentication_tokens USING btree (user_id);


--
-- TOC entry 3568 (class 1259 OID 17942)
-- Name: idx_doctor_availability; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_doctor_availability ON public.doctor_profiles USING btree (availability_status);


--
-- TOC entry 3569 (class 1259 OID 17943)
-- Name: idx_doctor_coordinates; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_doctor_coordinates ON public.doctor_profiles USING btree (latitude, longitude) WHERE ((latitude IS NOT NULL) AND (longitude IS NOT NULL));


--
-- TOC entry 3921 (class 0 OID 0)
-- Dependencies: 3569
-- Name: INDEX idx_doctor_coordinates; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON INDEX public.idx_doctor_coordinates IS 'Optimizes geolocation-based doctor searches';


--
-- TOC entry 3570 (class 1259 OID 17944)
-- Name: idx_doctor_location; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_doctor_location ON public.doctor_profiles USING btree (location_city, location_country);


--
-- TOC entry 3560 (class 1259 OID 17945)
-- Name: idx_doctor_patients_current_engagement; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_doctor_patients_current_engagement ON public.doctor_patients USING btree (current_engagement_id);


--
-- TOC entry 3561 (class 1259 OID 17946)
-- Name: idx_doctor_patients_doctor_id; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_doctor_patients_doctor_id ON public.doctor_patients USING btree (doctor_id);


--
-- TOC entry 3562 (class 1259 OID 17947)
-- Name: idx_doctor_patients_is_active; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_doctor_patients_is_active ON public.doctor_patients USING btree (is_active);


--
-- TOC entry 3563 (class 1259 OID 17948)
-- Name: idx_doctor_patients_patient_id; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_doctor_patients_patient_id ON public.doctor_patients USING btree (patient_id);


--
-- TOC entry 3571 (class 1259 OID 17949)
-- Name: idx_doctor_profiles_user_id; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_doctor_profiles_user_id ON public.doctor_profiles USING btree (user_id);


--
-- TOC entry 3572 (class 1259 OID 17950)
-- Name: idx_doctor_profiles_verified; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_doctor_profiles_verified ON public.doctor_profiles USING btree (is_verified);


--
-- TOC entry 3573 (class 1259 OID 17951)
-- Name: idx_doctor_rating; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_doctor_rating ON public.doctor_profiles USING btree (rating DESC);


--
-- TOC entry 3574 (class 1259 OID 17952)
-- Name: idx_doctor_specialization; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_doctor_specialization ON public.doctor_profiles USING btree (specialization);


--
-- TOC entry 3575 (class 1259 OID 17953)
-- Name: idx_doctor_verification_status; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_doctor_verification_status ON public.doctor_profiles USING btree (verification_status);


--
-- TOC entry 3594 (class 1259 OID 17954)
-- Name: idx_engagement_messages_created_at; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_engagement_messages_created_at ON public.engagement_messages USING btree (created_at);


--
-- TOC entry 3595 (class 1259 OID 17955)
-- Name: idx_engagement_messages_engagement_id; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_engagement_messages_engagement_id ON public.engagement_messages USING btree (engagement_id);


--
-- TOC entry 3596 (class 1259 OID 17956)
-- Name: idx_engagement_messages_sender; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_engagement_messages_sender ON public.engagement_messages USING btree (sender_id);


--
-- TOC entry 3609 (class 1259 OID 17957)
-- Name: idx_engagements_dates; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_engagements_dates ON public.engagements USING btree (start_at, end_at);


--
-- TOC entry 3610 (class 1259 OID 17958)
-- Name: idx_engagements_doctor_id; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_engagements_doctor_id ON public.engagements USING btree (doctor_id);


--
-- TOC entry 3611 (class 1259 OID 17959)
-- Name: idx_engagements_doctor_patient_status; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_engagements_doctor_patient_status ON public.engagements USING btree (doctor_id, patient_id, status) WHERE (status = ANY (ARRAY['active'::public.engagement_status, 'pending'::public.engagement_status]));


--
-- TOC entry 3612 (class 1259 OID 18176)
-- Name: idx_engagements_initiated_by; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_engagements_initiated_by ON public.engagements USING btree (initiated_by);


--
-- TOC entry 3613 (class 1259 OID 17960)
-- Name: idx_engagements_patient_id; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_engagements_patient_id ON public.engagements USING btree (patient_id);


--
-- TOC entry 3614 (class 1259 OID 17961)
-- Name: idx_engagements_status; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_engagements_status ON public.engagements USING btree (status);


--
-- TOC entry 3618 (class 1259 OID 17962)
-- Name: idx_message_templates_key_lang; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_message_templates_key_lang ON public.notification_message_templates USING btree (template_key, language_code);


--
-- TOC entry 3627 (class 1259 OID 17963)
-- Name: idx_notifications_is_read; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_notifications_is_read ON public.notifications USING btree (is_read);


--
-- TOC entry 3628 (class 1259 OID 17964)
-- Name: idx_notifications_priority; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_notifications_priority ON public.notifications USING btree (priority, sent_at DESC);


--
-- TOC entry 3629 (class 1259 OID 17965)
-- Name: idx_notifications_unpushed; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_notifications_unpushed ON public.notifications USING btree (user_id, sent_at) WHERE (((delivery_status ->> 'sse'::text))::boolean = false);


--
-- TOC entry 3630 (class 1259 OID 17966)
-- Name: idx_notifications_user_id; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_notifications_user_id ON public.notifications USING btree (user_id);


--
-- TOC entry 3631 (class 1259 OID 17967)
-- Name: idx_notifications_user_sentat; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_notifications_user_sentat ON public.notifications USING btree (user_id, sent_at);


--
-- TOC entry 3668 (class 1259 OID 18215)
-- Name: idx_otp_code; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_otp_code ON public.email_verification_otps USING btree (otp_code);


--
-- TOC entry 3669 (class 1259 OID 18214)
-- Name: idx_otp_expires_at; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_otp_expires_at ON public.email_verification_otps USING btree (expires_at);


--
-- TOC entry 3670 (class 1259 OID 18213)
-- Name: idx_otp_user_id; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_otp_user_id ON public.email_verification_otps USING btree (user_id);


--
-- TOC entry 3634 (class 1259 OID 17968)
-- Name: idx_patient_profiles_user_id; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_patient_profiles_user_id ON public.patient_profiles USING btree (user_id);


--
-- TOC entry 3615 (class 1259 OID 17969)
-- Name: idx_queue_pending; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_queue_pending ON public.message_queues USING btree (job_type, status, priority DESC, created_at) WHERE (status = 'pending'::public.job_status);


--
-- TOC entry 3657 (class 1259 OID 17970)
-- Name: idx_users_activity_status; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_users_activity_status ON public.users USING btree (activity_status, last_login_at);


--
-- TOC entry 3658 (class 1259 OID 17971)
-- Name: idx_users_deleted_at; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_users_deleted_at ON public.users USING btree (deleted_at);


--
-- TOC entry 3659 (class 1259 OID 17972)
-- Name: idx_users_email; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_users_email ON public.users USING btree (email);


--
-- TOC entry 3660 (class 1259 OID 17973)
-- Name: idx_users_is_active; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_users_is_active ON public.users USING btree (is_active);


--
-- TOC entry 3661 (class 1259 OID 17974)
-- Name: idx_users_language; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_users_language ON public.users USING btree (language);


--
-- TOC entry 3580 (class 1259 OID 17975)
-- Name: idx_verification_questions_doctor; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_verification_questions_doctor ON public.doctor_verification_questions USING btree (doctor_id);


--
-- TOC entry 3581 (class 1259 OID 17976)
-- Name: idx_verification_questions_verified; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_verification_questions_verified ON public.doctor_verification_questions USING btree (verified_at);


--
-- TOC entry 3717 (class 2620 OID 17977)
-- Name: engagements engagement_notification_trigger; Type: TRIGGER; Schema: public; Owner: postgres
--

CREATE TRIGGER engagement_notification_trigger AFTER UPDATE OF status ON public.engagements FOR EACH ROW EXECUTE FUNCTION public.create_engagement_notification();


--
-- TOC entry 3718 (class 2620 OID 17978)
-- Name: engagements engagement_status_change; Type: TRIGGER; Schema: public; Owner: postgres
--

CREATE TRIGGER engagement_status_change AFTER INSERT OR UPDATE OF status ON public.engagements FOR EACH ROW EXECUTE FUNCTION public.update_relationship_status_on_engagement();


--
-- TOC entry 3711 (class 2620 OID 17979)
-- Name: doctor_patients relationship_access_change; Type: TRIGGER; Schema: public; Owner: postgres
--

CREATE TRIGGER relationship_access_change AFTER UPDATE OF relationship_status ON public.doctor_patients FOR EACH ROW WHEN (((old.relationship_status)::text IS DISTINCT FROM (new.relationship_status)::text)) EXECUTE FUNCTION public.notify_access_rule_change();


--
-- TOC entry 3719 (class 2620 OID 17980)
-- Name: engagements set_engagement_id; Type: TRIGGER; Schema: public; Owner: postgres
--

CREATE TRIGGER set_engagement_id BEFORE INSERT ON public.engagements FOR EACH ROW EXECUTE FUNCTION public.generate_engagement_id();


--
-- TOC entry 3721 (class 2620 OID 18238)
-- Name: notifications trg_auto_queue_email; Type: TRIGGER; Schema: public; Owner: postgres
--

CREATE TRIGGER trg_auto_queue_email AFTER INSERT ON public.notifications FOR EACH ROW EXECUTE FUNCTION public.trigger_queue_email_job();


--
-- TOC entry 3715 (class 2620 OID 17982)
-- Name: doctor_reviews trg_update_doctor_rating; Type: TRIGGER; Schema: public; Owner: postgres
--

CREATE TRIGGER trg_update_doctor_rating AFTER INSERT OR DELETE OR UPDATE ON public.doctor_reviews FOR EACH ROW EXECUTE FUNCTION public.update_doctor_rating();


--
-- TOC entry 3713 (class 2620 OID 17983)
-- Name: doctor_profiles trg_update_profile_completion; Type: TRIGGER; Schema: public; Owner: postgres
--

CREATE TRIGGER trg_update_profile_completion BEFORE INSERT OR UPDATE ON public.doctor_profiles FOR EACH ROW EXECUTE FUNCTION public.update_profile_completion();


--
-- TOC entry 3709 (class 2620 OID 17984)
-- Name: active_service_subscriptions update_active_service_subscriptions_updated_at; Type: TRIGGER; Schema: public; Owner: postgres
--

CREATE TRIGGER update_active_service_subscriptions_updated_at BEFORE UPDATE ON public.active_service_subscriptions FOR EACH ROW EXECUTE FUNCTION public.update_updated_at_column();


--
-- TOC entry 3710 (class 2620 OID 17985)
-- Name: ai_chat_sessions update_ai_chat_sessions_updated_at; Type: TRIGGER; Schema: public; Owner: postgres
--

CREATE TRIGGER update_ai_chat_sessions_updated_at BEFORE UPDATE ON public.ai_chat_sessions FOR EACH ROW EXECUTE FUNCTION public.update_updated_at_column();


--
-- TOC entry 3712 (class 2620 OID 17986)
-- Name: doctor_patients update_doctor_patients_updated_at; Type: TRIGGER; Schema: public; Owner: postgres
--

CREATE TRIGGER update_doctor_patients_updated_at BEFORE UPDATE ON public.doctor_patients FOR EACH ROW EXECUTE FUNCTION public.update_updated_at_column();


--
-- TOC entry 3714 (class 2620 OID 17987)
-- Name: doctor_profiles update_doctor_profiles_updated_at; Type: TRIGGER; Schema: public; Owner: postgres
--

CREATE TRIGGER update_doctor_profiles_updated_at BEFORE UPDATE ON public.doctor_profiles FOR EACH ROW EXECUTE FUNCTION public.update_updated_at_column();


--
-- TOC entry 3716 (class 2620 OID 17988)
-- Name: engagement_access_rules update_engagement_access_rules_updated_at; Type: TRIGGER; Schema: public; Owner: postgres
--

CREATE TRIGGER update_engagement_access_rules_updated_at BEFORE UPDATE ON public.engagement_access_rules FOR EACH ROW EXECUTE FUNCTION public.update_updated_at_column();


--
-- TOC entry 3720 (class 2620 OID 17989)
-- Name: engagements update_engagements_updated_at; Type: TRIGGER; Schema: public; Owner: postgres
--

CREATE TRIGGER update_engagements_updated_at BEFORE UPDATE ON public.engagements FOR EACH ROW EXECUTE FUNCTION public.update_updated_at_column();


--
-- TOC entry 3722 (class 2620 OID 17990)
-- Name: patient_profiles update_patient_profiles_updated_at; Type: TRIGGER; Schema: public; Owner: postgres
--

CREATE TRIGGER update_patient_profiles_updated_at BEFORE UPDATE ON public.patient_profiles FOR EACH ROW EXECUTE FUNCTION public.update_updated_at_column();


--
-- TOC entry 3723 (class 2620 OID 17991)
-- Name: system_settings update_system_settings_updated_at; Type: TRIGGER; Schema: public; Owner: postgres
--

CREATE TRIGGER update_system_settings_updated_at BEFORE UPDATE ON public.system_settings FOR EACH ROW EXECUTE FUNCTION public.update_updated_at_column();


--
-- TOC entry 3724 (class 2620 OID 17992)
-- Name: users update_users_updated_at; Type: TRIGGER; Schema: public; Owner: postgres
--

CREATE TRIGGER update_users_updated_at BEFORE UPDATE ON public.users FOR EACH ROW EXECUTE FUNCTION public.update_updated_at_column();


--
-- TOC entry 3725 (class 2620 OID 17993)
-- Name: users user_welcome_notification; Type: TRIGGER; Schema: public; Owner: postgres
--

CREATE TRIGGER user_welcome_notification AFTER UPDATE OF email_verified_at ON public.users FOR EACH ROW WHEN (((old.email_verified_at IS NULL) AND (new.email_verified_at IS NOT NULL))) EXECUTE FUNCTION public.user_welcome_notification();


--
-- TOC entry 3671 (class 2606 OID 17994)
-- Name: active_service_subscriptions active_service_subscriptions_user_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.active_service_subscriptions
    ADD CONSTRAINT active_service_subscriptions_user_id_fkey FOREIGN KEY (user_id) REFERENCES public.users(id) ON DELETE CASCADE;


--
-- TOC entry 3672 (class 2606 OID 17999)
-- Name: ai_chat_messages ai_chat_messages_sender_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.ai_chat_messages
    ADD CONSTRAINT ai_chat_messages_sender_id_fkey FOREIGN KEY (sender_id) REFERENCES public.users(id);


--
-- TOC entry 3673 (class 2606 OID 18004)
-- Name: ai_chat_messages ai_chat_messages_session_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.ai_chat_messages
    ADD CONSTRAINT ai_chat_messages_session_id_fkey FOREIGN KEY (session_id) REFERENCES public.ai_chat_sessions(id) ON DELETE CASCADE;


--
-- TOC entry 3674 (class 2606 OID 18009)
-- Name: ai_chat_sessions ai_chat_sessions_patient_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.ai_chat_sessions
    ADD CONSTRAINT ai_chat_sessions_patient_id_fkey FOREIGN KEY (patient_id) REFERENCES public.patient_profiles(id) ON DELETE CASCADE;


--
-- TOC entry 3675 (class 2606 OID 18014)
-- Name: audit_log audit_log_user_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.audit_log
    ADD CONSTRAINT audit_log_user_id_fkey FOREIGN KEY (user_id) REFERENCES public.users(id);


--
-- TOC entry 3676 (class 2606 OID 18019)
-- Name: doctor_patients doctor_patients_current_engagement_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.doctor_patients
    ADD CONSTRAINT doctor_patients_current_engagement_id_fkey FOREIGN KEY (current_engagement_id) REFERENCES public.engagements(id) ON DELETE SET NULL;


--
-- TOC entry 3677 (class 2606 OID 18024)
-- Name: doctor_patients doctor_patients_doctor_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.doctor_patients
    ADD CONSTRAINT doctor_patients_doctor_id_fkey FOREIGN KEY (doctor_id) REFERENCES public.doctor_profiles(id) ON DELETE CASCADE;


--
-- TOC entry 3678 (class 2606 OID 18029)
-- Name: doctor_patients doctor_patients_patient_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.doctor_patients
    ADD CONSTRAINT doctor_patients_patient_id_fkey FOREIGN KEY (patient_id) REFERENCES public.patient_profiles(id) ON DELETE CASCADE;


--
-- TOC entry 3679 (class 2606 OID 18034)
-- Name: doctor_patients doctor_patients_relationship_status_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.doctor_patients
    ADD CONSTRAINT doctor_patients_relationship_status_fkey FOREIGN KEY (relationship_status) REFERENCES public.engagement_access_rules(rule_name);


--
-- TOC entry 3680 (class 2606 OID 18039)
-- Name: doctor_profiles doctor_profiles_user_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.doctor_profiles
    ADD CONSTRAINT doctor_profiles_user_id_fkey FOREIGN KEY (user_id) REFERENCES public.users(id) ON DELETE CASCADE;


--
-- TOC entry 3682 (class 2606 OID 18044)
-- Name: doctor_reviews doctor_reviews_doctor_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.doctor_reviews
    ADD CONSTRAINT doctor_reviews_doctor_id_fkey FOREIGN KEY (doctor_id) REFERENCES public.doctor_profiles(id);


--
-- TOC entry 3683 (class 2606 OID 18049)
-- Name: doctor_reviews doctor_reviews_patient_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.doctor_reviews
    ADD CONSTRAINT doctor_reviews_patient_id_fkey FOREIGN KEY (patient_id) REFERENCES public.patient_profiles(id);


--
-- TOC entry 3684 (class 2606 OID 18054)
-- Name: doctor_verification_questions doctor_verification_questions_doctor_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.doctor_verification_questions
    ADD CONSTRAINT doctor_verification_questions_doctor_id_fkey FOREIGN KEY (doctor_id) REFERENCES public.doctor_profiles(id) ON DELETE CASCADE;


--
-- TOC entry 3707 (class 2606 OID 18203)
-- Name: email_verification_otps email_verification_otps_user_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.email_verification_otps
    ADD CONSTRAINT email_verification_otps_user_id_fkey FOREIGN KEY (user_id) REFERENCES public.users(id) ON DELETE CASCADE;


--
-- TOC entry 3685 (class 2606 OID 18059)
-- Name: engagement_analytics engagement_analytics_engagement_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.engagement_analytics
    ADD CONSTRAINT engagement_analytics_engagement_id_fkey FOREIGN KEY (engagement_id) REFERENCES public.engagements(id) ON DELETE CASCADE;


--
-- TOC entry 3686 (class 2606 OID 18064)
-- Name: engagement_events engagement_events_engagement_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.engagement_events
    ADD CONSTRAINT engagement_events_engagement_id_fkey FOREIGN KEY (engagement_id) REFERENCES public.engagements(id) ON DELETE CASCADE;


--
-- TOC entry 3687 (class 2606 OID 18069)
-- Name: engagement_events engagement_events_triggered_by_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.engagement_events
    ADD CONSTRAINT engagement_events_triggered_by_fkey FOREIGN KEY (triggered_by) REFERENCES public.users(id);


--
-- TOC entry 3688 (class 2606 OID 18074)
-- Name: engagement_messages engagement_messages_engagement_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.engagement_messages
    ADD CONSTRAINT engagement_messages_engagement_id_fkey FOREIGN KEY (engagement_id) REFERENCES public.engagements(id) ON DELETE CASCADE;


--
-- TOC entry 3689 (class 2606 OID 18079)
-- Name: engagement_messages engagement_messages_recipient_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.engagement_messages
    ADD CONSTRAINT engagement_messages_recipient_id_fkey FOREIGN KEY (recipient_id) REFERENCES public.users(id);


--
-- TOC entry 3690 (class 2606 OID 18084)
-- Name: engagement_messages engagement_messages_sender_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.engagement_messages
    ADD CONSTRAINT engagement_messages_sender_id_fkey FOREIGN KEY (sender_id) REFERENCES public.users(id);


--
-- TOC entry 3691 (class 2606 OID 18089)
-- Name: engagement_sessions engagement_sessions_engagement_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.engagement_sessions
    ADD CONSTRAINT engagement_sessions_engagement_id_fkey FOREIGN KEY (engagement_id) REFERENCES public.engagements(id) ON DELETE CASCADE;


--
-- TOC entry 3692 (class 2606 OID 18094)
-- Name: engagement_sessions engagement_sessions_user_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.engagement_sessions
    ADD CONSTRAINT engagement_sessions_user_id_fkey FOREIGN KEY (user_id) REFERENCES public.users(id);


--
-- TOC entry 3693 (class 2606 OID 18099)
-- Name: engagement_verification_tokens engagement_verification_tokens_doctor_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.engagement_verification_tokens
    ADD CONSTRAINT engagement_verification_tokens_doctor_id_fkey FOREIGN KEY (doctor_id) REFERENCES public.users(id);


--
-- TOC entry 3694 (class 2606 OID 18104)
-- Name: engagement_verification_tokens engagement_verification_tokens_engagement_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.engagement_verification_tokens
    ADD CONSTRAINT engagement_verification_tokens_engagement_id_fkey FOREIGN KEY (engagement_id) REFERENCES public.engagements(id) ON DELETE CASCADE;


--
-- TOC entry 3695 (class 2606 OID 18109)
-- Name: engagement_verification_tokens engagement_verification_tokens_patient_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.engagement_verification_tokens
    ADD CONSTRAINT engagement_verification_tokens_patient_id_fkey FOREIGN KEY (patient_id) REFERENCES public.users(id);


--
-- TOC entry 3696 (class 2606 OID 18114)
-- Name: engagement_verification_tokens engagement_verification_tokens_verified_by_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.engagement_verification_tokens
    ADD CONSTRAINT engagement_verification_tokens_verified_by_fkey FOREIGN KEY (verified_by) REFERENCES public.users(id);


--
-- TOC entry 3697 (class 2606 OID 18119)
-- Name: engagements engagements_access_rule_name_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.engagements
    ADD CONSTRAINT engagements_access_rule_name_fkey FOREIGN KEY (access_rule_name) REFERENCES public.engagement_access_rules(rule_name);


--
-- TOC entry 3698 (class 2606 OID 18124)
-- Name: engagements engagements_doctor_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.engagements
    ADD CONSTRAINT engagements_doctor_id_fkey FOREIGN KEY (doctor_id) REFERENCES public.doctor_profiles(id) ON DELETE CASCADE;


--
-- TOC entry 3699 (class 2606 OID 18129)
-- Name: engagements engagements_ended_by_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.engagements
    ADD CONSTRAINT engagements_ended_by_fkey FOREIGN KEY (ended_by) REFERENCES public.users(id);


--
-- TOC entry 3700 (class 2606 OID 18134)
-- Name: engagements engagements_patient_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.engagements
    ADD CONSTRAINT engagements_patient_id_fkey FOREIGN KEY (patient_id) REFERENCES public.patient_profiles(id) ON DELETE CASCADE;


--
-- TOC entry 3681 (class 2606 OID 18139)
-- Name: doctor_profiles fk_doctor_user; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.doctor_profiles
    ADD CONSTRAINT fk_doctor_user FOREIGN KEY (user_id) REFERENCES public.users(id) ON DELETE CASCADE;


--
-- TOC entry 3708 (class 2606 OID 18208)
-- Name: email_verification_otps fk_otp_user; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.email_verification_otps
    ADD CONSTRAINT fk_otp_user FOREIGN KEY (user_id) REFERENCES public.users(id) ON DELETE CASCADE;


--
-- TOC entry 3702 (class 2606 OID 18144)
-- Name: patient_profiles fk_patient_user; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.patient_profiles
    ADD CONSTRAINT fk_patient_user FOREIGN KEY (user_id) REFERENCES public.users(id) ON DELETE CASCADE;


--
-- TOC entry 3701 (class 2606 OID 18149)
-- Name: notifications notifications_user_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.notifications
    ADD CONSTRAINT notifications_user_id_fkey FOREIGN KEY (user_id) REFERENCES public.users(id) ON DELETE CASCADE;


--
-- TOC entry 3703 (class 2606 OID 18154)
-- Name: patient_profiles patient_profiles_user_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.patient_profiles
    ADD CONSTRAINT patient_profiles_user_id_fkey FOREIGN KEY (user_id) REFERENCES public.users(id) ON DELETE CASCADE;


--
-- TOC entry 3704 (class 2606 OID 18159)
-- Name: security_authentication_tokens security_authentication_tokens_user_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.security_authentication_tokens
    ADD CONSTRAINT security_authentication_tokens_user_id_fkey FOREIGN KEY (user_id) REFERENCES public.users(id) ON DELETE CASCADE;


--
-- TOC entry 3705 (class 2606 OID 18164)
-- Name: url_shortcuts url_shortcuts_user_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.url_shortcuts
    ADD CONSTRAINT url_shortcuts_user_id_fkey FOREIGN KEY (user_id) REFERENCES public.users(id) ON DELETE CASCADE;


--
-- TOC entry 3706 (class 2606 OID 18169)
-- Name: user_management_metrics user_management_metrics_user_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.user_management_metrics
    ADD CONSTRAINT user_management_metrics_user_id_fkey FOREIGN KEY (user_id) REFERENCES public.users(id) ON DELETE CASCADE;


-- Completed on 2026-02-14 05:30:39

--
-- PostgreSQL database dump complete
--

\unrestrict MtVGBaywtJ9cEi5dmvWSbWefiuAyrh0zWxeG9uDcRiR8QE60jxS1vPsevm4NJPi

