-- ============================================================================
-- BABIXGO MONOPOLYGO MANAGER - SCHEMA VERIFICATION SCRIPT
-- ============================================================================
-- This script verifies that all schema objects exist and are correctly configured
-- Run this after migration to ensure everything is set up properly
--
-- USAGE: Copy and paste this entire script into Supabase SQL Editor and run it
-- Each SELECT statement will produce a result showing the verification status
-- ============================================================================

-- ============================================================================
-- 1. CHECK EXTENSIONS
-- ============================================================================

-- Expected: uuid-ossp extension should be installed
SELECT
    '1. EXTENSIONS' as "Check",
    extname as "Extension Name",
    extversion as "Version"
FROM pg_extension
WHERE extname = 'uuid-ossp';

-- ============================================================================
-- 2. CHECK ENUMS
-- ============================================================================

-- Expected: account_status and event_status enums with their values
SELECT
    '2. ENUM TYPES' as "Check",
    typname as "Enum Type",
    array_agg(enumlabel ORDER BY enumsortorder) as "Values"
FROM pg_type t
JOIN pg_enum e ON t.oid = e.enumtypid
WHERE typname IN ('account_status', 'event_status')
GROUP BY typname
ORDER BY typname;

-- ============================================================================
-- 3. CHECK TABLES AND COLUMN COUNTS
-- ============================================================================

-- Expected column counts:
--   accounts: 21
--   customer_accounts: 14
--   customers: 5
--   events: 7
--   teams: 11
--   schema_version: 3

SELECT
    '3. TABLES & COLUMNS' as "Check",
    table_name as "Table",
    COUNT(*) as "Column Count",
    CASE table_name
        WHEN 'accounts' THEN CASE WHEN COUNT(*) = 21 THEN '✓' ELSE '✗ Expected 21' END
        WHEN 'customer_accounts' THEN CASE WHEN COUNT(*) = 14 THEN '✓' ELSE '✗ Expected 14' END
        WHEN 'customers' THEN CASE WHEN COUNT(*) = 5 THEN '✓' ELSE '✗ Expected 5' END
        WHEN 'events' THEN CASE WHEN COUNT(*) = 7 THEN '✓' ELSE '✗ Expected 7' END
        WHEN 'teams' THEN CASE WHEN COUNT(*) = 11 THEN '✓' ELSE '✗ Expected 11' END
        WHEN 'schema_version' THEN CASE WHEN COUNT(*) = 3 THEN '✓' ELSE '✗ Expected 3' END
        ELSE '?'
    END as "Status"
FROM information_schema.columns
WHERE table_schema = 'public'
AND table_name IN ('accounts', 'events', 'customers', 'customer_accounts', 'teams', 'schema_version')
GROUP BY table_name
ORDER BY table_name;

-- ============================================================================
-- 4. CHECK CRITICAL COLUMNS
-- ============================================================================

-- Expected: All critical columns should exist (Status = ✓)
SELECT
    '4. CRITICAL COLUMNS' as "Check",
    CASE
        WHEN EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name='accounts' AND column_name='is_customer_account') THEN '✓'
        ELSE '✗'
    END as "Status",
    'accounts.is_customer_account' as "Column"
UNION ALL
SELECT
    '4. CRITICAL COLUMNS',
    CASE
        WHEN EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name='accounts' AND column_name='customer_account_id') THEN '✓'
        ELSE '✗'
    END,
    'accounts.customer_account_id'
UNION ALL
SELECT
    '4. CRITICAL COLUMNS',
    CASE
        WHEN EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name='accounts' AND column_name='suspension_status') THEN '✓'
        ELSE '✗'
    END,
    'accounts.suspension_status'
UNION ALL
SELECT
    '4. CRITICAL COLUMNS',
    CASE
        WHEN EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name='customer_accounts' AND column_name='service_partner') THEN '✓'
        ELSE '✗'
    END,
    'customer_accounts.service_partner'
UNION ALL
SELECT
    '4. CRITICAL COLUMNS',
    CASE
        WHEN EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name='customer_accounts' AND column_name='service_race') THEN '✓'
        ELSE '✗'
    END,
    'customer_accounts.service_race'
UNION ALL
SELECT
    '4. CRITICAL COLUMNS',
    CASE
        WHEN EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name='customer_accounts' AND column_name='service_boost') THEN '✓'
        ELSE '✗'
    END,
    'customer_accounts.service_boost';

-- ============================================================================
-- 5. CHECK FOREIGN KEY CONSTRAINTS
-- ============================================================================

-- Expected: 10 foreign key constraints across all tables
SELECT
    '5. FOREIGN KEYS' as "Check",
    tc.constraint_name as "Constraint Name",
    tc.table_name as "From Table",
    kcu.column_name as "From Column",
    ccu.table_name as "To Table",
    ccu.column_name as "To Column"
FROM information_schema.table_constraints AS tc
JOIN information_schema.key_column_usage AS kcu
    ON tc.constraint_name = kcu.constraint_name
    AND tc.table_schema = kcu.table_schema
JOIN information_schema.constraint_column_usage AS ccu
    ON ccu.constraint_name = tc.constraint_name
    AND ccu.table_schema = tc.table_schema
WHERE tc.constraint_type = 'FOREIGN KEY'
AND tc.table_name IN ('accounts', 'events', 'customers', 'customer_accounts', 'teams')
ORDER BY tc.table_name, tc.constraint_name;

-- ============================================================================
-- 6. CHECK INDEXES
-- ============================================================================

-- Expected: Multiple indexes for performance optimization
SELECT
    '6. INDEXES' as "Check",
    tablename as "Table",
    indexname as "Index Name",
    CASE WHEN indexdef LIKE '%UNIQUE%' THEN 'UNIQUE' ELSE 'INDEX' END as "Type"
FROM pg_indexes
WHERE schemaname = 'public'
AND tablename IN ('accounts', 'events', 'customers', 'customer_accounts', 'teams')
ORDER BY tablename, indexname;

-- ============================================================================
-- 7. CHECK TRIGGERS
-- ============================================================================

-- Expected: 5 triggers (one update_*_updated_at trigger per table)
SELECT
    '7. TRIGGERS' as "Check",
    event_object_table as "Table",
    trigger_name as "Trigger Name",
    action_timing as "Timing",
    event_manipulation as "Event"
FROM information_schema.triggers
WHERE event_object_schema = 'public'
AND event_object_table IN ('accounts', 'events', 'customers', 'customer_accounts', 'teams')
ORDER BY event_object_table, trigger_name;

-- ============================================================================
-- 8. CHECK VIEWS
-- ============================================================================

-- Expected: 3 views (active_accounts, teams_with_details, events_with_stats)
SELECT
    '8. VIEWS' as "Check",
    table_name as "View Name",
    CASE
        WHEN table_name = 'active_accounts' THEN '✓'
        WHEN table_name = 'teams_with_details' THEN '✓'
        WHEN table_name = 'events_with_stats' THEN '✓'
        ELSE '?'
    END as "Status"
FROM information_schema.views
WHERE table_schema = 'public'
AND table_name IN ('active_accounts', 'teams_with_details', 'events_with_stats')
ORDER BY table_name;

-- ============================================================================
-- 9. CHECK ROW LEVEL SECURITY
-- ============================================================================

-- Expected: All tables should have RLS enabled (rowsecurity = true)
SELECT
    '9. ROW LEVEL SECURITY' as "Check",
    tablename as "Table",
    CASE WHEN rowsecurity THEN '✓ Enabled' ELSE '✗ Disabled' END as "RLS Status"
FROM pg_tables
WHERE schemaname = 'public'
AND tablename IN ('accounts', 'events', 'customers', 'customer_accounts', 'teams')
ORDER BY tablename;

-- ============================================================================
-- 10. CHECK POLICIES
-- ============================================================================

-- Expected: Each table should have at least one policy
SELECT
    '10. RLS POLICIES' as "Check",
    tablename as "Table",
    policyname as "Policy Name",
    cmd as "Command",
    CASE WHEN 'anon' = ANY(roles) THEN '⚠ Allows Anonymous' ELSE '✓' END as "Security Note"
FROM pg_policies
WHERE schemaname = 'public'
AND tablename IN ('accounts', 'events', 'customers', 'customer_accounts', 'teams')
ORDER BY tablename, policyname;

-- ============================================================================
-- 11. CHECK SCHEMA VERSIONS
-- ============================================================================

-- Expected: Versions 1-5 should be present
SELECT
    '11. SCHEMA VERSIONS' as "Check",
    version as "Version",
    description as "Description",
    applied_at as "Applied At"
FROM schema_version
ORDER BY version;

-- ============================================================================
-- 12. CHECK TABLE ROW COUNTS
-- ============================================================================

-- Shows how many records are in each table
SELECT
    '12. TABLE STATISTICS' as "Check",
    'accounts' as "Table",
    COUNT(*) as "Row Count"
FROM accounts
UNION ALL
SELECT '12. TABLE STATISTICS', 'events', COUNT(*) FROM events
UNION ALL
SELECT '12. TABLE STATISTICS', 'customers', COUNT(*) FROM customers
UNION ALL
SELECT '12. TABLE STATISTICS', 'customer_accounts', COUNT(*) FROM customer_accounts
UNION ALL
SELECT '12. TABLE STATISTICS', 'teams', COUNT(*) FROM teams;

-- ============================================================================
-- 13. SUMMARY CHECK
-- ============================================================================

-- Overall summary of schema health
SELECT
    '13. SUMMARY' as "Check",
    'Tables' as "Component",
    COUNT(DISTINCT table_name)::text || ' / 6 expected' as "Status"
FROM information_schema.tables
WHERE table_schema = 'public'
AND table_name IN ('accounts', 'events', 'customers', 'customer_accounts', 'teams', 'schema_version')

UNION ALL

SELECT
    '13. SUMMARY',
    'Views',
    COUNT(DISTINCT table_name)::text || ' / 3 expected'
FROM information_schema.views
WHERE table_schema = 'public'
AND table_name IN ('active_accounts', 'teams_with_details', 'events_with_stats')

UNION ALL

SELECT
    '13. SUMMARY',
    'Triggers',
    COUNT(DISTINCT trigger_name)::text || ' / 5 expected'
FROM information_schema.triggers
WHERE event_object_schema = 'public'
AND event_object_table IN ('accounts', 'events', 'customers', 'customer_accounts', 'teams')

UNION ALL

SELECT
    '13. SUMMARY',
    'Foreign Keys',
    COUNT(DISTINCT constraint_name)::text || ' / 10 expected'
FROM information_schema.table_constraints
WHERE constraint_type = 'FOREIGN KEY'
AND table_name IN ('accounts', 'events', 'customers', 'customer_accounts', 'teams');

-- ============================================================================
-- VERIFICATION COMPLETE
-- ============================================================================
-- Check all results above. Look for:
-- ✓ All tables have correct column counts
-- ✓ All critical columns exist
-- ✓ All foreign keys are present
-- ✓ All triggers are active
-- ✓ All views exist
-- ✓ RLS is enabled on all tables
-- ⚠ Note: Anonymous access is currently allowed (development mode)
-- ============================================================================
