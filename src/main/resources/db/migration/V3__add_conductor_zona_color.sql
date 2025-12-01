-- Agregar campos necesarios según nuevos requerimientos

-- Agregar conductor, zona y color a vehículos (solo si no existen)
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'vehicle' AND column_name = 'conductor') THEN
        ALTER TABLE vehicle ADD COLUMN conductor VARCHAR(255) NOT NULL DEFAULT 'Sin asignar';
END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'vehicle' AND column_name = 'zona') THEN
        ALTER TABLE vehicle ADD COLUMN zona VARCHAR(100);
END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'vehicle' AND column_name = 'color') THEN
        ALTER TABLE vehicle ADD COLUMN color VARCHAR(7) NOT NULL DEFAULT '#3B82F6';
END IF;
END $$;

-- Agregar zona a clientes para organización geográfica (solo si no existe)
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'customer' AND column_name = 'zona') THEN
        ALTER TABLE customer ADD COLUMN zona VARCHAR(100);
END IF;
END $$;

-- Eliminar campos innecesarios de customer
ALTER TABLE customer
DROP COLUMN IF EXISTS es_nuevo,
DROP COLUMN IF EXISTS demanda_promedio_semanal,
DROP COLUMN IF EXISTS factor_estacionalidad;

-- Eliminar tablas innecesarias
DROP TABLE IF EXISTS traffic_event CASCADE;

-- Crear índice para búsqueda por zona (solo si no existen)
CREATE INDEX IF NOT EXISTS idx_vehicle_zona ON vehicle(zona) WHERE zona IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_customer_zona ON customer(zona) WHERE zona IS NOT NULL;

-- Comentarios para documentación
COMMENT ON COLUMN vehicle.conductor IS 'Nombre del conductor asignado al vehículo';
COMMENT ON COLUMN vehicle.zona IS 'Zona geográfica asignada al vehículo para optimización';
COMMENT ON COLUMN vehicle.color IS 'Color hexadecimal para identificación visual en el mapa';
COMMENT ON COLUMN customer.zona IS 'Zona geográfica del cliente para agrupación de entregas';
