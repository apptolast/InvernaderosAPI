-- =============================================================================
-- V36: Añadir campo client_name a devices, alerts y settings
-- Fecha: 2026-03-23
--
-- Nombre legible para mostrar al usuario en el frontend en lugar de
-- nombres tecnicos (ej: "Velocidad del viento interior" en vez de
-- "WS90_INTERIOR_WIND_SPEED").
-- =============================================================================

ALTER TABLE metadata.devices ADD COLUMN client_name VARCHAR(255) DEFAULT NULL;
ALTER TABLE metadata.alerts ADD COLUMN client_name VARCHAR(255) DEFAULT NULL;
ALTER TABLE metadata.settings ADD COLUMN client_name VARCHAR(255) DEFAULT NULL;
