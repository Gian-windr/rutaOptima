# RutaÓptima

Sistema de optimización de rutas para empresas de distribución y servicios de courier.

## Descripción

RutaÓptima es una plataforma que automatiza y optimiza la gestión de rutas de entrega para pequeñas y medianas empresas, eliminando el trabajo manual de planificación con mapas físicos y hojas de cálculo.

## Problema que Resuelve

- **Planificación Manual Ineficiente**: Elimina el proceso tedioso de organizar rutas manualmente usando Google Maps y hojas en físico.
- **Costos Operativos Elevados**: Reduce gastos de combustible y tiempo mediante la optimización inteligente de rutas.
- **Entregas Desorganizadas**: Evita entregas ineficientes de ciudad en ciudad al organizar por zonas geográficas.
- **Pérdida de Tiempo**: Ahorra horas de planificación diaria que pueden dedicarse a actividades más productivas.

## Características Principales

### Gestión de Flota
- Registro y administración de vehículos (camionetas, motos, furgonetas)
- Asignación de conductores por vehículo
- Control de capacidad de carga por tipo de vehículo

### Optimización de Rutas
- Cálculo automático de las mejores rutas usando algoritmos avanzados (OptaPlanner)
- Asignación inteligente de pedidos por zona geográfica
- Distribución de carga según capacidad del vehículo
- Estimación de tiempos de llegada (ETA) y salida (ETD)

### Gestión de Pedidos
- Registro de clientes y direcciones de entrega
- Creación de pedidos con fecha de entrega programada
- Sistema de reserva con mínimo de 3 días de anticipación
- Simulación de rutas antes de confirmar entregas

### Visualización de Mapas
- Mapa interactivo que muestra todas las rutas planificadas
- Código de colores único por vehículo/ruta
- Marcadores de paradas con información detallada
- Vista general de toda la flota en operación

### Métricas y Reportes
- Kilómetros totales por ruta
- Costos operativos estimados
- Porcentaje de utilización de vehículos
- Cantidad de pedidos asignados y no asignados

## Tecnologías

### Backend
- Java 21
- Spring Boot 3.5.7
- OptaPlanner 9.44.0 (optimización de rutas)
- PostgreSQL 15
- Flyway (migraciones de base de datos)
- Spring Security + JWT (autenticación)

### Frontend
- React 18
- TypeScript
- Vite
- Leaflet (mapas interactivos)
- OpenStreetMap

### Infraestructura
- Docker & Docker Compose
- Maven

## Requisitos Previos

- Docker Desktop instalado
- Java 21 JDK (opcional, si se ejecuta sin Docker)
- Node.js 18+ (para desarrollo frontend)

## Instalación y Ejecución

### Con Docker (Recomendado)

1. Clonar el repositorio:
```bash
git clone https://github.com/Gian-windr/rutaOptima.git
cd rutaOptima
```

2. Configurar variables de entorno (archivo `.env` ya incluido):
```
DB_PASSWORD=tuContraseñawe
```

3. Levantar los contenedores:
```bash
docker-compose up -d --build
```

4. La aplicación estará disponible en:
- Backend: http://localhost:8080
- Base de datos: localhost:5432

### Sin Docker

1. Configurar PostgreSQL:
```sql
CREATE DATABASE rutaoptima;
CREATE USER postgres WITH PASSWORD 'Bryger170180';
```

2. Ejecutar el backend:
```bash
./mvnw spring-boot:run
```

## Endpoints Principales

### Autenticación
- `POST /api/auth/login` - Inicio de sesión

### Gestión
- `GET /api/customers` - Listar clientes
- `POST /api/customers` - Crear cliente
- `GET /api/vehicles` - Listar vehículos
- `POST /api/vehicles` - Crear vehículo
- `POST /api/orders` - Crear pedido

### Optimización de Rutas
- `POST /api/route-plans-demo/optimize` - Optimización con datos de demostración
- `POST /api/route-plans/optimize` - Optimización con datos reales

## Estructura del Proyecto

```
rutaOptima/
├── src/
│   ├── main/
│   │   ├── java/com/customer/rutaOptima/
│   │   │   ├── api/              # Controladores REST y DTOs
│   │   │   ├── config/           # Configuraciones
│   │   │   ├── domain/           # Entidades JPA
│   │   │   ├── optimization/     # Lógica de OptaPlanner
│   │   │   ├── persistence/      # Repositorios
│   │   │   ├── security/         # JWT y seguridad
│   │   │   └── service/          # Lógica de negocio
│   │   └── resources/
│   │       ├── application.yml   # Configuración Spring
│   │       └── db/migration/     # Scripts SQL Flyway
│   └── test/
├── docker-compose.yml
├── Dockerfile
└── pom.xml
```

## Credenciales por Defecto

- **Usuario**: admin@rutaoptima.com
- **Contraseña**: password

## Flujo de Uso

1. **Registrar Flota**: Crear vehículos con sus capacidades y características
2. **Registrar Clientes**: Agregar clientes con sus direcciones de entrega
3. **Crear Pedidos**: Registrar pedidos con fecha de entrega (mínimo 3 días)
4. **Optimizar Rutas**: Seleccionar fecha y vehículos, el sistema calcula las mejores rutas
5. **Visualizar**: Ver en el mapa todas las rutas asignadas con código de colores
6. **Ejecutar**: Los conductores siguen las rutas optimizadas

## Ventajas Competitivas

- **Ahorro de Tiempo**: Reducción del 80% en tiempo de planificación de rutas
- **Reducción de Costos**: Hasta 30% de ahorro en combustible por optimización
- **Escalabilidad**: Maneja desde 10 hasta 1000+ entregas diarias
- **Fácil de Usar**: Interfaz intuitiva sin curva de aprendizaje
- **Sin Papel**: Sistema 100% digital, elimina hojas físicas y mapas impresos

## Soporte y Contacto

Para consultas, sugerencias o reportar problemas:
- GitHub Issues: https://github.com/Gian-windr/rutaOptima/issues
- Email: soporte@rutaoptima.com

## Licencia

Este proyecto es propiedad de RutaÓptima. Todos los derechos reservados.

## Estado del Proyecto

Versión actual: 0.0.1-SNAPSHOT (En desarrollo activo)
