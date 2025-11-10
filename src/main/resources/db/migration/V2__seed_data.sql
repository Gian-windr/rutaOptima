-- ==============================================
-- RutaOptima - Datos de Ejemplo (Seeds)
-- ==============================================

-- Usuario administrador (password: admin123)
INSERT INTO users (email, password_hash, rol, activo) VALUES
('admin@rutaoptima.com', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'ADMIN', true),
('despachador@rutaoptima.com', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'USER', true);

-- Vehículos de la flota
-- Depot central: Lima Centro (-12.046374, -77.042793)
INSERT INTO vehicle (nombre, patente, tipo, capacidad_cantidad, capacidad_volumen, capacidad_peso, velocidad_kmh, costo_km, activo, depot_latitud, depot_longitud, jornada_inicio, jornada_fin) VALUES
('Furgoneta Grande 1', 'ABC-123', 'FURGONETA_GRANDE', 100.00, 15.00, 1500.00, 45.00, 2.50, true, -12.046374, -77.042793, '08:00:00', '18:00:00'),
('Furgoneta Grande 2', 'DEF-456', 'FURGONETA_GRANDE', 100.00, 15.00, 1500.00, 45.00, 2.50, true, -12.046374, -77.042793, '08:00:00', '18:00:00'),
('Furgoneta Mediana 1', 'GHI-789', 'FURGONETA_MEDIANA', 60.00, 10.00, 800.00, 50.00, 1.80, true, -12.046374, -77.042793, '08:00:00', '18:00:00'),
('Furgoneta Mediana 2', 'JKL-012', 'FURGONETA_MEDIANA', 60.00, 10.00, 800.00, 50.00, 1.80, true, -12.046374, -77.042793, '08:00:00', '18:00:00'),
('Moto Delivery', 'MNO-345', 'MOTO', 15.00, 1.50, 50.00, 60.00, 0.80, true, -12.046374, -77.042793, '08:00:00', '20:00:00');

-- Clientes (30 clientes en diferentes zonas de Lima)
-- Algunos marcados como nuevos (es_nuevo = true) para validar regla de 5 días
INSERT INTO customer (nombre, direccion, latitud, longitud, es_nuevo, ventana_horaria_inicio, ventana_horaria_fin, demanda_promedio_semanal, factor_estacionalidad, telefono, email, activo) VALUES
-- Clientes establecidos (es_nuevo = false)
('Bodega San Juan', 'Av. Arequipa 1234, Miraflores', -12.119650, -77.034110, false, '09:00:00', '13:00:00', 25.50, 1.0, '999111222', 'sanjuan@mail.com', true),
('Minimarket El Sol', 'Jr. De la Unión 567, Lima Centro', -12.046650, -77.030120, false, '08:00:00', '12:00:00', 35.00, 1.2, '999222333', 'elsol@mail.com', true),
('Tienda Lupita', 'Av. Javier Prado 890, San Isidro', -12.095000, -77.035500, false, '10:00:00', '14:00:00', 20.00, 0.9, '999333444', 'lupita@mail.com', true),
('Comercial Pérez', 'Av. Brasil 2345, Pueblo Libre', -12.078900, -77.069800, false, '09:00:00', '18:00:00', 45.00, 1.1, '999444555', 'perez@mail.com', true),
('Bodega Central', 'Av. Colonial 678, Callao', -12.054800, -77.118900, false, '08:00:00', '17:00:00', 55.00, 1.3, '999555666', 'central@mail.com', true),
('Market Express', 'Av. Universitaria 1111, Los Olivos', -11.983500, -77.061200, false, '09:00:00', '13:00:00', 30.00, 1.0, '999666777', 'express@mail.com', true),
('Tienda Familia', 'Av. Angamos 2222, Surquillo', -12.114200, -77.019800, false, '10:00:00', '15:00:00', 22.00, 0.95, '999777888', 'familia@mail.com', true),
('Bodega Don José', 'Av. Benavides 3333, Miraflores', -12.127800, -77.025600, false, '08:30:00', '12:30:00', 28.00, 1.05, '999888999', 'donjose@mail.com', true),
('Minimarket La Esquina', 'Av. Larco 444, Miraflores', -12.122500, -77.029400, false, '09:00:00', '14:00:00', 32.00, 1.15, '999999000', 'esquina@mail.com', true),
('Comercial Rojas', 'Av. Grau 555, Barranco', -12.144900, -77.019300, false, '08:00:00', '16:00:00', 40.00, 1.2, '999000111', 'rojas@mail.com', true),
('Bodega Santa Rosa', 'Av. Venezuela 666, Lima', -12.063500, -77.028700, false, '09:00:00', '13:00:00', 26.00, 1.0, '999111000', 'santarosa@mail.com', true),
('Tienda Del Pueblo', 'Av. Túpac Amaru 777, Independencia', -11.999000, -77.052200, false, '08:00:00', '12:00:00', 35.00, 1.1, '999222111', 'delpueblo@mail.com', true),
('Market Popular', 'Av. México 888, La Victoria', -12.067800, -77.017200, false, '10:00:00', '14:00:00', 29.00, 1.0, '999333222', 'popular@mail.com', true),
('Bodega Los Andes', 'Av. Abancay 999, Lima', -12.052400, -77.031100, false, '09:00:00', '17:00:00', 38.00, 1.25, '999444333', 'losandes@mail.com', true),
('Comercial Lima', 'Av. Aviación 1010, San Borja', -12.089800, -77.001200, false, '08:00:00', '18:00:00', 50.00, 1.3, '999555444', 'comlima@mail.com', true),
('Minimarket Surco', 'Av. Tomás Marsano 1212, Surco', -12.155600, -77.006700, false, '09:00:00', '13:00:00', 33.00, 1.05, '999666555', 'surco@mail.com', true),
('Bodega El Pino', 'Av. La Marina 1313, San Miguel', -12.077400, -77.092100, false, '08:30:00', '12:30:00', 27.00, 1.0, '999777666', 'elpino@mail.com', true),
('Tienda Norte', 'Av. Túpac Amaru 1414, Comas', -11.938900, -77.048800, false, '10:00:00', '15:00:00', 24.00, 0.9, '999888777', 'norte@mail.com', true),
('Market Sur', 'Av. Los Próceres 1515, Villa El Salvador', -12.218900, -76.933400, false, '09:00:00', '14:00:00', 31.00, 1.1, '999999888', 'sur@mail.com', true),
('Comercial Este', 'Av. Separadora Industrial 1616, Ate', -12.029800, -76.957900, false, '08:00:00', '16:00:00', 42.00, 1.2, '999000999', 'este@mail.com', true),

-- Clientes NUEVOS (es_nuevo = true) para validar regla de 5 días
('Bodega Nueva Victoria', 'Av. 28 de Julio 1717, La Victoria', -12.072300, -77.016800, true, '09:00:00', '13:00:00', 15.00, 1.0, '998111222', 'nuevavictoria@mail.com', true),
('Market Recién Abierto', 'Av. Petit Thouars 1818, Lince', -12.082100, -77.029900, true, '10:00:00', '14:00:00', 12.00, 1.0, '998222333', 'reciente@mail.com', true),
('Tienda Inicio', 'Av. Arenales 1919, Jesús María', -12.076500, -77.045600, true, '08:00:00', '12:00:00', 18.00, 1.0, '998333444', 'inicio@mail.com', true),
('Bodega Primer Día', 'Av. Alfonso Ugarte 2020, Lima', -12.056700, -77.042300, true, '09:00:00', '17:00:00', 20.00, 1.0, '998444555', 'primerdia@mail.com', true),
('Comercial Estreno', 'Av. Argentina 2121, Lima', -12.056100, -77.069400, true, '08:00:00', '18:00:00', 25.00, 1.0, '998555666', 'estreno@mail.com', true),
('Minimarket Novato', 'Av. Universitaria 2222, San Martín de Porres', -12.001200, -77.080100, true, '09:00:00', '13:00:00', 16.00, 1.0, '998666777', 'novato@mail.com', true),
('Bodega Recién Llegados', 'Av. Canta Callao 2323, San Miguel', -12.082900, -77.103200, true, '10:00:00', '15:00:00', 14.00, 1.0, '998777888', 'llegados@mail.com', true),
('Tienda Principiante', 'Av. Faucett 2424, Callao', -12.061800, -77.098700, true, '08:30:00', '12:30:00', 19.00, 1.0, '998888999', 'principiante@mail.com', true),
('Market Debut', 'Av. Óscar R. Benavides 2525, Lima', -12.065400, -77.080900, true, '09:00:00', '14:00:00', 17.00, 1.0, '998999000', 'debut@mail.com', true),
('Comercial Apertura', 'Av. Angamos Oeste 2626, Miraflores', -12.120800, -77.042500, true, '08:00:00', '16:00:00', 22.00, 1.0, '998000111', 'apertura@mail.com', true);

-- Pedidos para el 15 de junio de 2025 (fecha en el futuro desde la perspectiva del sistema)
-- Mezcla de pedidos de clientes establecidos y nuevos con diferentes características
INSERT INTO orders (customer_id, fecha_entrega, cantidad, volumen, peso, estado, ventana_horaria_inicio, ventana_horaria_fin, prioridad, tiempo_servicio_estimado_min, notas) VALUES
-- Pedidos de clientes establecidos
(1, '2025-06-15', 12.00, 1.50, 120.00, 'PENDIENTE', '09:00:00', '13:00:00', 1, 10, 'Entrega en puerta trasera'),
(2, '2025-06-15', 18.50, 2.20, 185.00, 'PENDIENTE', '08:00:00', '12:00:00', 1, 15, 'Llamar antes de llegar'),
(3, '2025-06-15', 8.00, 1.00, 80.00, 'PENDIENTE', '10:00:00', '14:00:00', 1, 8, NULL),
(4, '2025-06-15', 25.00, 3.50, 250.00, 'PENDIENTE', '09:00:00', '18:00:00', 2, 20, 'Cliente prioritario'),
(5, '2025-06-15', 30.00, 4.00, 300.00, 'PENDIENTE', '08:00:00', '17:00:00', 2, 25, 'Verificar inventario'),
(6, '2025-06-15', 15.00, 1.80, 150.00, 'PENDIENTE', '09:00:00', '13:00:00', 1, 12, NULL),
(7, '2025-06-15', 10.00, 1.20, 100.00, 'PENDIENTE', '10:00:00', '15:00:00', 1, 10, 'Frágil'),
(8, '2025-06-15', 14.00, 1.70, 140.00, 'PENDIENTE', '08:30:00', '12:30:00', 1, 10, NULL),
(9, '2025-06-15', 16.50, 2.00, 165.00, 'PENDIENTE', '09:00:00', '14:00:00', 1, 12, 'Verificar fecha de vencimiento'),
(10, '2025-06-15', 22.00, 2.80, 220.00, 'PENDIENTE', '08:00:00', '16:00:00', 2, 18, NULL),
(11, '2025-06-15', 13.00, 1.60, 130.00, 'PENDIENTE', '09:00:00', '13:00:00', 1, 10, NULL),
(12, '2025-06-15', 19.00, 2.40, 190.00, 'PENDIENTE', '08:00:00', '12:00:00', 1, 15, 'Descargar en almacén'),
(13, '2025-06-15', 14.50, 1.75, 145.00, 'PENDIENTE', '10:00:00', '14:00:00', 1, 10, NULL),
(14, '2025-06-15', 20.00, 2.60, 200.00, 'PENDIENTE', '09:00:00', '17:00:00', 2, 15, 'Pedido grande'),
(15, '2025-06-15', 28.00, 3.80, 280.00, 'PENDIENTE', '08:00:00', '18:00:00', 3, 20, 'Cliente VIP'),
(16, '2025-06-15', 17.00, 2.10, 170.00, 'PENDIENTE', '09:00:00', '13:00:00', 1, 12, NULL),
(17, '2025-06-15', 13.50, 1.65, 135.00, 'PENDIENTE', '08:30:00', '12:30:00', 1, 10, NULL),
(18, '2025-06-15', 11.00, 1.35, 110.00, 'PENDIENTE', '10:00:00', '15:00:00', 1, 10, 'Zona alejada'),
(19, '2025-06-15', 16.00, 1.95, 160.00, 'PENDIENTE', '09:00:00', '14:00:00', 1, 12, NULL),
(20, '2025-06-15', 23.00, 3.00, 230.00, 'PENDIENTE', '08:00:00', '16:00:00', 2, 18, 'Ruta alternativa por construcción'),
(1, '2025-06-15', 8.50, 1.05, 85.00, 'PENDIENTE', '09:00:00', '13:00:00', 1, 8, 'Segundo pedido del día'),
(3, '2025-06-15', 9.00, 1.10, 90.00, 'PENDIENTE', '10:00:00', '14:00:00', 1, 10, 'Urgente'),
(5, '2025-06-15', 12.50, 1.55, 125.00, 'PENDIENTE', '08:00:00', '17:00:00', 2, 12, NULL),
(7, '2025-06-15', 7.00, 0.85, 70.00, 'PENDIENTE', '10:00:00', '15:00:00', 1, 8, NULL),
(9, '2025-06-15', 10.50, 1.30, 105.00, 'PENDIENTE', '09:00:00', '14:00:00', 1, 10, NULL),

-- Pedidos de clientes NUEVOS (válidos porque la fecha de entrega está a más de 5 días)
-- Estos pedidos serían creados el 10 de junio o antes
(21, '2025-06-15', 8.00, 1.00, 80.00, 'PENDIENTE', '09:00:00', '13:00:00', 1, 8, 'Primer pedido - cliente nuevo'),
(22, '2025-06-15', 6.50, 0.80, 65.00, 'PENDIENTE', '10:00:00', '14:00:00', 1, 8, 'Cliente nuevo - verificar dirección'),
(23, '2025-06-15', 9.50, 1.15, 95.00, 'PENDIENTE', '08:00:00', '12:00:00', 1, 10, 'Nuevo cliente'),
(24, '2025-06-15', 10.00, 1.25, 100.00, 'PENDIENTE', '09:00:00', '17:00:00', 1, 10, 'Primera entrega'),
(25, '2025-06-15', 12.00, 1.50, 120.00, 'PENDIENTE', '08:00:00', '18:00:00', 2, 12, 'Cliente nuevo - llamar confirmación'),
(26, '2025-06-15', 8.50, 1.05, 85.00, 'PENDIENTE', '09:00:00', '13:00:00', 1, 8, NULL),
(27, '2025-06-15', 7.50, 0.90, 75.00, 'PENDIENTE', '10:00:00', '15:00:00', 1, 8, 'Nuevo - verificar contacto'),
(28, '2025-06-15', 9.00, 1.10, 90.00, 'PENDIENTE', '08:30:00', '12:30:00', 1, 10, NULL),
(29, '2025-06-15', 8.75, 1.08, 87.50, 'PENDIENTE', '09:00:00', '14:00:00', 1, 8, 'Primera vez'),
(30, '2025-06-15', 11.00, 1.35, 110.00, 'PENDIENTE', '08:00:00', '16:00:00', 2, 10, 'Cliente nuevo - ruta de prueba'),

-- Pedidos adicionales para otras fechas (para pruebas)
(1, '2025-06-16', 15.00, 1.85, 150.00, 'PENDIENTE', '09:00:00', '13:00:00', 1, 12, 'Pedido para mañana'),
(2, '2025-06-16', 20.00, 2.50, 200.00, 'PENDIENTE', '08:00:00', '12:00:00', 1, 15, NULL),
(4, '2025-06-17', 18.00, 2.25, 180.00, 'PENDIENTE', '09:00:00', '18:00:00', 1, 15, 'Pedido futuro'),
(6, '2025-06-20', 25.00, 3.20, 250.00, 'PENDIENTE', '09:00:00', '13:00:00', 2, 20, 'Pedido semanal'),
(10, '2025-06-20', 30.00, 3.90, 300.00, 'PENDIENTE', '08:00:00', '16:00:00', 2, 25, 'Reabastecimiento');

-- Eventos de tráfico (ejemplos para re-optimización)
INSERT INTO traffic_event (fecha_hora, descripcion, factor_retraso, latitud, longitud, radio_afectacion_km, activo) VALUES
('2025-06-15 08:30:00', 'Accidente en Av. Javier Prado altura San Isidro', 1.5, -12.095000, -77.035500, 2.0, false),
('2025-06-15 10:00:00', 'Manifestación en Av. Abancay - tráfico lento', 2.0, -12.052400, -77.031100, 1.5, false),
('2025-06-15 14:00:00', 'Trabajos de mantenimiento en Av. Brasil', 1.3, -12.078900, -77.069800, 1.0, false);

-- Comentarios informativos
COMMENT ON TABLE customer IS 'Datos de ejemplo: 30 clientes (20 establecidos + 10 nuevos) distribuidos en Lima';
COMMENT ON TABLE vehicle IS 'Datos de ejemplo: 5 vehículos con diferentes capacidades';
COMMENT ON TABLE orders IS 'Datos de ejemplo: ~40 pedidos para el 15/06/2025, cumpliendo regla de 5 días para clientes nuevos';
