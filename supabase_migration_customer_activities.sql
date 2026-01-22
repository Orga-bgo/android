-- ============================================================================
-- MIGRATION: Add Customer Activities Table for Audit Trail
-- Version: 5
-- Description: Adds comprehensive activity tracking for customer management
-- ============================================================================

-- Create customer_activities table
CREATE TABLE IF NOT EXISTS customer_activities (
    id BIGSERIAL PRIMARY KEY,
    customer_id BIGINT REFERENCES customers(id) ON DELETE SET NULL,
    
    -- Activity classification
    activity_type VARCHAR(50) NOT NULL,
    activity_category VARCHAR(50) NOT NULL,
    
    -- Activity details
    description TEXT NOT NULL,
    details TEXT,
    
    -- Related entities
    customer_account_id BIGINT REFERENCES customer_accounts(id) ON DELETE SET NULL,
    
    -- Metadata
    performed_by VARCHAR(100),
    
    -- Timestamp
    created_at TIMESTAMP DEFAULT NOW()
);

-- Add comments
COMMENT ON TABLE customer_activities IS 'Audit trail and activity history for customers';
COMMENT ON COLUMN customer_activities.activity_type IS 'Type of activity performed';
COMMENT ON COLUMN customer_activities.activity_category IS 'Category of the activity (customer/account/service)';
COMMENT ON COLUMN customer_activities.description IS 'Human-readable description of the activity';
COMMENT ON COLUMN customer_activities.details IS 'Detailed information in JSON format (optional)';

-- Create indexes
CREATE INDEX IF NOT EXISTS idx_customer_activities_customer_id ON customer_activities(customer_id);
CREATE INDEX IF NOT EXISTS idx_customer_activities_type ON customer_activities(activity_type);
CREATE INDEX IF NOT EXISTS idx_customer_activities_category ON customer_activities(activity_category);
CREATE INDEX IF NOT EXISTS idx_customer_activities_account_id ON customer_activities(customer_account_id);
CREATE INDEX IF NOT EXISTS idx_customer_activities_created_at ON customer_activities(created_at DESC);

-- Enable RLS
ALTER TABLE customer_activities ENABLE ROW LEVEL SECURITY;

-- Drop existing policies if they exist (idempotent)
DO $$ 
BEGIN
    DROP POLICY IF EXISTS "Allow authenticated read customer_activities" ON customer_activities;
    DROP POLICY IF EXISTS "Allow authenticated insert customer_activities" ON customer_activities;
    DROP POLICY IF EXISTS "Allow authenticated update customer_activities" ON customer_activities;
    DROP POLICY IF EXISTS "Allow authenticated delete customer_activities" ON customer_activities;
EXCEPTION
    WHEN undefined_object THEN NULL;
END $$;

-- Add policies
-- Allow authenticated users to read audit records
CREATE POLICY "Allow authenticated read customer_activities" ON customer_activities
    FOR SELECT
    USING (auth.role() = 'authenticated');

-- Allow authenticated users to insert audit records
CREATE POLICY "Allow authenticated insert customer_activities" ON customer_activities
    FOR INSERT
    WITH CHECK (auth.role() = 'authenticated');

-- Allow authenticated users to update audit records (restricted)
CREATE POLICY "Allow authenticated update customer_activities" ON customer_activities
    FOR UPDATE
    USING (auth.role() = 'authenticated')
    WITH CHECK (auth.role() = 'authenticated');

-- Allow authenticated users to delete audit records (restricted)
CREATE POLICY "Allow authenticated delete customer_activities" ON customer_activities
    FOR DELETE
    USING (auth.role() = 'authenticated');
    
-- Update schema version
INSERT INTO schema_version (version, description) VALUES
(5, 'Added customer_activities table for comprehensive audit trail and activity tracking')
ON CONFLICT (version) DO UPDATE 
SET description = EXCLUDED.description,
    applied_at = NOW();

-- ============================================================================
-- MIGRATION COMPLETE
-- ============================================================================
