-- Migración para corregir la primary key de sensor_readings
-- De: PRIMARY KEY (time)
-- A:  PRIMARY KEY (time, sensor_id)
--
-- Esto permite almacenar múltiples lecturas de diferentes sensores
-- con el mismo timestamp, resolviendo el bug donde solo se guardaba
-- el último valor de cada batch MQTT

-- Paso 1: Eliminar la constraint de primary key existente
ALTER TABLE public.sensor_readings
DROP CONSTRAINT IF EXISTS sensor_readings_pkey;

-- Paso 2: Crear nueva primary key compuesta
ALTER TABLE public.sensor_readings
ADD CONSTRAINT sensor_readings_pkey
PRIMARY KEY (time, sensor_id);

-- Verificación: Esta query debe mostrar la nueva PK compuesta
-- SELECT constraint_name, constraint_type
-- FROM information_schema.table_constraints
-- WHERE table_name = 'sensor_readings' AND table_schema = 'public';
