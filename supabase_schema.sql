-- ============================================================================
-- BABIXGO MONOPOLYGO MANAGER - SUPABASE SCHEMA
-- ============================================================================

-- Enable UUID extension
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- ============================================================================
-- ENUMS
-- ============================================================================

CREATE TYPE account_status AS ENUM ('active', 'suspended', 'banned', 'inactive');
CREATE TYPE event_status AS ENUM ('planned', 'active', 'completed', 'cancelled');

-- ============================================================================
-- ACCOUNTS TABLE
-- ============================================================================

CREATE TABLE accounts (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE,
    user_id VARCHAR(50), -- MonopolyGo User ID
    short_link TEXT,
    friend_link TEXT,
    friend_code VARCHAR(50),
    
    -- Status
    account_status account_status DEFAULT 'active',
    
    -- Suspension tracking (simplified)
    suspension_status VARCHAR(10) DEFAULT '0' CHECK (suspension_status IN ('0', '3', '7', 'perm')),
    
    -- Device IDs
    ssaid VARCHAR(200),
    gaid VARCHAR(200),
    device_id VARCHAR(200),
    device_token VARCHAR(200),
    app_set_id VARCHAR(200),
    
    -- Flags
    has_error BOOLEAN DEFAULT FALSE,
    
    -- Customer Account Link
    is_customer_account BOOLEAN DEFAULT FALSE,
    customer_account_id BIGINT, -- Foreign key added later after customer_accounts table creation
    
    -- Metadata
    note TEXT,
    last_played TIMESTAMP,
    last_synced_at TIMESTAMP DEFAULT NOW(),
    
    -- Timestamps
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW(),
    deleted_at TIMESTAMP
);

-- ============================================================================
-- EVENTS TABLE (Tycoon Racers)
-- ============================================================================

CREATE TABLE events (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    status event_status DEFAULT 'planned',
    
    -- Timestamps
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW()
);

-- ============================================================================
-- CUSTOMERS TABLE
-- ============================================================================

CREATE TABLE customers (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    notes TEXT,
    
    -- Timestamps
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW()
);

-- ============================================================================
-- CUSTOMER ACTIVITIES TABLE (Audit Trail & History Tracking)
-- ============================================================================

CREATE TABLE customer_activities (
    id BIGSERIAL PRIMARY KEY,
    customer_id BIGINT NOT NULL REFERENCES customers(id) ON DELETE CASCADE,
    
    -- Activity classification
    activity_type VARCHAR(50) NOT NULL, -- 'create', 'update', 'delete', 'account_add', 'account_update', 'account_delete', 'service_change'
    activity_category VARCHAR(50) NOT NULL, -- 'customer', 'account', 'service'
    
    -- Activity details
    description TEXT NOT NULL, -- Human-readable description
    details TEXT, -- JSON with detailed changes
    
    -- Related entities
    customer_account_id BIGINT REFERENCES customer_accounts(id) ON DELETE SET NULL,
    
    -- Metadata
    performed_by VARCHAR(100), -- Optional: user who performed the action
    
    -- Timestamp
    created_at TIMESTAMP DEFAULT NOW()
);

COMMENT ON TABLE customer_activities IS 'Audit trail and activity history for customers';
COMMENT ON COLUMN customer_activities.activity_type IS 'Type of activity performed';
COMMENT ON COLUMN customer_activities.activity_category IS 'Category of the activity (customer/account/service)';
COMMENT ON COLUMN customer_activities.description IS 'Human-readable description of the activity';
COMMENT ON COLUMN customer_activities.details IS 'Detailed information in JSON format (optional)';

-- ============================================================================
-- CUSTOMER ACCOUNTS TABLE
-- ============================================================================

CREATE TABLE customer_accounts (
    id BIGSERIAL PRIMARY KEY,
    customer_id BIGINT NOT NULL REFERENCES customers(id) ON DELETE CASCADE,
    ingame_name VARCHAR(100),
    friend_link TEXT,
    friend_code VARCHAR(50),
    
    -- Services (multiple services possible - checkboxes)
    service_partner BOOLEAN DEFAULT FALSE,
    service_race BOOLEAN DEFAULT FALSE,
    service_boost BOOLEAN DEFAULT FALSE,
    
    -- Partner-specific data
    partner_count INTEGER CHECK (partner_count >= 1 AND partner_count <= 4),
    
    -- Boost-specific data
    backup_account_id BIGINT REFERENCES accounts(id) ON DELETE SET NULL,
    backup_created_at TIMESTAMP,
    credentials_username VARCHAR(100),
    credentials_password TEXT, -- AES-256 encrypted
    
    -- Timestamps
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW()
);

COMMENT ON TABLE customers IS 'Kunden-Verwaltung';
COMMENT ON COLUMN customers.name IS 'Kundenname';
COMMENT ON COLUMN customers.notes IS 'Optionale Notizen zum Kunden';

COMMENT ON TABLE customer_accounts IS 'Accounts eines Kunden mit Services';
COMMENT ON COLUMN customer_accounts.service_partner IS 'Partner-Service aktiviert';
COMMENT ON COLUMN customer_accounts.service_race IS 'Race-Service aktiviert';
COMMENT ON COLUMN customer_accounts.service_boost IS 'Boost-Service aktiviert';
COMMENT ON COLUMN customer_accounts.partner_count IS 'Anzahl Partner (1-4), nur relevant wenn service_partner=true';
COMMENT ON COLUMN customer_accounts.backup_account_id IS 'Verknüpfung zum Account-Backup (nur bei Boost)';
COMMENT ON COLUMN customer_accounts.backup_created_at IS 'Zeitpunkt der Backup-Erstellung';
COMMENT ON COLUMN customer_accounts.credentials_password IS 'Verschlüsseltes Passwort (AES-256)';

COMMENT ON COLUMN accounts.is_customer_account IS 'True = Account gehört zu Kunde und wird nicht in AccountListFragment angezeigt';
COMMENT ON COLUMN accounts.customer_account_id IS 'Verknüpfung zu customer_accounts';

-- ============================================================================
-- TEAMS TABLE
-- ============================================================================

CREATE TABLE teams (
    id BIGSERIAL PRIMARY KEY,
    event_id BIGINT NOT NULL REFERENCES events(id) ON DELETE CASCADE,
    name VARCHAR(100) NOT NULL,
    customer_id BIGINT REFERENCES customers(id) ON DELETE SET NULL,
    
    -- Account slots (4 per team)
    slot_1_account_id BIGINT REFERENCES accounts(id) ON DELETE SET NULL,
    slot_2_account_id BIGINT REFERENCES accounts(id) ON DELETE SET NULL,
    slot_3_account_id BIGINT REFERENCES accounts(id) ON DELETE SET NULL,
    slot_4_account_id BIGINT REFERENCES accounts(id) ON DELETE SET NULL,
    
    -- Timestamps
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW(),
    
    CONSTRAINT unique_team_name_per_event UNIQUE (event_id, name)
);

-- ============================================================================
-- FOREIGN KEY CONSTRAINTS (Added after table creation)
-- ============================================================================
-- 
-- This section contains ALTER TABLE statements to add foreign key constraints
-- that would otherwise create forward references (referencing tables that don't
-- exist yet at the time of table creation).
--
-- Specifically, accounts.customer_account_id references customer_accounts(id),
-- but customer_accounts is created AFTER accounts table, so we add the constraint
-- here using ALTER TABLE to avoid "relation does not exist" errors in PostgreSQL.
-- ============================================================================

-- Add foreign key from accounts to customer_accounts
-- This must be done after customer_accounts table is created to avoid forward reference
ALTER TABLE accounts
    ADD CONSTRAINT fk_accounts_customer_account_id
    FOREIGN KEY (customer_account_id)
    REFERENCES customer_accounts(id)
    ON DELETE SET NULL;

-- ============================================================================
-- INDEXES
-- ============================================================================

-- Accounts
CREATE INDEX idx_accounts_name ON accounts(name);
CREATE INDEX idx_accounts_user_id ON accounts(user_id);
CREATE INDEX idx_accounts_status ON accounts(account_status);
CREATE INDEX idx_accounts_deleted ON accounts(deleted_at) WHERE deleted_at IS NULL;
CREATE INDEX idx_accounts_suspension_status ON accounts(suspension_status);
CREATE INDEX idx_accounts_device_token ON accounts(device_token);
CREATE INDEX idx_accounts_app_set_id ON accounts(app_set_id);
CREATE INDEX idx_accounts_is_customer ON accounts(is_customer_account);
CREATE INDEX idx_accounts_customer_account_id ON accounts(customer_account_id);

-- Events
CREATE INDEX idx_events_date_range ON events(start_date, end_date);
CREATE INDEX idx_events_status ON events(status);

-- Customers
CREATE INDEX idx_customers_name ON customers(name);

-- Customer Accounts
CREATE INDEX idx_customer_accounts_customer_id ON customer_accounts(customer_id);
CREATE INDEX idx_customer_accounts_backup_id ON customer_accounts(backup_account_id);

-- Customer Activities
CREATE INDEX idx_customer_activities_customer_id ON customer_activities(customer_id);
CREATE INDEX idx_customer_activities_type ON customer_activities(activity_type);
CREATE INDEX idx_customer_activities_category ON customer_activities(activity_category);
CREATE INDEX idx_customer_activities_account_id ON customer_activities(customer_account_id);
CREATE INDEX idx_customer_activities_created_at ON customer_activities(created_at DESC);


-- Teams
CREATE INDEX idx_teams_event ON teams(event_id);
CREATE INDEX idx_teams_customer ON teams(customer_id);
CREATE INDEX idx_teams_slots ON teams(slot_1_account_id, slot_2_account_id, slot_3_account_id, slot_4_account_id);

-- ============================================================================
-- TRIGGERS FOR updated_at
-- ============================================================================

-- Function to update updated_at timestamp
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Apply to all tables
CREATE TRIGGER update_accounts_updated_at BEFORE UPDATE ON accounts
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_events_updated_at BEFORE UPDATE ON events
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_customers_updated_at BEFORE UPDATE ON customers
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_customer_accounts_updated_at BEFORE UPDATE ON customer_accounts
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_teams_updated_at BEFORE UPDATE ON teams
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- ============================================================================
-- VIEWS
-- ============================================================================

-- View: Active accounts (not deleted, not suspended)
CREATE OR REPLACE VIEW active_accounts AS
SELECT * FROM accounts 
WHERE deleted_at IS NULL AND suspension_status = '0'
ORDER BY name;

-- View: Teams with account names
CREATE OR REPLACE VIEW teams_with_details AS
SELECT 
    t.id,
    t.event_id,
    t.name AS team_name,
    e.name AS event_name,
    c.name AS customer_name,
    a1.name AS slot_1_name,
    a2.name AS slot_2_name,
    a3.name AS slot_3_name,
    a4.name AS slot_4_name,
    t.created_at,
    t.updated_at
FROM teams t
LEFT JOIN events e ON t.event_id = e.id
LEFT JOIN customers c ON t.customer_id = c.id
LEFT JOIN accounts a1 ON t.slot_1_account_id = a1.id
LEFT JOIN accounts a2 ON t.slot_2_account_id = a2.id
LEFT JOIN accounts a3 ON t.slot_3_account_id = a3.id
LEFT JOIN accounts a4 ON t.slot_4_account_id = a4.id;

-- View: Events with team count
CREATE OR REPLACE VIEW events_with_stats AS
SELECT 
    e.*,
    COUNT(t.id) AS team_count
FROM events e
LEFT JOIN teams t ON e.id = t.event_id
GROUP BY e.id;

-- ============================================================================
-- ROW LEVEL SECURITY (RLS)
-- ============================================================================

-- Enable RLS on all tables
ALTER TABLE accounts ENABLE ROW LEVEL SECURITY;
ALTER TABLE events ENABLE ROW LEVEL SECURITY;
ALTER TABLE customers ENABLE ROW LEVEL SECURITY;
ALTER TABLE customer_accounts ENABLE ROW LEVEL SECURITY;
ALTER TABLE customer_activities ENABLE ROW LEVEL SECURITY;
ALTER TABLE teams ENABLE ROW LEVEL SECURITY;

-- ⚠️ WARNING: Development/Testing Policies
-- These policies allow anonymous access for development and testing.
-- For PRODUCTION use, you should:
-- 1. Remove 'auth.role() = anon' from all policies
-- 2. Enable Supabase authentication in your app
-- 3. Implement user-specific policies based on user_id
-- 4. Consider separate policies for SELECT, INSERT, UPDATE, DELETE

-- Policies: Allow all operations for authenticated and anonymous users
CREATE POLICY "Allow all for authenticated users" ON accounts
    FOR ALL USING (auth.role() = 'authenticated' OR auth.role() = 'anon');

CREATE POLICY "Allow all for authenticated users" ON events
    FOR ALL USING (auth.role() = 'authenticated' OR auth.role() = 'anon');

CREATE POLICY "Allow all for authenticated users" ON customers
    FOR ALL USING (auth.role() = 'authenticated' OR auth.role() = 'anon');

CREATE POLICY "Allow all for authenticated users" ON customer_accounts
    FOR ALL USING (auth.role() = 'authenticated' OR auth.role() = 'anon');

CREATE POLICY "Allow all for authenticated users" ON customer_activities
    FOR ALL USING (auth.role() = 'authenticated' OR auth.role() = 'anon');

CREATE POLICY "Allow all for authenticated users" ON teams
    FOR ALL USING (auth.role() = 'authenticated' OR auth.role() = 'anon');

-- ============================================================================
-- SAMPLE DATA (Optional - for testing)
-- ============================================================================

-- ⚠️ IMPORTANT: Comment out this section for production deployments!
-- This sample data is for development and testing only.

/*
-- Sample accounts
INSERT INTO accounts (name, user_id, account_status) VALUES
('Test_Account_1', '123456789', 'active'),
('Test_Account_2', '987654321', 'active'),
('Test_Account_3', '555555555', 'suspended');

-- Sample event
INSERT INTO events (name, start_date, end_date, status) VALUES
('TR-001', '2024-02-01', '2024-02-05', 'planned');

-- Sample customer
INSERT INTO customers (name, notes) VALUES
('Test Customer', 'Sample customer for testing');

-- Sample customer account
INSERT INTO customer_accounts (customer_id, ingame_name, friend_code, service_partner, partner_count) VALUES
(1, 'TestPlayerName', 'FRIEND123', true, 4);

-- Sample team
INSERT INTO teams (event_id, name, customer_id, slot_1_account_id) VALUES
(1, 'Team Alpha', 1, 1);
*/

-- ============================================================================
-- SCHEMA VERSION
-- ============================================================================

CREATE TABLE IF NOT EXISTS schema_version (
    version INTEGER PRIMARY KEY,
    applied_at TIMESTAMP DEFAULT NOW(),
    description TEXT
);

INSERT INTO schema_version (version, description) VALUES
(1, 'Initial schema with accounts, events, customers, and teams'),
(2, 'Simplified suspension tracking - single status field instead of 4 counters'),
(3, 'Customer management restructure - multi-account support with services'),
(4, 'Fixed forward reference error - customer_account_id constraint moved to ALTER TABLE'),
(5, 'Added customer_activities table for comprehensive audit trail and activity tracking');

-- ============================================================================
-- MIGRATION NOTES
-- ============================================================================

-- Migration from version 1 to version 2:
-- If you have existing data with old suspension fields, run this migration:
/*
ALTER TABLE accounts DROP COLUMN IF EXISTS suspension_0_days CASCADE;
ALTER TABLE accounts DROP COLUMN IF EXISTS suspension_3_days CASCADE;
ALTER TABLE accounts DROP COLUMN IF EXISTS suspension_7_days CASCADE;
ALTER TABLE accounts DROP COLUMN IF EXISTS suspension_permanent CASCADE;
ALTER TABLE accounts DROP COLUMN IF EXISTS suspension_count CASCADE;
ALTER TABLE accounts DROP COLUMN IF EXISTS is_suspended CASCADE;

ALTER TABLE accounts ADD COLUMN IF NOT EXISTS suspension_status VARCHAR(10) DEFAULT '0';
ALTER TABLE accounts ADD CONSTRAINT check_suspension_status 
    CHECK (suspension_status IN ('0', '3', '7', 'perm'));
CREATE INDEX IF NOT EXISTS idx_accounts_suspension_status ON accounts(suspension_status);
UPDATE accounts SET suspension_status = '0' WHERE suspension_status IS NULL;

-- Recreate active_accounts view
CREATE OR REPLACE VIEW active_accounts AS
SELECT * FROM accounts 
WHERE deleted_at IS NULL AND suspension_status = '0'
ORDER BY name;
*/

-- Migration from version 3 to version 4:
-- Fix for forward reference error in accounts.customer_account_id
-- If you deployed version 3 and it failed due to forward reference, this fixes it:
/*
-- Drop the constraint if it was partially created
ALTER TABLE accounts DROP CONSTRAINT IF EXISTS accounts_customer_account_id_fkey;

-- Ensure the column exists (it should already be there)
ALTER TABLE accounts ADD COLUMN IF NOT EXISTS customer_account_id BIGINT;

-- Add the foreign key constraint properly
ALTER TABLE accounts
    ADD CONSTRAINT fk_accounts_customer_account_id
    FOREIGN KEY (customer_account_id)
    REFERENCES customer_accounts(id)
    ON DELETE SET NULL;
*/

-- ============================================================================
-- COMPLETION
-- ============================================================================

-- Schema erfolgreich erstellt!
-- Nächste Schritte:
-- 1. Kopiere Project URL aus Supabase Settings → API
-- 2. Kopiere anon/public key aus Supabase Settings → API
-- 3. Füge beides in gradle.properties ein
-- 4. Gradle Sync & Build APK
