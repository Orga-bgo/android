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
    
    -- Suspension tracking
    suspension_0_days INTEGER DEFAULT 0,
    suspension_3_days INTEGER DEFAULT 0,
    suspension_7_days INTEGER DEFAULT 0,
    suspension_permanent BOOLEAN DEFAULT FALSE,
    suspension_count INTEGER GENERATED ALWAYS AS (
        suspension_0_days + suspension_3_days + suspension_7_days + 
        CASE WHEN suspension_permanent THEN 1 ELSE 0 END
    ) STORED,
    
    -- Device IDs
    ssaid VARCHAR(200),
    gaid VARCHAR(200),
    device_id VARCHAR(200),
    
    -- Flags
    is_suspended BOOLEAN GENERATED ALWAYS AS (
        suspension_0_days > 0 OR suspension_3_days > 0 OR 
        suspension_7_days > 0 OR suspension_permanent
    ) STORED,
    has_error BOOLEAN DEFAULT FALSE,
    
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
    friend_link TEXT NOT NULL,
    friend_code VARCHAR(50),
    user_id VARCHAR(50), -- Extracted from friend link
    slots INTEGER DEFAULT 4, -- Number of account slots
    
    -- Timestamps
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW()
);

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
-- INDEXES
-- ============================================================================

-- Accounts
CREATE INDEX idx_accounts_name ON accounts(name);
CREATE INDEX idx_accounts_user_id ON accounts(user_id);
CREATE INDEX idx_accounts_status ON accounts(account_status);
CREATE INDEX idx_accounts_deleted ON accounts(deleted_at) WHERE deleted_at IS NULL;
CREATE INDEX idx_accounts_suspended ON accounts(is_suspended);

-- Events
CREATE INDEX idx_events_date_range ON events(start_date, end_date);
CREATE INDEX idx_events_status ON events(status);

-- Customers
CREATE INDEX idx_customers_name ON customers(name);
CREATE INDEX idx_customers_user_id ON customers(user_id);

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

CREATE TRIGGER update_teams_updated_at BEFORE UPDATE ON teams
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- ============================================================================
-- VIEWS
-- ============================================================================

-- View: Active accounts (not deleted, not suspended)
CREATE OR REPLACE VIEW active_accounts AS
SELECT * FROM accounts 
WHERE deleted_at IS NULL AND NOT is_suspended
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
INSERT INTO customers (name, friend_link, user_id, slots) VALUES
('Test Customer', 'https://mply.gg/add-friend/123456789', '123456789', 4);

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
(1, 'Initial schema with accounts, events, customers, and teams');

-- ============================================================================
-- COMPLETION
-- ============================================================================

-- Schema erfolgreich erstellt!
-- Nächste Schritte:
-- 1. Kopiere Project URL aus Supabase Settings → API
-- 2. Kopiere anon/public key aus Supabase Settings → API
-- 3. Füge beides in gradle.properties ein
-- 4. Gradle Sync & Build APK
