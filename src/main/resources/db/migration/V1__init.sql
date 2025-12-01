-- ==============================================
-- RutaOptima - Base de Datos Inicial
-- ==============================================

-- RutaOptima - Base de Datos Inicial (ajuste: horas -> timestamptz para mapear a Instant)

-- Tabla de usuarios para autenticación
CREATE TABLE users
(
    id            BIGSERIAL PRIMARY KEY,
    email         VARCHAR(255)             NOT NULL UNIQUE,
    password_hash VARCHAR(255)             NOT NULL,
    rol           VARCHAR(50)              NOT NULL DEFAULT 'USER',
    activo        BOOLEAN                  NOT NULL DEFAULT true,
    created_at    TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_users_email ON users (email);

-- Tabla de clientes
CREATE TABLE customer
(
    id                       BIGSERIAL PRIMARY KEY,
    nombre                   VARCHAR(255)             NOT NULL,
    direccion                TEXT                     NOT NULL,
    latitud                  DECIMAL(10, 8)           NOT NULL,
    longitud                 DECIMAL(11, 8)           NOT NULL,
    es_nuevo                 BOOLEAN                  NOT NULL DEFAULT true,
    ventana_horaria_inicio   TIMESTAMP WITH TIME ZONE,
    ventana_horaria_fin      TIMESTAMP WITH TIME ZONE,
    demanda_promedio_semanal DECIMAL(10, 2)                    DEFAULT 0,
    factor_estacionalidad    DECIMAL(5, 2)                     DEFAULT 1.0,
    telefono                 VARCHAR(50),
    email                    VARCHAR(255),
    activo                   BOOLEAN                  NOT NULL DEFAULT true,
    created_at               TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at               TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_customer_es_nuevo ON customer (es_nuevo);
CREATE INDEX idx_customer_activo ON customer (activo);
CREATE INDEX idx_customer_latitud_longitud ON customer (latitud, longitud);

-- Tabla de pedidos
CREATE TABLE orders
(
    id                           BIGSERIAL PRIMARY KEY,
    customer_id                  BIGINT                   NOT NULL REFERENCES customer (id) ON DELETE CASCADE,
    fecha_entrega                TIMESTAMP WITH TIME ZONE NOT NULL,
    cantidad                     DECIMAL(10, 2)           NOT NULL,
    volumen                      DECIMAL(10, 2),
    peso                         DECIMAL(10, 2),
    estado                       VARCHAR(50)              NOT NULL DEFAULT 'PENDIENTE',
    ventana_horaria_inicio       TIMESTAMP WITH TIME ZONE,
    ventana_horaria_fin          TIMESTAMP WITH TIME ZONE,
    prioridad                    INT                               DEFAULT 1,
    tiempo_servicio_estimado_min INT                               DEFAULT 10,
    notas                        TEXT,
    created_at                   TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at                   TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_cantidad_positive CHECK (cantidad > 0),
    CONSTRAINT chk_volumen_positive CHECK (volumen IS NULL OR volumen >= 0),
    CONSTRAINT chk_peso_positive CHECK (peso IS NULL OR peso >= 0),
    CONSTRAINT chk_tiempo_servicio_positive CHECK (tiempo_servicio_estimado_min IS NULL OR tiempo_servicio_estimado_min >= 0)
);

CREATE INDEX idx_orders_customer_id ON orders (customer_id);
CREATE INDEX idx_orders_fecha_entrega ON orders (fecha_entrega);
CREATE INDEX idx_orders_estado ON orders (estado);
CREATE INDEX idx_orders_fecha_customer ON orders (fecha_entrega, customer_id);

-- Tabla de vehículos
CREATE TABLE vehicle
(
    id                 BIGSERIAL PRIMARY KEY,
    nombre             VARCHAR(255)             NOT NULL,
    patente            VARCHAR(20)              NOT NULL UNIQUE,
    tipo               VARCHAR(100)             NOT NULL,
    capacidad_cantidad DECIMAL(10, 2)           NOT NULL,
    capacidad_volumen  DECIMAL(10, 2),
    capacidad_peso     DECIMAL(10, 2),
    velocidad_kmh      DECIMAL(5, 2)            NOT NULL DEFAULT 40.0,
    costo_km           DECIMAL(10, 2)           NOT NULL DEFAULT 1.5,
    activo             BOOLEAN                  NOT NULL DEFAULT true,
    depot_latitud      DECIMAL(10, 8)           NOT NULL,
    depot_longitud     DECIMAL(11, 8)           NOT NULL,
    jornada_inicio     TIMESTAMP WITH TIME ZONE          DEFAULT '1970-01-01 08:00:00+00',
    jornada_fin        TIMESTAMP WITH TIME ZONE          DEFAULT '1970-01-01 18:00:00+00',
    created_at         TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at         TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_capacidad_cantidad_positive CHECK (capacidad_cantidad > 0),
    CONSTRAINT chk_velocidad_positive CHECK (velocidad_kmh > 0),
    CONSTRAINT chk_costo_positive CHECK (costo_km >= 0)
);

CREATE INDEX idx_vehicle_activo ON vehicle (activo);
CREATE INDEX idx_vehicle_depot ON vehicle (depot_latitud, depot_longitud);

-- Tabla de planes de ruta
CREATE TABLE route_plan
(
    id                                BIGSERIAL PRIMARY KEY,
    fecha                             TIMESTAMP WITH TIME ZONE NOT NULL,
    estado                            VARCHAR(50)              NOT NULL DEFAULT 'CREATED',
    objetivo                          VARCHAR(50)              NOT NULL DEFAULT 'MINIMIZE_DISTANCE',
    allow_soft_time_window_violations BOOLEAN                  NOT NULL DEFAULT false,
    kms_totales                       DECIMAL(10, 2),
    tiempo_estimado_min               INT,
    costo_total                       DECIMAL(10, 2),
    vehiculos_utilizados              INT                               DEFAULT 0,
    pedidos_asignados                 INT                               DEFAULT 0,
    pedidos_no_asignados              INT                               DEFAULT 0,
    score                             VARCHAR(255),
    tiempo_optimizacion_seg           INT,
    max_optimization_time_seconds     INT,
    created_at                        TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at                        TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_route_plan_fecha ON route_plan (fecha);
CREATE INDEX idx_route_plan_estado ON route_plan (estado);
CREATE INDEX idx_route_plan_fecha_estado ON route_plan (fecha, estado);

-- Tabla de paradas de ruta
CREATE TABLE route_stop
(
    id                              BIGSERIAL PRIMARY KEY,
    route_plan_id                   BIGINT                   NOT NULL REFERENCES route_plan (id) ON DELETE CASCADE,
    vehicle_id                      BIGINT                   NOT NULL REFERENCES vehicle (id) ON DELETE CASCADE,
    order_id                        BIGINT                   NOT NULL REFERENCES orders (id) ON DELETE CASCADE,
    secuencia                       INT                      NOT NULL,
    eta                             TIMESTAMP WITH TIME ZONE,
    etd                             TIMESTAMP WITH TIME ZONE,
    distancia_km_desde_anterior     DECIMAL(10, 2),
    tiempo_viaje_min_desde_anterior INT,
    carga_acumulada_cantidad        DECIMAL(10, 2),
    carga_acumulada_volumen         DECIMAL(10, 2),
    carga_acumulada_peso            DECIMAL(10, 2),
    tiempo_espera_min               INT                               DEFAULT 0,
    created_at                      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_secuencia_positive CHECK (secuencia > 0)
);

CREATE INDEX idx_route_stop_route_plan ON route_stop (route_plan_id);
CREATE INDEX idx_route_stop_vehicle ON route_stop (vehicle_id);
CREATE INDEX idx_route_stop_order ON route_stop (order_id);
CREATE INDEX idx_route_stop_route_vehicle_seq ON route_stop (route_plan_id, vehicle_id, secuencia);
CREATE UNIQUE INDEX idx_route_stop_unique_order_per_plan ON route_stop (route_plan_id, order_id);

-- Tabla de eventos de tráfico (para re-optimización)
CREATE TABLE traffic_event
(
    id                  BIGSERIAL PRIMARY KEY,
    fecha_hora          TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    descripcion         TEXT                     NOT NULL,
    factor_retraso      DECIMAL(5, 2)            NOT NULL DEFAULT 1.0,
    latitud             DECIMAL(10, 8),
    longitud            DECIMAL(11, 8),
    radio_afectacion_km DECIMAL(10, 2),
    activo              BOOLEAN                  NOT NULL DEFAULT true,
    created_at          TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_factor_retraso_positive CHECK (factor_retraso >= 1.0)
);

CREATE INDEX idx_traffic_event_fecha_hora ON traffic_event (fecha_hora);
CREATE INDEX idx_traffic_event_activo ON traffic_event (activo);
CREATE INDEX idx_traffic_event_location ON traffic_event (latitud, longitud);

-- Comentarios en tablas
COMMENT ON TABLE users IS 'Usuarios del sistema para autenticación JWT';
COMMENT ON TABLE customer IS 'Clientes que reciben entregas';
COMMENT ON TABLE orders IS 'Pedidos a entregar en rutas';
COMMENT ON TABLE vehicle IS 'Vehículos disponibles para entregas';
COMMENT ON TABLE route_plan IS 'Planes de ruta optimizados';
COMMENT ON TABLE route_stop IS 'Paradas individuales en cada ruta';
COMMENT ON TABLE traffic_event IS 'Eventos de tráfico para re-optimización';

-- Función para actualizar timestamp
CREATE OR REPLACE FUNCTION update_updated_at_column()
    RETURNS TRIGGER AS
$$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Triggers para updated_at
CREATE TRIGGER update_users_updated_at
    BEFORE UPDATE
    ON users
    FOR EACH ROW
EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_customer_updated_at
    BEFORE UPDATE
    ON customer
    FOR EACH ROW
EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_orders_updated_at
    BEFORE UPDATE
    ON orders
    FOR EACH ROW
EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_vehicle_updated_at
    BEFORE UPDATE
    ON vehicle
    FOR EACH ROW
EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_route_plan_updated_at
    BEFORE UPDATE
    ON route_plan
    FOR EACH ROW
EXECUTE FUNCTION update_updated_at_column();
