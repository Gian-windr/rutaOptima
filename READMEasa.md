# 🚚 RutaOptima - Sistema de Optimización Logística

**RutaOptima** es un SaaS de optimización logística diseñado para PYMEs que gestionan su propia flota de reparto. Resuelve el problema de Vehicle Routing Problem with Time Windows (VRPTW) utilizando OptaPlanner, minimizando costos de operación y maximizando la eficiencia de entregas.

## 📋 Tabla de Contenidos

- [Características](#-características)
- [Tecnologías](#-tecnologías)
- [Arquitectura](#-arquitectura)
- [Requisitos](#-requisitos)
- [Instalación y Configuración](#-instalación-y-configuración)
- [Uso](#-uso)
- [API Endpoints](#-api-endpoints)
- [Reglas de Negocio](#-reglas-de-negocio)
- [Decisiones Técnicas](#-decisiones-técnicas)
- [Testing](#-testing)
- [Troubleshooting](#-troubleshooting)

## ✨ Características

### Funcionalidades Core

- ✅ **Optimización de rutas VRPTW**: Asignación inteligente de pedidos a vehículos considerando:
  - Capacidad de vehículos (cantidad, volumen, peso)
  - Ventanas horarias de entrega
  - Jornadas laborales de conductores
  - Minimización de distancia/tiempo/costo

- ✅ **Re-optimización dinámica**: Recalcular rutas cuando hay:
  - Cambios de tráfico
  - Retrasos de conductores
  - Pedidos urgentes de última hora

- ✅ **Regla de negocio de 5 días**: Para clientes nuevos, los pedidos deben realizarse con **mínimo 5 días de anticipación**

- ✅ **Gestión completa**:
  - Clientes (con ubicaciones geocodificadas)
  - Pedidos (con prioridades y ventanas horarias)
  - Vehículos (con capacidades heterogéneas)
  - Eventos de tráfico

- ✅ **Autenticación JWT**: Seguridad robusta con tokens JWT

- ✅ **Métricas en tiempo real**: Kilómetros totales, tiempo estimado, costo, vehículos utilizados

## 🛠 Tecnologías

### Backend

- **Java 21**: Última versión LTS
- **Spring Boot 3.5.7**: Framework principal
- **Spring Data JPA**: Persistencia con Hibernate
- **Spring Security**: Autenticación JWT
- **OptaPlanner 9.44.0**: Motor de optimización constraint-based
- **PostgreSQL 15+**: Base de datos relacional
- **Flyway**: Migraciones de BD versionadas
- **MapStruct**: Mapeo DTO-Entity
- **Lombok**: Reducción de boilerplate
- **Maven**: Gestión de dependencias

### DevOps

- **Docker & Docker Compose**: Containerización
- **Spring Boot Actuator**: Monitoreo y health checks

## 🏗 Arquitectura

### Estructura de Paquetes

```
com.customer.rutaOptima/
├── api/                    # Controladores REST
│   ├── dto/               # DTOs de request/response
│   └── controller/        # Endpoints REST
├── domain/                # Entidades JPA
├── persistence/           # Repositorios Spring Data
├── service/               # Lógica de negocio
├── optimization/          # Motor OptaPlanner
│   ├── domain/           # Planning entities y solution
│   └── constraints/      # Constraint provider (scoring)
├── security/              # JWT y configuración de seguridad
├── config/                # Configuraciones
│   └── exception/        # Manejo global de excepciones
└── RutaOptimaApplication  # Clase principal
```

### Modelo de Datos

**Entidades principales:**

- `User`: Usuarios del sistema (autenticación)
- `Customer`: Clientes que reciben entregas
- `Order`: Pedidos a entregar
- `Vehicle`: Vehículos de la flota
- `RoutePlan`: Plan de ruta optimizado
- `RouteStop`: Parada individual en una ruta
- `TrafficEvent`: Eventos de tráfico para re-optimización

**Diagrama ER (simplificado):**

```
User (1) ──────────────────────────────────────┐
                                               │
Customer (1) ────< (N) Order (N) >──── RoutePlan (1)
                                               │
Vehicle (N) ────────────────────────>──── RouteStop (N)
                                               │
TrafficEvent ──────────────────────────────────┘
```

## 📦 Requisitos

### Para desarrollo local

- **Java 21** (JDK)
- **Maven 3.9+**
- **PostgreSQL 15+**
- **Docker & Docker Compose** (recomendado)

### Para producción con Docker

- **Docker 20.10+**
- **Docker Compose 2.0+**

## 🚀 Instalación y Configuración

### Opción 1: Ejecución con Docker Compose (Recomendado)

1. **Clonar el repositorio**:
```powershell
cd "c:\Users\LENOVO LOQ\Desktop\VI - 2025 - 20\Customer Development\PROYECTO\rutaOptima"
```

2. **Construir y levantar los servicios**:
```powershell
docker-compose up -d --build
```

Esto levantará:
- PostgreSQL en puerto `5432`
- Aplicación Spring Boot en puerto `8080`
- pgAdmin en puerto `5050` (opcional)

3. **Verificar que todo esté corriendo**:
```powershell
docker-compose ps
curl http://localhost:8080/actuator/health
```

### Opción 2: Ejecución local (desarrollo)

1. **Iniciar PostgreSQL**:
```powershell
# Opción A: Con Docker
docker run --name rutaoptima-postgres -e POSTGRES_PASSWORD=postgres -e POSTGRES_DB=rutaoptima_dev -p 5432:5432 -d postgres:15-alpine

# Opción B: Instalar PostgreSQL localmente
# Crear base de datos: rutaoptima_dev
```

2. **Configurar application.yml** (ya configurado para perfil `dev`)

3. **Compilar el proyecto**:
```powershell
mvn clean package -DskipTests
```

4. **Ejecutar la aplicación**:
```powershell
mvn spring-boot:run
```

O:
```powershell
java -jar target\rutaOptima-0.0.1-SNAPSHOT.jar
```

La aplicación estará disponible en: `http://localhost:8080`

## 📘 Uso

### 1. Autenticación

Primero, obtén un token JWT:

**POST** `/api/auth/login`

```json
{
  "email": "admin@rutaoptima.com",
  "password": "admin123"
}
```

**Respuesta:**
```json
{
  "token": "eyJhbGciOiJIUzI1NiIs...",
  "type": "Bearer",
  "email": "admin@rutaoptima.com",
  "rol": "ADMIN"
}
```

Usa este token en todas las siguientes peticiones:
```
Authorization: Bearer {token}
```

### 2. Crear Clientes

**POST** `/api/customers`

```json
{
  "nombre": "Bodega Mi Barrio",
  "direccion": "Av. Principal 123, Lima",
  "latitud": -12.046374,
  "longitud": -77.042793,
  "esNuevo": false,
  "ventanaHorariaInicio": "09:00:00",
  "ventanaHorariaFin": "13:00:00",
  "telefono": "999888777",
  "email": "mibarrio@example.com"
}
```

### 3. Crear Pedidos

**POST** `/api/orders`

```json
{
  "customerId": 1,
  "fechaEntrega": "2025-06-15",
  "cantidad": 15.50,
  "volumen": 2.0,
  "peso": 155.0,
  "ventanaHorariaInicio": "09:00:00",
  "ventanaHorariaFin": "12:00:00",
  "prioridad": 1,
  "notas": "Llamar antes de llegar"
}
```

⚠️ **Importante**: Si el cliente es nuevo (`esNuevo=true`), la fecha de entrega debe ser **al menos 5 días después** de la fecha actual, o recibirás un error 422.

### 4. Crear Vehículos

**POST** `/api/vehicles`

```json
{
  "nombre": "Furgoneta 01",
  "tipo": "FURGONETA_GRANDE",
  "capacidadCantidad": 100.00,
  "capacidadVolumen": 15.00,
  "capacidadPeso": 1500.00,
  "velocidadKmh": 45.00,
  "costoKm": 2.50,
  "depotLatitud": -12.046374,
  "depotLongitud": -77.042793,
  "jornadaInicio": "08:00:00",
  "jornadaFin": "18:00:00"
}
```

### 5. Optimizar Rutas (¡El Core!)

**POST** `/api/route-plans/optimize`

```json
{
  "fecha": "2025-06-15",
  "vehicleIds": [1, 2, 3, 4, 5],
  "objective": "MINIMIZE_DISTANCE",
  "allowSoftTimeWindowViolations": false,
  "maxOptimizationTimeSeconds": 20
}
```

**Respuesta:**
```json
{
  "routePlanId": 42,
  "status": "OPTIMIZED",
  "metrics": {
    "totalKm": 128.4,
    "totalTimeMin": 540,
    "totalCost": 320.50,
    "vehiculosUtilizados": 4,
    "pedidosAsignados": 35,
    "pedidosNoAsignados": 0
  },
  "routes": [
    {
      "vehicleId": 1,
      "vehicleName": "Furgoneta 01",
      "stops": [
        {
          "orderId": 101,
          "customerId": 15,
          "customerName": "Bodega San Juan",
          "direccion": "Av. Arequipa 1234",
          "sequence": 1,
          "eta": "09:05",
          "etd": "09:15",
          "distanceKmFromPrev": 3.2,
          "travelTimeMinFromPrev": 8,
          "cargaAcumuladaCantidad": 12.00
        },
        // ... más paradas
      ],
      "totalKm": 35.2,
      "totalTimeMin": 145
    }
    // ... más vehículos
  ],
  "score": "0hard/-12840soft",
  "tiempoOptimizacionSeg": 18
}
```

### 6. Re-optimizar (con eventos de tráfico)

**POST** `/api/route-plans/{id}/reoptimize`

```json
{
  "trafficFactor": 1.5,
  "reason": "Accidente en Av. Javier Prado"
}
```

### 7. Consultar Ruta Optimizada

**GET** `/api/route-plans/{id}`

Devuelve el plan completo con todas las paradas ordenadas.

## 🔌 API Endpoints

### Autenticación

| Método | Endpoint | Descripción |
|--------|----------|-------------|
| POST | `/api/auth/login` | Iniciar sesión (devuelve JWT) |

### Clientes

| Método | Endpoint | Descripción |
|--------|----------|-------------|
| GET | `/api/customers` | Listar todos los clientes |
| GET | `/api/customers/{id}` | Obtener cliente por ID |
| POST | `/api/customers` | Crear nuevo cliente |
| PUT | `/api/customers/{id}` | Actualizar cliente |
| DELETE | `/api/customers/{id}` | Eliminar cliente |

### Pedidos

| Método | Endpoint | Descripción |
|--------|----------|-------------|
| GET | `/api/orders` | Listar pedidos (filtro por fecha) |
| GET | `/api/orders/{id}` | Obtener pedido por ID |
| POST | `/api/orders` | Crear nuevo pedido ⚠️ (valida regla 5 días) |
| PUT | `/api/orders/{id}` | Actualizar pedido |
| DELETE | `/api/orders/{id}` | Eliminar pedido |

### Vehículos

| Método | Endpoint | Descripción |
|--------|----------|-------------|
| GET | `/api/vehicles` | Listar vehículos activos |
| GET | `/api/vehicles/{id}` | Obtener vehículo por ID |
| POST | `/api/vehicles` | Crear nuevo vehículo |
| PUT | `/api/vehicles/{id}` | Actualizar vehículo |
| DELETE | `/api/vehicles/{id}` | Desactivar vehículo |

### Optimización de Rutas

| Método | Endpoint | Descripción |
|--------|----------|-------------|
| POST | `/api/route-plans/optimize` | Optimizar rutas para una fecha |
| GET | `/api/route-plans/{id}` | Obtener plan de ruta |
| POST | `/api/route-plans/{id}/reoptimize` | Re-optimizar con cambios |
| GET | `/api/route-plans` | Listar planes (filtro por fecha) |

### Actuator (Monitoreo)

| Método | Endpoint | Descripción |
|--------|----------|-------------|
| GET | `/actuator/health` | Estado de salud |
| GET | `/actuator/info` | Información de la app |
| GET | `/actuator/metrics` | Métricas de rendimiento |

## 📜 Reglas de Negocio

### 1. Regla de 5 Días para Clientes Nuevos ⭐

**Descripción**: Los pedidos de clientes marcados como "nuevos" (`esNuevo=true`) deben realizarse con **mínimo 5 días de anticipación**.

**Implementación**: `OrderService.validateNewCustomerLeadTime()`

**Ejemplo**:
- Hoy: 10 de junio de 2025
- Cliente nuevo quiere pedido para: 12 de junio ❌ RECHAZADO (solo 2 días)
- Cliente nuevo quiere pedido para: 16 de junio ✅ ACEPTADO (6 días)

**Respuesta de error (HTTP 422)**:
```json
{
  "timestamp": "2025-06-10T14:30:00",
  "status": 422,
  "error": "Business Rule Violation",
  "message": "Para clientes nuevos, los pedidos deben realizarse con mínimo 5 días de anticipación. Fecha actual: 2025-06-10, Fecha entrega: 2025-06-12 (solo 2 días de anticipación). Cliente: Bodega Nueva Victoria"
}
```

### 2. Constraints de Optimización

#### Hard Constraints (DEBEN cumplirse)

- ✅ Capacidad de cantidad del vehículo no excedida
- ✅ Capacidad de volumen del vehículo no excedida (si aplica)
- ✅ Capacidad de peso del vehículo no excedida (si aplica)
- ✅ Ventanas horarias de entrega respetadas
- ✅ Jornada laboral del vehículo no excedida

#### Soft Constraints (Objetivos de optimización)

- 📉 Minimizar distancia total recorrida
- 📉 Minimizar tiempo total de viaje
- 📉 Minimizar número de vehículos utilizados
- 📈 Preferir pedidos de alta prioridad primero

### 3. Demanda Dinámica y Estacionalidad

El modelo de datos incluye campos para gestionar variaciones estacionales:
- `demandaPromedioSemanal`: Demanda histórica promedio
- `factorEstacionalidad`: Factor multiplicador (1.0=normal, >1.0=alta temporada)

**Nota**: El MVP no implementa predicción automática, pero los endpoints permiten actualizar estos valores manualmente.

## 💡 Decisiones Técnicas

### ¿Por qué OptaPlanner?

1. **100% Java**: Integración nativa con Spring Boot sin necesidad de wrappers o llamadas externas
2. **Constraint Solving**: Modelado declarativo de restricciones (más mantenible que heurísticas custom)
3. **Escalabilidad**: Maneja eficientemente de 1 a 300+ pedidos
4. **Flexibilidad**: Fácil agregar nuevas constraints o cambiar la función de scoring
5. **Comunidad activa**: Documentación extensa y ejemplos de VRPTW

**Alternativas consideradas**:
- **OR-Tools**: Excelente pero requiere wrapper Python/C++, complejidad adicional
- **Heurísticas custom**: Menos robusto, difícil de mantener

### Arquitectura de la Solución OptaPlanner

**Planning Entity**: `Visit` (representa una visita a un cliente)

**Planning Variables**:
- `vehicle`: Vehículo asignado
- `previousVisit`: Visita anterior (para ordenamiento)

**Shadow Variables** (calculadas automáticamente):
- `arrivalTime`: Hora de llegada
- `cargaAcumuladaCantidad/Volumen/Peso`: Cargas acumuladas

**Planning Solution**: `VehicleRoutingSolution` (contiene todas las visitas y vehículos)

**Constraint Provider**: `VehicleRoutingConstraintProvider` (define scoring)

### Cálculo de Distancias

Usamos la **fórmula de Haversine** para calcular distancias entre coordenadas GPS:

```java
private double calcularDistanciaHaversine(double lat1, double lon1, double lat2, double lon2) {
    final int R = 6371; // Radio de la Tierra en km
    double latDistance = Math.toRadians(lat2 - lat1);
    double lonDistance = Math.toRadians(lon2 - lon1);
    double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
            + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
            * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
    double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    return R * c;
}
```

**Nota**: Para producción con datos reales, considerar integrar APIs de routing (Google Maps, Mapbox, OSRM) para obtener distancias reales por carretera.

### Factor de Tráfico

Los eventos de tráfico aplican un multiplicador al tiempo de viaje:
- `factorTrafico = 1.0`: Tráfico normal
- `factorTrafico = 1.5`: 50% más lento
- `factorTrafico = 2.0`: El doble de tiempo

Implementado en: `VehicleInfo.calcularTiempoViajeMinutos()`

## 🧪 Testing

### Tests Unitarios

Ejecutar:
```powershell
mvn test
```

**Tests incluidos**:
- `OrderServiceTest`: Valida regla de 5 días
- `HaversineDistanceTest`: Verifica cálculo de distancias
- `VehicleRoutingConstraintProviderTest`: Valida constraints
- `OptimizationIntegrationTest`: Test end-to-end con 20 pedidos

### Test Manual con Postman/cURL

**Colección Postman incluida en**: `postman_collection.json` (crear manualmente o usar cURL)

**Ejemplo cURL de flujo completo**:

```powershell
# 1. Login
$TOKEN = (Invoke-RestMethod -Uri http://localhost:8080/api/auth/login -Method POST -ContentType "application/json" -Body '{"email":"admin@rutaoptima.com","password":"admin123"}').token

# 2. Listar pedidos pendientes para el 15/06/2025
Invoke-RestMethod -Uri "http://localhost:8080/api/orders?fecha=2025-06-15&estado=PENDIENTE" -Method GET -Headers @{ Authorization = "Bearer $TOKEN" }

# 3. Optimizar rutas
Invoke-RestMethod -Uri http://localhost:8080/api/route-plans/optimize -Method POST -Headers @{ Authorization = "Bearer $TOKEN"; "Content-Type" = "application/json" } -Body '{"fecha":"2025-06-15","vehicleIds":[1,2,3,4,5],"objective":"MINIMIZE_DISTANCE"}'
```

## 🐛 Troubleshooting

### Error: "Port 5432 is already allocated"

PostgreSQL ya está corriendo. Opciones:
1. Detener PostgreSQL local: `Stop-Service postgresql-x64-15` (Windows)
2. Cambiar puerto en `docker-compose.yml`: `"5433:5432"`

### Error: "Unable to connect to database"

1. Verificar que PostgreSQL esté corriendo:
```powershell
docker-compose ps
```

2. Verificar logs:
```powershell
docker-compose logs postgres
docker-compose logs app
```

3. Verificar credenciales en `application.yml`

### Error: "OptaPlanner no encuentra solución"

**Posibles causas**:
1. Restricciones muy estrictas (ej: ventanas horarias imposibles)
2. Capacidad de vehículos insuficiente
3. Tiempo de optimización muy corto

**Soluciones**:
- Aumentar `maxOptimizationTimeSeconds` a 60-120s
- Activar `allowSoftTimeWindowViolations: true`
- Revisar logs de OptaPlanner: nivel DEBUG

### La aplicación no inicia

```powershell
# Limpiar y recompilar
mvn clean install -DskipTests

# Verificar versión de Java
java -version  # Debe ser 21

# Verificar variables de entorno
echo $env:JAVA_HOME
```

### Datos de ejemplo no cargados

Flyway debería cargar automáticamente `V2__seed_data.sql`. Verificar:

```powershell
# Conectar a la BD
docker exec -it rutaoptima-db psql -U postgres -d rutaoptima_prod

# Verificar datos
SELECT COUNT(*) FROM customer;
SELECT COUNT(*) FROM vehicle;
```

Si no hay datos:
```sql
-- Ejecutar manualmente el script
\i /path/to/V2__seed_data.sql
```

## 📊 Datos de Ejemplo Incluidos

El sistema viene con datos precargados (`V2__seed_data.sql`):

- **2 usuarios**: admin y despachador (password: `admin123`)
- **5 vehículos**: 2 furgonetas grandes, 2 medianas, 1 moto
- **30 clientes**: 20 establecidos + 10 nuevos (Lima, Perú)
- **~40 pedidos**: Para el 15/06/2025, incluyendo varios de clientes nuevos

**Password hasheado**: `$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy` = `admin123`

## 🚀 Próximos Pasos (Post-MVP)

- [ ] Dashboard web con mapa interactivo (Leaflet + OpenStreetMap)
- [ ] Integración con APIs de routing reales (Google Maps Directions)
- [ ] Notificaciones en tiempo real (WebSockets)
- [ ] Tracking GPS de conductores
- [ ] Predicción de demanda con ML
- [ ] Multi-depot (múltiples centros de distribución)
- [ ] Exportación de rutas a PDF/Excel
- [ ] Aplicación móvil para conductores

## 📄 Licencia

Este proyecto es un MVP académico/demostrativo. Para uso comercial, contactar al equipo de desarrollo.

## 👥 Equipo

Desarrollado por el equipo de RutaOptima.

---

**¿Preguntas o problemas?** Abrir un issue o contactar a: dev@rutaoptima.com

**¡Feliz optimización de rutas! 🚚📦✨**
