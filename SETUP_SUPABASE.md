# Supabase Database Setup Guide

## Problem
Your application is now connecting to Supabase successfully, but the database schema (tables, functions, triggers) doesn't exist yet.

## Solution: Run DB.sql on Supabase

### Option 1: Using Supabase Dashboard (Recommended)

1. **Go to Supabase Dashboard**
   - Visit: https://supabase.com/dashboard
   - Select your project

2. **Open SQL Editor**
   - Click on "SQL Editor" in the left sidebar
   - Click "New Query"

3. **Copy and Execute Schema**
   - Open `src/main/resources/DB.sql`
   - Copy the ENTIRE contents
   - Paste into the Supabase SQL Editor
   - Click "Run" (or press Ctrl+Enter)

4. **Verify Tables Created**
   - Go to "Table Editor" in the left sidebar
   - You should see all tables: `users`, `doctor_profiles`, `patient_profiles`, `engagements`, etc.

### Option 2: Using psql Command Line

```bash
# Navigate to your backend directory
cd f:\documents\Nuralhealer-main\Nuralhealer\backend\backend

# Run the SQL file against Supabase
psql -h aws-1-eu-central-1.pooler.supabase.com -p 5432 -d postgres -U postgres.uupudhnxhjyupjgaswsh -f src/main/resources/DB.sql
```

When prompted, enter the password from your `.env` file: `NeuralHealer@026`

### Option 3: Using pgAdmin or DBeaver

1. **Create a new connection** with these details:
   - Host: `aws-1-eu-central-1.pooler.supabase.com`
   - Port: `5432`
   - Database: `postgres`
   - Username: `postgres.uupudhnxhjyupjgaswsh`
   - Password: `NeuralHealer@026`

2. **Execute the SQL file**
   - Open `DB.sql`
   - Execute it against the connection

## After Setup

Once the schema is created, restart your Spring Boot application:

```bash
mvn spring-boot:run
```

You should no longer see "relation does not exist" errors.

## Important Notes

- The schema only needs to be created **once** per database
- Future schema changes should be applied manually to Supabase
- Your local database and Supabase database are **separate** - changes to one don't affect the other
