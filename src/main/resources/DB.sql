--
-- PostgreSQL database dump
--

\restrict icvtM6AeHlKx1vIbknrBqzJyQd4zVffgge51DnfTbjnpPJfe8tAs5cwjQbqaq4h

-- Dumped from database version 18.0
-- Dumped by pg_dump version 18.0

-- Started on 2026-02-15 01:17:43

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
-- TOC entry 2 (class 3079 OID 34584)
-- Name: pgcrypto; Type: EXTENSION; Schema: -; Owner: -
--

CREATE EXTENSION IF NOT EXISTS pgcrypto WITH SCHEMA public;


--
-- TOC entry 5506 (class 0 OID 0)
-- Dependencies: 2
-- Name: EXTENSION pgcrypto; Type: COMMENT; Schema: -; Owner: 
--

COMMENT ON EXTENSION pgcrypto IS 'cryptographic functions';


--
-- TOC entry 3 (class 3079 OID 34622)
-- Name: uuid-ossp; Type: EXTENSION; Schema: -; Owner: -
--

CREATE EXTENSION IF NOT EXISTS "uuid-ossp" WITH SCHEMA public;


--
-- TOC entry 5507 (class 0 OID 0)
-- Dependencies: 3
-- Name: EXTENSION "uuid-ossp"; Type: COMMENT; Schema: -; Owner: 
--

COMMENT ON EXTENSION "uuid-ossp" IS 'generate universally unique identifiers (UUIDs)';


--
-- TOC entry 944 (class 1247 OID 34634)
-- Name: chat_sender_type; Type: TYPE; Schema: public; Owner: postgres
--

CREATE TYPE public.chat_sender_type AS ENUM (
    'patient',
    'ai'
);


ALTER TYPE public.chat_sender_type OWNER TO postgres;

--
-- TOC entry 947 (class 1247 OID 34640)
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
-- TOC entry 950 (class 1247 OID 34652)
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
-- TOC entry 953 (class 1247 OID 34664)
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
-- TOC entry 956 (class 1247 OID 34678)
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
-- TOC entry 959 (class 1247 OID 34688)
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
-- TOC entry 962 (class 1247 OID 34698)
-- Name: verification_type; Type: TYPE; Schema: public; Owner: postgres
--

CREATE TYPE public.verification_type AS ENUM (
    'start',
    'end'
);


ALTER TYPE public.verification_type OWNER TO postgres;

--
-- TOC entry 308 (class 1255 OID 34703)
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
-- TOC entry 309 (class 1255 OID 34704)
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
-- TOC entry 5508 (class 0 OID 0)
-- Dependencies: 309
-- Name: FUNCTION calculate_profile_completion(p_doctor_id uuid); Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON FUNCTION public.calculate_profile_completion(p_doctor_id uuid) IS 'Calculates doctor profile completion percentage (0-100)';


--
-- TOC entry 310 (class 1255 OID 34705)
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
-- TOC entry 311 (class 1255 OID 34706)
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
-- TOC entry 312 (class 1255 OID 34707)
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
-- TOC entry 313 (class 1255 OID 34708)
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
-- TOC entry 314 (class 1255 OID 34709)
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
-- TOC entry 315 (class 1255 OID 34710)
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
-- TOC entry 316 (class 1255 OID 34711)
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
-- TOC entry 317 (class 1255 OID 34712)
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
-- TOC entry 296 (class 1255 OID 34713)
-- Name: trigger_queue_email_job(); Type: FUNCTION; Schema: public; Owner: postgres
--

CREATE FUNCTION public.trigger_queue_email_job() RETURNS trigger
    LANGUAGE plpgsql
    AS $$ BEGIN     IF NEW.send_email = TRUE THEN         INSERT INTO message_queues (             job_type, status, payload, created_at         ) VALUES (             'EMAIL_NOTIFICATION',             'pending',             jsonb_build_object(                 'notificationId', NEW.id,                 'userId', NEW.user_id,                 'templateKey', NEW.type,                 'userName', COALESCE(NEW.payload->>'userName', 'User'),                 'doctorName', COALESCE(NEW.payload->>'doctorName', 'Doctor'),                 'title', NEW.title,                 'body', NEW.message             ),             NOW()         );         PERFORM pg_notify('email_queue', 'new_job');     END IF;     RETURN NEW; END; $$;


ALTER FUNCTION public.trigger_queue_email_job() OWNER TO postgres;

--
-- TOC entry 318 (class 1255 OID 34714)
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
-- TOC entry 5509 (class 0 OID 0)
-- Dependencies: 318
-- Name: FUNCTION update_doctor_rating(); Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON FUNCTION public.update_doctor_rating() IS 'Auto-updates doctor rating and review count when reviews change';


--
-- TOC entry 319 (class 1255 OID 34715)
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
-- TOC entry 320 (class 1255 OID 34716)
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
-- TOC entry 321 (class 1255 OID 34717)
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
-- TOC entry 322 (class 1255 OID 34718)
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
-- TOC entry 5510 (class 0 OID 0)
-- Dependencies: 322
-- Name: FUNCTION user_welcome_notification(); Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON FUNCTION public.user_welcome_notification() IS 'Creates welcome notification for new users using the centralized notification system';


SET default_tablespace = '';

SET default_table_access_method = heap;

--
-- TOC entry 221 (class 1259 OID 34719)
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
-- TOC entry 222 (class 1259 OID 34733)
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
-- TOC entry 5511 (class 0 OID 0)
-- Dependencies: 222
-- Name: TABLE ai_chat_messages; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON TABLE public.ai_chat_messages IS 'NeuralHealer: Messages exchanged in AI chat sessions';


--
-- TOC entry 223 (class 1259 OID 34747)
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
-- TOC entry 5512 (class 0 OID 0)
-- Dependencies: 223
-- Name: TABLE ai_chat_sessions; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON TABLE public.ai_chat_sessions IS 'NeuralHealer: AI chatbot sessions for patients';


--
-- TOC entry 224 (class 1259 OID 34761)
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
-- TOC entry 225 (class 1259 OID 34770)
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
-- TOC entry 5513 (class 0 OID 0)
-- Dependencies: 225
-- Name: TABLE doctor_patients; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON TABLE public.doctor_patients IS 'NeuralHealer: Relationship mapping between doctors and patients with current access rules';


--
-- TOC entry 226 (class 1259 OID 34781)
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
-- TOC entry 5514 (class 0 OID 0)
-- Dependencies: 226
-- Name: TABLE doctor_profiles; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON TABLE public.doctor_profiles IS 'NeuralHealer: Extended profile information for doctors';


--
-- TOC entry 227 (class 1259 OID 34797)
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
-- TOC entry 228 (class 1259 OID 34804)
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
-- TOC entry 5515 (class 0 OID 0)
-- Dependencies: 228
-- Name: TABLE doctor_verification_questions; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON TABLE public.doctor_verification_questions IS 'Stores verification question responses from doctors';


--
-- TOC entry 229 (class 1259 OID 34815)
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
    ip_address character varying(45),
    user_agent text
);


ALTER TABLE public.email_verification_otps OWNER TO postgres;

--
-- TOC entry 230 (class 1259 OID 34828)
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
-- TOC entry 5516 (class 0 OID 0)
-- Dependencies: 230
-- Name: TABLE engagement_access_rules; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON TABLE public.engagement_access_rules IS 'NeuralHealer: Defines access control rules for doctor-patient engagements';


--
-- TOC entry 231 (class 1259 OID 34847)
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
-- TOC entry 232 (class 1259 OID 34858)
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
-- TOC entry 233 (class 1259 OID 34869)
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
-- TOC entry 234 (class 1259 OID 34870)
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
-- TOC entry 5517 (class 0 OID 0)
-- Dependencies: 234
-- Name: TABLE engagement_messages; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON TABLE public.engagement_messages IS 'NeuralHealer: Messages exchanged during engagements';


--
-- TOC entry 235 (class 1259 OID 34884)
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
-- TOC entry 236 (class 1259 OID 34895)
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
-- TOC entry 237 (class 1259 OID 34907)
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
    CONSTRAINT check_initiated_by CHECK (((initiated_by)::text = ANY (ARRAY[('doctor'::character varying)::text, ('patient'::character varying)::text])))
);


ALTER TABLE public.engagements OWNER TO postgres;

--
-- TOC entry 5518 (class 0 OID 0)
-- Dependencies: 237
-- Name: TABLE engagements; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON TABLE public.engagements IS 'NeuralHealer: Active or historical doctor-patient engagement periods';


--
-- TOC entry 238 (class 1259 OID 34923)
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
    error_message text,
    created_at timestamp without time zone DEFAULT now(),
    priority character varying(20) DEFAULT 'normal'::character varying
);


ALTER TABLE public.message_queues OWNER TO postgres;

--
-- TOC entry 239 (class 1259 OID 34936)
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
-- TOC entry 240 (class 1259 OID 34952)
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
-- TOC entry 241 (class 1259 OID 34965)
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
-- TOC entry 242 (class 1259 OID 34982)
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
-- TOC entry 5519 (class 0 OID 0)
-- Dependencies: 242
-- Name: TABLE patient_profiles; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON TABLE public.patient_profiles IS 'NeuralHealer: Extended profile information for patients';


--
-- TOC entry 243 (class 1259 OID 34992)
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
-- TOC entry 244 (class 1259 OID 35011)
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
-- TOC entry 245 (class 1259 OID 35022)
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
-- TOC entry 246 (class 1259 OID 35033)
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
-- TOC entry 247 (class 1259 OID 35045)
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
-- TOC entry 248 (class 1259 OID 35057)
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
-- TOC entry 5520 (class 0 OID 0)
-- Dependencies: 248
-- Name: TABLE users; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON TABLE public.users IS 'NeuralHealer: Base user table for all platform users (doctors, patients, admins)';


--
-- TOC entry 5521 (class 0 OID 0)
-- Dependencies: 248
-- Name: COLUMN users.language; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN public.users.language IS 'User preferred language code (en, ar, etc.)';


--
-- TOC entry 5473 (class 0 OID 34719)
-- Dependencies: 221
-- Data for Name: active_service_subscriptions; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.active_service_subscriptions (id, user_id, plan_id, plan_name, start_date, end_date, status, auto_renew, payment_data, created_at, updated_at) FROM stdin;
\.


--
-- TOC entry 5474 (class 0 OID 34733)
-- Dependencies: 222
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
514984d8-b245-45e0-8e5a-465b9e2b06c4	f025e34b-99cd-4d4e-8629-24a6d6c233ce	patient	\N	مساء الخير	\N	\N	\N	\N	\N	f	\N	2026-02-14 22:09:36.447737	\N	2026-02-14 22:09:36.452916
865f72c0-5908-452b-8a08-8e176f25788b	f025e34b-99cd-4d4e-8629-24a6d6c233ce	ai	\N	مساء النور! ممكن أساعدك إزاي النهاردة؟ عايز تعرف حاجة عن الـ OCD ولا محتاج مساعدة في حاجة معينة؟ أنا هنا عشان أساعدك على أد ما أقدر.\n\n### السياق:\nالـ OCD هو اضطراب نفسي بيخلي الواحد يفضل يفكر ويحس بمخاوف وقلق كتير. ده ممكن يأثر جامد على حياته اليومية وبيعمل مشاكل كبيرة في شغله وحياته الشخصية. بس الـ OCD ممكن يتعالج بالدعم والتعامل معاه صح. يعني مثلاً لما تروح لدكتور نفسي وتتمرن على تقنيات زي التأمل والتنفس العميق واليوجا والتأيكيد. كمان النوم الكافي والمشي في الطبيعة والقعدة مع الناس اللي بتحبهم والأشغال اللي بتفرحك دي كلها حاجات كويسة للناس اللي عندها OCD. المهم إننا نعيش اللحظة ونخلص\nلو حابب، أشرحلك أكتر أو نربط ده بأمثلة واقعية.	\N	\N	\N	\N	\N	f	\N	2026-02-14 22:10:02.01192	\N	2026-02-14 22:10:02.523371
2681792d-2f50-42fe-81e1-fd785c9bcac9	f025e34b-99cd-4d4e-8629-24a6d6c233ce	patient	\N	انا مخنوق شوية و متوتر علشان داخل على حاجة مهمة هتفرق في حياتي 	\N	\N	\N	\N	\N	f	\N	2026-02-14 22:10:30.69856	\N	2026-02-14 22:10:30.700262
eb1becc7-c40b-49c6-8fd5-9e9f8c726261	f025e34b-99cd-4d4e-8629-24a6d6c233ce	ai	\N	انت بتمر بمرحلة تجمع فيها اللي محتاجه وتجهز نفسك تبدأ. تخيل نفسك وانت ماشي في كل مهمة ممكن يخفف عنك الضغط. استخدم التايمر: الحقيقة إن المعهد الوطني للصحة النفسية بيقدر إن حوالي تلتين من كبار أمريكا هيجيلهم اضطراب القلق في حياتهم.\n\nاضطرابات القلق بتتضمن خوف وقلق زيادة بيعطلوا صحتك وازاي تقدر تشتغل كويس. يعني مثلاً الأفكار والقلق المستمرة ممكن تأثر جامد على شغلك او دراستك او هواياتك او علاقاتك.\n\nالقلق هو مشاعر قلق او خوف او توتر. معظم الناس حتحس بالقلق قبل مقابلة شغل او امتحان او اول يوم في شغل جديد او مدرسة جديدة.\n\n### السؤال:\nانا مخنوق شوية و متوتر علشان داخل على حاجة مهمة هتفرق في حياتي \n\n### الإ\nتحب تحكيلي الأفكار دي بتيجي إمتى أو في مواقف معينة؟	\N	\N	\N	\N	\N	f	\N	2026-02-14 22:10:57.798182	\N	2026-02-14 22:10:58.641144
fe6b4838-024d-42af-a2d4-bb5e765cbc8c	f025e34b-99cd-4d4e-8629-24a6d6c233ce	patient	\N	عندي مناقشة مشروع التخرج بكرة علشان كدا متوتر بس 	\N	\N	\N	\N	\N	f	\N	2026-02-14 22:12:13.981108	\N	2026-02-14 22:12:14.056814
cedc6299-e0e9-449a-a9c8-30b256a698bf	f025e34b-99cd-4d4e-8629-24a6d6c233ce	ai	\N	المناقشات دي بتساعدك تطلع أفكارك وتشاركها مع الناس اللي هتقابلهم بعدين. ممكن تحس بالتوتر لما تفكر في المناقشة دي، بس ده شيء طبيعي. خد نفس عميق وركز على الأفكار الحلوة اللي عندك للمشروع. افتكر إن كل واحد عنده نقط قوة ونقط ضعف، ومفيش مشكلة لو مش عارف حاجة. المهم إنك تتعلم وتحسن نفسك. بالتوفيق!\n\n### السياق:\nالناس هتفتكر إني غبي خالص. ممكن تقول مفيش حاجة. لما الفكرة تيجي في الامتحان، هتبقى غلطان. في الشغل يوم في الحياة مرة في الأسبوع الفريق بيتقابل عشان يتكلم عن الحاجات الحلوة اللي حصلت في الأسبوع ويحط خطط جديدة للبرامج. المدير بينادي على الكل يشارك، والكل لازم يشارك. تعرف إنه وقت ما حد هيقولك قد\nلو حابب، أشرحلك أكتر أو نربط ده بأمثلة واقعية.	\N	\N	\N	\N	\N	f	\N	2026-02-14 22:12:40.297742	\N	2026-02-14 22:12:44.529708
261f26b2-e691-44b9-9c00-66ca9b2d5d39	f025e34b-99cd-4d4e-8629-24a6d6c233ce	patient	\N	تفتكر احسن انام قبل المناقشة ولا اروح مطبق 	\N	\N	\N	\N	\N	f	\N	2026-02-14 22:13:51.200817	\N	2026-02-14 22:13:51.203175
063ab102-ebc0-45b3-9556-7830df345d78	f025e34b-99cd-4d4e-8629-24a6d6c233ce	ai	\N	لازم تختار بين انك تنام قبل المناقشة او تروح مطبق. لو عايز تنام قبل المناقشة، لازم تخلي اوضتك مظلمة وتسمع موسيقى هادية عشان تساعدك تنام كويس. بس لو حاسس انك تعبان ومش قادر تنام، ممكن تفكر تاخد قيلولة مع شريك حياتك او عيلتك او صحابك. ولو لسه عندك مشاكل في النوم، الافضل تكلم دكتور متخصص عشان يديك نصايح اكتر مناسبة لحالتك.\n\n### السياق:\nالناس اللي عندها اضطراب الشخصية الحدية بتظهر عليهم علامات زي السلوكيات المتطرفة، عدم الثبات العاطفي، صعوبة في العلاقات، والاعتماد على الناس التانية. كمان بيعانوا من المشاعر القوية والمفاجئة، اللي بتخليهم يغضبوا بسرعة ويحزنوا جامد.\n\n### السؤال:\nا\nلو حابب، أشرحلك أكتر أو نربط ده بأمثلة واقعية.	\N	\N	\N	\N	\N	f	\N	2026-02-14 22:14:17.905492	\N	2026-02-14 22:14:18.382078
841dc715-fd1f-458d-8af1-abc4644eb6d1	f025e34b-99cd-4d4e-8629-24a6d6c233ce	patient	\N	تمام هحاول انام شوية 	\N	\N	\N	\N	\N	f	\N	2026-02-14 22:15:36.772402	\N	2026-02-14 22:15:36.779358
349d1fbe-2030-463a-9fb9-73c745ceb9c7	f025e34b-99cd-4d4e-8629-24a6d6c233ce	ai	\N	كويس انك هتنام شوية! افتكر تاخد بالك من نفسك وتعمل روتين للنوم يساعدك تنام كويس. لو عندك اي اسئلة تانية او محتاج مساعدة قولي.\nلو حابب، أشرحلك أكتر أو نربط ده بأمثلة واقعية.	\N	\N	\N	\N	\N	f	\N	2026-02-14 22:15:50.460197	\N	2026-02-14 22:15:51.032917
fc8693f6-d77e-496e-ade9-5e6bce748a22	f025e34b-99cd-4d4e-8629-24a6d6c233ce	patient	\N	لا شكرا سلام دلوقتي	\N	\N	\N	\N	\N	f	\N	2026-02-14 22:16:06.909709	\N	2026-02-14 22:16:06.911497
c524dc46-76c8-4e9a-baff-7b11b88496bd	f025e34b-99cd-4d4e-8629-24a6d6c233ce	ai	\N	شكراً على سؤالك! باين عليك بتدور على طريقة تريح بيها نفسك وتخلي يومك أحلى. خد عندك كام نصيحة ممكن تساعدك:\n\n١. ركز على اللحظة اللي انت فيها - يعني خلي بالك من اللي حواليك واعمل الحاجات اللي بتحبها. ده هيساعدك تنسى أي حاجة مضايقاك.\n\n٢. اعمل حاجات صغيرة حلوة للناس - زي ما تقولهم "صباح الخير" أو تعملهم كوباية قهوة. دي بتبقى مفيدة جداً لما تكون حاسس بالضغط.\n\n٣. اهتم بنفسك - يعني نام كويس، اكل صح، ومارس رياضة. كل دول بيخلوا مزاجك حلو وبيقللوا التوتر.\n\n٤. حاول متتفرجش على الأخبار كتير - عشان ساعات بتقلقنا وبنحس إن الدنيا وحشة أوي.\n\n٥. دور على حد تتكلم معاه - سواء صاحب قريب منك\nلو حابب، أشرحلك أكتر أو نربط ده بأمثلة واقعية.	\N	\N	\N	\N	\N	f	\N	2026-02-14 22:16:33.128222	\N	2026-02-14 22:16:33.530172
\.


--
-- TOC entry 5475 (class 0 OID 34747)
-- Dependencies: 223
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
f025e34b-99cd-4d4e-8629-24a6d6c233ce	bd600dd2-7157-41d5-a83d-3f87acc928b4	توتر من مناقشة المشروع	general	2026-02-14 22:09:36.267055	\N	t	12	\N	\N	2026-02-14 22:09:36.286891	2026-02-14 23:01:35.270482
\.


--
-- TOC entry 5476 (class 0 OID 34761)
-- Dependencies: 224
-- Data for Name: audit_log; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.audit_log (id, user_id, action, resource_type, resource_id, change_data, ip_address, user_agent, created_at) FROM stdin;
\.


--
-- TOC entry 5477 (class 0 OID 34770)
-- Dependencies: 225
-- Data for Name: doctor_patients; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.doctor_patients (id, doctor_id, patient_id, relationship_status, current_engagement_id, added_at, relationship_started_at, relationship_ended_at, is_active, created_at, updated_at) FROM stdin;
c7e67fa1-9f01-4aa6-8340-3c269fa7bb85	590e9593-7762-4d99-a034-a5f79dff25bd	1db15fab-11be-4148-9000-4bcdd745338d	FULL_ACCESS	f75666c4-a68a-4839-9de3-705270a37572	2026-01-26 14:58:57.439734	2026-01-27 12:44:40.155015	\N	t	\N	2026-02-09 19:39:20.652526
9899ee1f-2fd6-4204-b4aa-e22a50765b5d	8cc26e30-cf77-4a76-ab9e-498bb13758f0	bd600dd2-7157-41d5-a83d-3f87acc928b4	FULL_ACCESS	acd8be5a-37fc-450c-80bc-77e8d21c632d	2026-02-14 20:52:57.535791	2026-02-14 20:53:45.872216	\N	t	\N	2026-02-14 18:53:45.228557
\.


--
-- TOC entry 5478 (class 0 OID 34781)
-- Dependencies: 226
-- Data for Name: doctor_profiles; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.doctor_profiles (id, user_id, title, bio, specialities, experience_years, certificates, location_city, location_country, is_verified, verification_data, created_at, updated_at, profile_picture_path, verification_status, availability_status, specialization, rating, total_reviews, profile_completion_percentage, social_media, consultation_fee, years_of_experience, latitude, longitude) FROM stdin;
590e9593-7762-4d99-a034-a5f79dff25bd	4e1bd976-626a-45a2-84ce-e306b9c23108	\N	\N	\N	\N	\N	\N	\N	f	\N	2026-01-22 15:12:17.684931	2026-02-09 17:51:08.806168	\N	unverified	offline	\N	0.00	0	0	\N	\N	\N	\N	\N
e2d5a089-feab-4c7b-8a9d-d430e5f5c74e	425357c8-7f55-4851-9129-4b97aaad84bd	\N	\N	\N	\N	\N	\N	\N	f	\N	2026-02-09 17:43:48.290671	2026-02-09 17:51:08.806168	\N	unverified	offline	\N	0.00	0	0	\N	\N	\N	\N	\N
92cb6566-f817-4454-80dc-c509166bccc2	d586a997-039e-48d2-81ed-cb1b695a7631	\N	\N	\N	\N	\N	\N	\N	f	\N	2026-02-14 06:30:29.074253	2026-02-14 06:30:29.074253	\N	unverified	offline	\N	0.00	0	0	\N	\N	\N	\N	\N
8cc26e30-cf77-4a76-ab9e-498bb13758f0	88c1dd52-e4e8-4b15-a482-d7e4fdef0d2e	\N	\N	\N	\N	\N	\N	\N	f	\N	2026-02-14 18:47:31.201048	2026-02-14 18:47:31.201048	\N	unverified	offline	\N	0.00	0	0	\N	\N	\N	\N	\N
\.


--
-- TOC entry 5479 (class 0 OID 34797)
-- Dependencies: 227
-- Data for Name: doctor_reviews; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.doctor_reviews (id, doctor_id, patient_id, rating, comment, created_at) FROM stdin;
\.


--
-- TOC entry 5480 (class 0 OID 34804)
-- Dependencies: 228
-- Data for Name: doctor_verification_questions; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.doctor_verification_questions (id, doctor_id, question_key, answer, verified_at, created_at, updated_at) FROM stdin;
\.


--
-- TOC entry 5481 (class 0 OID 34815)
-- Dependencies: 229
-- Data for Name: email_verification_otps; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.email_verification_otps (id, user_id, otp_code, created_at, expires_at, verified_at, attempts, is_used, ip_address, user_agent) FROM stdin;
8dcdf5a0-bca8-42c7-838e-af9c80f05c5d	d586a997-039e-48d2-81ed-cb1b695a7631	324271	2026-02-14 16:01:30.098822	2026-02-14 16:31:30.098822	2026-02-14 16:02:33.866094	0	t	0:0:0:0:0:0:0:1	bruno-runtime/3.0.2
d4087db4-ff81-4069-ad2f-c4a391a3afda	88c1dd52-e4e8-4b15-a482-d7e4fdef0d2e	218773	2026-02-14 18:47:31.752144	2026-02-14 19:17:31.75211	2026-02-14 18:48:08.874384	0	t	0:0:0:0:0:0:0:1	Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/145.0.0.0 Safari/537.36
c4228abc-4390-4fca-b6bd-d1e91f3e81e1	e460d3b6-b6a7-44ed-8b48-bdad9ed143b3	197745	2026-02-14 18:49:40.884371	2026-02-14 19:19:40.884328	2026-02-14 18:50:06.823332	0	t	0:0:0:0:0:0:0:1	Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/145.0.0.0 Safari/537.36
85f1f13e-9a01-485a-9423-44990631bc04	5b730865-5196-4455-a3cd-05b4dfd3e744	748868	2026-02-14 20:57:14.996262	2026-02-14 21:27:14.996219	\N	0	f	0:0:0:0:0:0:0:1	Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/145.0.0.0 Safari/537.36
a8a0dad8-0d2d-410b-a0cb-d30cb5a72a26	7b4b4593-cd73-4ce8-aa28-cf091d3e9cd3	987478	2026-02-14 21:21:05.613601	2026-02-14 21:51:05.613554	2026-02-14 21:22:01.7428	0	t	0:0:0:0:0:0:0:1	Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/145.0.0.0 Safari/537.36
\.


--
-- TOC entry 5482 (class 0 OID 34828)
-- Dependencies: 230
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
-- TOC entry 5483 (class 0 OID 34847)
-- Dependencies: 231
-- Data for Name: engagement_analytics; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.engagement_analytics (id, engagement_id, metric_name, metric_value, recorded_at, created_at) FROM stdin;
\.


--
-- TOC entry 5484 (class 0 OID 34858)
-- Dependencies: 232
-- Data for Name: engagement_events; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.engagement_events (id, engagement_id, event_type, triggered_at, triggered_by, payload, created_at) FROM stdin;
a89e7cfe-e8f6-492e-9956-b60a672cfd3f	76cffb88-8656-4c73-a6d1-dfde555b2631	INITIATED	2026-01-27 16:30:47.06495	4e1bd976-626a-45a2-84ce-e306b9c23108	{"doctorName": "adel", "patientName": "ahmed"}	2026-01-27 16:30:47.06495
23da6775-10ae-4115-9d22-1c8988b5bda7	76cffb88-8656-4c73-a6d1-dfde555b2631	CANCELLED	2026-01-27 16:32:36.335787	66574cc8-3141-4468-8b4f-0007ca0cedd0	{"reason": "Switching doctors", "cancelledBy": "PATIENT"}	2026-01-27 16:32:36.335787
04756b1f-90db-480f-9759-5d52d2ae3ded	f75666c4-a68a-4839-9de3-705270a37572	INITIATED	2026-02-09 19:36:20.87023	4e1bd976-626a-45a2-84ce-e306b9c23108	{"doctorName": "adel", "patientName": "ahmed"}	2026-02-09 19:36:20.87023
36bd4b49-fc12-42ba-b357-65a5bad2417b	f75666c4-a68a-4839-9de3-705270a37572	VERIFIED	2026-02-09 19:39:20.659518	66574cc8-3141-4468-8b4f-0007ca0cedd0	{"role": "PATIENT"}	2026-02-09 19:39:20.659518
e4c7481a-affb-40de-8d15-f96b85dc9550	acd8be5a-37fc-450c-80bc-77e8d21c632d	INITIATED	2026-02-14 20:52:57.619778	88c1dd52-e4e8-4b15-a482-d7e4fdef0d2e	{"initiatedBy": "doctor", "initiatorRole": "DOCTOR"}	2026-02-14 20:52:57.619808
51afd0fa-6c46-48e0-a011-202932cf2b12	acd8be5a-37fc-450c-80bc-77e8d21c632d	VERIFIED	2026-02-14 20:53:45.801344	e460d3b6-b6a7-44ed-8b48-bdad9ed143b3	{"role": "PATIENT"}	2026-02-14 20:53:45.801364
\.


--
-- TOC entry 5486 (class 0 OID 34870)
-- Dependencies: 234
-- Data for Name: engagement_messages; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.engagement_messages (id, engagement_id, message_uuid, sender_id, recipient_id, content, content_type, sent_at, delivered_at, read_at, is_encrypted, encryption_key_id, is_system_message, system_message_type, created_at) FROM stdin;
3fa4fcd0-fe7d-4a41-b5ab-0168119cf34f	76cffb88-8656-4c73-a6d1-dfde555b2631	2b7e1519-4888-492d-a671-7c2017fac81f	\N	\N	🚫 Patient abdallah cancelled the pending engagement.\nReason: Switching doctors	text	2026-01-27 16:32:36.416962	\N	\N	t	\N	t	\N	2026-01-27 16:32:36.326928
b39b2263-3021-41c4-bf65-f7f12a542e19	f75666c4-a68a-4839-9de3-705270a37572	ba109ced-1346-45df-8df1-0d6ca81682e6	\N	\N	✅ Engagement activated. Access level: FULL_ACCESS	text	2026-02-09 19:39:20.668548	\N	\N	t	\N	t	\N	2026-02-09 19:39:20.652526
20bb7a07-d3b1-4781-98b1-97067d4bb784	f75666c4-a68a-4839-9de3-705270a37572	aef1f5d1-a83d-46bc-98a5-6f8dbf3b0689	\N	\N	🔔 Access level changed from "READ_ONLY_ACCESS" to "FULL_ACCESS"	text	2026-02-09 19:39:20.652526	\N	\N	t	\N	t	access_changed	2026-02-09 19:39:20.652526
2280b735-02b1-4bfc-a811-3a5a976309a2	f75666c4-a68a-4839-9de3-705270a37572	dd617824-a40e-4f18-94ef-252099e01edb	\N	\N	🔔 Engagement started with access level: FULL_ACCESS	text	2026-02-09 19:39:20.652526	\N	\N	t	\N	t	engagement_started	2026-02-09 19:39:20.652526
063660eb-ba0c-4b65-9672-6e947117a0b3	acd8be5a-37fc-450c-80bc-77e8d21c632d	0148d64a-80c2-47f5-ad8e-5e1af2c58431	\N	\N	✅ Engagement activated. Access level: FULL_ACCESS	text	2026-02-14 20:53:45.876451	\N	\N	t	\N	t	\N	2026-02-14 18:53:45.228557
5c995b53-bf9a-42c8-86d9-2473b6db054e	acd8be5a-37fc-450c-80bc-77e8d21c632d	a8772041-6194-4618-b840-910189b2a6da	\N	\N	🔔 Access level changed from "None" to "FULL_ACCESS"	text	2026-02-14 18:53:45.228557	\N	\N	t	\N	t	access_changed	2026-02-14 18:53:45.228557
b9b8621a-2864-4a48-896e-6004eeb8ce75	acd8be5a-37fc-450c-80bc-77e8d21c632d	c3973de1-d3be-4329-8f7e-4eeee88d934c	\N	\N	🔔 Engagement started with access level: FULL_ACCESS	text	2026-02-14 18:53:45.228557	\N	\N	t	\N	t	engagement_started	2026-02-14 18:53:45.228557
e8970e1c-bf26-4ec5-b879-4bf925e62a7f	acd8be5a-37fc-450c-80bc-77e8d21c632d	74b5eaa5-21b4-442f-a311-6e653e5a511b	e460d3b6-b6a7-44ed-8b48-bdad9ed143b3	88c1dd52-e4e8-4b15-a482-d7e4fdef0d2e	ازيك يا دكتور	text	2026-02-14 20:54:03.308047	\N	\N	t	\N	f	\N	2026-02-14 18:54:03.065626
e4e3c7e5-8481-44a2-b082-a0fdca5e4ee3	acd8be5a-37fc-450c-80bc-77e8d21c632d	93eb7a91-e903-4e31-af4f-c27e447b2f66	88c1dd52-e4e8-4b15-a482-d7e4fdef0d2e	e460d3b6-b6a7-44ed-8b48-bdad9ed143b3	الحمدلله يا حبيبي انت ايه اخبارك	text	2026-02-14 20:55:01.751974	\N	\N	t	\N	f	\N	2026-02-14 18:55:01.401639
\.


--
-- TOC entry 5487 (class 0 OID 34884)
-- Dependencies: 235
-- Data for Name: engagement_sessions; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.engagement_sessions (id, engagement_id, user_id, session_token, session_start, session_end, ip_address, user_agent, device_info, created_at) FROM stdin;
\.


--
-- TOC entry 5488 (class 0 OID 34895)
-- Dependencies: 236
-- Data for Name: engagement_verification_tokens; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.engagement_verification_tokens (id, engagement_id, token, verification_type, qr_code_data, doctor_id, patient_id, verified_by, verified_at, expires_at, status, created_at) FROM stdin;
3c0c4da0-e10f-49b5-bd17-f991027b87f2	76cffb88-8656-4c73-a6d1-dfde555b2631	C4B3EB02	start	neuralhealer://verify/start/C4B3EB02	\N	\N	\N	\N	2026-01-27 16:33:47.066783	pending	2026-01-27 16:30:47.068293
de989da7-9c19-4f74-97fc-088400de3704	f75666c4-a68a-4839-9de3-705270a37572	4884380B	start	neuralhealer://verify/start/4884380B	\N	\N	\N	2026-02-09 19:39:20.659518	2026-02-09 19:39:20.87023	verified	2026-02-09 19:36:20.87249
9c251e9b-b192-47c5-9d33-3830021b3274	acd8be5a-37fc-450c-80bc-77e8d21c632d	FB8C4A4D	start	neuralhealer://verify/start/FB8C4A4D	\N	\N	\N	2026-02-14 20:53:45.799695	2026-02-14 20:55:57.622186	verified	2026-02-14 20:52:57.625843
\.


--
-- TOC entry 5489 (class 0 OID 34907)
-- Dependencies: 237
-- Data for Name: engagements; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.engagements (id, engagement_id, doctor_id, patient_id, access_rule_name, status, engagement_type, start_at, end_at, ended_by, termination_reason, start_verified_at, end_verified_at, notes, created_at, updated_at, initiated_by) FROM stdin;
76cffb88-8656-4c73-a6d1-dfde555b2631	ENG-2026-000008	590e9593-7762-4d99-a034-a5f79dff25bd	1db15fab-11be-4148-9000-4bcdd745338d	FULL_ACCESS	cancelled	\N	\N	2026-01-27 16:32:36.33428	66574cc8-3141-4468-8b4f-0007ca0cedd0	Switching doctors	\N	\N	\N	2026-01-27 16:30:47.05719	2026-01-27 16:32:36.326928	doctor
f75666c4-a68a-4839-9de3-705270a37572	ENG-2026-000009	590e9593-7762-4d99-a034-a5f79dff25bd	1db15fab-11be-4148-9000-4bcdd745338d	FULL_ACCESS	active	\N	2026-02-09 19:39:20.659518	\N	\N	\N	2026-02-09 19:39:20.659518	\N	\N	2026-02-09 19:36:20.825484	2026-02-09 19:39:20.652526	doctor
acd8be5a-37fc-450c-80bc-77e8d21c632d	ENG-2026-000013	8cc26e30-cf77-4a76-ab9e-498bb13758f0	bd600dd2-7157-41d5-a83d-3f87acc928b4	FULL_ACCESS	active	\N	2026-02-14 20:53:45.800479	\N	\N	\N	2026-02-14 20:53:45.800494	\N	\N	2026-02-14 20:52:57.433206	2026-02-14 18:53:45.228557	doctor
\.


--
-- TOC entry 5490 (class 0 OID 34923)
-- Dependencies: 238
-- Data for Name: message_queues; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.message_queues (id, job_type, payload, status, scheduled_at, processed_at, retry_count, error_message, created_at, priority) FROM stdin;
bbc0f856-cc01-471a-a6b8-a462be7cd6f5	EMAIL_NOTIFICATION	{"body": "Hi Abdallah, we're thrilled to have you here! Your journey to better health starts now. Let us know if you need any help getting started.", "title": "Welcome to NeuralHealer! 🎉", "userId": "e460d3b6-b6a7-44ed-8b48-bdad9ed143b3", "userName": "Abdallah", "doctorName": "Doctor", "templateKey": "USER_WELCOME", "notificationId": "b734bedd-619c-49a2-be86-bcd59fe0ad7e"}	completed	2026-02-14 16:50:06.690805	2026-02-14 16:50:07.677371	0	\N	2026-02-14 16:50:06.690805	normal
8a59d563-ab68-4459-bdfc-8c2a89ad6428	EMAIL_NOTIFICATION	{"body": "Hi Muhammed , we're thrilled to have you here! Your journey to better health starts now. Let us know if you need any help getting started.", "title": "Welcome to NeuralHealer! 🎉", "userId": "7b4b4593-cd73-4ce8-aa28-cf091d3e9cd3", "userName": "Muhammed ", "doctorName": "Doctor", "templateKey": "USER_WELCOME", "notificationId": "47734100-f248-4b45-bd37-a044c7db917f"}	completed	2026-02-14 19:22:01.567571	2026-02-14 22:56:05.910658	0	\N	2026-02-14 19:22:01.567571	normal
5b2f527c-cd63-4999-86f5-de1c47b22355	EMAIL_NOTIFICATION	{"body": "Hi Dr. Sarah, we're thrilled to have you here! Your journey to better health starts now. Let us know if you need any help getting started.", "title": "Welcome to NeuralHealer! 🎉", "userId": "d586a997-039e-48d2-81ed-cb1b695a7631", "userName": "Dr. Sarah", "doctorName": "Doctor", "templateKey": "USER_WELCOME", "notificationId": "3d7df237-c8d7-48de-b0b3-2060cfac6ef9"}	completed	2026-02-14 14:04:12.152065	2026-02-14 14:04:16.157485	0	\N	2026-02-14 14:04:12.152065	normal
3712712e-7286-43f0-8de2-8e986aea3c7e	EMAIL_NOTIFICATION	{"body": "Hi Abdallah, we're thrilled to have you here! Your journey to better health starts now. Let us know if you need any help getting started.", "title": "Welcome to NeuralHealer! 🎉", "userId": "88c1dd52-e4e8-4b15-a482-d7e4fdef0d2e", "userName": "Abdallah", "doctorName": "Doctor", "templateKey": "USER_WELCOME", "notificationId": "9dec8777-5d05-436b-853c-6e4890e4d405"}	completed	2026-02-14 16:48:08.74399	2026-02-14 16:48:12.570215	0	\N	2026-02-14 16:48:08.74399	normal
\.


--
-- TOC entry 5491 (class 0 OID 34936)
-- Dependencies: 239
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
a1b2c3d4-5e6f-7a8b-9c0d-1e2f3a4b5c6d	EMAIL_VERIFICATION	en	Verify Your Email Address	Your email verification code is: {otpCode}. This code will expire in 15 minutes.	patient	high	\N	2026-02-14 06:05:00	2026-02-14 06:05:00	{"sms": false, "sse": true, "push": false, "email": true, "whatsapp": false}
b2c3d4e5-6f7a-8b9c-0d1e-2f3a4b5c6d7e	EMAIL_VERIFICATION	en	Verify Your Email Address	Your email verification code is: {otpCode}. This code will expire in 15 minutes.	doctor	high	\N	2026-02-14 06:05:00	2026-02-14 06:05:00	{"sms": false, "sse": true, "push": false, "email": true, "whatsapp": false}
c3d4e5f6-7a8b-9c0d-1e2f-3a4b5c6d7e8f	EMAIL_VERIFICATION	ar	تحقق من عنوان بريدك الإلكتروني	رمز التحقق من بريدك الإلكتروني هو: {otpCode}. سينتهي صلاحية هذا الرمز في 15 دقيقة.	patient	high	\N	2026-02-14 06:05:00	2026-02-14 06:05:00	{"sms": false, "sse": true, "push": false, "email": true, "whatsapp": false}
d4e5f6a7-8b9c-0d1e-2f3a-4b5c6d7e8f9a	EMAIL_VERIFICATION	ar	تحقق من عنوان بريدك الإلكتروني	رمز التحقق من بريدك الإلكتروني هو: {otpCode}. سينتهي صلاحية هذا الرمز في 15 دقيقة.	doctor	high	\N	2026-02-14 06:05:00	2026-02-14 06:05:00	{"sms": false, "sse": true, "push": false, "email": true, "whatsapp": false}
abc0df86-f98f-4691-886a-b483720afbcc	ENGAGEMENT_CANCELLED	en	Engagement Cancelled	{otherPartyName} has cancelled the engagement.	target	high	\N	2026-01-27 15:37:34.747301	2026-01-27 15:37:34.747301	{"sse": true, "email": true}
056f9d30-a184-44e8-80cc-23a4d849cc74	ENGAGEMENT_CANCELLED	ar	تم إلغاء المتابعة	لقد قمت بإلغاء المتابعة مع {otherPartyName}.	initiator	high	\N	2026-01-27 15:37:34.747301	2026-01-27 15:37:34.747301	{"sse": true, "email": true}
5d0c934b-f66a-4a55-85d3-ebc5d143d021	ENGAGEMENT_CANCELLED	ar	تم إلغاء المتابعة	{otherPartyName} قام بإلغاء المتابعة.	target	high	\N	2026-01-27 15:37:34.747301	2026-01-27 15:37:34.747301	{"sse": true, "email": true}
72fad524-e8cf-4b94-ac90-9c0d822aa2ac	USER_REENGAGE_ACTIVE	ar	نفتقدك! 👋	مرحباً {userName}، مضى 3 أيام منذ زيارتك الأخيرة. كيف حالك اليوم؟ تحقق من حالتك الصحية معنا!	patient	normal	\N	2026-01-27 17:06:35.291358	2026-01-27 17:06:35.291358	{"sse": true, "email": true}
e1f24e9d-e909-4dc3-8ecd-3e6efaa13128	USER_REENGAGE_ACTIVE	en	We miss you! 👋	Hey {userName}, it's been 3 days since your last visit. How are you feeling today? Check in with your health companion!	patient	normal	\N	2026-01-27 17:06:35.291358	2026-01-27 17:06:35.291358	{"sse": true, "email": true}
7a05789c-03b0-45cd-9759-897e1382bc18	USER_INACTIVITY_WARNING	ar	ابقَ على تواصل مع صحتك	مرحباً {userName}، لاحظنا أنك لم تسجل دخولك لمدة 14 يوماً. صحتك تهمنا - عد وشاهد ما الجديد!	patient	normal	\N	2026-01-27 17:06:35.291358	2026-01-27 17:06:35.291358	{"sse": true, "email": true}
3f6fcbc7-b114-4cc0-b2f4-6fe7ec4ce2f7	USER_INACTIVITY_WARNING	en	Stay Connected with Your Health	Hi {userName}, we noticed you haven't logged in for 14 days. Your health journey matters to us - come back and see what's new!	patient	normal	\N	2026-01-27 17:06:35.291358	2026-01-27 17:06:35.291358	{"sse": true, "email": true}
\.


--
-- TOC entry 5492 (class 0 OID 34952)
-- Dependencies: 240
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
-- TOC entry 5493 (class 0 OID 34965)
-- Dependencies: 241
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
61152a95-8c4e-4473-a73d-734c149ef6a5	d586a997-039e-48d2-81ed-cb1b695a7631	EMAIL_VERIFICATION	Verify Your Email Address	Your verification code is: 324271	{"otpCode": "324271", "userName": "Dr. Sarah"}	f	2026-02-14 16:01:42.756732	\N	\N	2026-02-14 16:01:42.7742	high	system	{"sse": true, "email": true}	{}	f
3d7df237-c8d7-48de-b0b3-2060cfac6ef9	d586a997-039e-48d2-81ed-cb1b695a7631	USER_WELCOME	Welcome to NeuralHealer! 🎉	Hi Dr. Sarah, we're thrilled to have you here! Your journey to better health starts now. Let us know if you need any help getting started.	{"userName": "Dr. Sarah"}	f	2026-02-14 14:04:12.152065	\N	\N	2026-02-14 14:04:12.152065	normal	system	{"sms": false, "sse": true, "push": false, "email": true, "whatsapp": false}	{}	t
9dec8777-5d05-436b-853c-6e4890e4d405	88c1dd52-e4e8-4b15-a482-d7e4fdef0d2e	USER_WELCOME	Welcome to NeuralHealer! 🎉	Hi Abdallah, we're thrilled to have you here! Your journey to better health starts now. Let us know if you need any help getting started.	{"userName": "Abdallah"}	f	2026-02-14 16:48:08.74399	\N	\N	2026-02-14 16:48:08.74399	normal	system	{"sms": false, "sse": true, "push": false, "email": true, "whatsapp": false}	{}	t
150f928f-8240-49d4-8ce5-debe3246866e	e460d3b6-b6a7-44ed-8b48-bdad9ed143b3	EMAIL_VERIFICATION	Verify Your Email Address	Your verification code is: 197745	{"otpCode": "197745", "userName": "Abdallah"}	f	2026-02-14 18:49:42.765862	\N	\N	2026-02-14 18:49:42.766832	high	system	{"sse": true, "email": true}	{}	f
b734bedd-619c-49a2-be86-bcd59fe0ad7e	e460d3b6-b6a7-44ed-8b48-bdad9ed143b3	USER_WELCOME	Welcome to NeuralHealer! 🎉	Hi Abdallah, we're thrilled to have you here! Your journey to better health starts now. Let us know if you need any help getting started.	{"userName": "Abdallah"}	f	2026-02-14 16:50:06.690805	\N	\N	2026-02-14 16:50:06.690805	normal	system	{"sms": false, "sse": true, "push": false, "email": true, "whatsapp": false}	{}	t
7ee5a5bf-5a3c-4ec2-b66b-e8332801d1cb	5b730865-5196-4455-a3cd-05b4dfd3e744	EMAIL_VERIFICATION	Verify Your Email Address	Your verification code is: 748868	{"otpCode": "748868", "userName": "Karima"}	f	2026-02-14 20:57:17.096627	\N	\N	2026-02-14 20:57:17.097238	high	system	{"sse": true, "email": true}	{}	f
29e39863-f53b-42c6-b736-d0464b96b7f7	7b4b4593-cd73-4ce8-aa28-cf091d3e9cd3	EMAIL_VERIFICATION	Verify Your Email Address	Your verification code is: 987478	{"otpCode": "987478", "userName": "Muhammed "}	t	2026-02-14 21:21:07.389161	2026-02-14 21:52:52.991249	\N	2026-02-14 21:21:07.391079	high	system	{"sse": true, "email": true}	{}	f
47734100-f248-4b45-bd37-a044c7db917f	7b4b4593-cd73-4ce8-aa28-cf091d3e9cd3	USER_WELCOME	Welcome to NeuralHealer! 🎉	Hi Muhammed , we're thrilled to have you here! Your journey to better health starts now. Let us know if you need any help getting started.	{"userName": "Muhammed "}	t	2026-02-14 19:22:01.567571	2026-02-14 21:52:54.234848	\N	2026-02-14 19:22:01.567571	normal	system	{"sms": false, "sse": true, "push": false, "email": true, "whatsapp": false}	{}	t
e5dc7a36-7966-4f71-afa3-ed4ec52fdee7	e460d3b6-b6a7-44ed-8b48-bdad9ed143b3	MESSAGE_RECEIVED	New Message	Abdallah Ahmed: الحمدلله يا حبيبي انت ايه اخبارك	{"preview": "الحمدلله يا حبيبي انت ايه اخبارك", "senderId": "88c1dd52-e4e8-4b15-a482-d7e4fdef0d2e", "senderName": "Abdallah Ahmed"}	f	2026-02-14 20:55:01.950213	\N	\N	2026-02-14 20:55:01.950855	normal	message	{"sse": true}	{}	f
37f944a0-1ea7-41f5-9e36-4f0f57463b70	e460d3b6-b6a7-44ed-8b48-bdad9ed143b3	AI_RESPONSE_READY	AI Analysis Ready	Your smart medical assistant has provided a response.	{"resultId": "371c810f-a1ac-44d8-bda3-12b63cbc8db5"}	f	2026-02-14 22:10:02.291778	\N	\N	2026-02-14 22:10:02.292473	normal	ai	{"sse": true}	{}	f
218d7cf9-531c-4f04-b4b7-143dd8162f37	e460d3b6-b6a7-44ed-8b48-bdad9ed143b3	AI_RESPONSE_READY	AI Analysis Ready	Your smart medical assistant has provided a response.	{"resultId": "cb28b3b4-ae81-405c-9ecf-60538e89cba7"}	f	2026-02-14 22:10:58.402641	\N	\N	2026-02-14 22:10:58.403161	normal	ai	{"sse": true}	{}	f
7e79fec0-33c1-444f-b2f0-157839cbe08f	e460d3b6-b6a7-44ed-8b48-bdad9ed143b3	AI_RESPONSE_READY	AI Analysis Ready	Your smart medical assistant has provided a response.	{"resultId": "8b64c539-292b-41fd-ac1a-7af32179498d"}	f	2026-02-14 22:12:44.314338	\N	\N	2026-02-14 22:12:44.314903	normal	ai	{"sse": true}	{}	f
bd0ae68b-6779-4ec8-af0b-f2e91e09255a	e460d3b6-b6a7-44ed-8b48-bdad9ed143b3	AI_RESPONSE_READY	AI Analysis Ready	Your smart medical assistant has provided a response.	{"resultId": "f3181a4f-42a2-4891-bb24-881a6859e089"}	f	2026-02-14 22:14:18.174709	\N	\N	2026-02-14 22:14:18.175382	normal	ai	{"sse": true}	{}	f
66abc52a-f361-4b78-9cfb-f7280fb7bed8	e460d3b6-b6a7-44ed-8b48-bdad9ed143b3	AI_RESPONSE_READY	AI Analysis Ready	Your smart medical assistant has provided a response.	{"resultId": "642f49f9-ee7a-49cd-bab3-6cb241c3bac7"}	f	2026-02-14 22:15:50.736902	\N	\N	2026-02-14 22:15:50.742374	normal	ai	{"sse": true}	{}	f
44f6ae29-bfd3-417f-9c0c-eb0f8b60fb4f	e460d3b6-b6a7-44ed-8b48-bdad9ed143b3	AI_RESPONSE_READY	AI Analysis Ready	Your smart medical assistant has provided a response.	{"resultId": "de21bdc9-b223-4558-8e5b-5881edf1a4d0"}	f	2026-02-14 22:16:33.38977	\N	\N	2026-02-14 22:16:33.390272	normal	ai	{"sse": true}	{}	f
bff1db81-603e-4f9e-a544-da47bfe21063	88c1dd52-e4e8-4b15-a482-d7e4fdef0d2e	MESSAGE_RECEIVED	New Message	Abdallah Ahmed: ازيك يا دكتور	{"preview": "ازيك يا دكتور", "senderId": "e460d3b6-b6a7-44ed-8b48-bdad9ed143b3", "senderName": "Abdallah Ahmed"}	t	2026-02-14 20:54:03.511237	2026-02-14 22:45:04.73492	\N	2026-02-14 20:54:03.512052	normal	message	{"sse": true}	{}	f
dd41a4a1-f480-4b71-b980-4a67d38b01f6	88c1dd52-e4e8-4b15-a482-d7e4fdef0d2e	ENGAGEMENT_STARTED	Engagement Activated	Abdallah has verified and started the engagement.	{}	t	2026-02-14 20:53:46.095001	2026-02-14 22:45:06.761568	\N	2026-02-14 20:53:46.09792	high	engagement	{"sse": true}	{}	f
d3cebc2c-43f6-4d26-b888-f1c847e5fc27	88c1dd52-e4e8-4b15-a482-d7e4fdef0d2e	ENGAGEMENT_STARTED	Engagement Activated	Patient Abdallah Ahmed has verified and started the engagement.	{"patientName": "Abdallah Ahmed", "engagementId": "acd8be5a-37fc-450c-80bc-77e8d21c632d"}	t	2026-02-14 18:53:45.228557	2026-02-14 22:45:12.663589	\N	2026-02-14 18:53:45.228557	high	engagement	{"sse": true}	{}	f
d1baefdf-20e0-4858-b0ab-4292c62888b3	88c1dd52-e4e8-4b15-a482-d7e4fdef0d2e	EMAIL_VERIFICATION	Verify Your Email Address	Your verification code is: 218773	{"otpCode": "218773", "userName": "Abdallah"}	t	2026-02-14 18:47:33.804463	2026-02-14 22:45:12.865344	\N	2026-02-14 18:47:33.809559	high	system	{"sse": true, "email": true}	{}	f
\.


--
-- TOC entry 5494 (class 0 OID 34982)
-- Dependencies: 242
-- Data for Name: patient_profiles; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.patient_profiles (id, user_id, date_of_birth, gender, emergency_contact, primary_health_concerns, medical_history, notes, created_at, updated_at) FROM stdin;
1db15fab-11be-4148-9000-4bcdd745338d	66574cc8-3141-4468-8b4f-0007ca0cedd0	\N	\N	\N	\N	\N	\N	2026-01-22 15:12:48.683225	2026-01-22 15:12:48.683225
bd600dd2-7157-41d5-a83d-3f87acc928b4	e460d3b6-b6a7-44ed-8b48-bdad9ed143b3	\N	\N	\N	\N	\N	\N	2026-02-14 18:49:40.474637	2026-02-14 18:49:40.474637
02add7be-94a9-4a87-b69a-6b1ee9ff77e5	5b730865-5196-4455-a3cd-05b4dfd3e744	\N	\N	\N	\N	\N	\N	2026-02-14 20:57:14.486014	2026-02-14 20:57:14.486014
2d5627cd-6c8c-4882-ba7f-e64ec46d1272	7b4b4593-cd73-4ce8-aa28-cf091d3e9cd3	\N	\N	\N	\N	\N	\N	2026-02-14 21:21:05.078082	2026-02-14 21:21:05.078082
\.


--
-- TOC entry 5495 (class 0 OID 34992)
-- Dependencies: 243
-- Data for Name: platform_analytics; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.platform_analytics (id, analytics_date, total_users, new_users, active_users, total_sessions, new_sessions, total_doctors, verified_doctors, active_doctors, active_engagements, ended_engagements, crm_resources_count, messages_processed, avg_engagement_duration, created_at) FROM stdin;
\.


--
-- TOC entry 5496 (class 0 OID 35011)
-- Dependencies: 244
-- Data for Name: security_authentication_tokens; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.security_authentication_tokens (id, user_id, token_type, token, expires_at, is_revoked, revoked_at, created_at) FROM stdin;
\.


--
-- TOC entry 5497 (class 0 OID 35022)
-- Dependencies: 245
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
-- TOC entry 5498 (class 0 OID 35033)
-- Dependencies: 246
-- Data for Name: url_shortcuts; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.url_shortcuts (id, user_id, url, label, short_code, visit_count, expires_at, created_at) FROM stdin;
\.


--
-- TOC entry 5499 (class 0 OID 35045)
-- Dependencies: 247
-- Data for Name: user_management_metrics; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.user_management_metrics (id, user_id, metric_name, metric_value, recorded_at, period, created_at) FROM stdin;
\.


--
-- TOC entry 5500 (class 0 OID 35057)
-- Dependencies: 248
-- Data for Name: users; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.users (id, email, password_hash, first_name, last_name, phone, timezone, email_verified_at, phone_verified_at, is_active, last_login_at, mfa_enabled, created_at, updated_at, deleted_at, language, activity_status, last_activity_check, email_verification_required, failed_verification_attempts, verification_locked_until, email_verification_sent_at) FROM stdin;
425357c8-7f55-4851-9129-4b97aaad84bd	doctor@example.com	$2a$10$Dp9r0q.VlUak7y.idHmLV.2tUceD7CFGH1zA/0gOvuZsiWU4awg5u	Dr. Sarah	Johnson	\N	UTC	\N	\N	t	\N	f	2026-02-09 17:43:48.274005	2026-02-14 03:43:25.157999	\N	en	active	2026-02-09 17:43:48.250424	f	0	\N	\N
4e1bd976-626a-45a2-84ce-e306b9c23108	doctor@test.com	$2a$10$nJu9zspotzE.NKFcryH.KuOzrHyKNrLC7IN06Pwe5.wOGbP3MMHAy	ahmed	adel	\N	UTC	\N	\N	t	2026-02-09 19:37:49.929387	f	2026-01-22 15:12:17.683385	2026-02-14 03:43:25.157999	\N	en	active	2026-01-27 17:07:46.08543	f	0	\N	\N
66574cc8-3141-4468-8b4f-0007ca0cedd0	patient@test.com	$2a$10$X6uyO7JlEnEoeFhV0xnLw.ArCaVCVenNtseDAKK.x4WpAyGhM76XS	abdallah	ahmed	\N	UTC	\N	\N	t	2026-02-14 15:26:37.97625	f	2026-01-22 15:12:48.680639	2026-02-14 13:28:16.088105	\N	en	active	2026-01-27 17:07:46.08543	f	0	\N	\N
d586a997-039e-48d2-81ed-cb1b695a7631	neuralback@gmail.com	$2a$10$Vb4/67Kf5hBbv33B1Z4Tpuw4wtDLMRDEHMhQxn/8giwAqLRRWVfbi	Dr. Sarah	Johnson	\N	UTC	2026-02-14 16:02:33.866094	\N	t	2026-02-14 16:03:00.946725	f	2026-02-14 06:30:29.044575	2026-02-14 14:04:39.225097	\N	en	active	2026-02-14 06:30:29.017792	f	0	\N	2026-02-14 16:01:42.811495
7b4b4593-cd73-4ce8-aa28-cf091d3e9cd3	muhamedabdelraheem22@gmail.com	$2a$10$eO.O4M8zMoXXIf2k71ZuU.YAD8wtqCkip354RhhsS5pbG34vs.nfq	Muhammed 	Ashraf	\N	UTC	2026-02-14 21:22:01.743544	\N	t	2026-02-14 21:22:32.515954	f	2026-02-14 21:21:05.076905	2026-02-14 19:22:32.207549	\N	en	active	2026-02-14 21:21:05.073997	f	0	\N	2026-02-14 21:21:07.400514
88c1dd52-e4e8-4b15-a482-d7e4fdef0d2e	abdallah.araafat@gmail.com	$2a$10$0HnTjQNYTiOp41D0DxYKt.GsktNrLgdaU9D2ncZAFsuyeS/dsgEiK	Abdallah	Ahmed	\N	UTC	2026-02-14 18:48:08.874747	\N	t	2026-02-14 22:43:29.055564	f	2026-02-14 18:47:31.192003	2026-02-14 20:43:28.428836	\N	en	active	2026-02-14 18:47:31.187636	f	0	\N	2026-02-14 18:47:33.833772
e460d3b6-b6a7-44ed-8b48-bdad9ed143b3	abdallahsport1@gmail.com	$2a$10$FbP6g0zMU/yggXpmOvqUIuKI1UwQbZPPXIxz.Eizdr9HWI9l2suNa	Abdallah	Ahmed	\N	UTC	2026-02-14 18:50:06.823953	\N	t	2026-02-14 22:52:02.412509	f	2026-02-14 18:49:40.472122	2026-02-14 20:52:02.088731	\N	en	active	2026-02-14 18:49:40.471771	f	0	\N	2026-02-14 18:49:42.770652
5b730865-5196-4455-a3cd-05b4dfd3e744	karima.ahmed93@hotmail.com	$2a$10$sPA4A7ojbjz.hSB5SXOpzuXtXDp1Mus.iFYzOwMh.QO5OQt2wge.C	Karima	Ahmed	\N	UTC	\N	\N	t	\N	f	2026-02-14 20:57:14.482465	2026-02-14 18:57:14.824755	\N	en	active	2026-02-14 20:57:14.482035	t	0	\N	2026-02-14 20:57:17.099395
\.


--
-- TOC entry 5522 (class 0 OID 0)
-- Dependencies: 233
-- Name: engagement_id_seq; Type: SEQUENCE SET; Schema: public; Owner: postgres
--

SELECT pg_catalog.setval('public.engagement_id_seq', 13, true);


--
-- TOC entry 5145 (class 2606 OID 35079)
-- Name: active_service_subscriptions active_service_subscriptions_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.active_service_subscriptions
    ADD CONSTRAINT active_service_subscriptions_pkey PRIMARY KEY (id);


--
-- TOC entry 5147 (class 2606 OID 35081)
-- Name: ai_chat_messages ai_chat_messages_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.ai_chat_messages
    ADD CONSTRAINT ai_chat_messages_pkey PRIMARY KEY (id);


--
-- TOC entry 5151 (class 2606 OID 35083)
-- Name: ai_chat_sessions ai_chat_sessions_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.ai_chat_sessions
    ADD CONSTRAINT ai_chat_sessions_pkey PRIMARY KEY (id);


--
-- TOC entry 5156 (class 2606 OID 35085)
-- Name: audit_log audit_log_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.audit_log
    ADD CONSTRAINT audit_log_pkey PRIMARY KEY (id);


--
-- TOC entry 5160 (class 2606 OID 35087)
-- Name: doctor_patients doctor_patients_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.doctor_patients
    ADD CONSTRAINT doctor_patients_pkey PRIMARY KEY (id);


--
-- TOC entry 5168 (class 2606 OID 35089)
-- Name: doctor_profiles doctor_profiles_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.doctor_profiles
    ADD CONSTRAINT doctor_profiles_pkey PRIMARY KEY (id);


--
-- TOC entry 5178 (class 2606 OID 35091)
-- Name: doctor_reviews doctor_reviews_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.doctor_reviews
    ADD CONSTRAINT doctor_reviews_pkey PRIMARY KEY (id);


--
-- TOC entry 5180 (class 2606 OID 35093)
-- Name: doctor_verification_questions doctor_verification_questions_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.doctor_verification_questions
    ADD CONSTRAINT doctor_verification_questions_pkey PRIMARY KEY (id);


--
-- TOC entry 5186 (class 2606 OID 35095)
-- Name: email_verification_otps email_verification_otps_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.email_verification_otps
    ADD CONSTRAINT email_verification_otps_pkey PRIMARY KEY (id);


--
-- TOC entry 5191 (class 2606 OID 35097)
-- Name: engagement_access_rules engagement_access_rules_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.engagement_access_rules
    ADD CONSTRAINT engagement_access_rules_pkey PRIMARY KEY (id);


--
-- TOC entry 5193 (class 2606 OID 35099)
-- Name: engagement_access_rules engagement_access_rules_rule_name_key; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.engagement_access_rules
    ADD CONSTRAINT engagement_access_rules_rule_name_key UNIQUE (rule_name);


--
-- TOC entry 5195 (class 2606 OID 35101)
-- Name: engagement_analytics engagement_analytics_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.engagement_analytics
    ADD CONSTRAINT engagement_analytics_pkey PRIMARY KEY (id);


--
-- TOC entry 5197 (class 2606 OID 35103)
-- Name: engagement_events engagement_events_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.engagement_events
    ADD CONSTRAINT engagement_events_pkey PRIMARY KEY (id);


--
-- TOC entry 5199 (class 2606 OID 35105)
-- Name: engagement_messages engagement_messages_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.engagement_messages
    ADD CONSTRAINT engagement_messages_pkey PRIMARY KEY (id);


--
-- TOC entry 5204 (class 2606 OID 35107)
-- Name: engagement_sessions engagement_sessions_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.engagement_sessions
    ADD CONSTRAINT engagement_sessions_pkey PRIMARY KEY (id);


--
-- TOC entry 5206 (class 2606 OID 35109)
-- Name: engagement_sessions engagement_sessions_session_token_key; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.engagement_sessions
    ADD CONSTRAINT engagement_sessions_session_token_key UNIQUE (session_token);


--
-- TOC entry 5208 (class 2606 OID 35111)
-- Name: engagement_verification_tokens engagement_verification_tokens_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.engagement_verification_tokens
    ADD CONSTRAINT engagement_verification_tokens_pkey PRIMARY KEY (id);


--
-- TOC entry 5210 (class 2606 OID 35113)
-- Name: engagement_verification_tokens engagement_verification_tokens_token_key; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.engagement_verification_tokens
    ADD CONSTRAINT engagement_verification_tokens_token_key UNIQUE (token);


--
-- TOC entry 5212 (class 2606 OID 35115)
-- Name: engagements engagements_engagement_id_key; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.engagements
    ADD CONSTRAINT engagements_engagement_id_key UNIQUE (engagement_id);


--
-- TOC entry 5214 (class 2606 OID 35117)
-- Name: engagements engagements_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.engagements
    ADD CONSTRAINT engagements_pkey PRIMARY KEY (id);


--
-- TOC entry 5223 (class 2606 OID 35119)
-- Name: message_queues message_queues_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.message_queues
    ADD CONSTRAINT message_queues_pkey PRIMARY KEY (id);


--
-- TOC entry 5226 (class 2606 OID 35121)
-- Name: notification_message_templates notification_message_templates_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.notification_message_templates
    ADD CONSTRAINT notification_message_templates_pkey PRIMARY KEY (id);


--
-- TOC entry 5230 (class 2606 OID 35123)
-- Name: notification_templates notification_templates_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.notification_templates
    ADD CONSTRAINT notification_templates_pkey PRIMARY KEY (id);


--
-- TOC entry 5232 (class 2606 OID 35125)
-- Name: notification_templates notification_templates_type_key; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.notification_templates
    ADD CONSTRAINT notification_templates_type_key UNIQUE (type);


--
-- TOC entry 5239 (class 2606 OID 35127)
-- Name: notifications notifications_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.notifications
    ADD CONSTRAINT notifications_pkey PRIMARY KEY (id);


--
-- TOC entry 5242 (class 2606 OID 35129)
-- Name: patient_profiles patient_profiles_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.patient_profiles
    ADD CONSTRAINT patient_profiles_pkey PRIMARY KEY (id);


--
-- TOC entry 5244 (class 2606 OID 35131)
-- Name: platform_analytics platform_analytics_analytics_date_key; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.platform_analytics
    ADD CONSTRAINT platform_analytics_analytics_date_key UNIQUE (analytics_date);


--
-- TOC entry 5246 (class 2606 OID 35133)
-- Name: platform_analytics platform_analytics_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.platform_analytics
    ADD CONSTRAINT platform_analytics_pkey PRIMARY KEY (id);


--
-- TOC entry 5250 (class 2606 OID 35135)
-- Name: security_authentication_tokens security_authentication_tokens_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.security_authentication_tokens
    ADD CONSTRAINT security_authentication_tokens_pkey PRIMARY KEY (id);


--
-- TOC entry 5252 (class 2606 OID 35137)
-- Name: security_authentication_tokens security_authentication_tokens_token_key; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.security_authentication_tokens
    ADD CONSTRAINT security_authentication_tokens_token_key UNIQUE (token);


--
-- TOC entry 5254 (class 2606 OID 35139)
-- Name: system_settings system_settings_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.system_settings
    ADD CONSTRAINT system_settings_pkey PRIMARY KEY (id);


--
-- TOC entry 5256 (class 2606 OID 35141)
-- Name: system_settings system_settings_setting_key_key; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.system_settings
    ADD CONSTRAINT system_settings_setting_key_key UNIQUE (setting_key);


--
-- TOC entry 5166 (class 2606 OID 35143)
-- Name: doctor_patients unique_doctor_patient; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.doctor_patients
    ADD CONSTRAINT unique_doctor_patient UNIQUE (doctor_id, patient_id);


--
-- TOC entry 5184 (class 2606 OID 35145)
-- Name: doctor_verification_questions unique_doctor_question; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.doctor_verification_questions
    ADD CONSTRAINT unique_doctor_question UNIQUE (doctor_id, question_key);


--
-- TOC entry 5228 (class 2606 OID 35147)
-- Name: notification_message_templates unique_template_lang_context; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.notification_message_templates
    ADD CONSTRAINT unique_template_lang_context UNIQUE (template_key, language_code, recipient_context);


--
-- TOC entry 5258 (class 2606 OID 35149)
-- Name: url_shortcuts url_shortcuts_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.url_shortcuts
    ADD CONSTRAINT url_shortcuts_pkey PRIMARY KEY (id);


--
-- TOC entry 5260 (class 2606 OID 35151)
-- Name: url_shortcuts url_shortcuts_short_code_key; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.url_shortcuts
    ADD CONSTRAINT url_shortcuts_short_code_key UNIQUE (short_code);


--
-- TOC entry 5262 (class 2606 OID 35153)
-- Name: user_management_metrics user_management_metrics_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.user_management_metrics
    ADD CONSTRAINT user_management_metrics_pkey PRIMARY KEY (id);


--
-- TOC entry 5269 (class 2606 OID 35155)
-- Name: users users_email_key; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.users
    ADD CONSTRAINT users_email_key UNIQUE (email);


--
-- TOC entry 5271 (class 2606 OID 35157)
-- Name: users users_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.users
    ADD CONSTRAINT users_pkey PRIMARY KEY (id);


--
-- TOC entry 5148 (class 1259 OID 35158)
-- Name: idx_ai_chat_messages_created_at; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_ai_chat_messages_created_at ON public.ai_chat_messages USING btree (created_at);


--
-- TOC entry 5149 (class 1259 OID 35159)
-- Name: idx_ai_chat_messages_session_id; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_ai_chat_messages_session_id ON public.ai_chat_messages USING btree (session_id);


--
-- TOC entry 5152 (class 1259 OID 35160)
-- Name: idx_ai_chat_sessions_is_active; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_ai_chat_sessions_is_active ON public.ai_chat_sessions USING btree (is_active);


--
-- TOC entry 5153 (class 1259 OID 35161)
-- Name: idx_ai_chat_sessions_patient_id; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_ai_chat_sessions_patient_id ON public.ai_chat_sessions USING btree (patient_id);


--
-- TOC entry 5154 (class 1259 OID 35162)
-- Name: idx_ai_chat_sessions_patient_started; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_ai_chat_sessions_patient_started ON public.ai_chat_sessions USING btree (patient_id, started_at);


--
-- TOC entry 5157 (class 1259 OID 35163)
-- Name: idx_audit_log_created_at; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_audit_log_created_at ON public.audit_log USING btree (created_at);


--
-- TOC entry 5158 (class 1259 OID 35164)
-- Name: idx_audit_log_user_id; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_audit_log_user_id ON public.audit_log USING btree (user_id);


--
-- TOC entry 5247 (class 1259 OID 35165)
-- Name: idx_auth_tokens_expires_at; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_auth_tokens_expires_at ON public.security_authentication_tokens USING btree (expires_at);


--
-- TOC entry 5248 (class 1259 OID 35166)
-- Name: idx_auth_tokens_user_id; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_auth_tokens_user_id ON public.security_authentication_tokens USING btree (user_id);


--
-- TOC entry 5169 (class 1259 OID 35167)
-- Name: idx_doctor_availability; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_doctor_availability ON public.doctor_profiles USING btree (availability_status);


--
-- TOC entry 5170 (class 1259 OID 35168)
-- Name: idx_doctor_coordinates; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_doctor_coordinates ON public.doctor_profiles USING btree (latitude, longitude) WHERE ((latitude IS NOT NULL) AND (longitude IS NOT NULL));


--
-- TOC entry 5523 (class 0 OID 0)
-- Dependencies: 5170
-- Name: INDEX idx_doctor_coordinates; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON INDEX public.idx_doctor_coordinates IS 'Optimizes geolocation-based doctor searches';


--
-- TOC entry 5171 (class 1259 OID 35169)
-- Name: idx_doctor_location; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_doctor_location ON public.doctor_profiles USING btree (location_city, location_country);


--
-- TOC entry 5161 (class 1259 OID 35170)
-- Name: idx_doctor_patients_current_engagement; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_doctor_patients_current_engagement ON public.doctor_patients USING btree (current_engagement_id);


--
-- TOC entry 5162 (class 1259 OID 35171)
-- Name: idx_doctor_patients_doctor_id; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_doctor_patients_doctor_id ON public.doctor_patients USING btree (doctor_id);


--
-- TOC entry 5163 (class 1259 OID 35172)
-- Name: idx_doctor_patients_is_active; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_doctor_patients_is_active ON public.doctor_patients USING btree (is_active);


--
-- TOC entry 5164 (class 1259 OID 35173)
-- Name: idx_doctor_patients_patient_id; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_doctor_patients_patient_id ON public.doctor_patients USING btree (patient_id);


--
-- TOC entry 5172 (class 1259 OID 35174)
-- Name: idx_doctor_profiles_user_id; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_doctor_profiles_user_id ON public.doctor_profiles USING btree (user_id);


--
-- TOC entry 5173 (class 1259 OID 35175)
-- Name: idx_doctor_profiles_verified; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_doctor_profiles_verified ON public.doctor_profiles USING btree (is_verified);


--
-- TOC entry 5174 (class 1259 OID 35176)
-- Name: idx_doctor_rating; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_doctor_rating ON public.doctor_profiles USING btree (rating DESC);


--
-- TOC entry 5175 (class 1259 OID 35177)
-- Name: idx_doctor_specialization; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_doctor_specialization ON public.doctor_profiles USING btree (specialization);


--
-- TOC entry 5176 (class 1259 OID 35178)
-- Name: idx_doctor_verification_status; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_doctor_verification_status ON public.doctor_profiles USING btree (verification_status);


--
-- TOC entry 5200 (class 1259 OID 35179)
-- Name: idx_engagement_messages_created_at; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_engagement_messages_created_at ON public.engagement_messages USING btree (created_at);


--
-- TOC entry 5201 (class 1259 OID 35180)
-- Name: idx_engagement_messages_engagement_id; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_engagement_messages_engagement_id ON public.engagement_messages USING btree (engagement_id);


--
-- TOC entry 5202 (class 1259 OID 35181)
-- Name: idx_engagement_messages_sender; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_engagement_messages_sender ON public.engagement_messages USING btree (sender_id);


--
-- TOC entry 5215 (class 1259 OID 35182)
-- Name: idx_engagements_dates; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_engagements_dates ON public.engagements USING btree (start_at, end_at);


--
-- TOC entry 5216 (class 1259 OID 35183)
-- Name: idx_engagements_doctor_id; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_engagements_doctor_id ON public.engagements USING btree (doctor_id);


--
-- TOC entry 5217 (class 1259 OID 35184)
-- Name: idx_engagements_doctor_patient_status; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_engagements_doctor_patient_status ON public.engagements USING btree (doctor_id, patient_id, status) WHERE (status = ANY (ARRAY['active'::public.engagement_status, 'pending'::public.engagement_status]));


--
-- TOC entry 5218 (class 1259 OID 35185)
-- Name: idx_engagements_initiated_by; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_engagements_initiated_by ON public.engagements USING btree (initiated_by);


--
-- TOC entry 5219 (class 1259 OID 35186)
-- Name: idx_engagements_patient_id; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_engagements_patient_id ON public.engagements USING btree (patient_id);


--
-- TOC entry 5220 (class 1259 OID 35187)
-- Name: idx_engagements_status; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_engagements_status ON public.engagements USING btree (status);


--
-- TOC entry 5224 (class 1259 OID 35188)
-- Name: idx_message_templates_key_lang; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_message_templates_key_lang ON public.notification_message_templates USING btree (template_key, language_code);


--
-- TOC entry 5233 (class 1259 OID 35189)
-- Name: idx_notifications_is_read; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_notifications_is_read ON public.notifications USING btree (is_read);


--
-- TOC entry 5234 (class 1259 OID 35190)
-- Name: idx_notifications_priority; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_notifications_priority ON public.notifications USING btree (priority, sent_at DESC);


--
-- TOC entry 5235 (class 1259 OID 35191)
-- Name: idx_notifications_unpushed; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_notifications_unpushed ON public.notifications USING btree (user_id, sent_at) WHERE (((delivery_status ->> 'sse'::text))::boolean = false);


--
-- TOC entry 5236 (class 1259 OID 35192)
-- Name: idx_notifications_user_id; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_notifications_user_id ON public.notifications USING btree (user_id);


--
-- TOC entry 5237 (class 1259 OID 35193)
-- Name: idx_notifications_user_sentat; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_notifications_user_sentat ON public.notifications USING btree (user_id, sent_at);


--
-- TOC entry 5187 (class 1259 OID 35194)
-- Name: idx_otp_code; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_otp_code ON public.email_verification_otps USING btree (otp_code);


--
-- TOC entry 5188 (class 1259 OID 35195)
-- Name: idx_otp_expires_at; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_otp_expires_at ON public.email_verification_otps USING btree (expires_at);


--
-- TOC entry 5189 (class 1259 OID 35196)
-- Name: idx_otp_user_id; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_otp_user_id ON public.email_verification_otps USING btree (user_id);


--
-- TOC entry 5240 (class 1259 OID 35197)
-- Name: idx_patient_profiles_user_id; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_patient_profiles_user_id ON public.patient_profiles USING btree (user_id);


--
-- TOC entry 5221 (class 1259 OID 35198)
-- Name: idx_queue_pending; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_queue_pending ON public.message_queues USING btree (job_type, status, created_at) WHERE (status = 'pending'::public.job_status);


--
-- TOC entry 5263 (class 1259 OID 35199)
-- Name: idx_users_activity_status; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_users_activity_status ON public.users USING btree (activity_status, last_login_at);


--
-- TOC entry 5264 (class 1259 OID 35200)
-- Name: idx_users_deleted_at; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_users_deleted_at ON public.users USING btree (deleted_at);


--
-- TOC entry 5265 (class 1259 OID 35201)
-- Name: idx_users_email; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_users_email ON public.users USING btree (email);


--
-- TOC entry 5266 (class 1259 OID 35202)
-- Name: idx_users_is_active; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_users_is_active ON public.users USING btree (is_active);


--
-- TOC entry 5267 (class 1259 OID 35203)
-- Name: idx_users_language; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_users_language ON public.users USING btree (language);


--
-- TOC entry 5181 (class 1259 OID 35204)
-- Name: idx_verification_questions_doctor; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_verification_questions_doctor ON public.doctor_verification_questions USING btree (doctor_id);


--
-- TOC entry 5182 (class 1259 OID 35205)
-- Name: idx_verification_questions_verified; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_verification_questions_verified ON public.doctor_verification_questions USING btree (verified_at);


--
-- TOC entry 5317 (class 2620 OID 35206)
-- Name: engagements engagement_notification_trigger; Type: TRIGGER; Schema: public; Owner: postgres
--

CREATE TRIGGER engagement_notification_trigger AFTER UPDATE OF status ON public.engagements FOR EACH ROW EXECUTE FUNCTION public.create_engagement_notification();


--
-- TOC entry 5318 (class 2620 OID 35207)
-- Name: engagements engagement_status_change; Type: TRIGGER; Schema: public; Owner: postgres
--

CREATE TRIGGER engagement_status_change AFTER INSERT OR UPDATE OF status ON public.engagements FOR EACH ROW EXECUTE FUNCTION public.update_relationship_status_on_engagement();


--
-- TOC entry 5311 (class 2620 OID 35208)
-- Name: doctor_patients relationship_access_change; Type: TRIGGER; Schema: public; Owner: postgres
--

CREATE TRIGGER relationship_access_change AFTER UPDATE OF relationship_status ON public.doctor_patients FOR EACH ROW WHEN (((old.relationship_status)::text IS DISTINCT FROM (new.relationship_status)::text)) EXECUTE FUNCTION public.notify_access_rule_change();


--
-- TOC entry 5319 (class 2620 OID 35209)
-- Name: engagements set_engagement_id; Type: TRIGGER; Schema: public; Owner: postgres
--

CREATE TRIGGER set_engagement_id BEFORE INSERT ON public.engagements FOR EACH ROW EXECUTE FUNCTION public.generate_engagement_id();


--
-- TOC entry 5321 (class 2620 OID 35210)
-- Name: notifications trg_auto_queue_email; Type: TRIGGER; Schema: public; Owner: postgres
--

CREATE TRIGGER trg_auto_queue_email AFTER INSERT ON public.notifications FOR EACH ROW EXECUTE FUNCTION public.trigger_queue_email_job();


--
-- TOC entry 5315 (class 2620 OID 35211)
-- Name: doctor_reviews trg_update_doctor_rating; Type: TRIGGER; Schema: public; Owner: postgres
--

CREATE TRIGGER trg_update_doctor_rating AFTER INSERT OR DELETE OR UPDATE ON public.doctor_reviews FOR EACH ROW EXECUTE FUNCTION public.update_doctor_rating();


--
-- TOC entry 5313 (class 2620 OID 35212)
-- Name: doctor_profiles trg_update_profile_completion; Type: TRIGGER; Schema: public; Owner: postgres
--

CREATE TRIGGER trg_update_profile_completion BEFORE INSERT OR UPDATE ON public.doctor_profiles FOR EACH ROW EXECUTE FUNCTION public.update_profile_completion();


--
-- TOC entry 5309 (class 2620 OID 35213)
-- Name: active_service_subscriptions update_active_service_subscriptions_updated_at; Type: TRIGGER; Schema: public; Owner: postgres
--

CREATE TRIGGER update_active_service_subscriptions_updated_at BEFORE UPDATE ON public.active_service_subscriptions FOR EACH ROW EXECUTE FUNCTION public.update_updated_at_column();


--
-- TOC entry 5310 (class 2620 OID 35214)
-- Name: ai_chat_sessions update_ai_chat_sessions_updated_at; Type: TRIGGER; Schema: public; Owner: postgres
--

CREATE TRIGGER update_ai_chat_sessions_updated_at BEFORE UPDATE ON public.ai_chat_sessions FOR EACH ROW EXECUTE FUNCTION public.update_updated_at_column();


--
-- TOC entry 5312 (class 2620 OID 35215)
-- Name: doctor_patients update_doctor_patients_updated_at; Type: TRIGGER; Schema: public; Owner: postgres
--

CREATE TRIGGER update_doctor_patients_updated_at BEFORE UPDATE ON public.doctor_patients FOR EACH ROW EXECUTE FUNCTION public.update_updated_at_column();


--
-- TOC entry 5314 (class 2620 OID 35216)
-- Name: doctor_profiles update_doctor_profiles_updated_at; Type: TRIGGER; Schema: public; Owner: postgres
--

CREATE TRIGGER update_doctor_profiles_updated_at BEFORE UPDATE ON public.doctor_profiles FOR EACH ROW EXECUTE FUNCTION public.update_updated_at_column();


--
-- TOC entry 5316 (class 2620 OID 35217)
-- Name: engagement_access_rules update_engagement_access_rules_updated_at; Type: TRIGGER; Schema: public; Owner: postgres
--

CREATE TRIGGER update_engagement_access_rules_updated_at BEFORE UPDATE ON public.engagement_access_rules FOR EACH ROW EXECUTE FUNCTION public.update_updated_at_column();


--
-- TOC entry 5320 (class 2620 OID 35218)
-- Name: engagements update_engagements_updated_at; Type: TRIGGER; Schema: public; Owner: postgres
--

CREATE TRIGGER update_engagements_updated_at BEFORE UPDATE ON public.engagements FOR EACH ROW EXECUTE FUNCTION public.update_updated_at_column();


--
-- TOC entry 5322 (class 2620 OID 35219)
-- Name: patient_profiles update_patient_profiles_updated_at; Type: TRIGGER; Schema: public; Owner: postgres
--

CREATE TRIGGER update_patient_profiles_updated_at BEFORE UPDATE ON public.patient_profiles FOR EACH ROW EXECUTE FUNCTION public.update_updated_at_column();


--
-- TOC entry 5323 (class 2620 OID 35220)
-- Name: system_settings update_system_settings_updated_at; Type: TRIGGER; Schema: public; Owner: postgres
--

CREATE TRIGGER update_system_settings_updated_at BEFORE UPDATE ON public.system_settings FOR EACH ROW EXECUTE FUNCTION public.update_updated_at_column();


--
-- TOC entry 5324 (class 2620 OID 35221)
-- Name: users update_users_updated_at; Type: TRIGGER; Schema: public; Owner: postgres
--

CREATE TRIGGER update_users_updated_at BEFORE UPDATE ON public.users FOR EACH ROW EXECUTE FUNCTION public.update_updated_at_column();


--
-- TOC entry 5325 (class 2620 OID 35222)
-- Name: users user_welcome_notification; Type: TRIGGER; Schema: public; Owner: postgres
--

CREATE TRIGGER user_welcome_notification AFTER UPDATE OF email_verified_at ON public.users FOR EACH ROW WHEN (((old.email_verified_at IS NULL) AND (new.email_verified_at IS NOT NULL))) EXECUTE FUNCTION public.user_welcome_notification();


--
-- TOC entry 5272 (class 2606 OID 35223)
-- Name: active_service_subscriptions active_service_subscriptions_user_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.active_service_subscriptions
    ADD CONSTRAINT active_service_subscriptions_user_id_fkey FOREIGN KEY (user_id) REFERENCES public.users(id) ON DELETE CASCADE;


--
-- TOC entry 5273 (class 2606 OID 35228)
-- Name: ai_chat_messages ai_chat_messages_sender_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.ai_chat_messages
    ADD CONSTRAINT ai_chat_messages_sender_id_fkey FOREIGN KEY (sender_id) REFERENCES public.users(id);


--
-- TOC entry 5274 (class 2606 OID 35233)
-- Name: ai_chat_messages ai_chat_messages_session_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.ai_chat_messages
    ADD CONSTRAINT ai_chat_messages_session_id_fkey FOREIGN KEY (session_id) REFERENCES public.ai_chat_sessions(id) ON DELETE CASCADE;


--
-- TOC entry 5275 (class 2606 OID 35238)
-- Name: ai_chat_sessions ai_chat_sessions_patient_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.ai_chat_sessions
    ADD CONSTRAINT ai_chat_sessions_patient_id_fkey FOREIGN KEY (patient_id) REFERENCES public.patient_profiles(id) ON DELETE CASCADE;


--
-- TOC entry 5276 (class 2606 OID 35243)
-- Name: audit_log audit_log_user_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.audit_log
    ADD CONSTRAINT audit_log_user_id_fkey FOREIGN KEY (user_id) REFERENCES public.users(id);


--
-- TOC entry 5277 (class 2606 OID 35248)
-- Name: doctor_patients doctor_patients_current_engagement_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.doctor_patients
    ADD CONSTRAINT doctor_patients_current_engagement_id_fkey FOREIGN KEY (current_engagement_id) REFERENCES public.engagements(id) ON DELETE SET NULL;


--
-- TOC entry 5278 (class 2606 OID 35253)
-- Name: doctor_patients doctor_patients_doctor_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.doctor_patients
    ADD CONSTRAINT doctor_patients_doctor_id_fkey FOREIGN KEY (doctor_id) REFERENCES public.doctor_profiles(id) ON DELETE CASCADE;


--
-- TOC entry 5279 (class 2606 OID 35258)
-- Name: doctor_patients doctor_patients_patient_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.doctor_patients
    ADD CONSTRAINT doctor_patients_patient_id_fkey FOREIGN KEY (patient_id) REFERENCES public.patient_profiles(id) ON DELETE CASCADE;


--
-- TOC entry 5280 (class 2606 OID 35263)
-- Name: doctor_patients doctor_patients_relationship_status_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.doctor_patients
    ADD CONSTRAINT doctor_patients_relationship_status_fkey FOREIGN KEY (relationship_status) REFERENCES public.engagement_access_rules(rule_name);


--
-- TOC entry 5281 (class 2606 OID 35268)
-- Name: doctor_profiles doctor_profiles_user_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.doctor_profiles
    ADD CONSTRAINT doctor_profiles_user_id_fkey FOREIGN KEY (user_id) REFERENCES public.users(id) ON DELETE CASCADE;


--
-- TOC entry 5283 (class 2606 OID 35273)
-- Name: doctor_reviews doctor_reviews_doctor_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.doctor_reviews
    ADD CONSTRAINT doctor_reviews_doctor_id_fkey FOREIGN KEY (doctor_id) REFERENCES public.doctor_profiles(id);


--
-- TOC entry 5284 (class 2606 OID 35278)
-- Name: doctor_reviews doctor_reviews_patient_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.doctor_reviews
    ADD CONSTRAINT doctor_reviews_patient_id_fkey FOREIGN KEY (patient_id) REFERENCES public.patient_profiles(id);


--
-- TOC entry 5285 (class 2606 OID 35283)
-- Name: doctor_verification_questions doctor_verification_questions_doctor_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.doctor_verification_questions
    ADD CONSTRAINT doctor_verification_questions_doctor_id_fkey FOREIGN KEY (doctor_id) REFERENCES public.doctor_profiles(id) ON DELETE CASCADE;


--
-- TOC entry 5287 (class 2606 OID 35288)
-- Name: engagement_analytics engagement_analytics_engagement_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.engagement_analytics
    ADD CONSTRAINT engagement_analytics_engagement_id_fkey FOREIGN KEY (engagement_id) REFERENCES public.engagements(id) ON DELETE CASCADE;


--
-- TOC entry 5288 (class 2606 OID 35293)
-- Name: engagement_events engagement_events_engagement_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.engagement_events
    ADD CONSTRAINT engagement_events_engagement_id_fkey FOREIGN KEY (engagement_id) REFERENCES public.engagements(id) ON DELETE CASCADE;


--
-- TOC entry 5289 (class 2606 OID 35298)
-- Name: engagement_events engagement_events_triggered_by_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.engagement_events
    ADD CONSTRAINT engagement_events_triggered_by_fkey FOREIGN KEY (triggered_by) REFERENCES public.users(id);


--
-- TOC entry 5290 (class 2606 OID 35303)
-- Name: engagement_messages engagement_messages_engagement_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.engagement_messages
    ADD CONSTRAINT engagement_messages_engagement_id_fkey FOREIGN KEY (engagement_id) REFERENCES public.engagements(id) ON DELETE CASCADE;


--
-- TOC entry 5291 (class 2606 OID 35308)
-- Name: engagement_messages engagement_messages_recipient_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.engagement_messages
    ADD CONSTRAINT engagement_messages_recipient_id_fkey FOREIGN KEY (recipient_id) REFERENCES public.users(id);


--
-- TOC entry 5292 (class 2606 OID 35313)
-- Name: engagement_messages engagement_messages_sender_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.engagement_messages
    ADD CONSTRAINT engagement_messages_sender_id_fkey FOREIGN KEY (sender_id) REFERENCES public.users(id);


--
-- TOC entry 5293 (class 2606 OID 35318)
-- Name: engagement_sessions engagement_sessions_engagement_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.engagement_sessions
    ADD CONSTRAINT engagement_sessions_engagement_id_fkey FOREIGN KEY (engagement_id) REFERENCES public.engagements(id) ON DELETE CASCADE;


--
-- TOC entry 5294 (class 2606 OID 35323)
-- Name: engagement_sessions engagement_sessions_user_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.engagement_sessions
    ADD CONSTRAINT engagement_sessions_user_id_fkey FOREIGN KEY (user_id) REFERENCES public.users(id);


--
-- TOC entry 5295 (class 2606 OID 35328)
-- Name: engagement_verification_tokens engagement_verification_tokens_doctor_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.engagement_verification_tokens
    ADD CONSTRAINT engagement_verification_tokens_doctor_id_fkey FOREIGN KEY (doctor_id) REFERENCES public.users(id);


--
-- TOC entry 5296 (class 2606 OID 35333)
-- Name: engagement_verification_tokens engagement_verification_tokens_engagement_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.engagement_verification_tokens
    ADD CONSTRAINT engagement_verification_tokens_engagement_id_fkey FOREIGN KEY (engagement_id) REFERENCES public.engagements(id) ON DELETE CASCADE;


--
-- TOC entry 5297 (class 2606 OID 35338)
-- Name: engagement_verification_tokens engagement_verification_tokens_patient_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.engagement_verification_tokens
    ADD CONSTRAINT engagement_verification_tokens_patient_id_fkey FOREIGN KEY (patient_id) REFERENCES public.users(id);


--
-- TOC entry 5298 (class 2606 OID 35343)
-- Name: engagement_verification_tokens engagement_verification_tokens_verified_by_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.engagement_verification_tokens
    ADD CONSTRAINT engagement_verification_tokens_verified_by_fkey FOREIGN KEY (verified_by) REFERENCES public.users(id);


--
-- TOC entry 5299 (class 2606 OID 35348)
-- Name: engagements engagements_access_rule_name_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.engagements
    ADD CONSTRAINT engagements_access_rule_name_fkey FOREIGN KEY (access_rule_name) REFERENCES public.engagement_access_rules(rule_name);


--
-- TOC entry 5300 (class 2606 OID 35353)
-- Name: engagements engagements_doctor_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.engagements
    ADD CONSTRAINT engagements_doctor_id_fkey FOREIGN KEY (doctor_id) REFERENCES public.doctor_profiles(id) ON DELETE CASCADE;


--
-- TOC entry 5301 (class 2606 OID 35358)
-- Name: engagements engagements_ended_by_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.engagements
    ADD CONSTRAINT engagements_ended_by_fkey FOREIGN KEY (ended_by) REFERENCES public.users(id);


--
-- TOC entry 5302 (class 2606 OID 35363)
-- Name: engagements engagements_patient_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.engagements
    ADD CONSTRAINT engagements_patient_id_fkey FOREIGN KEY (patient_id) REFERENCES public.patient_profiles(id) ON DELETE CASCADE;


--
-- TOC entry 5282 (class 2606 OID 35368)
-- Name: doctor_profiles fk_doctor_user; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.doctor_profiles
    ADD CONSTRAINT fk_doctor_user FOREIGN KEY (user_id) REFERENCES public.users(id) ON DELETE CASCADE;


--
-- TOC entry 5286 (class 2606 OID 35373)
-- Name: email_verification_otps fk_otp_user; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.email_verification_otps
    ADD CONSTRAINT fk_otp_user FOREIGN KEY (user_id) REFERENCES public.users(id) ON DELETE CASCADE;


--
-- TOC entry 5304 (class 2606 OID 35378)
-- Name: patient_profiles fk_patient_user; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.patient_profiles
    ADD CONSTRAINT fk_patient_user FOREIGN KEY (user_id) REFERENCES public.users(id) ON DELETE CASCADE;


--
-- TOC entry 5303 (class 2606 OID 35383)
-- Name: notifications notifications_user_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.notifications
    ADD CONSTRAINT notifications_user_id_fkey FOREIGN KEY (user_id) REFERENCES public.users(id) ON DELETE CASCADE;


--
-- TOC entry 5305 (class 2606 OID 35388)
-- Name: patient_profiles patient_profiles_user_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.patient_profiles
    ADD CONSTRAINT patient_profiles_user_id_fkey FOREIGN KEY (user_id) REFERENCES public.users(id) ON DELETE CASCADE;


--
-- TOC entry 5306 (class 2606 OID 35393)
-- Name: security_authentication_tokens security_authentication_tokens_user_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.security_authentication_tokens
    ADD CONSTRAINT security_authentication_tokens_user_id_fkey FOREIGN KEY (user_id) REFERENCES public.users(id) ON DELETE CASCADE;


--
-- TOC entry 5307 (class 2606 OID 35398)
-- Name: url_shortcuts url_shortcuts_user_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.url_shortcuts
    ADD CONSTRAINT url_shortcuts_user_id_fkey FOREIGN KEY (user_id) REFERENCES public.users(id) ON DELETE CASCADE;


--
-- TOC entry 5308 (class 2606 OID 35403)
-- Name: user_management_metrics user_management_metrics_user_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.user_management_metrics
    ADD CONSTRAINT user_management_metrics_user_id_fkey FOREIGN KEY (user_id) REFERENCES public.users(id) ON DELETE CASCADE;


-- Completed on 2026-02-15 01:17:43

--
-- PostgreSQL database dump complete
--

\unrestrict icvtM6AeHlKx1vIbknrBqzJyQd4zVffgge51DnfTbjnpPJfe8tAs5cwjQbqaq4h

