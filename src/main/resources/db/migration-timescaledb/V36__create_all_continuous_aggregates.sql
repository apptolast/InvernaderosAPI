-- =============================================================================
-- V36: Create all 4 continuous aggregates for temporal aggregation strategy
-- Date: 2026-03-26
--
-- Aggregation strategy for mobile app chart scales:
--   DAY   -> readings_hourly  (~24 points)
--   WEEK  -> readings_daily   (~7 points)
--   MONTH -> readings_daily   (~30 points)
--   YEAR  -> readings_weekly  (~52 points)
--   ALL   -> readings_monthly (variable, ~12-60 points)
--
-- All aggregates source from iot.sensor_readings (deduped hypertable).
-- Boolean values were fixed in V35 (true/false -> 1/0).
-- TimescaleDB 2.23.0 confirmed: time_bucket('1 month') supported.
-- =============================================================================

-- ─────────────────────────────────────────────────────────────────────────────
-- HOURLY: 1-hour buckets for DAY scale
-- ─────────────────────────────────────────────────────────────────────────────
CREATE MATERIALIZED VIEW iot.readings_hourly
WITH (timescaledb.continuous) AS
SELECT
    time_bucket('1 hour', time) AS bucket,
    code,
    AVG(value::double precision)    AS avg_value,
    MIN(value::double precision)    AS min_value,
    MAX(value::double precision)    AS max_value,
    STDDEV(value::double precision) AS stddev_value,
    COUNT(*)                        AS count_readings
FROM iot.sensor_readings
GROUP BY bucket, code
WITH NO DATA;

SELECT add_continuous_aggregate_policy('iot.readings_hourly',
    start_offset    => INTERVAL '3 hours',
    end_offset      => INTERVAL '1 hour',
    schedule_interval => INTERVAL '1 hour');

-- ─────────────────────────────────────────────────────────────────────────────
-- DAILY: 1-day buckets for WEEK and MONTH scales
-- ─────────────────────────────────────────────────────────────────────────────
CREATE MATERIALIZED VIEW iot.readings_daily
WITH (timescaledb.continuous) AS
SELECT
    time_bucket('1 day', time) AS bucket,
    code,
    AVG(value::double precision)    AS avg_value,
    MIN(value::double precision)    AS min_value,
    MAX(value::double precision)    AS max_value,
    STDDEV(value::double precision) AS stddev_value,
    COUNT(*)                        AS count_readings
FROM iot.sensor_readings
GROUP BY bucket, code
WITH NO DATA;

SELECT add_continuous_aggregate_policy('iot.readings_daily',
    start_offset    => INTERVAL '3 days',
    end_offset      => INTERVAL '1 day',
    schedule_interval => INTERVAL '6 hours');

-- ─────────────────────────────────────────────────────────────────────────────
-- WEEKLY: 7-day buckets for YEAR scale
-- Note: time_bucket('7 days') anchors at 2000-01-03 (Monday) by default.
-- This produces evenly spaced points, which is what matters for charts.
-- ─────────────────────────────────────────────────────────────────────────────
CREATE MATERIALIZED VIEW iot.readings_weekly
WITH (timescaledb.continuous) AS
SELECT
    time_bucket('7 days', time) AS bucket,
    code,
    AVG(value::double precision)    AS avg_value,
    MIN(value::double precision)    AS min_value,
    MAX(value::double precision)    AS max_value,
    STDDEV(value::double precision) AS stddev_value,
    COUNT(*)                        AS count_readings
FROM iot.sensor_readings
GROUP BY bucket, code
WITH NO DATA;

SELECT add_continuous_aggregate_policy('iot.readings_weekly',
    start_offset    => INTERVAL '21 days',
    end_offset      => INTERVAL '7 days',
    schedule_interval => INTERVAL '1 day');

-- ─────────────────────────────────────────────────────────────────────────────
-- MONTHLY: 1-month buckets for ALL scale
-- Requires TimescaleDB 2.9+ (confirmed 2.23.0)
-- ─────────────────────────────────────────────────────────────────────────────
CREATE MATERIALIZED VIEW iot.readings_monthly
WITH (timescaledb.continuous) AS
SELECT
    time_bucket('1 month', time) AS bucket,
    code,
    AVG(value::double precision)    AS avg_value,
    MIN(value::double precision)    AS min_value,
    MAX(value::double precision)    AS max_value,
    STDDEV(value::double precision) AS stddev_value,
    COUNT(*)                        AS count_readings
FROM iot.sensor_readings
GROUP BY bucket, code
WITH NO DATA;

SELECT add_continuous_aggregate_policy('iot.readings_monthly',
    start_offset    => INTERVAL '3 months',
    end_offset      => INTERVAL '1 month',
    schedule_interval => INTERVAL '1 day');
