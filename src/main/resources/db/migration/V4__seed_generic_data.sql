-- ==============================================
-- RutaOptima - Datos Genéricos para Testing
-- ==============================================

-- Limpiar datos existentes
TRUNCATE TABLE route_stop CASCADE;
TRUNCATE TABLE route_plan CASCADE;
TRUNCATE TABLE orders CASCADE;
TRUNCATE TABLE vehicle CASCADE;
TRUNCATE TABLE customer CASCADE;

-- ============================================
-- VEHÍCULOS CON CONDUCTORES Y ZONAS
-- ============================================
INSERT INTO vehicle (nombre, patente, tipo, conductor, zona, color, 
                     capacidad_cantidad, capacidad_volumen, capacidad_peso, 
                     velocidad_kmh, costo_km, activo, 
                     depot_latitud, depot_longitud, 
                     jornada_inicio, jornada_fin, 
                     created_at, updated_at)
VALUES 
-- Vehículo Norte
('Camión Norte 1', 'PTN-001', 'CAMION', 'Juan Pérez', 'Norte', '#3B82F6',
 150.00, 20.00, 2000.00, 45.00, 3.50, true,
 -12.046374, -77.042793,
 '1970-01-01 08:00:00+00'::timestamptz, '1970-01-01 18:00:00+00'::timestamptz,
 NOW(), NOW()),

-- Vehículo Sur  
('Furgoneta Sur 1', 'PTS-002', 'FURGONETA_GRANDE', 'María García', 'Sur', '#10B981',
 100.00, 15.00, 1500.00, 50.00, 2.50, true,
 -12.046374, -77.042793,
 '1970-01-01 08:00:00+00'::timestamptz, '1970-01-01 18:00:00+00'::timestamptz,
 NOW(), NOW()),

-- Vehículo Este
('Furgoneta Este 1', 'PTE-003', 'FURGONETA_MEDIANA', 'Carlos López', 'Este', '#8B5CF6',
 80.00, 12.00, 1200.00, 55.00, 2.00, true,
 -12.046374, -77.042793,
 '1970-01-01 08:00:00+00'::timestamptz, '1970-01-01 18:00:00+00'::timestamptz,
 NOW(), NOW()),

-- Vehículo Centro
('Moto Centro 1', 'PTC-004', 'MOTO', 'Ana Torres', 'Centro', '#F59E0B',
 20.00, 2.00, 100.00, 70.00, 1.00, true,
 -12.046374, -77.042793,
 '1970-01-01 08:00:00+00'::timestamptz, '1970-01-01 22:00:00+00'::timestamptz,
 NOW(), NOW());

-- ============================================
-- CLIENTES POR ZONAS
-- ============================================

-- ZONA NORTE (5 clientes)
INSERT INTO customer (nombre, direccion, latitud, longitud, zona,
                     ventana_horaria_inicio, ventana_horaria_fin,
                     telefono, email, activo, created_at, updated_at)
VALUES 
('Comercial Norte A', 'Av. Túpac Amaru 1500, Los Olivos', -11.98350, -77.06120, 'Norte',
 '1970-01-01 09:00:00+00'::timestamptz, '1970-01-01 13:00:00+00'::timestamptz,
 '987654321', 'norte.a@empresa.com', true, NOW(), NOW()),

('Distribuidora Norte B', 'Av. Universitaria 2200, Independencia', -11.99900, -77.05220, 'Norte',
 '1970-01-01 08:00:00+00'::timestamptz, '1970-01-01 12:00:00+00'::timestamptz,
 '987654322', 'norte.b@empresa.com', true, NOW(), NOW()),

('Almacenes Norte C', 'Av. Carlos Izaguirre 890, San Martín de Porres', -11.96780, -77.06890, 'Norte',
 '1970-01-01 10:00:00+00'::timestamptz, '1970-01-01 14:00:00+00'::timestamptz,
 '987654323', 'norte.c@empresa.com', true, NOW(), NOW()),

('Tienda Norte D', 'Jr. Los Jazmines 456, Comas', -11.93890, -77.04880, 'Norte',
 '1970-01-01 09:00:00+00'::timestamptz, '1970-01-01 15:00:00+00'::timestamptz,
 '987654324', 'norte.d@empresa.com', true, NOW(), NOW()),

('Bodega Norte E', 'Av. Gerardo Unger 780, Comas', -11.94560, -77.05340, 'Norte',
 '1970-01-01 08:00:00+00'::timestamptz, '1970-01-01 16:00:00+00'::timestamptz,
 '987654325', 'norte.e@empresa.com', true, NOW(), NOW());

-- ZONA SUR (5 clientes)
INSERT INTO customer (nombre, direccion, latitud, longitud, zona,
                     ventana_horaria_inicio, ventana_horaria_fin,
                     telefono, email, activo, created_at, updated_at)
VALUES 
('Comercial Sur A', 'Av. Benavides 3500, Surco', -12.15560, -77.00670, 'Sur',
 '1970-01-01 09:00:00+00'::timestamptz, '1970-01-01 13:00:00+00'::timestamptz,
 '987654331', 'sur.a@empresa.com', true, NOW(), NOW()),

('Distribuidora Sur B', 'Av. Tomás Marsano 2800, Surco', -12.16890, -77.01230, 'Sur',
 '1970-01-01 08:00:00+00'::timestamptz, '1970-01-01 14:00:00+00'::timestamptz,
 '987654332', 'sur.b@empresa.com', true, NOW(), NOW()),

('Almacenes Sur C', 'Av. Angamos 4500, Surquillo', -12.11420, -77.01980, 'Sur',
 '1970-01-01 10:00:00+00'::timestamptz, '1970-01-01 15:00:00+00'::timestamptz,
 '987654333', 'sur.c@empresa.com', true, NOW(), NOW()),

('Tienda Sur D', 'Av. Grau 890, Barranco', -12.14490, -77.01930, 'Sur',
 '1970-01-01 09:00:00+00'::timestamptz, '1970-01-01 13:00:00+00'::timestamptz,
 '987654334', 'sur.d@empresa.com', true, NOW(), NOW()),

('Bodega Sur E', 'Av. Prolongación Benavides 1200, Santiago de Surco', -12.17890, -77.00450, 'Sur',
 '1970-01-01 08:00:00+00'::timestamptz, '1970-01-01 12:00:00+00'::timestamptz,
 '987654335', 'sur.e@empresa.com', true, NOW(), NOW());

-- ZONA ESTE (5 clientes)
INSERT INTO customer (nombre, direccion, latitud, longitud, zona,
                     ventana_horaria_inicio, ventana_horaria_fin,
                     telefono, email, activo, created_at, updated_at)
VALUES 
('Comercial Este A', 'Av. Separadora Industrial 1800, Ate', -12.02980, -76.95790, 'Este',
 '1970-01-01 09:00:00+00'::timestamptz, '1970-01-01 13:00:00+00'::timestamptz,
 '987654341', 'este.a@empresa.com', true, NOW(), NOW()),

('Distribuidora Este B', 'Av. Nicolás Ayllón 2500, El Agustino', -12.04560, -76.98230, 'Este',
 '1970-01-01 08:00:00+00'::timestamptz, '1970-01-01 14:00:00+00'::timestamptz,
 '987654342', 'este.b@empresa.com', true, NOW(), NOW()),

('Almacenes Este C', 'Av. Los Frutales 890, Santa Anita', -12.04780, -76.97560, 'Este',
 '1970-01-01 10:00:00+00'::timestamptz, '1970-01-01 15:00:00+00'::timestamptz,
 '987654343', 'este.c@empresa.com', true, NOW(), NOW()),

('Tienda Este D', 'Jr. Las Palmeras 456, La Molina', -12.08120, -76.93450, 'Este',
 '1970-01-01 09:00:00+00'::timestamptz, '1970-01-01 13:00:00+00'::timestamptz,
 '987654344', 'este.d@empresa.com', true, NOW(), NOW()),

('Bodega Este E', 'Av. Javier Prado Este 5600, La Molina', -12.09230, -76.95670, 'Este',
 '1970-01-01 08:00:00+00'::timestamptz, '1970-01-01 16:00:00+00'::timestamptz,
 '987654345', 'este.e@empresa.com', true, NOW(), NOW());

-- ZONA CENTRO (5 clientes)
INSERT INTO customer (nombre, direccion, latitud, longitud, zona,
                     ventana_horaria_inicio, ventana_horaria_fin,
                     telefono, email, activo, created_at, updated_at)
VALUES 
('Comercial Centro A', 'Jr. De la Unión 890, Lima Centro', -12.04665, -77.03012, 'Centro',
 '1970-01-01 09:00:00+00'::timestamptz, '1970-01-01 13:00:00+00'::timestamptz,
 '987654351', 'centro.a@empresa.com', true, NOW(), NOW()),

('Distribuidora Centro B', 'Av. Abancay 1200, Lima', -12.05240, -77.03110, 'Centro',
 '1970-01-01 08:00:00+00'::timestamptz, '1970-01-01 14:00:00+00'::timestamptz,
 '987654352', 'centro.b@empresa.com', true, NOW(), NOW()),

('Almacenes Centro C', 'Av. Venezuela 780, Lima', -12.06350, -77.02870, 'Centro',
 '1970-01-01 10:00:00+00'::timestamptz, '1970-01-01 15:00:00+00'::timestamptz,
 '987654353', 'centro.c@empresa.com', true, NOW(), NOW()),

('Tienda Centro D', 'Av. Alfonso Ugarte 1500, Lima', -12.05670, -77.04230, 'Centro',
 '1970-01-01 09:00:00+00'::timestamptz, '1970-01-01 13:00:00+00'::timestamptz,
 '987654354', 'centro.d@empresa.com', true, NOW(), NOW()),

('Bodega Centro E', 'Jr. Ancash 567, Lima', -12.04890, -77.02980, 'Centro',
 '1970-01-01 08:00:00+00'::timestamptz, '1970-01-01 16:00:00+00'::timestamptz,
 '987654355', 'centro.e@empresa.com', true, NOW(), NOW());

-- ============================================
-- ÓRDENES DE PRUEBA (3 días en adelante)
-- ============================================
-- Las órdenes deben tener fecha_entrega al menos 3 días después
INSERT INTO orders (customer_id, fecha_entrega, cantidad, volumen, peso, 
                   estado, prioridad, tiempo_servicio_estimado_min, 
                   created_at, updated_at)
SELECT 
    id,
    (NOW() + INTERVAL '4 days')::timestamptz,
    ROUND((RANDOM() * 50 + 10)::numeric, 2),
    ROUND((RANDOM() * 5 + 1)::numeric, 2),
    ROUND((RANDOM() * 100 + 20)::numeric, 2),
    'PENDIENTE',
    1,
    15,
    NOW(),
    NOW()
FROM customer
LIMIT 15;

-- Comentarios para referencia
COMMENT ON TABLE vehicle IS 'Vehículos de la flota con conductor y zona asignada';
COMMENT ON TABLE customer IS 'Clientes organizados por zonas geográficas';
COMMENT ON TABLE orders IS 'Pedidos con validación de 3 días mínimo';
