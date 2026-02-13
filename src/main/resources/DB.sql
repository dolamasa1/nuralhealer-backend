-- ================================================================
-- NEURALHEALER - COMPLETE DATABASE SCHEMA
-- PostgreSQL Implementation
-- Healthcare Platform with AI-Powered Patient Care
-- ================================================================

-- Create the database
CREATE DATABASE neuralhealer
    WITH 
    ENCODING = 'UTF8'
    LC_COLLATE = 'en_US.UTF-8'
    LC_CTYPE = 'en_US.UTF-8'
    TEMPLATE = template0;

-- Connect to the database
--\c neuralhealer

-- Enable required extensions
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

COMMENT ON DATABASE neuralhealer IS 'NeuralHealer - AI-Powered Healthcare Platform Database';

-- ================================================================
-- ENUMS
-- ================================================================

CREATE TYPE engagement_status AS ENUM ('pending', 'active', 'ended', 'archived', 'cancelled');
CREATE TYPE verification_type AS ENUM ('start', 'end');
CREATE TYPE token_status AS ENUM ('pending', 'verified', 'expired', 'cancelled');
CREATE TYPE subscription_status AS ENUM ('active', 'expired', 'cancelled', 'pending');
CREATE TYPE chat_sender_type AS ENUM ('patient', 'ai');
CREATE TYPE job_status AS ENUM ('pending', 'processing', 'completed', 'failed', 'retry');
CREATE TYPE notification_source AS ENUM ('engagement', 'message', 'system', 'ai', 'reminder', 'admin');

CREATE TYPE engagement_status AS ENUM (
  'pending',
  'active',
  'ended',
  'archived',
  'cancelled'
);

CREATE TYPE verification_type AS ENUM (
  'start',
  'end'
);

CREATE TYPE token_status AS ENUM (
  'pending',
  'verified',
  'expired',
  'cancelled'
);

CREATE TYPE subscription_status AS ENUM (
  'active',
  'expired',
  'cancelled',
  'pending'
);

CREATE TYPE chat_sender_type AS ENUM (
  'patient',
  'ai'
);

CREATE TYPE job_status AS ENUM (
  'pending',
  'processing',
  'completed',
  'failed',
  'retry'
);

CREATE TYPE notification_source AS ENUM (
  'engagement',
  'message',
  'system',
  'ai',
  'reminder',
  'admin'
);

-- ================================================================
-- 1. ENGAGEMENT RULES SYSTEM
-- ================================================================

CREATE TABLE engagement_access_rules (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  rule_name VARCHAR(255) UNIQUE NOT NULL,
  can_view_all_history BOOLEAN DEFAULT false,
  can_view_current_only BOOLEAN DEFAULT true,
  can_view_patient_profile BOOLEAN DEFAULT true,
  can_modify_notes BOOLEAN DEFAULT true,
  can_message_patient BOOLEAN DEFAULT true,
  retains_period_access BOOLEAN DEFAULT false,
  retains_history_access BOOLEAN DEFAULT false,
  retains_no_access BOOLEAN DEFAULT true,
  description TEXT,
  is_active BOOLEAN DEFAULT true,
  created_at TIMESTAMP DEFAULT now(),
  updated_at TIMESTAMP DEFAULT now()
);

CREATE TABLE engagement_access_rules (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  rule_name VARCHAR(255) UNIQUE NOT NULL,
  
  -- View permissions
  can_view_all_history BOOLEAN DEFAULT false,
  can_view_current_only BOOLEAN DEFAULT true,
  can_view_patient_profile BOOLEAN DEFAULT true,
  can_modify_notes BOOLEAN DEFAULT true,
  can_message_patient BOOLEAN DEFAULT true,
  
  -- Retention after engagement ends
  retains_period_access BOOLEAN DEFAULT false,
  retains_history_access BOOLEAN DEFAULT false,
  retains_no_access BOOLEAN DEFAULT true,
  
  description TEXT,
  is_active BOOLEAN DEFAULT true,
  
  created_at TIMESTAMP DEFAULT now(),
  updated_at TIMESTAMP DEFAULT now()
);

-- ================================================================
-- 2. USERS (Base table)
-- ================================================================

CREATE TABLE users (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  email VARCHAR(255) UNIQUE NOT NULL,
  password_hash VARCHAR(255) NOT NULL,
  first_name VARCHAR(100) NOT NULL,
  last_name VARCHAR(100) NOT NULL,
  phone VARCHAR(20),
  timezone VARCHAR(50) DEFAULT 'UTC',
  email_verified_at TIMESTAMP,
  phone_verified_at TIMESTAMP,
  is_active BOOLEAN DEFAULT true,
  last_login_at TIMESTAMP,
  mfa_enabled BOOLEAN DEFAULT false,
  
  created_at TIMESTAMP DEFAULT now(),
  updated_at TIMESTAMP DEFAULT now(),
  deleted_at TIMESTAMP
);

-- ================================================================
-- 3. PROFILE TABLES
-- ================================================================

CREATE TABLE doctor_profiles (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,

  title VARCHAR(100),
  bio TEXT,
  specialization VARCHAR(100),                    -- e.g. 'Psychiatrist', 'Therapist'
  years_of_experience INTEGER,
  certificates JSONB,

  location_city VARCHAR(100),
  location_country VARCHAR(100),
  latitude DECIMAL(10,8),
  longitude DECIMAL(11,8),

  -- Enhanced fields from your update
  profile_picture_path VARCHAR(500),
  verification_status VARCHAR(50) DEFAULT 'unverified', -- unverified, pending, verified
  availability_status VARCHAR(50) DEFAULT 'offline',    -- online, offline, busy
  rating DECIMAL(3,2) DEFAULT 0.00,
  total_reviews INTEGER DEFAULT 0,
  profile_completion_percentage INTEGER DEFAULT 0,
  social_media JSONB,                                   -- {linkedin, twitter, ...}
  consultation_fee DECIMAL(10,2),

  created_at TIMESTAMP DEFAULT now(),
  updated_at TIMESTAMP DEFAULT now(),

  CONSTRAINT fk_doctor_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE TABLE patient_profiles (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  date_of_birth DATE,
  gender VARCHAR(20),
  emergency_contact VARCHAR(255),
  primary_health_concerns JSONB,
  medical_history JSONB,
  notes TEXT,
  
  created_at TIMESTAMP DEFAULT now(),
  updated_at TIMESTAMP DEFAULT now(),
  
  CONSTRAINT fk_patient_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- ================================================================
-- 4. NEW: DOCTOR REVIEWS & VERIFICATION
-- ================================================================

CREATE TABLE doctor_reviews (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  doctor_id UUID NOT NULL REFERENCES doctor_profiles(id) ON DELETE CASCADE,
  patient_id UUID NOT NULL REFERENCES patient_profiles(id) ON DELETE CASCADE,
  rating INTEGER CHECK (rating >= 1 AND rating <= 5),
  comment TEXT,
  created_at TIMESTAMP DEFAULT NOW()
);

CREATE TABLE doctor_verification_questions (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  doctor_id UUID NOT NULL REFERENCES doctor_profiles(id) ON DELETE CASCADE,
  question_key VARCHAR(100) NOT NULL,
  answer TEXT,
  verified_at TIMESTAMP,
  created_at TIMESTAMP DEFAULT NOW(),
  updated_at TIMESTAMP DEFAULT NOW(),
  CONSTRAINT unique_doctor_question UNIQUE (doctor_id, question_key)
);

-- ================================================================
-- 4. ENGAGEMENTS
-- ================================================================

CREATE TABLE engagements (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  engagement_id VARCHAR(100) UNIQUE,
  doctor_id UUID NOT NULL REFERENCES doctor_profiles(id) ON DELETE CASCADE,
  patient_id UUID NOT NULL REFERENCES patient_profiles(id) ON DELETE CASCADE,
  access_rule_name VARCHAR(255) NOT NULL REFERENCES engagement_access_rules(rule_name),
  
  -- Track who initiated the engagement (doctor or patient)
  initiated_by VARCHAR(10) NOT NULL DEFAULT 'doctor',
  
  status engagement_status DEFAULT 'pending',
  engagement_type VARCHAR(50),
  start_at TIMESTAMP,
  end_at TIMESTAMP,
  
  ended_by UUID REFERENCES users(id),
  termination_reason TEXT,
  
  start_verified_at TIMESTAMP,
  end_verified_at TIMESTAMP,
  
  notes TEXT,
  
  created_at TIMESTAMP DEFAULT now(),
  updated_at TIMESTAMP DEFAULT now(),
  
  -- Constraint to ensure initiated_by is valid
  CONSTRAINT check_initiated_by CHECK (initiated_by IN ('doctor', 'patient'))
);


-- ================================================================
-- 5. DOCTOR-PATIENT RELATIONSHIPS
-- ================================================================

CREATE TABLE doctor_patients (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  doctor_id UUID NOT NULL REFERENCES doctor_profiles(id) ON DELETE CASCADE,
  patient_id UUID NOT NULL REFERENCES patient_profiles(id) ON DELETE CASCADE,
  relationship_status VARCHAR(255) REFERENCES engagement_access_rules(rule_name),
  current_engagement_id UUID REFERENCES engagements(id) ON DELETE SET NULL,
  
  added_at TIMESTAMP DEFAULT now(),
  relationship_started_at TIMESTAMP,
  relationship_ended_at TIMESTAMP,
  is_active BOOLEAN DEFAULT true,
  
  created_at TIMESTAMP DEFAULT now(),
  updated_at TIMESTAMP DEFAULT now(),
  
  CONSTRAINT unique_doctor_patient UNIQUE (doctor_id, patient_id)
);

-- ================================================================
-- 6. ENGAGEMENT 2FA VERIFICATION
-- ================================================================

CREATE TABLE engagement_verification_tokens (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  engagement_id UUID NOT NULL REFERENCES engagements(id) ON DELETE CASCADE,
  token VARCHAR(255) UNIQUE NOT NULL,
  verification_type verification_type,
  qr_code_data TEXT,
  doctor_id UUID REFERENCES users(id),
  patient_id UUID REFERENCES users(id),
  verified_by UUID REFERENCES users(id),
  verified_at TIMESTAMP,
  expires_at TIMESTAMP NOT NULL,
  status token_status DEFAULT 'pending',
  created_at TIMESTAMP DEFAULT now()
);

-- ================================================================
-- 7. ENGAGEMENT SUB-TABLES
-- ================================================================

CREATE TABLE engagement_messages (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  engagement_id UUID NOT NULL REFERENCES engagements(id) ON DELETE CASCADE,
  message_uuid UUID DEFAULT gen_random_uuid(),
  
  sender_id UUID REFERENCES users(id),
  recipient_id UUID REFERENCES users(id),
  
  content TEXT,
  content_type VARCHAR(50) DEFAULT 'text',
  
  sent_at TIMESTAMP DEFAULT now(),
  delivered_at TIMESTAMP,
  read_at TIMESTAMP,
  
  is_encrypted BOOLEAN DEFAULT true,
  encryption_key_id VARCHAR(255),
  
  -- System messages
  is_system_message BOOLEAN DEFAULT false,
  system_message_type VARCHAR(50),
  
  created_at TIMESTAMP DEFAULT now()
);

CREATE TABLE engagement_analytics (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  engagement_id UUID NOT NULL REFERENCES engagements(id) ON DELETE CASCADE,
  metric_name VARCHAR(100) NOT NULL,
  metric_value DECIMAL,
  recorded_at TIMESTAMP DEFAULT now(),
  created_at TIMESTAMP DEFAULT now()
);

CREATE TABLE engagement_events (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  engagement_id UUID NOT NULL REFERENCES engagements(id) ON DELETE CASCADE,
  event_type VARCHAR(100) NOT NULL,
  triggered_at TIMESTAMP DEFAULT now(),
  triggered_by UUID REFERENCES users(id),
  payload JSONB,
  created_at TIMESTAMP DEFAULT now()
);

-- ================================================================
-- 8. SUBSCRIPTIONS
-- ================================================================

CREATE TABLE active_service_subscriptions (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  plan_id VARCHAR(100) NOT NULL,
  plan_name VARCHAR(255),
  start_date TIMESTAMP NOT NULL,
  end_date TIMESTAMP,
  status subscription_status DEFAULT 'active',
  auto_renew BOOLEAN DEFAULT true,
  payment_data JSONB,
  
  created_at TIMESTAMP DEFAULT now(),
  updated_at TIMESTAMP DEFAULT now()
);

CREATE TABLE engagement_sessions (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  engagement_id UUID NOT NULL REFERENCES engagements(id) ON DELETE CASCADE,
  user_id UUID REFERENCES users(id),
  session_token VARCHAR(255) UNIQUE NOT NULL,
  session_start TIMESTAMP DEFAULT now(),
  session_end TIMESTAMP,
  ip_address INET,
  user_agent TEXT,
  device_info JSONB,
  
  created_at TIMESTAMP DEFAULT now()
);

-- ================================================================
-- 9. ANALYTICS
-- ================================================================

CREATE TABLE platform_analytics (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  analytics_date DATE NOT NULL UNIQUE,
  total_users INTEGER DEFAULT 0,
  new_users INTEGER DEFAULT 0,
  active_users INTEGER DEFAULT 0,
  total_sessions INTEGER DEFAULT 0,
  new_sessions INTEGER DEFAULT 0,
  total_doctors INTEGER DEFAULT 0,
  verified_doctors INTEGER DEFAULT 0,
  active_doctors INTEGER DEFAULT 0,
  active_engagements INTEGER DEFAULT 0,
  ended_engagements INTEGER DEFAULT 0,
  crm_resources_count INTEGER DEFAULT 0,
  messages_processed INTEGER DEFAULT 0,
  avg_engagement_duration INTERVAL,
  
  created_at TIMESTAMP DEFAULT now()
);

-- ================================================================
-- 10. SECURITY & AUDIT
-- ================================================================

CREATE TABLE audit_log (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id UUID REFERENCES users(id),
  action VARCHAR(100) NOT NULL,
  resource_type VARCHAR(100),
  resource_id VARCHAR(255),
  change_data JSONB,
  ip_address INET,
  user_agent TEXT,
  
  created_at TIMESTAMP DEFAULT now()
);

CREATE TABLE security_authentication_tokens (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  token_type VARCHAR(50) DEFAULT 'session',
  token VARCHAR(255) UNIQUE NOT NULL,
  expires_at TIMESTAMP NOT NULL,
  is_revoked BOOLEAN DEFAULT false,
  revoked_at TIMESTAMP,
  
  created_at TIMESTAMP DEFAULT now()
);

CREATE TABLE url_shortcuts (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  url TEXT NOT NULL,
  label VARCHAR(255),
  short_code VARCHAR(50) UNIQUE NOT NULL,
  visit_count INTEGER DEFAULT 0,
  expires_at TIMESTAMP,
  
  created_at TIMESTAMP DEFAULT now()
);

CREATE TABLE user_management_metrics (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  metric_name VARCHAR(100) NOT NULL,
  metric_value DECIMAL,
  recorded_at TIMESTAMP DEFAULT now(),
  period VARCHAR(20) DEFAULT 'daily',
  
  created_at TIMESTAMP DEFAULT now()
);

-- ================================================================
-- 11. SYSTEM TABLES
-- ================================================================

CREATE TABLE system_settings (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  setting_key VARCHAR(255) UNIQUE NOT NULL,
  setting_value JSONB,
  description TEXT,
  is_public BOOLEAN DEFAULT false,
  
  created_at TIMESTAMP DEFAULT now(),
  updated_at TIMESTAMP DEFAULT now()
);

-- ================================================================
-- 12. AI CHAT SESSIONS
-- ================================================================

CREATE TABLE ai_chat_sessions (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  patient_id UUID NOT NULL REFERENCES patient_profiles(id) ON DELETE CASCADE,
  session_title VARCHAR(255),
  session_type VARCHAR(50) DEFAULT 'general',
  
  started_at TIMESTAMP DEFAULT now(),
  ended_at TIMESTAMP,
  is_active BOOLEAN DEFAULT true,
  
  -- Analytics
  message_count INTEGER DEFAULT 0,
  total_duration INTERVAL,
  
  meta JSONB,
  
  created_at TIMESTAMP DEFAULT now(),
  updated_at TIMESTAMP DEFAULT now()
);

-- ================================================================
-- 13. AI CHAT MESSAGES
-- ================================================================

CREATE TABLE ai_chat_messages (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  session_id UUID NOT NULL REFERENCES ai_chat_sessions(id) ON DELETE CASCADE,
  
  sender_type chat_sender_type NOT NULL,
  sender_id UUID REFERENCES users(id),
  
  content TEXT NOT NULL,
  content_type VARCHAR(50) DEFAULT 'text',
  
  -- AI-specific fields
  ai_model VARCHAR(50),
  ai_response_time INTEGER,
  tokens_used INTEGER,
  
  sentiment_score DECIMAL,
  flagged_for_review BOOLEAN DEFAULT false,
  flag_reason VARCHAR(255),
  
  sent_at TIMESTAMP DEFAULT now(),
  read_at TIMESTAMP,
  
  created_at TIMESTAMP DEFAULT now()
);

-- ================================================================
-- 14. MESSAGE QUEUES & NOTIFICATIONS
-- ================================================================

CREATE TABLE message_queues (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  job_type VARCHAR(100) NOT NULL,
  payload JSONB,
  status job_status DEFAULT 'pending',
  scheduled_at TIMESTAMP DEFAULT now(),
  processed_at TIMESTAMP,
  retry_count INTEGER DEFAULT 0,
  error_message TEXT,
  
  created_at TIMESTAMP DEFAULT now()
);

CREATE TABLE notifications (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  type VARCHAR(100) NOT NULL,
  title VARCHAR(255),
  message TEXT,
  payload JSONB,
  priority VARCHAR(20) DEFAULT 'normal',
  source VARCHAR(50) DEFAULT 'engagement',
  delivery_status JSONB DEFAULT '{"sse": false, "email": false, "push": false}'::jsonb,
  metadata JSONB DEFAULT '{}'::jsonb,
  is_read BOOLEAN DEFAULT false,
  send_email BOOLEAN DEFAULT false, -- Controls if an email job should be queued
  sent_at TIMESTAMP DEFAULT now(),
  read_at TIMESTAMP,
  expires_at TIMESTAMP,
  
  created_at TIMESTAMP DEFAULT now()
);

-- NOTE: Redundant notification_templates table removed. Switched to notification_message_templates (centralized i18n).

-- ================================================================
-- INDEXES FOR PERFORMANCE
-- ================================================================

CREATE INDEX idx_doctor_profiles_user_id ON doctor_profiles(user_id);
CREATE INDEX idx_doctor_verification_status ON doctor_profiles(verification_status);
CREATE INDEX idx_doctor_availability ON doctor_profiles(availability_status);
CREATE INDEX idx_doctor_specialization ON doctor_profiles(specialization);
CREATE INDEX idx_doctor_rating ON doctor_profiles(rating DESC);
CREATE INDEX idx_doctor_location ON doctor_profiles(location_city, location_country);
CREATE INDEX idx_doctor_coordinates ON doctor_profiles(latitude, longitude)
    WHERE latitude IS NOT NULL AND longitude IS NOT NULL;

CREATE INDEX idx_verification_questions_doctor ON doctor_verification_questions(doctor_id);
CREATE INDEX idx_verification_questions_verified ON doctor_verification_questions(verified_at);

-- Fast lookup for pending email jobs
CREATE INDEX IF NOT EXISTS idx_queue_pending 
ON message_queues(job_type, status, created_at) 
WHERE status = 'pending';

-- Users indexes
CREATE INDEX idx_users_email ON users(email);
CREATE INDEX idx_users_is_active ON users(is_active);
CREATE INDEX idx_users_deleted_at ON users(deleted_at);

-- Profile indexes
CREATE INDEX idx_doctor_profiles_user_id ON doctor_profiles(user_id);
CREATE INDEX idx_doctor_profiles_verified ON doctor_profiles(is_verified);
CREATE INDEX idx_patient_profiles_user_id ON patient_profiles(user_id);

-- Engagement indexes
CREATE INDEX idx_engagements_doctor_id ON engagements(doctor_id);
CREATE INDEX idx_engagements_patient_id ON engagements(patient_id);
CREATE INDEX idx_engagements_status ON engagements(status);
CREATE INDEX idx_engagements_dates ON engagements(start_at, end_at);
CREATE INDEX idx_engagements_initiated_by ON engagements(initiated_by);

-- Doctor-patient relationship indexes
CREATE INDEX idx_doctor_patients_doctor_id ON doctor_patients(doctor_id);
CREATE INDEX idx_doctor_patients_patient_id ON doctor_patients(patient_id);
CREATE INDEX idx_doctor_patients_is_active ON doctor_patients(is_active);
CREATE INDEX idx_doctor_patients_current_engagement ON doctor_patients(current_engagement_id);

-- Message indexes
CREATE INDEX idx_engagement_messages_engagement_id ON engagement_messages(engagement_id);
CREATE INDEX idx_engagement_messages_sender ON engagement_messages(sender_id);
CREATE INDEX idx_engagement_messages_created_at ON engagement_messages(created_at);

-- AI chat indexes
CREATE INDEX idx_ai_chat_sessions_patient_id ON ai_chat_sessions(patient_id);
CREATE INDEX idx_ai_chat_sessions_is_active ON ai_chat_sessions(is_active);
CREATE INDEX idx_ai_chat_sessions_patient_started ON ai_chat_sessions(patient_id, started_at);
CREATE INDEX idx_ai_chat_messages_session_id ON ai_chat_messages(session_id);
CREATE INDEX idx_ai_chat_messages_created_at ON ai_chat_messages(created_at);

-- Audit and security indexes
CREATE INDEX idx_audit_log_user_id ON audit_log(user_id);
CREATE INDEX idx_audit_log_created_at ON audit_log(created_at);
CREATE INDEX idx_auth_tokens_user_id ON security_authentication_tokens(user_id);
CREATE INDEX idx_auth_tokens_expires_at ON security_authentication_tokens(expires_at);

-- 12. NOTIFICATION MESSAGE TEMPLATES (CENTRALIZED I18N)
CREATE TABLE notification_message_templates (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  
  -- Template identification
  template_key VARCHAR(100) NOT NULL,
  language_code VARCHAR(10) NOT NULL,
  
  -- Message content
  title TEXT NOT NULL,
  message TEXT NOT NULL,
  
  -- Context for perspective (who is receiving this?)
  recipient_context VARCHAR(50) NOT NULL, -- 'doctor', 'patient', 'initiator', 'target'
  
  -- Metadata
  default_priority VARCHAR(20) DEFAULT 'normal',
  notes TEXT,
  channels JSONB DEFAULT '{"email": false, "push": false, "sse": true}'::jsonb, 

  created_at TIMESTAMP DEFAULT NOW(),
  updated_at TIMESTAMP DEFAULT NOW(),
  
  -- Ensure one template per key+language+context
  CONSTRAINT unique_template_lang_context UNIQUE (template_key, language_code, recipient_context)
);

CREATE INDEX idx_message_templates_key_lang ON notification_message_templates(template_key, language_code);

-- Update users table with language preference
ALTER TABLE users ADD COLUMN language VARCHAR(10) DEFAULT 'en';
CREATE INDEX idx_users_language ON users(language);
COMMENT ON COLUMN users.language IS 'User preferred language code (en, ar, etc.)';

-- Notification indexes
CREATE INDEX idx_notifications_user_id ON notifications(user_id);
CREATE INDEX idx_notifications_is_read ON notifications(is_read);
CREATE INDEX idx_notifications_user_sentat ON notifications (user_id, sent_at);

-- Create index for polling
CREATE INDEX idx_notifications_unpushed 
ON notifications(user_id, sent_at) 
WHERE (delivery_status->>'sse')::boolean = false;

-- Create index for priority
CREATE INDEX idx_notifications_priority 
ON notifications(priority, sent_at DESC);

-- Performance optimization indexes
CREATE INDEX idx_engagements_doctor_patient_status 
ON engagements(doctor_id, patient_id, status) 
WHERE status IN ('active', 'pending');

-- 13. NOTIFICATION RENDERER HELPER
CREATE OR REPLACE FUNCTION get_notification_message(
    p_template_key VARCHAR,
    p_recipient_user_id UUID,
    p_recipient_context VARCHAR,
    p_placeholders JSONB DEFAULT '{}'::jsonb
)
RETURNS TABLE(title TEXT, message TEXT, priority VARCHAR) AS $$
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
$$ LANGUAGE plpgsql;

-- 14. INITIAL NOTIFICATION TEMPLATES
-- ENGAGEMENT_STARTED
INSERT INTO notification_message_templates (template_key, language_code, recipient_context, title, message, default_priority, channels) VALUES
('ENGAGEMENT_STARTED', 'en', 'doctor', 'Engagement Activated', 'Patient {patientName} has verified and started the engagement.', 'high', '{"email": true, "sse": true}'::jsonb),
('ENGAGEMENT_STARTED', 'ar', 'doctor', 'تم تفعيل المتابعة', 'المريض {patientName} قام بالتحقق وبدأ المتابعة.', 'high', '{"email": true, "sse": true}'::jsonb)
ON CONFLICT (template_key, language_code, recipient_context) DO UPDATE SET
    title = EXCLUDED.title,
    message = EXCLUDED.message,
    channels = EXCLUDED.channels,
    updated_at = NOW();

-- ENGAGEMENT_CANCELLED (context-aware)
INSERT INTO notification_message_templates (template_key, language_code, recipient_context, title, message, default_priority, channels) VALUES
('ENGAGEMENT_CANCELLED', 'en', 'initiator', 'Engagement Cancelled', 'You have cancelled the engagement with {otherPartyName}.', 'high', '{"email": true, "sse": true}'::jsonb),
('ENGAGEMENT_CANCELLED', 'ar', 'initiator', 'تم إلغاء المتابعة', 'لقد قمت بإلغاء المتابعة مع {otherPartyName}.', 'high', '{"email": true, "sse": true}'::jsonb),
('ENGAGEMENT_CANCELLED', 'en', 'target', 'Engagement Cancelled', '{otherPartyName} has cancelled the engagement.', 'high', '{"email": true, "sse": true}'::jsonb),
('ENGAGEMENT_CANCELLED', 'ar', 'target', 'تم إلغاء المتابعة', '{otherPartyName} قام بإلغاء المتابعة.', 'high', '{"email": true, "sse": true}'::jsonb);

-- ENGAGEMENT_ENDED
INSERT INTO notification_message_templates (template_key, language_code, recipient_context, title, message, default_priority) VALUES
('ENGAGEMENT_ENDED', 'en', 'doctor', 'Engagement Ended', 'Your engagement with {patientName} has ended.', 'normal'),
('ENGAGEMENT_ENDED', 'ar', 'doctor', 'انتهت المتابعة', 'انتهت متابعتك مع المريض {patientName}.', 'normal'),
('ENGAGEMENT_ENDED', 'en', 'patient', 'Engagement Ended', 'Your engagement with Dr. {doctorName} has ended.', 'normal'),
('ENGAGEMENT_ENDED', 'ar', 'patient', 'انتهت المتابعة', 'انتهت متابعتك مع الدكتور {doctorName}.', 'normal');

-- MESSAGE_RECEIVED
INSERT INTO notification_message_templates (template_key, language_code, recipient_context, title, message, default_priority) VALUES
('MESSAGE_RECEIVED', 'en', 'doctor', 'New Message', 'You received a message from {patientName}.', 'normal'),
('MESSAGE_RECEIVED', 'ar', 'doctor', 'رسالة جديدة', 'لديك رسالة جديدة من {patientName}.', 'normal'),
('MESSAGE_RECEIVED', 'en', 'patient', 'New Message', 'You received a message from Dr. {doctorName}.', 'normal'),
('MESSAGE_RECEIVED', 'ar', 'patient', 'رسالة جديدة', 'لديك رسالة جديدة من الدكتور {doctorName}.', 'normal');

-- AI_RESPONSE_READY
INSERT INTO notification_message_templates (template_key, language_code, recipient_context, title, message, default_priority) VALUES
('AI_RESPONSE_READY', 'en', 'patient', 'AI Analysis Ready', 'Your AI health analysis is ready to view.', 'normal'),
('AI_RESPONSE_READY', 'ar', 'patient', 'التحليل الذكي جاهز', 'تحليل صحتك بالذكاء الاصطناعي جاهز للعرض.', 'normal');

-- SYSTEM_ALERT
INSERT INTO notification_message_templates (template_key, language_code, recipient_context, title, message, default_priority) VALUES
('SYSTEM_ALERT', 'en', 'doctor', 'System Alert', '{alertMessage}', 'critical'),
('SYSTEM_ALERT', 'ar', 'doctor', 'تنبيه النظام', '{alertMessage}', 'critical'),
('SYSTEM_ALERT', 'en', 'patient', 'System Alert', '{alertMessage}', 'critical'),
('SYSTEM_ALERT', 'ar', 'patient', 'تنبيه النظام', '{alertMessage}', 'critical');

-- ACCESS_LEVEL_CHANGED
INSERT INTO notification_message_templates (template_key, language_code, recipient_context, title, message, default_priority) VALUES
('ACCESS_LEVEL_CHANGED', 'en', 'doctor', 'Access Updated', 'Access level changed from "{oldAccess}" to "{newAccess}" for patient {patientName}.', 'normal'),
('ACCESS_LEVEL_CHANGED', 'ar', 'doctor', 'تم تحديث الصلاحيات', 'تم تغيير مستوى الوصول من "{oldAccess}" إلى "{newAccess}" للمريض {patientName}.', 'normal');

-- USER_WELCOME (Triggered on user creation)
INSERT INTO notification_message_templates (template_key, language_code, title, message, recipient_context, default_priority, channels) VALUES 
('USER_WELCOME', 'en', 'Welcome to NeuralHealer! 🎉', 'Hi {userName}, we''re thrilled to have you here! Your journey to better health starts now. Let us know if you need any help getting started.', 'patient', 'normal', '{"email": true, "sse": true}'::jsonb),
('USER_WELCOME', 'ar', 'مرحباً بك في NeuralHealer! 🎉', 'مرحباً {userName}، يسعدنا انضمامك! رحلتك نحو صحة أفضل تبدأ الآن. أخبرنا إذا احتجت أي مساعدة للبدء.', 'patient', 'normal', '{"email": true, "sse": true}'::jsonb)
ON CONFLICT (template_key, language_code, recipient_context) DO UPDATE SET
    title = EXCLUDED.title,
    message = EXCLUDED.message,
    channels = EXCLUDED.channels,
    updated_at = NOW();

-- Re-engagement for Active Users (3 days inactive)
INSERT INTO notification_message_templates (template_key, language_code, title, message, recipient_context, default_priority, channels) VALUES
('USER_REENGAGE_ACTIVE', 'en', 'We miss you! 👋', 'Hey {userName}, it''s been 3 days since your last visit. How are you feeling today? Check in with your health companion!', 'patient', 'normal', '{"email": true, "sse": true}'::jsonb),
('USER_REENGAGE_ACTIVE', 'ar', 'نفتقدك! 👋', 'مرحباً {userName}، مضى 3 أيام منذ زيارتك الأخيرة. كيف حالك اليوم؟ تحقق من حالتك الصحية معنا!', 'patient', 'normal', '{"email": true, "sse": true}'::jsonb)
ON CONFLICT (template_key, language_code, recipient_context) DO UPDATE SET
    title = EXCLUDED.title,
    message = EXCLUDED.message,
    channels = EXCLUDED.channels,
    updated_at = NOW();

-- Warning Before Becoming Inactive (14 days for inactive users)
INSERT INTO notification_message_templates (template_key, language_code, title, message, recipient_context, default_priority, channels) VALUES
('USER_INACTIVITY_WARNING', 'en', 'Stay Connected with Your Health', 'Hi {userName}, we noticed you haven''t logged in for 14 days. Your health journey matters to us - come back and see what''s new!', 'patient', 'normal', '{"email": true, "sse": true}'::jsonb),
('USER_INACTIVITY_WARNING', 'ar', 'ابقَ على تواصل مع صحتك', 'مرحباً {userName}، لاحظنا أنك لم تسجل دخولك لمدة 14 يوماً. صحتك تهمنا - عد وشاهد ما الجديد!', 'patient', 'normal', '{"email": true, "sse": true}'::jsonb)
ON CONFLICT (template_key, language_code, recipient_context) DO UPDATE SET
    title = EXCLUDED.title,
    message = EXCLUDED.message,
    channels = EXCLUDED.channels,
    updated_at = NOW();

-- 15. SYSTEM SETTINGS FOR NOTIFICATIONS

INSERT INTO system_settings (setting_key, setting_value, description, is_public) VALUES
('doctor_verification_questions',
 '[
   {"key": "license_number", "label": "Medical License Number", "required": true, "type": "text"},
   {"key": "graduation_year", "label": "Year of Graduation", "required": true, "type": "number"},
   {"key": "medical_school", "label": "Medical School / University", "required": false, "type": "text"},
   {"key": "specialty_certification", "label": "Board Certification Number", "required": false, "type": "text"},
   {"key": "current_practice", "label": "Current Practice/Clinic Name", "required": false, "type": "text"}
 ]'::jsonb,
 'List of verification questions for doctors',
 false)
ON CONFLICT (setting_key) DO UPDATE SET
  setting_value = EXCLUDED.setting_value,
  updated_at = NOW();

INSERT INTO system_settings (setting_key, setting_value, description, is_public) VALUES
('notification_active_user_threshold_days', '3', 'Days of inactivity before re-engagement notification for active users', false),
('notification_inactive_warning_days', '14', 'Days of inactivity before warning notification', false),
('notification_inactive_status_days', '4', 'Days after warning before user marked as inactive (total 18 days)', false);

-- 16. USER ACTIVITY TRACKING
ALTER TABLE users ADD COLUMN activity_status VARCHAR(20) DEFAULT 'active';
ALTER TABLE users ADD COLUMN last_activity_check TIMESTAMP DEFAULT NOW();
CREATE INDEX idx_users_activity_status ON users(activity_status, last_login_at);
COMMENT ON COLUMN users.activity_status IS 'User activity life-cycle status (active, dormant, inactive)';

-- ================================================================
-- FUNCTIONS & TRIGGERS
-- ================================================================
-- 1. Auto-update doctor rating
CREATE OR REPLACE FUNCTION update_doctor_rating()
RETURNS TRIGGER AS $$
DECLARE
    v_avg_rating DECIMAL(3,2);
    v_total_reviews INTEGER;
BEGIN
    IF (TG_OP = 'DELETE') THEN
        SELECT COALESCE(ROUND(AVG(rating)::numeric, 2), 0.00), COUNT(*)
        INTO v_avg_rating, v_total_reviews
        FROM doctor_reviews WHERE doctor_id = OLD.doctor_id;
        
        UPDATE doctor_profiles 
        SET rating = v_avg_rating, total_reviews = v_total_reviews, updated_at = NOW()
        WHERE id = OLD.doctor_id;
    ELSE
        SELECT COALESCE(ROUND(AVG(rating)::numeric, 2), 0.00), COUNT(*)
        INTO v_avg_rating, v_total_reviews
        FROM doctor_reviews WHERE doctor_id = NEW.doctor_id;
        
        UPDATE doctor_profiles 
        SET rating = v_avg_rating, total_reviews = v_total_reviews, updated_at = NOW()
        WHERE id = NEW.doctor_id;
    END IF;
    RETURN NULL;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_update_doctor_rating
AFTER INSERT OR UPDATE OR DELETE ON doctor_reviews
FOR EACH ROW EXECUTE FUNCTION update_doctor_rating();

-- 2. Profile completion percentage
CREATE OR REPLACE FUNCTION calculate_profile_completion(p_doctor_id UUID)
RETURNS INTEGER AS $$
DECLARE
    v_score INTEGER := 0;
    v_doctor RECORD;
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
    IF v_doctor.social_media IS NOT NULL THEN v_score := v_score + 10; END IF;

    -- Verification (30)
    IF v_doctor.verification_status = 'verified' THEN 
        v_score := v_score + 30; 
    ELSIF v_doctor.verification_status = 'pending' THEN 
        v_score := v_score + 10; 
    END IF;

    RETURN v_score;
END;
$$ LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION update_profile_completion()
RETURNS TRIGGER AS $$
BEGIN
    NEW.profile_completion_percentage := calculate_profile_completion(NEW.id);
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_update_profile_completion
BEFORE UPDATE ON doctor_profiles
FOR EACH ROW
WHEN (
    OLD.title IS DISTINCT FROM NEW.title OR
    OLD.bio IS DISTINCT FROM NEW.bio OR
    OLD.specialization IS DISTINCT FROM NEW.specialization OR
    OLD.years_of_experience IS DISTINCT FROM NEW.years_of_experience OR
    OLD.profile_picture_path IS DISTINCT FROM NEW.profile_picture_path OR
    OLD.certificates IS DISTINCT FROM NEW.certificates OR
    OLD.social_media IS DISTINCT FROM NEW.social_media OR
    OLD.verification_status IS DISTINCT FROM NEW.verification_status OR
    OLD.consultation_fee IS DISTINCT FROM NEW.consultation_fee
)
EXECUTE FUNCTION update_profile_completion();

-- Auto-update updated_at timestamp
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
   NEW.updated_at = now();
   RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER update_users_updated_at BEFORE UPDATE ON users
FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_doctor_profiles_updated_at BEFORE UPDATE ON doctor_profiles
FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_patient_profiles_updated_at BEFORE UPDATE ON patient_profiles
FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_engagements_updated_at BEFORE UPDATE ON engagements
FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_doctor_patients_updated_at BEFORE UPDATE ON doctor_patients
FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_engagement_access_rules_updated_at BEFORE UPDATE ON engagement_access_rules
FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_system_settings_updated_at BEFORE UPDATE ON system_settings
FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_ai_chat_sessions_updated_at BEFORE UPDATE ON ai_chat_sessions
FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_active_service_subscriptions_updated_at BEFORE UPDATE ON active_service_subscriptions
FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- Auto-generate engagement_id
CREATE OR REPLACE FUNCTION generate_engagement_id()
RETURNS TRIGGER AS $$
BEGIN
   IF NEW.engagement_id IS NULL THEN
      NEW.engagement_id := 'ENG-' || EXTRACT(YEAR FROM now()) || '-' || LPAD(nextval('engagement_id_seq')::TEXT, 6, '0');
   END IF;
   RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE SEQUENCE engagement_id_seq;
CREATE TRIGGER set_engagement_id BEFORE INSERT ON engagements
FOR EACH ROW EXECUTE FUNCTION generate_engagement_id();

-- ================================================================
-- 17. ROBUST NOTIFICATION COMPONENTS (IDEMPOTENT)
-- ================================================================

-- Bulletproof message renderer
CREATE OR REPLACE FUNCTION get_notification_message(
    p_template_key VARCHAR,
    p_recipient_user_id UUID,
    p_recipient_context VARCHAR,
    p_placeholders JSONB DEFAULT '{}'::jsonb
)
RETURNS TABLE(title TEXT, message TEXT, priority VARCHAR) AS $$
DECLARE
    v_language VARCHAR(10);
    v_title TEXT;
    v_message TEXT;
    v_priority VARCHAR(20);
    v_placeholder_key TEXT;
    v_placeholder_value TEXT;
BEGIN
    -- 1. Language detection
    SELECT COALESCE(NULLIF(language, ''), 'en') INTO v_language
    FROM users WHERE id = p_recipient_user_id;
    v_language := COALESCE(v_language, 'en');
    
    -- 2. Primary lookup
    SELECT nmt.title, nmt.message, nmt.default_priority
    INTO v_title, v_message, v_priority
    FROM notification_message_templates nmt
    WHERE LOWER(nmt.template_key) = LOWER(p_template_key)
      AND nmt.language_code = v_language
      AND LOWER(nmt.recipient_context) = LOWER(p_recipient_context);
    
    -- 3. Language Fallback
    IF v_title IS NULL AND v_language != 'en' THEN
        SELECT nmt.title, nmt.message, nmt.default_priority
        INTO v_title, v_message, v_priority
        FROM notification_message_templates nmt
        WHERE LOWER(nmt.template_key) = LOWER(p_template_key)
          AND nmt.language_code = 'en'
          AND LOWER(nmt.recipient_context) = LOWER(p_recipient_context);
    END IF;

    -- 4. Context Fallback
    IF v_title IS NULL THEN
        SELECT nmt.title, nmt.message, nmt.default_priority
        INTO v_title, v_message, v_priority
        FROM notification_message_templates nmt
        WHERE LOWER(nmt.template_key) = LOWER(p_template_key)
          AND nmt.language_code = 'en'
        LIMIT 1;
    END IF;

    -- 5. Absolute Safety
    IF v_title IS NULL THEN
        v_title := INITCAP(REPLACE(p_template_key, '_', ' '));
        v_message := 'Notification: ' || p_template_key;
        v_priority := 'normal';
    END IF;
    
    -- 6. Placeholder replacement
    FOR v_placeholder_key, v_placeholder_value IN
        SELECT key, value FROM jsonb_each_text(p_placeholders)
    LOOP
        v_title := REPLACE(v_title, '{' || v_placeholder_key || '}', COALESCE(v_placeholder_value, ''));
        v_message := REPLACE(v_message, '{' || v_placeholder_key || '}', COALESCE(v_placeholder_value, ''));
    END LOOP;
    
    RETURN QUERY SELECT v_title, v_message, COALESCE(v_priority, 'normal');
END;
$$ LANGUAGE plpgsql;


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


-- Automated Email Queuing Trigger
CREATE OR REPLACE FUNCTION trigger_queue_email_job()
RETURNS TRIGGER AS $$
BEGIN
    RAISE NOTICE 'TRIGGER DEBUG: trg_auto_queue_email fired for notification %', NEW.id;
    RAISE NOTICE 'TRIGGER DEBUG: send_email = %', NEW.send_email;
    
    IF NEW.send_email = TRUE THEN
        RAISE NOTICE 'TRIGGER DEBUG: Creating email job in message_queues';
        
        -- Insert the job and capture the ID
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
        
        -- Send NOTIFY to wake up Java listener
        -- We don't have the job ID easily without RETURNING, 
        -- but we can just notify that a job exists or use the notification ID
        PERFORM pg_notify('email_queue', 'new_job');
        
        RAISE NOTICE 'TRIGGER DEBUG: Email job created and NOTIFY sent';
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


-- ================================================================
-- SAMPLE DATA - ENGAGEMENT ACCESS RULES
-- ================================================================

INSERT INTO engagement_access_rules (rule_name, can_view_all_history, can_view_current_only, can_view_patient_profile, can_modify_notes, can_message_patient, retains_period_access, retains_history_access, retains_no_access, description) VALUES
('FULL_ACCESS', true, true, true, true, true, true, false, false, 'Full access to all patient data and history'),
('CURRENT_ENGAGEMENT_ACCESS', false, true, true, true, true, false, false, true, 'Access only to current engagement period'),
('READ_ONLY_ACCESS', true, true, true, false, false, true, false, false, 'Read-only access to patient data'),
('LIMITED_ENGAGEMENT_ACCESS', false, true, false, false, true, false, false, true, 'Limited access during active engagement only'),
('NO_ACCESS', false, false, false, false, false, false, false, true, 'No access to patient data'),
-- new added 22/1/2026 --
('INITIAL_PENDING', false, false, false, false, false, false, false, true, 'First engagement request sent, not yet verified'),
('INITIAL_CANCELLED_PENDING', false, false, false, false, false, false, false, true, 'First engagement cancelled before activation');
-- done adding ---


-- ================================================================
-- CRITICAL TRIGGERS - AUTO-UPDATE RELATIONSHIP STATUS
-- ================================================================

-- Trigger to update doctor_patients relationship when engagement status changes
CREATE OR REPLACE FUNCTION update_relationship_status_on_engagement()
RETURNS TRIGGER AS $$
DECLARE
    current_rule RECORD;
    new_status VARCHAR;
BEGIN
    -- When engagement becomes active
    IF NEW.status = 'active' AND (OLD IS NULL OR OLD.status != 'active') THEN
        UPDATE doctor_patients
        SET 
            relationship_status = NEW.access_rule_name,
            current_engagement_id = NEW.id,
            relationship_started_at = NEW.start_at,
            is_active = true
        WHERE doctor_id = NEW.doctor_id 
          AND patient_id = NEW.patient_id;
        
        -- Send system message (Note: Still uses hardcoded English for system logs mostly, but labels are simplified)
        INSERT INTO engagement_messages (
            engagement_id,
            content,
            is_system_message,
            system_message_type,
            sent_at
        ) VALUES (
            NEW.id,
            '🔔 Engagement started with access level: ' || NEW.access_rule_name,
            true,
            'engagement_started',
            NOW()
        );
    END IF;
    
    -- When engagement ends
    IF NEW.status = 'ended' AND (OLD IS NULL OR OLD.status = 'active') THEN
        -- Get retention rules
        SELECT * INTO current_rule 
        FROM engagement_access_rules 
        WHERE rule_name = NEW.access_rule_name;
        
        -- Determine new status based on retention
        IF current_rule.retains_history_access OR current_rule.retains_period_access THEN
            new_status := NEW.access_rule_name; -- Keep same rule
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
        
        -- Send system message
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
$$ LANGUAGE plpgsql;

CREATE TRIGGER engagement_status_change
AFTER INSERT OR UPDATE OF status ON engagements
FOR EACH ROW
EXECUTE FUNCTION update_relationship_status_on_engagement();

-- Notify when relationship_status changes
CREATE OR REPLACE FUNCTION notify_access_rule_change()
RETURNS TRIGGER AS $$
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
$$ LANGUAGE plpgsql;

CREATE TRIGGER relationship_access_change
AFTER UPDATE OF relationship_status ON doctor_patients
FOR EACH ROW
WHEN (OLD.relationship_status IS DISTINCT FROM NEW.relationship_status)
EXECUTE FUNCTION notify_access_rule_change();



-- 3. Create a new, cleaner trigger function that calls create_system_notification directly
CREATE OR REPLACE FUNCTION user_welcome_notification()
RETURNS TRIGGER AS $$
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
$$ LANGUAGE plpgsql;




-- 4. Re-attach the trigger
CREATE TRIGGER user_welcome_notification
AFTER INSERT ON users
FOR EACH ROW
EXECUTE FUNCTION user_welcome_notification();

-- Also, let's check if there are any other functions calling send_welcome_notification
-- Since it was only used by this trigger, we're safe to remove it

-- Optional: Add comment to document the change
COMMENT ON FUNCTION user_welcome_notification() IS 'Creates welcome notification for new users using the centralized notification system';


-- 🏗️ ENHANCED TRIGGER SYSTEM
-- Trigger for Engagement Notifications (Centralized I18n)
CREATE OR REPLACE FUNCTION create_engagement_notification()
RETURNS TRIGGER AS $$
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
$$ LANGUAGE plpgsql;

CREATE TRIGGER engagement_notification_trigger
AFTER UPDATE OF status ON engagements
FOR EACH ROW
EXECUTE FUNCTION create_engagement_notification();

-- ================================================================
-- HELPER FUNCTIONS - ACCESS CONTROL
-- ================================================================

-- Function to get accessible engagement messages for a doctor viewing a patient
-- p_doctor_id and p_patient_id are profile IDs, not user IDs
CREATE OR REPLACE FUNCTION get_accessible_messages(
  p_doctor_id UUID,  -- doctor_profiles.id
  p_patient_id UUID  -- patient_profiles.id
)
RETURNS TABLE (
  message_id UUID,
  content TEXT,
  sender_id UUID,
  sent_at TIMESTAMP,
  is_system_message BOOLEAN
) AS $$
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
$$ LANGUAGE plpgsql;

-- Function to get accessible AI chat sessions for a doctor viewing a patient
CREATE OR REPLACE FUNCTION get_accessible_ai_chat_sessions(
  p_doctor_id UUID,  -- doctor_profiles.id
  p_patient_id UUID  -- patient_profiles.id
)
RETURNS TABLE (
  session_id UUID,
  session_title VARCHAR,
  started_at TIMESTAMP,
  message_count INTEGER,
  is_active BOOLEAN
) AS $$
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
$$ LANGUAGE plpgsql;

-- Function to check if doctor can view specific AI session
CREATE OR REPLACE FUNCTION can_doctor_view_ai_session(
    p_doctor_id UUID,   -- doctor_profiles.id
    p_patient_id UUID,  -- patient_profiles.id
    p_session_id UUID
) RETURNS BOOLEAN AS $$
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
$$ LANGUAGE plpgsql;

-- ================================================================
-- COMMENTS FOR DOCUMENTATION
-- ================================================================

COMMENT ON TABLE engagement_access_rules IS 'NeuralHealer: Defines access control rules for doctor-patient engagements';
COMMENT ON TABLE users IS 'NeuralHealer: Base user table for all platform users (doctors, patients, admins)';
COMMENT ON TABLE doctor_profiles IS 'NeuralHealer: Extended profile information for doctors';
COMMENT ON TABLE patient_profiles IS 'NeuralHealer: Extended profile information for patients';
COMMENT ON TABLE engagements IS 'NeuralHealer: Active or historical doctor-patient engagement periods';
COMMENT ON TABLE doctor_patients IS 'NeuralHealer: Relationship mapping between doctors and patients with current access rules';
COMMENT ON TABLE engagement_messages IS 'NeuralHealer: Messages exchanged during engagements';
COMMENT ON TABLE ai_chat_sessions IS 'NeuralHealer: AI chatbot sessions for patients';
COMMENT ON TABLE ai_chat_messages IS 'NeuralHealer: Messages exchanged in AI chat sessions';

-- ================================================================
-- NEURALHEALER METADATA
-- ================================================================

INSERT INTO system_settings (setting_key, setting_value, description, is_public) VALUES
('platform_name', '"NeuralHealer"', 'Platform name', true),
('platform_version', '"1.0.0"', 'Current platform version', true),
('database_version', '"1.0.0"', 'Database schema version', false),
('max_engagement_duration_days', '365', 'Maximum engagement duration in days', false),
('ai_model_version', '"gpt-4"', 'Default AI model for patient chats', false),
('require_2fa_for_engagement', 'true', 'Require 2FA verification for engagement start/end', false);

-- ================================================================
-- END OF NEURALHEALER SCHEMA
-- ================================================================
