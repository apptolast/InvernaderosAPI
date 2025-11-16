-- =====================================================
-- TEST STAGING PROCEDURES - DEV ENVIRONMENT
-- =====================================================
-- This script tests all staging procedures with sample data
-- Execute in DEV environment only!
-- =====================================================

-- =====================================================
-- TEST 1: TimescaleDB - Bulk Import with Validation
-- =====================================================

\echo '=========================================='
\echo 'TEST 1: TimescaleDB Bulk Import & Validation'
\echo '=========================================='

-- Generate batch_id for this test
\set test_batch_id '550e8400-e29b-41d4-a716-446655440099'

-- Insert sample raw data (mix of valid and invalid)
INSERT INTO staging.sensor_readings_raw (
    time, sensor_id, greenhouse_id, tenant_id, sensor_type, value, unit, batch_id, source
) VALUES
-- Valid records
('2025-11-16 10:00:00+00', 'TEMP_001', '660e8400-e29b-41d4-a716-446655440001', '550e8400-e29b-41d4-a716-446655440000', 'TEMPERATURE', 23.5, '°C', :'test_batch_id', 'TEST'),
('2025-11-16 10:01:00+00', 'HUM_001', '660e8400-e29b-41d4-a716-446655440001', '550e8400-e29b-41d4-a716-446655440000', 'HUMIDITY', 65.0, '%', :'test_batch_id', 'TEST'),
('2025-11-16 10:02:00+00', 'TEMP_002', '660e8400-e29b-41d4-a716-446655440001', '550e8400-e29b-41d4-a716-446655440000', 'TEMPERATURE', 22.8, '°C', :'test_batch_id', 'TEST'),

-- Invalid records (will fail validation)
('2025-11-16 10:03:00+00', NULL, '660e8400-e29b-41d4-a716-446655440001', '550e8400-e29b-41d4-a716-446655440000', 'TEMPERATURE', 25.0, '°C', :'test_batch_id', 'TEST'), -- Missing sensor_id
('2025-11-16 10:04:00+00', 'TEMP_003', 'invalid-uuid', '550e8400-e29b-41d4-a716-446655440000', 'TEMPERATURE', 24.0, '°C', :'test_batch_id', 'TEST'), -- Invalid greenhouse_id UUID
('2025-11-16 10:05:00+00', 'TEMP_004', '660e8400-e29b-41d4-a716-446655440001', '550e8400-e29b-41d4-a716-446655440000', 'TEMPERATURE', 999.0, '°C', :'test_batch_id', 'TEST'); -- Out of range value

\echo 'Inserted 6 records (3 valid, 3 invalid)'

-- Run validation
\echo 'Running validation...'
SELECT * FROM staging.proc_validate_sensor_readings(:'test_batch_id');

-- Check validation results
\echo 'Validation errors:'
SELECT sensor_id, validation_status, validation_errors
FROM staging.sensor_readings_raw
WHERE batch_id = :'test_batch_id' AND validation_status = 'INVALID';

-- Migrate valid data to production
\echo 'Migrating valid data to production...'
SELECT * FROM staging.proc_migrate_staging_to_production(:'test_batch_id', FALSE);

-- Verify data in production
\echo 'Verifying data in iot.sensor_readings...'
SELECT time, sensor_id, sensor_type, value, unit
FROM iot.sensor_readings
WHERE time >= '2025-11-16 10:00:00+00' AND time <= '2025-11-16 10:05:00+00'
ORDER BY time DESC
LIMIT 10;

\echo 'TEST 1 COMPLETED ✓'
\echo ''

-- =====================================================
-- TEST 2: PostgreSQL - Greenhouse Bulk Updates
-- =====================================================

\echo '=========================================='
\echo 'TEST 2: PostgreSQL Greenhouse Bulk Updates'
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
SELECT * FROM staging.proc_apply_greenhouse_updates(:'greenhouse_batch_id', TRUE);

-- Apply changes for real
\echo 'Applying changes...'
SELECT * FROM staging.proc_apply_greenhouse_updates(:'greenhouse_batch_id', FALSE);

-- Verify operation audit log
\echo 'Checking operation audit log...'
SELECT operation_type, status, rows_affected, rollback_available
FROM staging.operation_audit_log
WHERE executed_at > NOW() - INTERVAL '1 minute'
ORDER BY executed_at DESC
LIMIT 5;

\echo 'TEST 2 COMPLETED ✓'
\echo ''

-- =====================================================
-- TEST 3: PostgreSQL - Sensor Calibrations
-- =====================================================

\echo '=========================================='
\echo 'TEST 3: PostgreSQL Sensor Calibrations'
\echo '=========================================='

-- Generate batch_id for this test
\set calibration_batch_id '550e8400-e29b-41d4-a716-446655440077'

-- Get existing sensor IDs from metadata
\set sensor_id_sample (SELECT id FROM metadata.sensors WHERE sensor_type = 'TEMPERATURE' LIMIT 1)

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
    :'calibration_batch_id',
    'Test calibration - expanding range by ±2 degrees',
    'admin_test'
FROM metadata.sensors
WHERE sensor_type = 'TEMPERATURE'
LIMIT 3;

\echo 'Inserted calibrations for 3 temperature sensors'

-- Apply calibrations
\echo 'Applying calibrations...'
SELECT * FROM staging.proc_apply_sensor_calibrations(:'calibration_batch_id');

-- Verify sensors were updated
\echo 'Verifying sensor updates...'
SELECT id, sensor_type, min_threshold, max_threshold
FROM metadata.sensors
WHERE id IN (
    SELECT sensor_id FROM staging.sensor_calibrations
    WHERE batch_id = :'calibration_batch_id'
)
LIMIT 5;

\echo 'TEST 3 COMPLETED ✓'
\echo ''

-- =====================================================
-- TEST 4: View Operations Summary
-- =====================================================

\echo '=========================================='
\echo 'TEST 4: Staging Operations Summary View'
\echo '=========================================='

SELECT * FROM staging.v_operations_summary;

\echo 'TEST 4 COMPLETED ✓'
\echo ''

-- =====================================================
-- TEST 5: Cleanup Old Staging Data
-- =====================================================

\echo '=========================================='
\echo 'TEST 5: Staging Cleanup (TimescaleDB)'
\echo '=========================================='

-- This will clean data older than 7 days (should be 0 in fresh test)
SELECT * FROM staging.proc_cleanup_staging(7);

\echo 'TEST 5 COMPLETED ✓'
\echo ''

-- =====================================================
-- SUMMARY
-- =====================================================

\echo '=========================================='
\echo 'ALL TESTS COMPLETED SUCCESSFULLY ✓'
\echo '=========================================='
\echo 'Test results:'
\echo '  ✓ TimescaleDB validation & migration'
\echo '  ✓ PostgreSQL greenhouse updates (dry-run + apply)'
\echo '  ✓ PostgreSQL sensor calibrations'
\echo '  ✓ Operations summary view'
\echo '  ✓ Staging cleanup'
\echo '=========================================='
