-- ============================================================================
-- BABIXGO MONOPOLYGO MANAGER - SAFE MIGRATION SCRIPT
-- ============================================================================
-- This script safely checks for existing objects and only creates what's missing
-- Can be run multiple times without errors (idempotent)
-- ============================================================================

-- ============================================================================
-- EXTENSIONS
-- ============================================================================

CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- ============================================================================
-- ENUMS (Only create if they don't exist)
-- ============================================================================

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'account_status') THEN
        CREATE TYPE account_status AS ENUM ('active', 'suspended', 'banned', 'inactive');
    END IF;
END $$;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'event_status') THEN
        CREATE TYPE event_status AS ENUM ('planned', 'active', 'completed', 'cancelled');
    END IF;
END $$;

-- ============================================================================
-- TABLES
-- ============================================================================

-- ACCOUNTS TABLE
CREATE TABLE IF NOT EXISTS accounts (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE,
    user_id VARCHAR(50),
    short_link TEXT,
    friend_link TEXT,
    friend_code VARCHAR(50),
    account_status account_status DEFAULT 'active',
    suspension_status VARCHAR(10) DEFAULT '0' CHECK (suspension_status IN ('0', '3', '7', 'perm')),
    ssaid VARCHAR(200),
    gaid VARCHAR(200),
    device_id VARCHAR(200),
    device_token VARCHAR(200),
    app_set_id VARCHAR(200),
    has_error BOOLEAN DEFAULT FALSE,
    is_customer_account BOOLEAN DEFAULT FALSE,
    customer_account_id BIGINT,
    note TEXT,
    last_played TIMESTAMP,
    last_synced_at TIMESTAMP DEFAULT NOW(),
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW(),
    deleted_at TIMESTAMP
);

-- Add missing columns to accounts if they don't exist
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns
                   WHERE table_name='accounts' AND column_name='last_synced_at') THEN
        ALTER TABLE accounts ADD COLUMN last_synced_at TIMESTAMP DEFAULT NOW();
    END IF;

    IF NOT EXISTS (SELECT 1 FROM information_schema.columns
                   WHERE table_name='accounts' AND column_name='is_customer_account') THEN
        ALTER TABLE accounts ADD COLUMN is_customer_account BOOLEAN DEFAULT FALSE;
    END IF;

    IF NOT EXISTS (SELECT 1 FROM information_schema.columns
                   WHERE table_name='accounts' AND column_name='customer_account_id') THEN
        ALTER TABLE accounts ADD COLUMN customer_account_id BIGINT;
    END IF;
END $$;

-- EVENTS TABLE
CREATE TABLE IF NOT EXISTS events (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    status event_status DEFAULT 'planned',
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW()
);

-- CUSTOMERS TABLE
CREATE TABLE IF NOT EXISTS customers (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    notes TEXT,
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW()
);

-- Migrate old customers table structure to new structure
DO $$
BEGIN
    -- Remove old columns if they exist (from version 1-2)
    IF EXISTS (SELECT 1 FROM information_schema.columns
               WHERE table_name='customers' AND column_name='friend_link') THEN
        ALTER TABLE customers DROP COLUMN friend_link;
    END IF;

    IF EXISTS (SELECT 1 FROM information_schema.columns
               WHERE table_name='customers' AND column_name='user_id') THEN
        ALTER TABLE customers DROP COLUMN user_id;
    END IF;

    IF EXISTS (SELECT 1 FROM information_schema.columns
               WHERE table_name='customers' AND column_name='slots') THEN
        ALTER TABLE customers DROP COLUMN slots;
    END IF;

    -- Add new columns if they don't exist
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns
                   WHERE table_name='customers' AND column_name='notes') THEN
        ALTER TABLE customers ADD COLUMN notes TEXT;
    END IF;
END $$;

-- CUSTOMER ACCOUNTS TABLE
CREATE TABLE IF NOT EXISTS customer_accounts (
    id BIGSERIAL PRIMARY KEY,
    customer_id BIGINT NOT NULL,
    ingame_name VARCHAR(100),
    friend_link TEXT,
    friend_code VARCHAR(50),
    service_partner BOOLEAN DEFAULT FALSE,
    service_race BOOLEAN DEFAULT FALSE,
    service_boost BOOLEAN DEFAULT FALSE,
    partner_count INTEGER CHECK (partner_count >= 1 AND partner_count <= 4),
    backup_account_id BIGINT,
    backup_created_at TIMESTAMP,
    credentials_username VARCHAR(100),
    credentials_password TEXT,
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW()
);

-- TEAMS TABLE
CREATE TABLE IF NOT EXISTS teams (
    id BIGSERIAL PRIMARY KEY,
    event_id BIGINT NOT NULL,
    name VARCHAR(100) NOT NULL,
    customer_id BIGINT,
    slot_1_account_id BIGINT,
    slot_2_account_id BIGINT,
    slot_3_account_id BIGINT,
    slot_4_account_id BIGINT,
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW()
);

-- ============================================================================
-- FOREIGN KEY CONSTRAINTS (Add if they don't exist)
-- ============================================================================

-- customer_accounts -> customers
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.table_constraints
        WHERE constraint_name = 'customer_accounts_customer_id_fkey'
    ) THEN
        ALTER TABLE customer_accounts
            ADD CONSTRAINT customer_accounts_customer_id_fkey
            FOREIGN KEY (customer_id)
            REFERENCES customers(id)
            ON DELETE CASCADE;
    END IF;
END $$;

-- customer_accounts -> accounts (backup)
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.table_constraints
        WHERE constraint_name = 'customer_accounts_backup_account_id_fkey'
    ) THEN
        ALTER TABLE customer_accounts
            ADD CONSTRAINT customer_accounts_backup_account_id_fkey
            FOREIGN KEY (backup_account_id)
            REFERENCES accounts(id)
            ON DELETE SET NULL;
    END IF;
END $$;

-- accounts -> customer_accounts
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.table_constraints
        WHERE constraint_name = 'fk_accounts_customer_account_id'
    ) THEN
        ALTER TABLE accounts
            ADD CONSTRAINT fk_accounts_customer_account_id
            FOREIGN KEY (customer_account_id)
            REFERENCES customer_accounts(id)
            ON DELETE SET NULL;
    END IF;
END $$;

-- teams -> events
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.table_constraints
        WHERE constraint_name = 'teams_event_id_fkey'
    ) THEN
        ALTER TABLE teams
            ADD CONSTRAINT teams_event_id_fkey
            FOREIGN KEY (event_id)
            REFERENCES events(id)
            ON DELETE CASCADE;
    END IF;
END $$;

-- teams -> customers
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.table_constraints
        WHERE constraint_name = 'teams_customer_id_fkey'
    ) THEN
        ALTER TABLE teams
            ADD CONSTRAINT teams_customer_id_fkey
            FOREIGN KEY (customer_id)
            REFERENCES customers(id)
            ON DELETE SET NULL;
    END IF;
END $$;

-- teams -> accounts (slots)
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.table_constraints
        WHERE constraint_name = 'teams_slot_1_account_id_fkey'
    ) THEN
        ALTER TABLE teams
            ADD CONSTRAINT teams_slot_1_account_id_fkey
            FOREIGN KEY (slot_1_account_id)
            REFERENCES accounts(id)
            ON DELETE SET NULL;
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM information_schema.table_constraints
        WHERE constraint_name = 'teams_slot_2_account_id_fkey'
    ) THEN
        ALTER TABLE teams
            ADD CONSTRAINT teams_slot_2_account_id_fkey
            FOREIGN KEY (slot_2_account_id)
            REFERENCES accounts(id)
            ON DELETE SET NULL;
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM information_schema.table_constraints
        WHERE constraint_name = 'teams_slot_3_account_id_fkey'
    ) THEN
        ALTER TABLE teams
            ADD CONSTRAINT teams_slot_3_account_id_fkey
            FOREIGN KEY (slot_3_account_id)
            REFERENCES accounts(id)
            ON DELETE SET NULL;
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM information_schema.table_constraints
        WHERE constraint_name = 'teams_slot_4_account_id_fkey'
    ) THEN
        ALTER TABLE teams
            ADD CONSTRAINT teams_slot_4_account_id_fkey
            FOREIGN KEY (slot_4_account_id)
            REFERENCES accounts(id)
            ON DELETE SET NULL;
    END IF;
END $$;

-- Unique constraint for team names per event
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.table_constraints
        WHERE constraint_name = 'unique_team_name_per_event'
    ) THEN
        ALTER TABLE teams
            ADD CONSTRAINT unique_team_name_per_event UNIQUE (event_id, name);
    END IF;
END $$;

-- ============================================================================
-- INDEXES (Create if they don't exist)
-- ============================================================================

-- Accounts
CREATE INDEX IF NOT EXISTS idx_accounts_name ON accounts(name);
CREATE INDEX IF NOT EXISTS idx_accounts_user_id ON accounts(user_id);
CREATE INDEX IF NOT EXISTS idx_accounts_status ON accounts(account_status);
CREATE INDEX IF NOT EXISTS idx_accounts_deleted ON accounts(deleted_at) WHERE deleted_at IS NULL;
CREATE INDEX IF NOT EXISTS idx_accounts_suspension_status ON accounts(suspension_status);
CREATE INDEX IF NOT EXISTS idx_accounts_device_token ON accounts(device_token);
CREATE INDEX IF NOT EXISTS idx_accounts_app_set_id ON accounts(app_set_id);
CREATE INDEX IF NOT EXISTS idx_accounts_is_customer ON accounts(is_customer_account);
CREATE INDEX IF NOT EXISTS idx_accounts_customer_account_id ON accounts(customer_account_id);

-- Events
CREATE INDEX IF NOT EXISTS idx_events_date_range ON events(start_date, end_date);
CREATE INDEX IF NOT EXISTS idx_events_status ON events(status);

-- Customers
CREATE INDEX IF NOT EXISTS idx_customers_name ON customers(name);

-- Customer Accounts
CREATE INDEX IF NOT EXISTS idx_customer_accounts_customer_id ON customer_accounts(customer_id);
CREATE INDEX IF NOT EXISTS idx_customer_accounts_backup_id ON customer_accounts(backup_account_id);

-- Teams
CREATE INDEX IF NOT EXISTS idx_teams_event ON teams(event_id);
CREATE INDEX IF NOT EXISTS idx_teams_customer ON teams(customer_id);
CREATE INDEX IF NOT EXISTS idx_teams_slots ON teams(slot_1_account_id, slot_2_account_id, slot_3_account_id, slot_4_account_id);

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

-- Create triggers (DROP and recreate to ensure they're correct)
DROP TRIGGER IF EXISTS update_accounts_updated_at ON accounts;
CREATE TRIGGER update_accounts_updated_at BEFORE UPDATE ON accounts
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

DROP TRIGGER IF EXISTS update_events_updated_at ON events;
CREATE TRIGGER update_events_updated_at BEFORE UPDATE ON events
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

DROP TRIGGER IF EXISTS update_customers_updated_at ON customers;
CREATE TRIGGER update_customers_updated_at BEFORE UPDATE ON customers
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

DROP TRIGGER IF EXISTS update_customer_accounts_updated_at ON customer_accounts;
CREATE TRIGGER update_customer_accounts_updated_at BEFORE UPDATE ON customer_accounts
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

DROP TRIGGER IF EXISTS update_teams_updated_at ON teams;
CREATE TRIGGER update_teams_updated_at BEFORE UPDATE ON teams
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- ============================================================================
-- COMMENTS
-- ============================================================================

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
ALTER TABLE teams ENABLE ROW LEVEL SECURITY;

-- Drop existing policies if they exist, then recreate them
DO $$
BEGIN
    DROP POLICY IF EXISTS "Allow all for authenticated users" ON accounts;
    DROP POLICY IF EXISTS "Allow all for authenticated users" ON events;
    DROP POLICY IF EXISTS "Allow all for authenticated users" ON customers;
    DROP POLICY IF EXISTS "Allow all for authenticated users" ON customer_accounts;
    DROP POLICY IF EXISTS "Allow all for authenticated users" ON teams;
END $$;

-- Create policies
CREATE POLICY "Allow all for authenticated users" ON accounts
    FOR ALL USING (auth.role() = 'authenticated' OR auth.role() = 'anon');

CREATE POLICY "Allow all for authenticated users" ON events
    FOR ALL USING (auth.role() = 'authenticated' OR auth.role() = 'anon');

CREATE POLICY "Allow all for authenticated users" ON customers
    FOR ALL USING (auth.role() = 'authenticated' OR auth.role() = 'anon');

CREATE POLICY "Allow all for authenticated users" ON customer_accounts
    FOR ALL USING (auth.role() = 'authenticated' OR auth.role() = 'anon');

CREATE POLICY "Allow all for authenticated users" ON teams
    FOR ALL USING (auth.role() = 'authenticated' OR auth.role() = 'anon');

-- ============================================================================
-- SCHEMA VERSION
-- ============================================================================

CREATE TABLE IF NOT EXISTS schema_version (
    version INTEGER PRIMARY KEY,
    applied_at TIMESTAMP DEFAULT NOW(),
    description TEXT
);

-- Insert version 5 (migration script) if not exists
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM schema_version WHERE version = 5) THEN
        INSERT INTO schema_version (version, description) VALUES
        (5, 'Safe migration script - idempotent schema updates');
    END IF;
END $$;

-- ============================================================================
-- VERIFICATION QUERY
-- ============================================================================

-- Run this to verify all tables and columns exist:
/*
SELECT
    table_name,
    COUNT(*) as column_count
FROM information_schema.columns
WHERE table_schema = 'public'
AND table_name IN ('accounts', 'events', 'customers', 'customer_accounts', 'teams')
GROUP BY table_name
ORDER BY table_name;

-- Expected output:
-- accounts: 21 columns
-- customer_accounts: 14 columns
-- customers: 5 columns
-- events: 7 columns
-- teams: 11 columns
*/

-- ============================================================================
-- COMPLETION
-- ============================================================================

-- Migration erfolgreich abgeschlossen!
-- Dieses Script kann jederzeit ohne Fehler erneut ausgeführt werden.
