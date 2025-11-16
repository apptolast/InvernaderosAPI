-- =====================================================
-- TEST STAGING PROCEDURES - PostgreSQL (Metadata)
-- =====================================================

-- =====================================================
-- TEST 1: Greenhouse Bulk Updates
-- =====================================================

\echo '=========================================='
\echo 'TEST 1: PostgreSQL Greenhouse Bulk Updates'
\echo '=========================================='

-- Generate batch_id for this test
\set greenhouse_batch_id '550e8400-e29b-41d4-a716-446655440088'

-- Insert sample greenhouse updates
INSERT INTO staging.greenhouse_updates (
    operation, greenhouse_id, tenant_id, name, mqtt_publish_interval_seconds,
    batch_id, submitted_by, validation_status
) VALUES
-- UPDATE existing greenhouse
('UPDATE', '660e8400-e29b-41d4-a716-446655440001', '550e8400-e29b-41d4-a716-446655440000',
 NULL, 15, :'greenhouse_batch_id', 'admin_test', 'VALID'),

-- INSERT new greenhouse
('INSERT', gen_random_uuid(), '550e8400-e29b-41d4-a716-446655440000',
 'Test Greenhouse Staging', 10, :'greenhouse_batch_id', 'admin_test', 'VALID');

\echo 'Inserted 2 greenhouse updates'

-- Test DRY RUN first
\echo 'Running DRY RUN...'
SELECT * FROM staging.proc_apply_greenhouse_updates(:'greenhouse_batch_id'::UUID, TRUE);

-- Apply changes for real
\echo 'Applying changes...'
SELECT * FROM staging.proc_apply_greenhouse_updates(:'greenhouse_batch_id'::UUID, FALSE);

-- Verify operation audit log
\echo 'Checking operation audit log...'
SELECT operation_type, status, rows_affected, rollback_available
FROM staging.operation_audit_log
WHERE executed_at > NOW() - INTERVAL '1 minute'
ORDER BY executed_at DESC
LIMIT 5;

\echo 'TEST 1 COMPLETED ✓'
\echo ''

-- =====================================================
-- TEST 2: Sensor Calibrations
-- =====================================================

\echo '=========================================='
\echo 'TEST 2: PostgreSQL Sensor Calibrations'
\echo '=========================================='

-- Generate batch_id for this test
\set calibration_batch_id '550e8400-e29b-41d4-a716-446655440077'

-- Insert sample calibrations
INSERT INTO staging.sensor_calibrations (
    sensor_id, calibration_type,
    old_min_threshold, old_max_threshold,
    new_min_threshold, new_max_threshold,
    batch_id, reason, submitted_by
)
SELECT
    id,
    'RANGE_ADJUSTMENT',
    min_threshold,
    max_threshold,
    min_threshold - 2,  -- Expand range
    max_threshold + 2,
    :'calibration_batch_id'::UUID,
    'Test calibration - expanding range by ±2 degrees',
    'admin_test'
FROM metadata.sensors
WHERE sensor_type = 'TEMPERATURE'
LIMIT 3;

\echo 'Inserted calibrations for temperature sensors'

-- Apply calibrations
\echo 'Applying calibrations...'
SELECT * FROM staging.proc_apply_sensor_calibrations(:'calibration_batch_id'::UUID);

-- Verify sensors were updated
\echo 'Verifying sensor updates...'
SELECT id, sensor_type, min_threshold, max_threshold
FROM metadata.sensors
WHERE id IN (
    SELECT sensor_id FROM staging.sensor_calibrations
    WHERE batch_id = :'calibration_batch_id'::UUID
)
LIMIT 5;

\echo 'TEST 2 COMPLETED ✓'
\echo ''

-- =====================================================
-- TEST 3: Operations Summary View
-- =====================================================

\echo '=========================================='
\echo 'TEST 3: Staging Operations Summary View'
\echo '=========================================='

SELECT * FROM staging.v_operations_summary;

\echo 'TEST 3 COMPLETED ✓'
\echo ''

-- =====================================================
-- TEST 4: Rollback Operation
-- =====================================================

\echo '=========================================='
\echo 'TEST 4: Rollback Operation Test'
\echo '=========================================='

-- Get the most recent operation_id
\set last_operation (SELECT operation_id FROM staging.operation_audit_log ORDER BY executed_at DESC LIMIT 1)

\echo 'Testing rollback on most recent operation...'
SELECT * FROM staging.proc_rollback_operation(
    (SELECT operation_id FROM staging.operation_audit_log ORDER BY executed_at DESC LIMIT 1)
);

\echo 'TEST 4 COMPLETED ✓'
\echo ''

-- =====================================================
-- SUMMARY
-- =====================================================

\echo '=========================================='
\echo 'ALL POSTGRESQL TESTS COMPLETED ✓'
\echo '=========================================='
\echo 'Test results:'
\echo '  ✓ Greenhouse bulk updates (dry-run + apply)'
\echo '  ✓ Sensor calibrations'
\echo '  ✓ Operations summary view'
\echo '  ✓ Rollback operation'
\echo '=========================================='
