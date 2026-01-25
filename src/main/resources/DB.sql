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
  specialities JSONB,
  experience_years INTEGER,
  certificates JSONB,
  location_city VARCHAR(100),
  location_country VARCHAR(100),
  is_verified BOOLEAN DEFAULT false,
  verification_data JSONB,
  
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
-- 4. ENGAGEMENTS
-- ================================================================

CREATE TABLE engagements (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  engagement_id VARCHAR(100) UNIQUE,
  doctor_id UUID NOT NULL REFERENCES doctor_profiles(id) ON DELETE CASCADE,
  patient_id UUID NOT NULL REFERENCES patient_profiles(id) ON DELETE CASCADE,
  access_rule_name VARCHAR(255) NOT NULL REFERENCES engagement_access_rules(rule_name),
  
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
  updated_at TIMESTAMP DEFAULT now()
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
  sent_at TIMESTAMP DEFAULT now(),
  read_at TIMESTAMP,
  expires_at TIMESTAMP,
  
  created_at TIMESTAMP DEFAULT now()
);

-- 3. CREATE NOTIFICATION TEMPLATES TABLE
CREATE TABLE notification_templates (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  type VARCHAR(100) NOT NULL UNIQUE,
  title_template TEXT NOT NULL,
  message_template TEXT NOT NULL,
  default_priority VARCHAR(20) DEFAULT 'normal',
  default_channels JSONB DEFAULT '["sse"]'::jsonb,
  created_at TIMESTAMP DEFAULT NOW()
);

-- Insert default templates
INSERT INTO notification_templates (type, title_template, message_template, default_priority) VALUES
('ENGAGEMENT_STARTED', 'Engagement Activated', 'Patient {patientName} has verified and started the engagement.', 'high'),
('ENGAGEMENT_CANCELLED', 'Engagement Cancelled', '{actorName} has cancelled the engagement.', 'high'),
('MESSAGE_RECEIVED', 'New Message', 'You have a new message from {senderName}.', 'normal'),
('AI_RESPONSE_READY', 'AI Analysis Ready', 'Your AI health analysis is ready.', 'normal'),
('SYSTEM_ALERT', 'System Alert', '{alertMessage}', 'critical');

-- ================================================================
-- INDEXES FOR PERFORMANCE
-- ================================================================

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

-- Notification indexes
CREATE INDEX idx_notifications_user_id ON notifications(user_id);
CREATE INDEX idx_notifications_is_read ON notifications(is_read);

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

-- ================================================================
-- TRIGGERS
-- ================================================================

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
        
        -- Send system message
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

-- 🏗️ ENHANCED TRIGGER SYSTEM
-- Trigger for Engagement Notifications
CREATE OR REPLACE FUNCTION create_engagement_notification()
RETURNS TRIGGER AS $$
DECLARE
    v_patient_name TEXT;
    v_doctor_name TEXT;
    v_actor_name TEXT;
    v_target_user_id UUID;
    v_notification_type VARCHAR(100);
    v_title TEXT;
    v_message TEXT;
    v_priority VARCHAR(20);
BEGIN
    -- Get names (joining with users table)
    SELECT CONCAT(u.first_name, ' ', u.last_name) INTO v_patient_name
    FROM users u
    JOIN patient_profiles p ON u.id = p.user_id
    WHERE p.id = NEW.patient_id;
    
    SELECT CONCAT(u.first_name, ' ', u.last_name) INTO v_doctor_name
    FROM users u
    JOIN doctor_profiles d ON u.id = d.user_id
    WHERE d.id = NEW.doctor_id;
    
    -- Determine notification type and priority
    IF NEW.status = 'active' AND OLD.status = 'pending' THEN
        v_notification_type := 'ENGAGEMENT_STARTED';
        v_title := 'Engagement Activated';
        v_message := FORMAT('Patient %s has verified and started the engagement.', v_patient_name);
        v_priority := 'high';
        v_target_user_id := (SELECT user_id FROM doctor_profiles WHERE id = NEW.doctor_id);
        
    ELSIF NEW.status = 'cancelled' THEN
        v_notification_type := 'ENGAGEMENT_CANCELLED';
        v_title := 'Engagement Cancelled';
        
        -- Who cancelled?
        IF NEW.ended_by = (SELECT user_id FROM doctor_profiles WHERE id = NEW.doctor_id) THEN
            v_actor_name := FORMAT('Dr. %s', v_doctor_name);
            v_target_user_id := (SELECT user_id FROM patient_profiles WHERE id = NEW.patient_id);
        ELSE
            v_actor_name := v_patient_name;
            v_target_user_id := (SELECT user_id FROM doctor_profiles WHERE id = NEW.doctor_id);
        END IF;
        
        v_message := FORMAT('%s has cancelled the engagement.', v_actor_name);
        v_priority := 'high';
        
        -- For cancelled, notify BOTH parties
        INSERT INTO notifications (
            user_id, type, title, message, payload, priority, source, sent_at
        ) VALUES (
            (SELECT user_id FROM patient_profiles WHERE id = NEW.patient_id),
            v_notification_type,
            v_title,
            v_message,
            jsonb_build_object('engagementId', NEW.id, 'actor', v_actor_name, 'status', NEW.status),
            v_priority,
            'engagement',
            NOW()
        ), (
            (SELECT user_id FROM doctor_profiles WHERE id = NEW.doctor_id),
            v_notification_type,
            v_title,
            v_message,
            jsonb_build_object('engagementId', NEW.id, 'actor', v_actor_name, 'status', NEW.status),
            v_priority,
            'engagement',
            NOW()
        );
        
        RETURN NEW;
    END IF;
    
    -- Insert single notification
    IF v_target_user_id IS NOT NULL THEN
        INSERT INTO notifications (
            user_id, type, title, message, payload, priority, source, sent_at
        ) VALUES (
            v_target_user_id,
            v_notification_type,
            v_title,
            v_message,
            jsonb_build_object('engagementId', NEW.id, 'patientName', v_patient_name, 'doctorName', v_doctor_name),
            v_priority,
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
