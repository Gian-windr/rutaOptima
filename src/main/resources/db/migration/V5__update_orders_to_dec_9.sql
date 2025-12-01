-- ==============================================
-- Actualizar fechas de órdenes al 9 de diciembre 2025
-- ==============================================

UPDATE orders
SET fecha_entrega = '2025-12-09 08:00:00+00'::timestamptz,
    updated_at = NOW()
WHERE estado = 'PENDIENTE';

COMMENT ON TABLE orders IS 'Pedidos actualizados al 9 de diciembre de 2025';
