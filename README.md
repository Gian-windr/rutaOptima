# RutaOptima - Sistema de Optimización de Rutas Logísticas

Sistema SaaS para optimización de rutas de entrega diseñado para pequeñas y medianas empresas con flotas de vehículos heterogéneas. Utiliza algoritmos de programación por restricciones para resolver el problema de enrutamiento de vehículos con ventanas de tiempo (VRPTW).

## Características Principales

- **Optimización Inteligente**: Resuelve el VRPTW considerando capacidades heterogéneas, ventanas de tiempo y restricciones operativas
- **Re-optimización Dinámica**: Ajusta rutas en tiempo real ante eventos de tráfico
- **Regla de Negocio Crítica**: Validación automática de lead time de 5 días para nuevos clientes
- **Flotas Heterogéneas**: Soporte para vehículos con diferentes capacidades (cantidad, volumen, peso)
- **API REST Completa**: Endpoints documentados para todas las operaciones
- **Autenticación JWT**: Sistema de seguridad stateless para APIs
- **Containerización**: Despliegue con Docker y Docker Compose

## Stack Tecnológico

**Backend**
- Java 21 (LTS)
- Spring Boot 3.5.7
- Spring Data JPA
- Spring Security
- OptaPlanner 9.44.0 (Motor de optimización)

**Base de Datos**
- PostgreSQL 15+
- Flyway (Migraciones)

**Infraestructura**
- Docker & Docker Compose
- Maven

**Librerías**
- JWT (io.jsonwebtoken 0.12.5)
- MapStruct 1.5.5 + Lombok
- Spring Boot Actuator

## Arquitectura

Arquitectura en capas con separación de responsabilidades:

```
├── api/              # Controladores REST y DTOs
├── domain/           # Entidades JPA
├── service/          # Lógica de negocio
├── persistence/      # Repositorios
├── optimization/     # Modelo OptaPlanner
├── security/         # Configuración JWT
└── config/           # Configuración Spring
```

## Modelo de Optimización

El sistema implementa un solver constraint-based con:

**Restricciones Hard (No violables)**
- Capacidad de vehículos (cantidad, volumen, peso)
- Ventanas de tiempo de clientes
- Jornada laboral de conductores

**Restricciones Soft (Optimizables)**
- Minimizar distancia total
- Minimizar tiempo de viaje
- Reducir cantidad de vehículos utilizados
- Priorizar pedidos de alta importancia

## Instalación

### Requisitos Previos
- Java 21+
- Docker y Docker Compose
- Maven 3.8+

### Inicio Rápido con Docker

```bash
# Clonar repositorio
git clone https://github.com/Gian-windr/rutaOptima.git
cd rutaOptima

# Levantar todos los servicios
docker-compose up -d --build

# Verificar estado
curl http://localhost:8080/actuator/health
```

El sistema estará disponible en:
- API: http://localhost:8080
- PostgreSQL: localhost:5432
- pgAdmin: http://localhost:5050

### Ejecución Local

```bash
# Configurar base de datos PostgreSQL local
# Editar src/main/resources/application.yml

# Ejecutar aplicación
mvn spring-boot:run
```

## Uso

### Autenticación

```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "admin@rutaoptima.com",
    "password": "admin123"
  }'
```

Respuesta:
```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9...",
  "tipo": "Bearer",
  "expiraEn": 86400
}
```

### Optimizar Rutas

```bash
curl -X POST http://localhost:8080/api/route-plans/optimize \
  -H "Authorization: Bearer {token}" \
  -H "Content-Type: application/json" \
  -d '{
    "fecha": "2025-06-15",
    "vehicleIds": [1, 2, 3, 4, 5],
    "objective": "MINIMIZE_DISTANCE",
    "allowSoftTimeWindowViolations": false,
    "maxOptimizationTimeSeconds": 20
  }'
```

Respuesta:
```json
{
  "routePlanId": 1,
  "status": "OPTIMIZED",
  "score": "0hard/-234567soft",
  "metrics": {
    "totalKm": 234.56,
    "totalTimeMin": 456,
    "totalCost": 293.20,
    "vehiculosUtilizados": 4,
    "pedidosAsignados": 38
  },
  "vehicleRoutes": [...]
}
```

## Endpoints Principales

| Método | Endpoint | Descripción |
|--------|----------|-------------|
| POST | `/api/auth/login` | Autenticación |
| GET | `/api/customers` | Listar clientes |
| POST | `/api/customers` | Crear cliente |
| GET | `/api/vehicles` | Listar vehículos |
| GET | `/api/orders` | Listar pedidos |
| POST | `/api/orders` | Crear pedido (valida regla 5 días) |
| POST | `/api/route-plans/optimize` | Optimizar rutas |
| GET | `/api/route-plans/{id}` | Obtener plan con paradas |
| POST | `/api/route-plans/{id}/reoptimize` | Re-optimizar ante cambios |

## Reglas de Negocio

### Regla de 5 Días para Nuevos Clientes

Los clientes marcados como nuevos (`es_nuevo = true`) deben realizar pedidos con al menos 5 días de anticipación. Esta regla se valida automáticamente al crear un pedido.

**Ejemplo válido:**
- Fecha actual: 2025-11-10
- Cliente: Nuevo
- Fecha de entrega: 2025-11-16 o posterior ✓

**Ejemplo inválido:**
- Fecha actual: 2025-11-10
- Cliente: Nuevo
- Fecha de entrega: 2025-11-14 ✗

## Datos de Ejemplo

El sistema incluye datos semilla para pruebas:
- 2 usuarios (admin y despachador)
- 5 vehículos con capacidades variadas
- 30 clientes (20 establecidos, 10 nuevos)
- ~40 pedidos para fecha 2025-06-15
- Eventos de tráfico simulados

Credenciales por defecto:
- Email: `admin@rutaoptima.com`
- Password: `admin123`

## Configuración

Perfiles disponibles:
- `dev`: Desarrollo con logs detallados
- `prod`: Producción con configuración optimizada

Configuración clave en `application.yml`:
```yaml
optaplanner:
  solver:
    termination:
      spent-limit: 20s
    environment-mode: REPRODUCIBLE

jwt:
  secret: {tu-secreto-seguro}
  expiration: 86400000  # 24 horas
```

## Desarrollo

### Estructura del Proyecto

```
src/main/java/com/customer/rutaOptima/
├── api/
│   ├── controller/     # Controladores REST
│   └── dto/           # Objetos de transferencia
├── domain/            # Entidades JPA
├── service/           # Servicios de negocio
├── persistence/       # Repositorios
├── optimization/      # Modelo OptaPlanner
│   ├── domain/       # Entidades de optimización
│   └── solver/       # Restricciones y configuración
├── security/          # JWT y autenticación
├── config/           # Configuración Spring
└── exception/        # Manejo de excepciones
```

### Tests

```bash
# Ejecutar tests
mvn test

# Ejecutar con cobertura
mvn clean test jacoco:report
```

## Decisiones Técnicas

**OptaPlanner vs OR-Tools**: Se eligió OptaPlanner por su integración nativa con Spring Boot, sintaxis declarativa para restricciones y facilidad de mantenimiento.

**JWT Stateless**: Autenticación sin estado para escalabilidad horizontal y simplicidad en arquitecturas distribuidas.

**Flyway**: Control de versiones de base de datos para migraciones reproducibles y auditables.

## Troubleshooting

**Puerto 5432 ocupado**: Detener PostgreSQL local o cambiar puerto en `docker-compose.yml`

**OptaPlanner no encuentra solución**: Incrementar `maxOptimizationTimeSeconds` o revisar restricciones hard

**Error de conexión a BD**: Verificar que el contenedor de PostgreSQL esté activo con `docker-compose ps`

Ver logs detallados:
```bash
docker-compose logs -f app
```

## Contribución

1. Fork el proyecto
2. Crea una rama para tu feature (`git checkout -b feature/nueva-funcionalidad`)
3. Commit tus cambios (`git commit -m 'Agregar nueva funcionalidad'`)
4. Push a la rama (`git push origin feature/nueva-funcionalidad`)
5. Abre un Pull Request

## Licencia

Este proyecto es un MVP académico desarrollado como parte del curso de Customer Development.

## Contacto

- GitHub: [@Gian-windr](https://github.com/Gian-windr)
- Proyecto: [rutaOptima](https://github.com/Gian-windr/rutaOptima)

---

**Nota**: Este es un MVP funcional. Para producción se recomienda implementar tests de integración completos, monitoreo con herramientas como Prometheus/Grafana, y CI/CD con GitHub Actions.
