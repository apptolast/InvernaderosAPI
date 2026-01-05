-- V20: Add name field to devices table
-- This migration adds a human-readable name to identify devices
--
-- @see https://docs.spring.io/spring-boot/reference/data/sql.html#data.sql.datasource.flyway

-- Add name column to devices table
ALTER TABLE metadata.devices
ADD COLUMN IF NOT EXISTS name VARCHAR(100);

-- Add comment explaining the field
COMMENT ON COLUMN metadata.devices.name IS 'Human-readable name to identify the device (e.g., "Temperature Sensor Greenhouse 1")';

-- Create index for name searches
CREATE INDEX IF NOT EXISTS idx_devices_name ON metadata.devices(name) WHERE name IS NOT NULL;
