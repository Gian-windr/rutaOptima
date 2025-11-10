# 🚀 GUÍA RÁPIDA DE INICIO - RutaOptima MVP

## ⚡ Inicio Rápido (5 minutos)

### 1. Verificar Requisitos
```powershell
# Verificar Java 21
java -version

# Verificar Docker
docker --version
docker-compose --version
```

### 2. Levantar el Sistema
```powershell
# Ubicarse en el directorio del proyecto
cd "c:\Users\LENOVO LOQ\Desktop\VI - 2025 - 20\Customer Development\PROYECTO\rutaOptima"

# Opción A: Con Docker (RECOMENDADO - Todo automático)
docker-compose up -d --build

# Opción B: Local (requiere PostgreSQL instalado)
mvn spring-boot:run
```

### 3. Verificar que todo funciona
```powershell
# Health check
curl http://localhost:8080/actuator/health

# Debería devolver: {"status":"UP"}
```

## 📝 Prueba Rápida del Sistema

### 1. Obtener Token JWT
```powershell
$response = Invoke-RestMethod -Uri http://localhost:8080/api/auth/login -Method POST -ContentType "application/json" -Body '{"email":"admin@rutaoptima.com","password":"admin123"}'
$token = $response.token
Write-Host "Token obtenido: $token"
```

### 2. Listar Vehículos Disponibles
```powershell
$headers = @{
    "Authorization" = "Bearer $token"
    "Content-Type" = "application/json"
}

Invoke-RestMethod -Uri http://localhost:8080/api/vehicles -Method GET -Headers $headers | ConvertTo-Json -Depth 5
```

### 3. Listar Pedidos Pendientes para el 15/06/2025
```powershell
Invoke-RestMethod -Uri "http://localhost:8080/api/orders?fecha=2025-06-15&estado=PENDIENTE" -Method GET -Headers $headers | ConvertTo-Json -Depth 5
```

### 4. 🎯 OPTIMIZAR RUTAS (El momento de la verdad!)
```powershell
$optimizeBody = @{
    fecha = "2025-06-15"
    vehicleIds = @(1, 2, 3, 4, 5)
    objective = "MINIMIZE_DISTANCE"
    allowSoftTimeWindowViolations = $false
    maxOptimizationTimeSeconds = 20
} | ConvertTo-Json

$result = Invoke-RestMethod -Uri http://localhost:8080/api/route-plans/optimize -Method POST -Headers $headers -Body $optimizeBody

# Mostrar resultado
Write-Host "`n✅ OPTIMIZACIÓN COMPLETADA!" -ForegroundColor Green
Write-Host "Plan ID: $($result.routePlanId)"
Write-Host "Estado: $($result.status)"
Write-Host "Kilómetros totales: $($result.metrics.totalKm) km"
Write-Host "Tiempo estimado: $($result.metrics.totalTimeMin) minutos"
Write-Host "Vehículos utilizados: $($result.metrics.vehiculosUtilizados)"
Write-Host "Pedidos asignados: $($result.metrics.pedidosAsignados)"
Write-Host "Score: $($result.score)"

$result | ConvertTo-Json -Depth 10
```

## ⚠️ Probar la Regla de 5 Días

### Crear pedido para cliente NUEVO con menos de 5 días (DEBE FALLAR)
```powershell
$fechaCercana = (Get-Date).AddDays(3).ToString("yyyy-MM-dd")

$pedidoInvalido = @{
    customerId = 21  # Cliente nuevo (ver V2__seed_data.sql)
    fechaEntrega = $fechaCercana
    cantidad = 10.0
    volumen = 1.5
    peso = 100.0
    prioridad = 1
} | ConvertTo-Json

try {
    Invoke-RestMethod -Uri http://localhost:8080/api/orders -Method POST -Headers $headers -Body $pedidoInvalido
} catch {
    Write-Host "`n❌ ERROR ESPERADO (Regla 5 días):" -ForegroundColor Yellow
    Write-Host $_.ErrorDetails.Message
}
```

### Crear pedido para cliente NUEVO con más de 5 días (DEBE FUNCIONAR)
```powershell
$fechaValida = (Get-Date).AddDays(6).ToString("yyyy-MM-dd")

$pedidoValido = @{
    customerId = 21
    fechaEntrega = $fechaValida
    cantidad = 10.0
    volumen = 1.5
    peso = 100.0
    prioridad = 1
} | ConvertTo-Json

$pedidoCreado = Invoke-RestMethod -Uri http://localhost:8080/api/orders -Method POST -Headers $headers -Body $pedidoValido

Write-Host "`n✅ Pedido creado exitosamente!" -ForegroundColor Green
Write-Host "ID: $($pedidoCreado.id)"
```

## 🔍 Inspeccionar Base de Datos

### Opción 1: Con pgAdmin (incluido en Docker Compose)
1. Abrir navegador: http://localhost:5050
2. Login:
   - Email: admin@rutaoptima.com
   - Password: admin
3. Conectar a servidor:
   - Host: postgres
   - Port: 5432
   - Database: rutaoptima_prod
   - Username: postgres
   - Password: postgres

### Opción 2: Con psql (línea de comandos)
```powershell
docker exec -it rutaoptima-db psql -U postgres -d rutaoptima_prod

-- Comandos útiles:
\dt                          -- Listar tablas
SELECT COUNT(*) FROM orders; -- Contar pedidos
SELECT * FROM customer WHERE es_nuevo = true LIMIT 5;
SELECT * FROM route_plan;
\q                           -- Salir
```

## 📊 Ver Logs

```powershell
# Ver logs de la aplicación
docker-compose logs -f app

# Ver logs de PostgreSQL
docker-compose logs -f postgres

# Ver logs de un servicio específico
docker-compose logs --tail=100 app
```

## 🛑 Detener el Sistema

```powershell
# Detener contenedores (mantiene datos)
docker-compose stop

# Detener y eliminar contenedores (mantiene datos en volúmenes)
docker-compose down

# Detener, eliminar contenedores Y ELIMINAR DATOS
docker-compose down -v
```

## 🔄 Reiniciar desde Cero

```powershell
# Detener todo y limpiar
docker-compose down -v

# Limpiar imágenes viejas
docker system prune -f

# Reconstruir y levantar
docker-compose up -d --build

# Esperar 30 segundos y verificar
Start-Sleep -Seconds 30
curl http://localhost:8080/actuator/health
```

## 🐛 Troubleshooting Rápido

### Error: "Port 5432 is already allocated"
```powershell
# Verificar qué está usando el puerto
netstat -ano | findstr :5432

# Detener PostgreSQL local si está corriendo
Stop-Service postgresql-x64-15
```

### Error: "Cannot connect to database"
```powershell
# Verificar que el contenedor de DB está corriendo
docker-compose ps

# Ver logs de la BD
docker-compose logs postgres

# Reiniciar solo la BD
docker-compose restart postgres
```

### Error: "OptaPlanner no encuentra solución"
```powershell
# Ver logs detallados
docker-compose logs -f app

# Aumentar tiempo de optimización en el JSON request:
# "maxOptimizationTimeSeconds": 60
```

### La aplicación no responde
```powershell
# Verificar estado
docker-compose ps

# Reiniciar aplicación
docker-compose restart app

# Ver logs en tiempo real
docker-compose logs -f app
```

## 📚 Próximos Pasos

1. **Explorar el README.md completo** - Documentación detallada
2. **Revisar ARCHIVOS_PENDIENTES.md** - Si quieres extender funcionalidades
3. **Crear más pedidos y optimizar** - Experimenta con diferentes escenarios
4. **Probar re-optimización** - Simula cambios de tráfico
5. **Integrar con un mapa** - Visualizar rutas (futuro)

## 💡 Tips Útiles

### Reiniciar datos de ejemplo
```powershell
docker exec -it rutaoptima-db psql -U postgres -d rutaoptima_prod -c "TRUNCATE TABLE route_stop, route_plan, orders, vehicle, customer, users CASCADE;"

# Luego reiniciar la aplicación para que Flyway recree los datos
docker-compose restart app
```

### Ver todas las rutas optimizadas
```powershell
Invoke-RestMethod -Uri "http://localhost:8080/api/route-plans" -Method GET -Headers $headers | ConvertTo-Json -Depth 5
```

### Crear cliente nuevo rápido
```powershell
$nuevoCliente = @{
    nombre = "Mi Nueva Bodega"
    direccion = "Av. Test 123"
    latitud = -12.05
    longitud = -77.04
    esNuevo = $true
    telefono = "999888777"
} | ConvertTo-Json

Invoke-RestMethod -Uri http://localhost:8080/api/customers -Method POST -Headers $headers -Body $nuevoCliente
```

## ✅ Checklist de Verificación

- [ ] Docker Compose levantado sin errores
- [ ] Health check devuelve status UP
- [ ] Login devuelve token JWT
- [ ] Puedo listar vehículos
- [ ] Puedo listar pedidos
- [ ] Optimización de rutas funciona
- [ ] Regla de 5 días rechaza pedidos inválidos
- [ ] Regla de 5 días acepta pedidos válidos
- [ ] pgAdmin accesible (opcional)

**¡Todo listo! 🎉 Ahora tienes un sistema de optimización de rutas totalmente funcional.**

---

**¿Problemas?** Revisa el README.md completo o los logs con `docker-compose logs -f app`
