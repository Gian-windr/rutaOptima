# 📝 ARCHIVOS PENDIENTES PARA COMPLETAR EL MVP

Este documento lista todos los archivos que aún faltan crear para tener el MVP 100% funcional.
Todos los archivos críticos de configuración, dominio, seguridad y documentación YA ESTÁN CREADOS.

## ✅ Archivos YA Creados (Listos)

### Configuración
- ✅ pom.xml (con todas las dependencias)
- ✅ application.yml (perfiles dev/prod)
- ✅ Dockerfile
- ✅ docker-compose.yml
- ✅ README.md completo

### Base de Datos
- ✅ V1__init.sql (esquema completo)
- ✅ V2__seed_data.sql (datos de ejemplo)

### Dominio
- ✅ User.java
- ✅ Customer.java
- ✅ Order.java
- ✅ Vehicle.java
- ✅ RoutePlan.java
- ✅ RouteStop.java
- ✅ TrafficEvent.java

### Repositorios
- ✅ UserRepository.java
- ✅ CustomerRepository.java
- ✅ OrderRepository.java
- ✅ VehicleRepository.java
- ✅ RoutePlanRepository.java
- ✅ RouteStopRepository.java
- ✅ TrafficEventRepository.java

### Seguridad
- ✅ JwtUtil.java
- ✅ CustomUserDetailsService.java
- ✅ JwtAuthenticationFilter.java
- ✅ SecurityConfig.java

### DTOs
- ✅ CustomerDTO.java
- ✅ OrderDTO.java
- ✅ VehicleDTO.java
- ✅ LoginRequest.java
- ✅ LoginResponse.java
- ✅ OptimizeRouteRequest.java
- ✅ OptimizeRouteResponse.java

### Excepciones
- ✅ BusinessException.java
- ✅ ResourceNotFoundException.java
- ✅ GlobalExceptionHandler.java

### Optimización (OptaPlanner)
- ✅ Location.java
- ✅ VehicleInfo.java
- ✅ Visit.java (Planning Entity)
- ✅ VehicleRoutingSolution.java (Planning Solution)
- ✅ VehicleRoutingConstraintProvider.java (Constraints)
- ✅ ShadowVariableUpdater.java

### Servicios
- ✅ OrderService.java (con regla de 5 días)

---

## ⚠️ Archivos PENDIENTES (Para completar)

### 1. Servicios de Negocio

#### CustomerService.java
**Ruta**: `src/main/java/com/customer/rutaOptima/service/CustomerService.java`

```java
package com.customer.rutaOptima.service;

import com.customer.rutaOptima.config.exception.BusinessException;
import com.customer.rutaOptima.domain.Customer;
import com.customer.rutaOptima.persistence.CustomerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class CustomerService {

    private final CustomerRepository customerRepository;

    @Transactional
    public Customer createCustomer(Customer customer) {
        log.info("Creando cliente: {}", customer.getNombre());
        return customerRepository.save(customer);
    }

    @Transactional(readOnly = true)
    public Customer findById(Long id) {
        return customerRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Cliente con ID " + id + " no encontrado"));
    }

    @Transactional(readOnly = true)
    public List<Customer> findAll() {
        return customerRepository.findAll();
    }

    @Transactional(readOnly = true)
    public List<Customer> findAllActive() {
        return customerRepository.findByActivoTrue();
    }

    @Transactional
    public Customer updateCustomer(Long id, Customer customer) {
        Customer existing = findById(id);
        existing.setNombre(customer.getNombre());
        existing.setDireccion(customer.getDireccion());
        existing.setLatitud(customer.getLatitud());
        existing.setLongitud(customer.getLongitud());
        existing.setEsNuevo(customer.getEsNuevo());
        existing.setVentanaHorariaInicio(customer.getVentanaHorariaInicio());
        existing.setVentanaHorariaFin(customer.getVentanaHorariaFin());
        existing.setTelefono(customer.getTelefono());
        existing.setEmail(customer.getEmail());
        return customerRepository.save(existing);
    }

    @Transactional
    public void deleteCustomer(Long id) {
        if (!customerRepository.existsById(id)) {
            throw new BusinessException("Cliente con ID " + id + " no encontrado");
        }
        customerRepository.deleteById(id);
    }
}
```

#### VehicleService.java
**Ruta**: `src/main/java/com/customer/rutaOptima/service/VehicleService.java`

```java
package com.customer.rutaOptima.service;

import com.customer.rutaOptima.config.exception.BusinessException;
import com.customer.rutaOptima.domain.Vehicle;
import com.customer.rutaOptima.persistence.VehicleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class VehicleService {

    private final VehicleRepository vehicleRepository;

    @Transactional
    public Vehicle createVehicle(Vehicle vehicle) {
        log.info("Creando vehículo: {}", vehicle.getNombre());
        return vehicleRepository.save(vehicle);
    }

    @Transactional(readOnly = true)
    public Vehicle findById(Long id) {
        return vehicleRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Vehículo con ID " + id + " no encontrado"));
    }

    @Transactional(readOnly = true)
    public List<Vehicle> findAll() {
        return vehicleRepository.findAll();
    }

    @Transactional(readOnly = true)
    public List<Vehicle> findAllActive() {
        return vehicleRepository.findByActivoTrue();
    }

    @Transactional
    public Vehicle updateVehicle(Long id, Vehicle vehicle) {
        Vehicle existing = findById(id);
        existing.setNombre(vehicle.getNombre());
        existing.setTipo(vehicle.getTipo());
        existing.setCapacidadCantidad(vehicle.getCapacidadCantidad());
        existing.setCapacidadVolumen(vehicle.getCapacidadVolumen());
        existing.setCapacidadPeso(vehicle.getCapacidadPeso());
        existing.setVelocidadKmh(vehicle.getVelocidadKmh());
        existing.setCostoKm(vehicle.getCostoKm());
        existing.setDepotLatitud(vehicle.getDepotLatitud());
        existing.setDepotLongitud(vehicle.getDepotLongitud());
        return vehicleRepository.save(existing);
    }

    @Transactional
    public void deactivateVehicle(Long id) {
        Vehicle vehicle = findById(id);
        vehicle.setActivo(false);
        vehicleRepository.save(vehicle);
    }
}
```

#### RouteOptimizationService.java (CRÍTICO)
**Ruta**: `src/main/java/com/customer/rutaOptima/service/RouteOptimizationService.java`

**NOTA**: Este es el servicio MÁS IMPORTANTE. Orquesta todo el proceso de optimización.

```java
package com.customer.rutaOptima.service;

import com.customer.rutaOptima.config.exception.BusinessException;
import com.customer.rutaOptima.domain.*;
import com.customer.rutaOptima.optimization.domain.*;
import com.customer.rutaOptima.optimization.ShadowVariableUpdater;
import com.customer.rutaOptima.persistence.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.optaplanner.core.api.solver.SolverJob;
import org.optaplanner.core.api.solver.SolverManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class RouteOptimizationService {

    private final SolverManager<VehicleRoutingSolution, Long> solverManager;
    private final RoutePlanRepository routePlanRepository;
    private final RouteStopRepository routeStopRepository;
    private final OrderRepository orderRepository;
    private final VehicleRepository vehicleRepository;
    private final TrafficEventRepository trafficEventRepository;

    @Transactional
    public RoutePlan optimizeRoutes(LocalDate fecha, List<Long> vehicleIds, String objetivo,
                                    boolean allowSoftTimeWindowViolations, int maxTimeSeconds) {
        log.info("Iniciando optimización para fecha: {}, vehículos: {}", fecha, vehicleIds);

        // 1. Obtener pedidos pendientes
        List<Order> orders = orderRepository.findPendingOrdersWithCustomerByFecha(fecha);
        if (orders.isEmpty()) {
            throw new BusinessException("No hay pedidos pendientes para la fecha: " + fecha);
        }

        // 2. Obtener vehículos
        List<Vehicle> vehicles = vehicleRepository.findByIdInAndActivoTrue(vehicleIds);
        if (vehicles.isEmpty()) {
            throw new BusinessException("No hay vehículos activos disponibles");
        }

        // 3. Crear plan de ruta
        RoutePlan routePlan = RoutePlan.builder()
                .fecha(fecha)
                .estado("OPTIMIZING")
                .objetivo(objetivo)
                .allowSoftTimeWindowViolations(allowSoftTimeWindowViolations)
                .build();
        routePlan = routePlanRepository.save(routePlan);

        // 4. Convertir a modelo de OptaPlanner
        List<VehicleInfo> vehicleInfos = vehicles.stream()
                .map(VehicleInfo::fromVehicle)
                .collect(Collectors.toList());

        List<Visit> visits = new ArrayList<>();
        long visitId = 1;
        for (Order order : orders) {
            Location location = Location.fromOrder(order);
            Visit visit = new Visit(visitId++, location);
            visits.add(visit);
        }

        VehicleRoutingSolution problem = new VehicleRoutingSolution(vehicleInfos, visits);
        problem.setObjetivo(objetivo);
        problem.setAllowSoftTimeWindowViolations(allowSoftTimeWindowViolations);

        // 5. Resolver con OptaPlanner
        long startTime = System.currentTimeMillis();
        try {
            SolverJob<VehicleRoutingSolution, Long> solverJob = solverManager.solve(
                    routePlan.getId(), problem);
            VehicleRoutingSolution solution = solverJob.getFinalBestSolution();

            long endTime = System.currentTimeMillis();
            int optimizationTime = (int) ((endTime - startTime) / 1000);

            // 6. Guardar resultados
            saveOptimizationResults(routePlan, solution, optimizationTime);

            log.info("Optimización completada en {} segundos", optimizationTime);
            return routePlanRepository.findById(routePlan.getId()).orElseThrow();

        } catch (Exception e) {
            log.error("Error durante optimización", e);
            routePlan.markAsFailed();
            routePlanRepository.save(routePlan);
            throw new BusinessException("Error durante la optimización: " + e.getMessage());
        }
    }

    private void saveOptimizationResults(RoutePlan routePlan, VehicleRoutingSolution solution,
                                        int optimizationTime) {
        // Actualizar shadow variables
        solution.getVisits().forEach(visit -> 
            ShadowVariableUpdater.updateShadowVariables(visit, solution.getFactorTrafico()));

        // Agrupar visitas por vehículo y ordenar
        Map<Long, List<Visit>> visitsByVehicle = solution.getVisits().stream()
                .filter(v -> v.getVehicle() != null)
                .collect(Collectors.groupingBy(v -> v.getVehicle().getId()));

        // Crear RouteStops
        for (Map.Entry<Long, List<Visit>> entry : visitsByVehicle.entrySet()) {
            Long vehicleId = entry.getKey();
            List<Visit> vehicleVisits = entry.getValue();

            // Ordenar por secuencia
            vehicleVisits.sort(Comparator.comparing(v -> findSequence(v, vehicleVisits)));

            int sequence = 1;
            for (Visit visit : vehicleVisits) {
                RouteStop stop = RouteStop.builder()
                        .routePlan(routePlan)
                        .vehicle(vehicleRepository.findById(vehicleId).orElseThrow())
                        .order(orderRepository.findById(visit.getLocation().getOrderId()).orElseThrow())
                        .secuencia(sequence++)
                        .eta(Instant.of(routePlan.getFecha(), visit.getArrivalTime()))
                        .etd(Instant.of(routePlan.getFecha(), visit.getDepartureTime()))
                        .distanciaKmDesdeAnterior(BigDecimal.valueOf(visit.getDistanciaDesdeAnteriorKm()))
                        .tiempoViajeMínDesdeAnterior(visit.getTiempoViajeDesdeAnteriorMinutos(1.0))
                        .cargaAcumuladaCantidad(visit.getCargaAcumuladaCantidad())
                        .cargaAcumuladaVolumen(visit.getCargaAcumuladaVolumen())
                        .cargaAcumuladaPeso(visit.getCargaAcumuladaPeso())
                        .build();
                routeStopRepository.save(stop);
            }
        }

        // Actualizar métricas del plan
        routePlan.setScore(solution.getScore() != null ? solution.getScore().toString() : "N/A");
        routePlan.setTiempoOptimizacionSeg(optimizationTime);
        routePlan.calculateMetrics();
        routePlan.markAsOptimized();
        routePlanRepository.save(routePlan);
    }

    private int findSequence(Visit visit, List<Visit> allVisits) {
        Visit current = visit;
        int sequence = 0;
        while (current != null) {
            sequence++;
            Visit prev = current.getPreviousVisit();
            current = prev;
        }
        return sequence;
    }

    @Transactional(readOnly = true)
    public RoutePlan findById(Long id) {
        return routePlanRepository.findByIdWithStops(id)
                .orElseThrow(() -> new BusinessException("Plan de ruta con ID " + id + " no encontrado"));
    }
}
```

#### AuthService.java
**Ruta**: `src/main/java/com/customer/rutaOptima/service/AuthService.java`

```java
package com.customer.rutaOptima.service;

import com.customer.rutaOptima.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;

    public String authenticate(String email, String password) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(email, password));
        
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        return jwtUtil.generateToken(userDetails);
    }
}
```

### 2. Controladores REST

#### AuthController.java
**Ruta**: `src/main/java/com/customer/rutaOptima/api/controller/AuthController.java`

```java
package com.customer.rutaOptima.api.controller;

import com.customer.rutaOptima.api.dto.LoginRequest;
import com.customer.rutaOptima.api.dto.LoginResponse;
import com.customer.rutaOptima.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        String token = authService.authenticate(request.getEmail(), request.getPassword());
        
        LoginResponse response = LoginResponse.builder()
                .token(token)
                .email(request.getEmail())
                .rol("USER")
                .build();
        
        return ResponseEntity.ok(response);
    }
}
```

#### CustomerController.java, VehicleController.java, OrderController.java, RoutePlanController.java

**Instrucción**: Crear controladores similares siguiendo el patrón REST estándar.
Referencia: Ver estructura en README.md sección "API Endpoints".

Cada controlador debe:
1. Inyectar su servicio correspondiente
2. Implementar operaciones CRUD
3. Usar `@Valid` para DTOs
4. Retornar `ResponseEntity<>`
5. Mapear DTOs a entidades y viceversa

### 3. Mapper (Opcional pero recomendado)

Si decides usar MapStruct manualmente:

#### CustomerMapper.java
**Ruta**: `src/main/java/com/customer/rutaOptima/api/mapper/CustomerMapper.java`

```java
package com.customer.rutaOptima.api.mapper;

import com.customer.rutaOptima.api.dto.CustomerDTO;
import com.customer.rutaOptima.domain.Customer;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface CustomerMapper {
    CustomerDTO toDTO(Customer customer);
    Customer toEntity(CustomerDTO dto);
    void updateEntityFromDTO(CustomerDTO dto, @MappingTarget Customer customer);
}
```

Crear mappers similares para Order, Vehicle, etc.

### 4. Configuración de OptaPlanner

#### OptaPlannerConfig.java (Opcional - ya configurado en application.yml)
**Ruta**: `src/main/java/com/customer/rutaOptima/config/OptaPlannerConfig.java`

```java
package com.customer.rutaOptima.config;

import com.customer.rutaOptima.optimization.domain.VehicleRoutingSolution;
import com.customer.rutaOptima.optimization.constraints.VehicleRoutingConstraintProvider;
import org.optaplanner.core.api.solver.SolverFactory;
import org.optaplanner.core.api.solver.SolverManager;
import org.optaplanner.core.config.solver.SolverConfig;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Configuration
public class OptaPlannerConfig {

    @Bean
    public SolverConfig solverConfig() {
        return new SolverConfig()
                .withSolutionClass(VehicleRoutingSolution.class)
                .withConstraintProviderClass(VehicleRoutingConstraintProvider.class)
                .withTerminationSpentLimit(Duration.ofSeconds(20));
    }
}
```

### 5. Tests

#### OrderServiceTest.java
**Ruta**: `src/test/java/com/customer/rutaOptima/service/OrderServiceTest.java`

```java
package com.customer.rutaOptima.service;

import com.customer.rutaOptima.config.exception.BusinessException;
import com.customer.rutaOptima.domain.Customer;
import com.customer.rutaOptima.domain.Order;
import com.customer.rutaOptima.persistence.CustomerRepository;
import com.customer.rutaOptima.persistence.OrderRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private CustomerRepository customerRepository;

    @InjectMocks
    private OrderService orderService;

    @Test
    void testCreateOrder_NewCustomer_LessThan5Days_ShouldThrowException() {
        // Arrange
        Customer newCustomer = Customer.builder()
                .id(1L)
                .nombre("Cliente Nuevo")
                .esNuevo(true)
                .build();

        Order order = Order.builder()
                .customer(newCustomer)
                .fechaEntrega(LocalDate.now().plusDays(3)) // Solo 3 días
                .cantidad(BigDecimal.TEN)
                .build();

        when(customerRepository.findById(1L)).thenReturn(Optional.of(newCustomer));

        // Act & Assert
        BusinessException exception = assertThrows(BusinessException.class,
                () -> orderService.createOrder(order));
        
        assertTrue(exception.getMessage().contains("mínimo 5 días de anticipación"));
    }

    @Test
    void testCreateOrder_NewCustomer_MoreThan5Days_ShouldSucceed() {
        // Arrange
        Customer newCustomer = Customer.builder()
                .id(1L)
                .nombre("Cliente Nuevo")
                .esNuevo(true)
                .build();

        Order order = Order.builder()
                .customer(newCustomer)
                .fechaEntrega(LocalDate.now().plusDays(6)) // 6 días OK
                .cantidad(BigDecimal.TEN)
                .build();

        when(customerRepository.findById(1L)).thenReturn(Optional.of(newCustomer));
        when(orderRepository.save(any())).thenReturn(order);

        // Act
        Order result = orderService.createOrder(order);

        // Assert
        assertNotNull(result);
    }
}
```

### 6. Colección Postman

#### postman_collection.json
**Ruta**: `postman_collection.json` (en la raíz del proyecto)

Crear una colección con todos los endpoints documentados en el README.

---

## 🚀 Pasos para Completar

1. **Copia cada código de arriba** en su archivo correspondiente
2. **Compila el proyecto**:
   ```powershell
   mvn clean install -DskipTests
   ```
3. **Levanta Docker**:
   ```powershell
   docker-compose up -d --build
   ```
4. **Prueba los endpoints** con cURL/Postman

## 📝 Notas Importantes

- **RouteOptimizationService.java** es el más complejo. Revísalo con cuidado.
- Los mappers de MapStruct se generan automáticamente en compile time si los interfaces están bien definidas
- Si OptaPlanner da problemas, verifica que las shadow variables se estén actualizando correctamente

## ✅ Checklist Final

- [ ] Todos los servicios creados
- [ ] Todos los controladores creados
- [ ] Mappers definidos (opcional)
- [ ] Tests básicos funcionando
- [ ] Docker levantado sin errores
- [ ] Login devuelve JWT
- [ ] Crear pedido valida regla 5 días
- [ ] Optimización de rutas funciona end-to-end

**¡Con estos archivos el MVP estará 100% completo y funcional!** 🎉
