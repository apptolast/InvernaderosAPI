-- =============================================================================
-- V35: Fix boolean values in historical data + clean up orphaned objects
-- Date: 2026-03-26
--
-- Problem: 46 device/setting codes store "true"/"false" in sensor_readings.
-- The continuous aggregates use value::double precision which fails on these.
-- This is why readings_daily has 0 rows (refresh job fails 54% of the time).
--
-- Actions:
-- 1. Convert existing "true"/"false" to "1"/"0" in active tables
-- 2. Drop current continuous aggregates (will be recreated in V36 with weekly/monthly)
-- 3. Drop orphaned objects from old schema (public.sensor_readings, old aggregates)
-- =============================================================================

-- ─────────────────────────────────────────────────────────────────────────────
-- Part 1: Fix boolean values in active tables
-- Only ~4,026 rows affected in sensor_readings (verified in PROD)
-- Chunks are recent (< 7 days), not compressed, so UPDATE is fast
-- ─────────────────────────────────────────────────────────────────────────────
UPDATE iot.sensor_readings
SET value = CASE value WHEN 'true' THEN '1' WHEN 'false' THEN '0' END
WHERE value IN ('true', 'false');

UPDATE iot.device_current_values
SET value = CASE value WHEN 'true' THEN '1' WHEN 'false' THEN '0' END
WHERE value IN ('true', 'false');

-- NOTE: Not updating sensor_readings_raw (archival table).
-- Its chunks are compressed after 3 days, making UPDATE expensive.
-- Raw data is not used by continuous aggregates.

-- ─────────────────────────────────────────────────────────────────────────────
-- Part 2: Drop current continuous aggregates (recreated with improvements in V36)
-- readings_hourly: works but fails 31% due to booleans
-- readings_daily: 0 rows, fails 54% due to booleans
-- CASCADE also removes their refresh policies automatically
-- ─────────────────────────────────────────────────────────────────────────────
DROP MATERIALIZED VIEW IF EXISTS iot.readings_hourly CASCADE;
DROP MATERIALIZED VIEW IF EXISTS iot.readings_daily CASCADE;

-- ─────────────────────────────────────────────────────────────────────────────
-- Part 3: Drop orphaned continuous aggregate from old schema
-- public.sensor_readings_hourly is a continuous aggregate on public.sensor_readings
-- (old schema with greenhouse_id/sensor_type columns, no longer in use)
-- CASCADE removes its refresh policy (job 1006) automatically
-- ─────────────────────────────────────────────────────────────────────────────
DROP MATERIALIZED VIEW IF EXISTS public.sensor_readings_hourly CASCADE;

-- ─────────────────────────────────────────────────────────────────────────────
-- Part 4: Drop orphaned tables
-- These are empty hypertables/tables from the old init script that used
-- greenhouse_id/tenant_id/sensor_type columns (pre-V32 architecture)
-- ─────────────────────────────────────────────────────────────────────────────
DROP TABLE IF EXISTS public.sensor_readings CASCADE;
DROP TABLE IF EXISTS iot.sensor_readings_hourly CASCADE;
DROP TABLE IF EXISTS iot.sensor_readings_daily CASCADE;
DROP TABLE IF EXISTS iot.sensor_readings_monthly CASCADE;
