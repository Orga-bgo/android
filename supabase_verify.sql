-- ============================================================================
-- BABIXGO MONOPOLYGO MANAGER - SCHEMA VERIFICATION SCRIPT
-- ============================================================================
-- This script verifies that all schema objects exist and are correctly configured
-- Run this after migration to ensure everything is set up properly
-- ============================================================================

\echo '=========================================='
\echo 'SCHEMA VERIFICATION REPORT'
\echo '=========================================='
\echo ''

-- ============================================================================
-- 1. CHECK EXTENSIONS
-- ============================================================================

\echo '1. EXTENSIONS:'
SELECT
    extname as "Extension Name",
    extversion as "Version"
FROM pg_extension
WHERE extname = 'uuid-ossp';

\echo ''

-- ============================================================================
-- 2. CHECK ENUMS
-- ============================================================================

\echo '2. ENUM TYPES:'
SELECT
    typname as "Enum Type",
    array_agg(enumlabel ORDER BY enumsortorder) as "Values"
FROM pg_type t
JOIN pg_enum e ON t.oid = e.enumtypid
WHERE typname IN ('account_status', 'event_status')
GROUP BY typname
ORDER BY typname;

\echo ''

-- ============================================================================
-- 3. CHECK TABLES AND COLUMN COUNTS
-- ============================================================================

\echo '3. TABLES AND COLUMNS:'
SELECT
    table_name as "Table",
    COUNT(*) as "Column Count"
FROM information_schema.columns
WHERE table_schema = 'public'
AND table_name IN ('accounts', 'events', 'customers', 'customer_accounts', 'teams', 'schema_version')
GROUP BY table_name
ORDER BY table_name;

\echo ''
\echo 'Expected column counts:'
\echo '  accounts: 21'
\echo '  customer_accounts: 14'
\echo '  customers: 5'
\echo '  events: 7'
\echo '  teams: 11'
\echo '  schema_version: 3'
\echo ''

-- ============================================================================
-- 4. CHECK MISSING IMPORTANT COLUMNS
-- ============================================================================

\echo '4. CRITICAL COLUMNS CHECK:'

-- Check accounts table critical columns
SELECT
    CASE
        WHEN EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name='accounts' AND column_name='is_customer_account') THEN '✓'
        ELSE '✗'
    END as "Status",
    'accounts.is_customer_account' as "Column"
UNION ALL
SELECT
    CASE
        WHEN EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name='accounts' AND column_name='customer_account_id') THEN '✓'
        ELSE '✗'
    END,
    'accounts.customer_account_id'
UNION ALL
SELECT
    CASE
        WHEN EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name='accounts' AND column_name='suspension_status') THEN '✓'
        ELSE '✗'
    END,
    'accounts.suspension_status'
UNION ALL
SELECT
    CASE
        WHEN EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name='customer_accounts' AND column_name='service_partner') THEN '✓'
        ELSE '✗'
    END,
    'customer_accounts.service_partner'
UNION ALL
SELECT
    CASE
        WHEN EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name='customer_accounts' AND column_name='service_race') THEN '✓'
        ELSE '✗'
    END,
    'customer_accounts.service_race'
UNION ALL
SELECT
    CASE
        WHEN EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name='customer_accounts' AND column_name='service_boost') THEN '✓'
        ELSE '✗'
    END,
    'customer_accounts.service_boost';

\echo ''

-- ============================================================================
-- 5. CHECK FOREIGN KEY CONSTRAINTS
-- ============================================================================

\echo '5. FOREIGN KEY CONSTRAINTS:'
SELECT
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

\echo ''

-- ============================================================================
-- 6. CHECK INDEXES
-- ============================================================================

\echo '6. INDEXES:'
SELECT
    schemaname as "Schema",
    tablename as "Table",
    indexname as "Index Name",
    indexdef as "Definition"
FROM pg_indexes
WHERE schemaname = 'public'
AND tablename IN ('accounts', 'events', 'customers', 'customer_accounts', 'teams')
ORDER BY tablename, indexname;

\echo ''

-- ============================================================================
-- 7. CHECK TRIGGERS
-- ============================================================================

\echo '7. TRIGGERS:'
SELECT
    event_object_table as "Table",
    trigger_name as "Trigger Name",
    action_timing as "Timing",
    event_manipulation as "Event"
FROM information_schema.triggers
WHERE event_object_schema = 'public'
AND event_object_table IN ('accounts', 'events', 'customers', 'customer_accounts', 'teams')
ORDER BY event_object_table, trigger_name;

\echo ''

-- ============================================================================
-- 8. CHECK VIEWS
-- ============================================================================

\echo '8. VIEWS:'
SELECT
    table_name as "View Name",
    view_definition as "Definition (truncated)"
FROM information_schema.views
WHERE table_schema = 'public'
AND table_name IN ('active_accounts', 'teams_with_details', 'events_with_stats')
ORDER BY table_name;

\echo ''

-- ============================================================================
-- 9. CHECK ROW LEVEL SECURITY
-- ============================================================================

\echo '9. ROW LEVEL SECURITY:'
SELECT
    schemaname as "Schema",
    tablename as "Table",
    rowsecurity as "RLS Enabled"
FROM pg_tables
WHERE schemaname = 'public'
AND tablename IN ('accounts', 'events', 'customers', 'customer_accounts', 'teams')
ORDER BY tablename;

\echo ''

-- ============================================================================
-- 10. CHECK POLICIES
-- ============================================================================

\echo '10. RLS POLICIES:'
SELECT
    schemaname as "Schema",
    tablename as "Table",
    policyname as "Policy Name",
    permissive as "Permissive",
    roles as "Roles",
    cmd as "Command"
FROM pg_policies
WHERE schemaname = 'public'
AND tablename IN ('accounts', 'events', 'customers', 'customer_accounts', 'teams')
ORDER BY tablename, policyname;

\echo ''

-- ============================================================================
-- 11. CHECK SCHEMA VERSIONS
-- ============================================================================

\echo '11. SCHEMA VERSION HISTORY:'
SELECT
    version as "Version",
    description as "Description",
    applied_at as "Applied At"
FROM schema_version
ORDER BY version;

\echo ''

-- ============================================================================
-- 12. CHECK TABLE STATISTICS
-- ============================================================================

\echo '12. TABLE ROW COUNTS:'
SELECT
    'accounts' as "Table",
    COUNT(*) as "Row Count"
FROM accounts
UNION ALL
SELECT 'events', COUNT(*) FROM events
UNION ALL
SELECT 'customers', COUNT(*) FROM customers
UNION ALL
SELECT 'customer_accounts', COUNT(*) FROM customer_accounts
UNION ALL
SELECT 'teams', COUNT(*) FROM teams;

\echo ''

-- ============================================================================
-- 13. DETAILED COLUMN INFORMATION
-- ============================================================================

\echo '13. DETAILED COLUMN INFORMATION:'
SELECT
    table_name as "Table",
    column_name as "Column",
    data_type as "Type",
    is_nullable as "Nullable",
    column_default as "Default"
FROM information_schema.columns
WHERE table_schema = 'public'
AND table_name IN ('accounts', 'events', 'customers', 'customer_accounts', 'teams')
ORDER BY table_name, ordinal_position;

\echo ''
\echo '=========================================='
\echo 'VERIFICATION COMPLETE'
\echo '=========================================='
\echo ''
\echo 'Check the output above for any missing objects or inconsistencies.'
\echo 'All tables should have RLS enabled and proper policies configured.'
\echo ''
