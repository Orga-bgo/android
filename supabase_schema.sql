-- Supabase Database Schema for MonopolyGo Account Management
-- Run this in Supabase SQL Editor to set up the database

-- Enable UUID extension
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- ============================================
-- ACCOUNTS TABLE
-- ============================================
CREATE TABLE IF NOT EXISTS accounts (
    id BIGSERIAL PRIMARY KEY,
    
    -- Basic Information
    name TEXT NOT NULL UNIQUE,
    user_id TEXT,
    short_link TEXT,
    friend_link TEXT,
    friend_code TEXT,
    
    -- Account Status
    account_status TEXT DEFAULT 'active' 
        CHECK (account_status IN ('active', 'suspended', 'banned', 'inactive')),
    
    -- Suspension Tracking
    suspension_0_days INTEGER DEFAULT 0,
    suspension_3_days INTEGER DEFAULT 0,
    suspension_7_days INTEGER DEFAULT 0,
    suspension_permanent BOOLEAN DEFAULT FALSE,
    suspension_count INTEGER DEFAULT 0,
    
    -- Device Identifiers
    ssaid TEXT,
    gaid TEXT,
    device_id TEXT,
    
    -- Flags
    is_suspended BOOLEAN DEFAULT FALSE,
    has_error BOOLEAN DEFAULT FALSE,
    
    -- Notes
    note TEXT,
    
    -- Timestamps
    last_played TIMESTAMP,
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW(),
    deleted_at TIMESTAMP
);

-- ============================================
-- INDEXES FOR PERFORMANCE
-- ============================================
CREATE INDEX IF NOT EXISTS idx_accounts_name ON accounts(name);
CREATE INDEX IF NOT EXISTS idx_accounts_status ON accounts(account_status);
CREATE INDEX IF NOT EXISTS idx_accounts_deleted ON accounts(deleted_at);
CREATE INDEX IF NOT EXISTS idx_accounts_user_id ON accounts(user_id);
CREATE INDEX IF NOT EXISTS idx_accounts_created_at ON accounts(created_at);
CREATE INDEX IF NOT EXISTS idx_accounts_last_played ON accounts(last_played);

-- ============================================
-- ROW LEVEL SECURITY (RLS)
-- ============================================
ALTER TABLE accounts ENABLE ROW LEVEL SECURITY;

-- ⚠️⚠️⚠️ CRITICAL SECURITY WARNING ⚠️⚠️⚠️
-- The policy below allows UNRESTRICTED anonymous access to ALL account data!
-- This is ONLY suitable for development/testing environments.
-- DO NOT deploy to production with this policy!
--
-- For production, you MUST implement proper authentication and replace
-- this policy with user-specific policies (see examples below).
-- ⚠️⚠️⚠️ END WARNING ⚠️⚠️⚠️

-- Development/Testing: Allow anonymous access
DROP POLICY IF EXISTS "Allow anonymous access" ON accounts;
CREATE POLICY "Allow anonymous access" 
    ON accounts 
    FOR ALL 
    USING (true);

-- Production Example (commented out):
-- Before deploying to production:
-- 1. Remove the "Allow anonymous access" policy above
-- 2. Uncomment and customize the policies below
-- 3. Test thoroughly before deployment
--
-- DROP POLICY IF EXISTS "Allow anonymous access" ON accounts;
-- 
-- CREATE POLICY "Users can view their own accounts"
--     ON accounts FOR SELECT
--     USING (auth.uid()::text = user_id);
-- 
-- CREATE POLICY "Users can insert their own accounts"
--     ON accounts FOR INSERT
--     WITH CHECK (auth.uid()::text = user_id);
-- 
-- CREATE POLICY "Users can update their own accounts"
--     ON accounts FOR UPDATE
--     USING (auth.uid()::text = user_id);
-- 
-- CREATE POLICY "Users can delete their own accounts"
--     ON accounts FOR DELETE
--     USING (auth.uid()::text = user_id);

-- ============================================
-- TRIGGERS
-- ============================================

-- Auto-update updated_at timestamp
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS update_accounts_updated_at ON accounts;
CREATE TRIGGER update_accounts_updated_at
    BEFORE UPDATE ON accounts
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

-- Auto-calculate suspension_count
CREATE OR REPLACE FUNCTION update_suspension_count()
RETURNS TRIGGER AS $$
BEGIN
    NEW.suspension_count = 
        COALESCE(NEW.suspension_0_days, 0) + 
        COALESCE(NEW.suspension_3_days, 0) + 
        COALESCE(NEW.suspension_7_days, 0) + 
        CASE WHEN NEW.suspension_permanent THEN 1 ELSE 0 END;
    
    NEW.is_suspended = (NEW.suspension_count > 0);
    
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS update_accounts_suspension_count ON accounts;
CREATE TRIGGER update_accounts_suspension_count
    BEFORE INSERT OR UPDATE ON accounts
    FOR EACH ROW
    EXECUTE FUNCTION update_suspension_count();

-- ============================================
-- VIEWS FOR COMMON QUERIES
-- ============================================

-- Active accounts (not deleted, not suspended)
CREATE OR REPLACE VIEW active_accounts AS
SELECT 
    id,
    name,
    user_id,
    short_link,
    friend_link,
    friend_code,
    account_status,
    last_played,
    created_at
FROM accounts
WHERE deleted_at IS NULL 
  AND account_status = 'active'
  AND is_suspended = false
ORDER BY name ASC;

-- Suspended accounts
CREATE OR REPLACE VIEW suspended_accounts AS
SELECT 
    id,
    name,
    user_id,
    account_status,
    suspension_0_days,
    suspension_3_days,
    suspension_7_days,
    suspension_permanent,
    suspension_count,
    last_played
FROM accounts
WHERE deleted_at IS NULL 
  AND is_suspended = true
ORDER BY suspension_count DESC, name ASC;

-- Recently played accounts
CREATE OR REPLACE VIEW recently_played_accounts AS
SELECT 
    id,
    name,
    user_id,
    account_status,
    last_played,
    created_at
FROM accounts
WHERE deleted_at IS NULL
  AND last_played IS NOT NULL
ORDER BY last_played DESC
LIMIT 50;

-- ============================================
-- UTILITY FUNCTIONS
-- ============================================

-- Get account statistics
CREATE OR REPLACE FUNCTION get_account_stats()
RETURNS TABLE (
    total_accounts BIGINT,
    active_accounts BIGINT,
    suspended_accounts BIGINT,
    banned_accounts BIGINT,
    total_suspensions BIGINT
) AS $$
BEGIN
    RETURN QUERY
    SELECT 
        COUNT(*) FILTER (WHERE deleted_at IS NULL) as total_accounts,
        COUNT(*) FILTER (WHERE deleted_at IS NULL AND account_status = 'active') as active_accounts,
        COUNT(*) FILTER (WHERE deleted_at IS NULL AND is_suspended = true) as suspended_accounts,
        COUNT(*) FILTER (WHERE deleted_at IS NULL AND account_status = 'banned') as banned_accounts,
        SUM(suspension_count) FILTER (WHERE deleted_at IS NULL) as total_suspensions
    FROM accounts;
END;
$$ LANGUAGE plpgsql;

-- ============================================
-- SAMPLE DATA (for testing)
-- ============================================
-- Uncomment to insert sample data

-- INSERT INTO accounts (name, user_id, account_status, short_link, friend_code) VALUES
-- ('TestAccount1', 'user_001', 'active', 'https://short.link/abc123', 'ABC123'),
-- ('TestAccount2', 'user_002', 'active', 'https://short.link/def456', 'DEF456'),
-- ('SuspendedAccount', 'user_003', 'suspended', 'https://short.link/ghi789', 'GHI789');
-- 
-- UPDATE accounts SET suspension_3_days = 1 WHERE name = 'SuspendedAccount';

-- ============================================
-- VERIFICATION QUERIES
-- ============================================
-- Run these to verify the setup

-- Check table structure
-- SELECT column_name, data_type, is_nullable
-- FROM information_schema.columns
-- WHERE table_name = 'accounts'
-- ORDER BY ordinal_position;

-- Check indexes
-- SELECT indexname, indexdef
-- FROM pg_indexes
-- WHERE tablename = 'accounts';

-- Check triggers
-- SELECT trigger_name, event_manipulation, event_object_table
-- FROM information_schema.triggers
-- WHERE event_object_table = 'accounts';

-- Check RLS policies
-- SELECT policyname, permissive, roles, cmd, qual
-- FROM pg_policies
-- WHERE tablename = 'accounts';

-- Get statistics
-- SELECT * FROM get_account_stats();

-- ============================================
-- SETUP COMPLETE
-- ============================================
-- Your database is now ready for the MonopolyGo Android App!
-- 
-- Next steps:
-- 1. Copy your Supabase URL and Anon Key
-- 2. Update gradle.properties with your credentials
-- 3. Build and run the Android app
-- 4. Test account creation and sync
