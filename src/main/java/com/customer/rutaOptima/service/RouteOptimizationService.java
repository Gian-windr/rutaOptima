package com.customer.rutaOptima.service;

import com.customer.rutaOptima.api.dto.OptimizeRouteRequest;
import com.customer.rutaOptima.api.dto.OptimizeRouteResponse;
import com.customer.rutaOptima.domain.*;
import com.customer.rutaOptima.config.exception.BusinessException;
import com.customer.rutaOptima.config.exception.ResourceNotFoundException;
import com.customer.rutaOptima.optimization.domain.Location;
import com.customer.rutaOptima.optimization.domain.VehicleInfo;
import com.customer.rutaOptima.optimization.domain.VehicleRoutingSolution;
import com.customer.rutaOptima.optimization.domain.Visit;
import com.customer.rutaOptima.persistence.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.optaplanner.core.api.solver.SolverJob;
import org.optaplanner.core.api.solver.SolverManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class RouteOptimizationService {

    private final OrderRepository orderRepository;
    private final VehicleRepository vehicleRepository;
    private final RoutePlanRepository routePlanRepository;
    private final RouteStopRepository routeStopRepository;
    private final TrafficEventRepository trafficEventRepository;
    private final SolverManager<VehicleRoutingSolution, Long> solverManager;

    /**
     * Optimiza las rutas para una fecha específica usando OptaPlanner.
     */
    @Transactional
    public OptimizeRouteResponse optimizeRoutes(OptimizeRouteRequest request, Instant startInclusive,
            Instant endExclusive) {
        log.info("Iniciando optimización para fecha: {} ({} - {})",
                request.getFecha(), startInclusive, endExclusive);

        // 1. Validar y obtener datos (pedidos entre instants)
        List<Order> orders = getOrdersForOptimization(startInclusive, endExclusive);
        List<Vehicle> vehicles = getVehiclesForOptimization(request.getVehicleIds());

        if (orders.isEmpty()) {
            throw new BusinessException("No hay pedidos pendientes para la fecha especificada");
        }

        if (vehicles.isEmpty()) {
            throw new BusinessException("No hay vehículos disponibles para la optimización");
        }

        // 2. Crear el plan de rutas usando el instante de inicio del día
        RoutePlan routePlan = createRoutePlan(request, startInclusive, orders, vehicles);

        // 3. Construir el problema de optimización
        VehicleRoutingSolution problem = buildOptimizationProblem(orders, vehicles, routePlan.getId());

        log.info("Problema construido: {} visitas, {} vehículos",
                problem.getVisits().size(), problem.getVehicles().size());

        // 4. Resolver con OptaPlanner
        routePlan.setEstado(RoutePlan.Estado.OPTIMIZING);
        routePlanRepository.save(routePlan);

        try {
            SolverJob<VehicleRoutingSolution, Long> solverJob = solverManager.solve(
                    routePlan.getId(),
                    problem);

            // Esperar a que termine la optimización (síncrono)
            VehicleRoutingSolution solution = solverJob.getFinalBestSolution();

            // Log detallado de la solución
            log.info("Optimización completada. Score: {}", solution.getScore());
            log.info("Total visitas en solución: {}", solution.getVisits().size());

            long assignedVisits = solution.getVisits().stream()
                    .filter(v -> v.getVehicle() != null)
                    .count();
            long unassignedVisits = solution.getVisits().stream()
                    .filter(v -> v.getVehicle() == null)
                    .count();

            log.info("Visitas asignadas: {}, No asignadas: {}", assignedVisits, unassignedVisits);

            if (assignedVisits == 0) {
                log.warn("ADVERTENCIA: OptaPlanner no asignó ninguna visita a vehículos!");
                log.warn("Esto puede indicar:");
                log.warn("1. Capacidades de vehículos insuficientes");
                log.warn("2. Restricciones muy estrictas (ventanas horarias)");
                log.warn("3. Configuración incorrecta de OptaPlanner");
                log.warn("Vehículos disponibles: {}", problem.getVehicles().size());
                problem.getVehicles()
                        .forEach(v -> log.warn("  - Vehículo ID={}, Cap.Cantidad={}, Cap.Volumen={}, Cap.Peso={}",
                                v.getId(), v.getCapacidadCantidad(), v.getCapacidadVolumen(), v.getCapacidadPeso()));
                log.warn("Demanda total:");
                double totalCantidad = problem.getVisits().stream()
                        .mapToDouble(v -> v.getLocation().getDemandaCantidad()).sum();
                double totalVolumen = problem.getVisits().stream()
                        .mapToDouble(v -> v.getLocation().getDemandaVolumen()).sum();
                double totalPeso = problem.getVisits().stream()
                        .mapToDouble(v -> v.getLocation().getDemandaPeso()).sum();
                log.warn("  - Total Cantidad: {}, Volumen: {}, Peso: {}", totalCantidad, totalVolumen, totalPeso);
            }

            // 5. Guardar resultados
            saveOptimizationResults(routePlan, solution);

            // 6. CRÍTICO: Recargar el plan con todos los stops guardados desde la BD
            routePlan = routePlanRepository.findByIdWithStops(routePlan.getId())
                    .orElseThrow(() -> new ResourceNotFoundException("RoutePlan no encontrado después de guardar"));

            log.info("Plan recargado desde BD con {} stops", routePlan.getStops().size());

            // 7. Calcular métricas desde los datos recargados de la BD
            routePlan.calculateMetrics();

            // 8. Guardar las métricas calculadas
            routePlanRepository.save(routePlan);

            log.info("Métricas calculadas - KM: {}, Tiempo: {} min, Costo: {}, Stops: {}",
                    routePlan.getKmsTotales(), routePlan.getTiempoEstimadoMin(),
                    routePlan.getCostoTotal(), routePlan.getStops().size());

            // 9. Construir respuesta desde los datos guardados en BD
            return buildResponseFromPlan(routePlan);

        } catch (Exception e) {
            log.error("Error durante la optimización", e);
            routePlan.setEstado(RoutePlan.Estado.FAILED);
            routePlanRepository.save(routePlan);
            throw new BusinessException("Error al optimizar rutas: " + e.getMessage());
        }
    }

    /**
     * Re-optimiza un plan existente considerando eventos de tráfico.
     */
    @Transactional
    public OptimizeRouteResponse reoptimizeRoutes(Long routePlanId) {
        log.info("Re-optimizando plan: {}", routePlanId);

        RoutePlan existingPlan = routePlanRepository.findByIdWithStops(routePlanId)
                .orElseThrow(() -> new ResourceNotFoundException("Plan de rutas no encontrado: " + routePlanId));

        if (existingPlan.getEstado() != RoutePlan.Estado.OPTIMIZED) {
            throw new BusinessException("Solo se pueden re-optimizar planes en estado OPTIMIZED");
        }

        // Obtener eventos de tráfico activos
        List<TrafficEvent> activeEvents = trafficEventRepository.findActiveEventsSince(
                existingPlan.getCreatedAt());

        if (activeEvents.isEmpty()) {
            log.info("No hay eventos de tráfico activos, se retorna el plan existente");
            return buildResponseFromPlan(existingPlan);
        }

        log.info("Eventos de tráfico activos: {}", activeEvents.size());

        // Reconstruir problema con los mismos datos
        List<Order> orders = existingPlan.getStops().stream()
                .map(RouteStop::getOrder)
                .distinct()
                .collect(Collectors.toList());

        List<Vehicle> vehicles = existingPlan.getStops().stream()
                .map(RouteStop::getVehicle)
                .distinct()
                .collect(Collectors.toList());

        // Crear nuevo plan de rutas
        RoutePlan newPlan = new RoutePlan();
        newPlan.setFecha(existingPlan.getFecha());
        newPlan.setObjetivo(existingPlan.getObjetivo());
        newPlan.setMaxOptimizationTimeSeconds(existingPlan.getMaxOptimizationTimeSeconds());
        newPlan.setEstado(RoutePlan.Estado.OPTIMIZING);
        newPlan = routePlanRepository.save(newPlan);

        // Construir problema considerando tráfico
        VehicleRoutingSolution problem = buildOptimizationProblem(orders, vehicles, newPlan.getId());
        applyTrafficEvents(problem, activeEvents);

        try {
            SolverJob<VehicleRoutingSolution, Long> solverJob = solverManager.solve(
                    newPlan.getId(),
                    problem);

            VehicleRoutingSolution solution = solverJob.getFinalBestSolution();

            saveOptimizationResults(newPlan, solution);

            log.info("Re-optimización completada. Score: {}", solution.getScore());

            return buildResponse(newPlan, solution);

        } catch (Exception e) {
            log.error("Error durante la re-optimización", e);
            newPlan.setEstado(RoutePlan.Estado.FAILED);
            routePlanRepository.save(newPlan);
            throw new BusinessException("Error al re-optimizar rutas: " + e.getMessage());
        }
    }

    /**
     * Obtiene los pedidos pendientes para optimización.
     */
    private List<Order> getOrdersForOptimization(Instant startInclusive, Instant endExclusive) {
        return orderRepository.findPendingOrdersWithCustomerBetween(startInclusive, endExclusive);
    }

    /**
     * Obtiene los vehículos disponibles para optimización.
     */
    private List<Vehicle> getVehiclesForOptimization(List<Long> vehicleIds) {
        if (vehicleIds == null || vehicleIds.isEmpty()) {
            return vehicleRepository.findByActivoTrue();
        }

        List<Vehicle> vehicles = vehicleRepository.findAllById(vehicleIds);

        // Validar que todos los vehículos existen y están activos
        if (vehicles.size() != vehicleIds.size()) {
            throw new BusinessException("Algunos vehículos no fueron encontrados");
        }

        List<Vehicle> inactiveVehicles = vehicles.stream()
                .filter(v -> !v.getActivo())
                .collect(Collectors.toList());

        if (!inactiveVehicles.isEmpty()) {
            throw new BusinessException("Algunos vehículos están inactivos: " +
                    inactiveVehicles.stream().map(Vehicle::getId).map(String::valueOf)
                            .collect(Collectors.joining(", ")));
        }

        return vehicles;
    }

    /**
     * Crea una entidad RoutePlan inicial.
     */
    private RoutePlan createRoutePlan(OptimizeRouteRequest request, Instant startInclusive, List<Order> orders,
            List<Vehicle> vehicles) {
        RoutePlan plan = new RoutePlan();
        String fechaStr = request.getFecha();
        if (fechaStr == null || fechaStr.isBlank()) {
            throw new BusinessException("Fecha inválida o vacía en request");
        }

        Instant fechaInstant;
        try {
            // Intentar formato ISO_INSTANT (ej. 2023-05-01T00:00:00Z)
            fechaInstant = Instant.parse(fechaStr);
        } catch (java.time.format.DateTimeParseException e1) {
            try {
                // Intentar formato LocalDate (ej. 2023-05-01) -> inicio del día en zona por
                // defecto
                java.time.LocalDate ld = java.time.LocalDate.parse(fechaStr);
                fechaInstant = ld.atStartOfDay(java.time.ZoneId.systemDefault()).toInstant();
            } catch (java.time.format.DateTimeParseException e2) {
                try {
                    // Intentar LocalDateTime (ej. 2023-05-01T15:30) -> zona por defecto
                    java.time.LocalDateTime ldt = java.time.LocalDateTime.parse(fechaStr);
                    fechaInstant = ldt.atZone(java.time.ZoneId.systemDefault()).toInstant();
                } catch (java.time.format.DateTimeParseException e3) {
                    throw new BusinessException(
                            "Formato de fecha inválido. Use ISO-8601 (ej. 2023-05-01T00:00:00Z) o yyyy-MM-dd");
                }
            }
        }

        plan.setFecha(fechaInstant);
        plan.setObjetivo(request.getObjective());
        plan.setKmsTotales(BigDecimal.ZERO);
        plan.setCostoTotal(BigDecimal.ZERO);
        plan.setPedidosAsignados(orders.toArray().length);
        plan.setVehiculosUtilizados(vehicles.toArray().length);
        plan.setMaxOptimizationTimeSeconds(request.getMaxOptimizationTimeSeconds());
        plan.setEstado(RoutePlan.Estado.CREATED);
        return routePlanRepository.save(plan);
    }

    /**
     * Construye el problema de optimización para OptaPlanner.
     */
    private VehicleRoutingSolution buildOptimizationProblem(
            List<Order> orders,
            List<Vehicle> vehicles,
            Long routePlanId) {

        List<VehicleInfo> vehicleInfos = vehicles.stream()
                .map(this::toVehicleInfo)
                .collect(Collectors.toList());

        List<Visit> visits = orders.stream()
                .map(order -> toVisit(order, routePlanId))
                .collect(Collectors.toList());

        if (!vehicleInfos.isEmpty()) {
            log.info("Pre-asignando {} visitas a {} vehículos usando estrategia round-robin",
                    visits.size(), vehicleInfos.size());

            int vehicleIndex = 0;
            for (Visit visit : visits) {
                VehicleInfo assignedVehicle = vehicleInfos.get(vehicleIndex);
                visit.setVehicle(assignedVehicle);

                visit.setAccumulatedCantidad(visit.getLocation().getDemandaCantidad());
                visit.setAccumulatedVolumen(visit.getLocation().getDemandaVolumen());
                visit.setAccumulatedPeso(visit.getLocation().getDemandaPeso());

                log.debug("Visita {} pre-asignada a vehículo {}", visit.getId(), assignedVehicle.getId());

                vehicleIndex = (vehicleIndex + 1) % vehicleInfos.size();
            }

            log.info("Pre-asignación completada. OptaPlanner ahora optimizará estas asignaciones.");
        } else {
            log.error("No hay vehículos disponibles para asignar visitas!");
        }

        // Crear solución
        VehicleRoutingSolution solution = new VehicleRoutingSolution();
        solution.setVehicles(vehicleInfos);
        solution.setVisits(visits);

        return solution;
    }

    /**
     * Convierte un Vehicle a VehicleInfo.
     */
    private VehicleInfo toVehicleInfo(Vehicle vehicle) {
        VehicleInfo info = new VehicleInfo();
        info.setId(vehicle.getId());
        info.setPatente(vehicle.getPatente());
        info.setCapacidadCantidad(vehicle.getCapacidadCantidad().doubleValue());
        info.setCapacidadVolumen(vehicle.getCapacidadVolumen().doubleValue());
        info.setCapacidadPeso(vehicle.getCapacidadPeso().doubleValue());
        info.setVelocidadKmh(vehicle.getVelocidadKmh().doubleValue());
        info.setCostoKm(vehicle.getCostoKm().doubleValue());
        info.setDepotLatitud(vehicle.getDepotLatitud().doubleValue());
        info.setDepotLongitud(vehicle.getDepotLongitud().doubleValue());
        info.setJornadaInicio(vehicle.getJornadaInicio());
        info.setJornadaFin(vehicle.getJornadaFin());
        return info;
    }

    /**
     * Convierte un Order a Visit.
     */
    private Visit toVisit(Order order, Long routePlanId) {
        Visit visit = new Visit();
        visit.setId(order.getId());
        visit.setRoutePlanId(routePlanId);

        Location location = new Location();
        location.setOrderId(order.getId());
        location.setCustomerId(order.getCustomer().getId());
        location.setNombre(order.getCustomer().getNombre());
        location.setDireccion(order.getCustomer().getDireccion());
        location.setLatitud(order.getCustomer().getLatitud().doubleValue());
        location.setLongitud(order.getCustomer().getLongitud().doubleValue());
        location.setDemandaCantidad(order.getCantidad().doubleValue());
        location.setDemandaVolumen(order.getVolumen().doubleValue());
        location.setDemandaPeso(order.getPeso().doubleValue());
        location.setVentanaInicio(order.getVentanaHorariaInicioEfectiva());
        location.setVentanaFin(order.getVentanaHorariaFinEfectiva());
        location.setTiempoServicioMin(order.getTiempoServicioEstimadoMin());
        location.setPrioridad(order.getPrioridad());

        visit.setLocation(location);

        return visit;
    }

    /**
     * Aplica eventos de tráfico al problema de optimización.
     */
    private void applyTrafficEvents(VehicleRoutingSolution problem, List<TrafficEvent> events) {
        for (Visit visit : problem.getVisits()) {
            for (TrafficEvent event : events) {
                if (event.afectaUbicacion(
                        BigDecimal.valueOf(visit.getLocation().getLatitud()),
                        BigDecimal.valueOf(visit.getLocation().getLongitud()))) {

                    // Aumentar tiempo de servicio por el factor de retraso
                    int tiempoOriginal = visit.getLocation().getTiempoServicioMin();
                    int tiempoAjustado = (int) Math.ceil(tiempoOriginal * event.getFactorRetraso().doubleValue());
                    visit.getLocation().setTiempoServicioMin(tiempoAjustado);

                    log.debug("Evento de tráfico aplicado a visita {}: tiempo {} -> {} min",
                            visit.getId(), tiempoOriginal, tiempoAjustado);
                }
            }
        }
    }

    /**
     * Guarda los resultados de la optimización en la base de datos.
     */
    private void saveOptimizationResults(RoutePlan routePlan, VehicleRoutingSolution solution) {
        // Eliminar paradas anteriores si existen
        routeStopRepository.deleteByRoutePlan(routePlan);

        // Agrupar visitas por vehículo y ordenar por secuencia
        Map<Long, List<Visit>> visitsByVehicle = solution.getVisits().stream()
                .filter(v -> v.getVehicle() != null)
                .collect(Collectors.groupingBy(v -> v.getVehicle().getId()));

        log.info("=== GUARDANDO RESULTADOS DE OPTIMIZACIÓN ===");
        log.info("Total visitas en solución: {}", solution.getVisits().size());
        log.info("Visitas con vehículo asignado: {}",
                solution.getVisits().stream().filter(v -> v.getVehicle() != null).count());
        log.info("Visitas sin vehículo: {}", solution.getVisits().stream().filter(v -> v.getVehicle() == null).count());
        log.info("Vehículos con visitas asignadas: {}", visitsByVehicle.size());

        // Procesar cada vehículo
        for (Map.Entry<Long, List<Visit>> entry : visitsByVehicle.entrySet()) {
            Long vehicleId = entry.getKey();
            List<Visit> vehicleVisits = entry.getValue().stream()
                    .sorted(Comparator.comparing(this::calculateSequence))
                    .collect(Collectors.toList());

            // Obtener información del vehículo
            VehicleInfo vehicleInfo = solution.getVehicles().stream()
                    .filter(v -> v.getId().equals(vehicleId))
                    .findFirst()
                    .orElse(null);

            log.info("Vehículo {} [{}]: {} visitas asignadas",
                    vehicleId,
                    vehicleInfo != null ? vehicleInfo.getPatente() : "?",
                    vehicleVisits.size());

            // Calcular distancias y tiempos para cada visita
            Location previousLocation = null;

            int stopsGuardadosParaVehiculo = 0;
            for (int i = 0; i < vehicleVisits.size(); i++) {
                Visit visit = vehicleVisits.get(i);
                RouteStop stop = createRouteStop(visit, routePlan);

                // Calcular distancia desde parada anterior o depot
                if (previousLocation == null && vehicleInfo != null) {
                    // Primera parada: calcular desde depot
                    double latDepot = vehicleInfo.getDepotLatitud();
                    double lonDepot = vehicleInfo.getDepotLongitud();
                    double distanceKm = calcularDistanciaHaversine(
                            latDepot, lonDepot,
                            visit.getLocation().getLatitud(), visit.getLocation().getLongitud());
                    stop.setDistanciaKmDesdeAnterior(BigDecimal.valueOf(distanceKm));

                    if (vehicleInfo.getVelocidadKmh() > 0) {
                        int travelTimeMin = (int) Math.ceil((distanceKm / vehicleInfo.getVelocidadKmh()) * 60);
                        stop.setTiempoViajeMínDesdeAnterior(travelTimeMin);
                    }
                } else if (previousLocation != null) {
                    // Paradas subsiguientes: calcular desde parada anterior
                    double distanceKm = previousLocation.calcularDistanciaKm(visit.getLocation());
                    stop.setDistanciaKmDesdeAnterior(BigDecimal.valueOf(distanceKm));

                    if (visit.getVehicle() != null && vehicleInfo != null && vehicleInfo.getVelocidadKmh() > 0) {
                        int travelTimeMin = (int) Math.ceil((distanceKm / vehicleInfo.getVelocidadKmh()) * 60);
                        stop.setTiempoViajeMínDesdeAnterior(travelTimeMin);
                    }
                }

                // Guardar el stop en la BD (ya tiene la relación con routePlan)
                Objects.requireNonNull(stop, "RouteStop no puede ser null");
                RouteStop savedStop = routeStopRepository.save(stop);

                // CRÍTICO: Agregar el stop a la colección del plan para evitar orphanRemoval
                routePlan.getStops().add(savedStop);

                stopsGuardadosParaVehiculo++;

                log.debug("  Stop #{} guardado: Order={}, Customer={}, Distancia={}km, Tiempo={}min",
                        savedStop.getSecuencia(),
                        savedStop.getOrder().getId(),
                        savedStop.getOrder().getCustomer().getNombre(),
                        savedStop.getDistanciaKmDesdeAnterior(),
                        savedStop.getTiempoViajeMínDesdeAnterior());

                previousLocation = visit.getLocation();
            }

            log.info("  ✓ {} stops guardados en BD para vehículo {}", stopsGuardadosParaVehiculo, vehicleId);
        }

        int totalStopsGuardados = visitsByVehicle.values().stream().mapToInt(List::size).sum();
        log.info("=== TOTAL: {} stops guardados en BD ===", totalStopsGuardados);

        // Actualizar otros campos del plan ANTES de calcular métricas
        routePlan.setScore(solution.getScore() != null ? solution.getScore().toString() : "N/A");
        routePlan.setEstado(RoutePlan.Estado.OPTIMIZED);

        // CRÍTICO: Guardar el plan con el estado
        // Los stops ya están guardados individualmente
        routePlanRepository.save(routePlan);

        log.info("RoutePlan guardado con estado {}", routePlan.getEstado());
    }

    /**
     * Fórmula de Haversine para calcular distancia entre dos puntos en la Tierra
     */
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

    /**
     * Crea una RouteStop desde una Visit.
     */
    private RouteStop createRouteStop(Visit visit, RoutePlan routePlan) {
        RouteStop stop = new RouteStop();
        stop.setRoutePlan(routePlan);

        Long visitId = visit.getId();
        Long vehicleId = visit.getVehicle().getId();

        Objects.requireNonNull(visitId, "Visit ID no puede ser null");
        Objects.requireNonNull(vehicleId, "Vehicle ID no puede ser null");

        // Buscar el pedido y vehículo reales
        Order order = orderRepository.findById(visitId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found: " + visit.getId()));
        Vehicle vehicle = vehicleRepository.findById(vehicleId)
                .orElseThrow(() -> new ResourceNotFoundException("Vehicle not found: " + visit.getVehicle().getId()));

        stop.setOrder(order);
        stop.setVehicle(vehicle);
        stop.setSecuencia(calculateSequence(visit));
        stop.setEta(visit.getArrivalTime());
        stop.setEtd(visit.getArrivalTime() != null
                ? visit.getArrivalTime().plus(Duration.ofMinutes(visit.getLocation().getTiempoServicioMin()))
                : null);
        stop.setCargaAcumuladaCantidad(
                BigDecimal.valueOf(visit.getAccumulatedCantidad() != null ? visit.getAccumulatedCantidad() : 0.0));
        stop.setCargaAcumuladaVolumen(
                BigDecimal.valueOf(visit.getAccumulatedVolumen() != null ? visit.getAccumulatedVolumen() : 0.0));
        stop.setCargaAcumuladaPeso(
                BigDecimal.valueOf(visit.getAccumulatedPeso() != null ? visit.getAccumulatedPeso() : 0.0));

        return stop;
    }

    /**
     * Calcula la secuencia de una visita en su ruta.
     */
    private Integer calculateSequence(Visit visit) {
        int sequence = 1;
        Visit current = visit;

        while (current.getPreviousVisit() != null) {
            sequence++;
            current = current.getPreviousVisit();
        }

        return sequence;
    }

    /**
     * Construye la respuesta DTO desde el plan y la solución.
     */
    private OptimizeRouteResponse buildResponse(RoutePlan routePlan, VehicleRoutingSolution solution) {
        OptimizeRouteResponse response = new OptimizeRouteResponse();
        response.setRoutePlanId(routePlan.getId());
        response.setStatus(routePlan.getEstado().name());
        response.setScore(solution.getScore() != null ? solution.getScore().toString() : "N/A");

        // Métricas
        buildMetrics(routePlan, response);

        // Lista plana de todas las paradas ordenadas por vehículo y secuencia
        List<OptimizeRouteResponse.StopDTO> allStops = solution.getVisits().stream()
                .filter(v -> v.getVehicle() != null)
                .sorted(Comparator.comparing((Visit v) -> v.getVehicle().getId())
                        .thenComparing(this::calculateSequence))
                .map(this::buildStopDTO)
                .collect(Collectors.toList());

        response.setStops(allStops);

        // Rutas por vehículo (opcional)
        Map<Long, List<Visit>> visitsByVehicle = solution.getVisits().stream()
                .filter(v -> v.getVehicle() != null)
                .collect(Collectors.groupingBy(v -> v.getVehicle().getId()));

        List<OptimizeRouteResponse.VehicleRouteDTO> vehicleRoutes = new ArrayList<>();
        for (Map.Entry<Long, List<Visit>> entry : visitsByVehicle.entrySet()) {
            OptimizeRouteResponse.VehicleRouteDTO route = buildVehicleRoute(entry.getKey(), entry.getValue());
            vehicleRoutes.add(route);
        }

        response.setVehicleRoutes(vehicleRoutes);

        return response;
    }

    /**
     * Construye la respuesta desde un plan guardado.
     */
    public OptimizeRouteResponse buildResponseFromPlan(RoutePlan routePlan) {
        OptimizeRouteResponse response = new OptimizeRouteResponse();
        response.setRoutePlanId(routePlan.getId());
        response.setStatus(routePlan.getEstado().name());
        response.setScore(routePlan.getScore());

        // Métricas
        buildMetrics(routePlan, response);

        // Lista plana de todas las paradas
        List<OptimizeRouteResponse.StopDTO> allStops = routePlan.getStops().stream()
                .sorted(Comparator.comparing((RouteStop s) -> s.getVehicle().getId())
                        .thenComparing(RouteStop::getSecuencia))
                .map(this::buildStopDTOFromRouteStop)
                .collect(Collectors.toList());

        response.setStops(allStops);

        // Agrupar stops por vehículo
        Map<Long, List<RouteStop>> stopsByVehicle = routePlan.getStops().stream()
                .collect(Collectors.groupingBy(s -> s.getVehicle().getId()));

        List<OptimizeRouteResponse.VehicleRouteDTO> vehicleRoutes = new ArrayList<>();
        for (Map.Entry<Long, List<RouteStop>> entry : stopsByVehicle.entrySet()) {
            OptimizeRouteResponse.VehicleRouteDTO route = buildVehicleRouteFromStops(
                    entry.getKey(), entry.getValue());
            vehicleRoutes.add(route);
        }

        response.setVehicleRoutes(vehicleRoutes);

        return response;
    }

    private void buildMetrics(RoutePlan routePlan, OptimizeRouteResponse response) {
        OptimizeRouteResponse.MetricsDTO metrics = new OptimizeRouteResponse.MetricsDTO();
        metrics.setTotalKm(routePlan.getKmsTotales() != null ? routePlan.getKmsTotales() : BigDecimal.ZERO);
        metrics.setTotalTimeMin(routePlan.getTiempoEstimadoMin() != null ? routePlan.getTiempoEstimadoMin() : 0);
        metrics.setTotalCost(routePlan.getCostoTotal() != null ? routePlan.getCostoTotal() : BigDecimal.ZERO);
        metrics.setVehiculosUtilizados(
                routePlan.getVehiculosUtilizados() != null ? routePlan.getVehiculosUtilizados() : 0);
        metrics.setPedidosAsignados(routePlan.getPedidosAsignados() != null ? routePlan.getPedidosAsignados() : 0);

        // Calcular pedidos no asignados si no está seteado
        Integer pedidosNoAsignados = routePlan.getPedidosNoAsignados();
        if (pedidosNoAsignados == null) {
            pedidosNoAsignados = 0;
        }
        metrics.setPedidosNoAsignados(pedidosNoAsignados);

        response.setMetrics(metrics);
    }

    /**
     * Construye la información de ruta de un vehículo.
     */
    private OptimizeRouteResponse.VehicleRouteDTO buildVehicleRoute(Long vehicleId, List<Visit> visits) {
        OptimizeRouteResponse.VehicleRouteDTO route = new OptimizeRouteResponse.VehicleRouteDTO();
        route.setVehicleId(vehicleId);

        // Ordenar visitas por secuencia
        List<Visit> sortedVisits = visits.stream()
                .sorted(Comparator.comparing(this::calculateSequence))
                .collect(Collectors.toList());

        List<OptimizeRouteResponse.StopDTO> stops = sortedVisits.stream()
                .map(this::buildStopDTO)
                .collect(Collectors.toList());

        route.setStops(stops);

        return route;
    }

    /**
     * Construye la información de ruta desde stops guardados.
     */
    private OptimizeRouteResponse.VehicleRouteDTO buildVehicleRouteFromStops(
            Long vehicleId, List<RouteStop> stops) {

        OptimizeRouteResponse.VehicleRouteDTO route = new OptimizeRouteResponse.VehicleRouteDTO();
        route.setVehicleId(vehicleId);

        List<OptimizeRouteResponse.StopDTO> stopDTOs = stops.stream()
                .sorted(Comparator.comparing(RouteStop::getSecuencia))
                .map(this::buildStopDTOFromRouteStop)
                .collect(Collectors.toList());

        route.setStops(stopDTOs);

        return route;
    }

    /**
     * Construye un StopDTO desde una Visit.
     */
    private OptimizeRouteResponse.StopDTO buildStopDTO(Visit visit) {
        OptimizeRouteResponse.StopDTO dto = new OptimizeRouteResponse.StopDTO();
        dto.setOrderId(visit.getId());
        dto.setCustomerId(visit.getLocation().getCustomerId());
        dto.setCustomerName(visit.getLocation().getNombre());
        dto.setDireccion(visit.getLocation().getDireccion());
        dto.setSequence(calculateSequence(visit));
        dto.setEta(visit.getArrivalTime() != null ? visit.getArrivalTime().toString() : null);
        dto.setEtd(visit.getArrivalTime() != null
                ? visit.getArrivalTime().plus(Duration.ofMinutes(visit.getLocation().getTiempoServicioMin())).toString()
                : null);
        dto.setLatitude(visit.getLocation().getLatitud());
        dto.setLongitude(visit.getLocation().getLongitud());
        dto.setCantidad(BigDecimal.valueOf(visit.getLocation().getDemandaCantidad()));
        dto.setVolumen(BigDecimal.valueOf(visit.getLocation().getDemandaVolumen()));
        dto.setPeso(BigDecimal.valueOf(visit.getLocation().getDemandaPeso()));
        dto.setCargaAcumuladaCantidad(
                BigDecimal.valueOf(visit.getAccumulatedCantidad() != null ? visit.getAccumulatedCantidad() : 0.0));
        dto.setCargaAcumuladaVolumen(
                BigDecimal.valueOf(visit.getAccumulatedVolumen() != null ? visit.getAccumulatedVolumen() : 0.0));
        dto.setCargaAcumuladaPeso(
                BigDecimal.valueOf(visit.getAccumulatedPeso() != null ? visit.getAccumulatedPeso() : 0.0));

        // Calcular distancia y tiempo desde la parada anterior
        Visit previousVisit = visit.getPreviousVisit();
        if (previousVisit != null && previousVisit.getLocation() != null) {
            double distanceKm = visit.getLocation().calcularDistanciaKm(previousVisit.getLocation());
            dto.setDistanceKmFromPrev(BigDecimal.valueOf(distanceKm));

            // Estimar tiempo de viaje (asumiendo velocidad del vehículo)
            if (visit.getVehicle() != null) {
                int travelTimeMin = (int) Math.ceil((distanceKm / visit.getVehicle().getVelocidadKmh()) * 60);
                dto.setTravelTimeMinFromPrev(travelTimeMin);
            } else {
                dto.setTravelTimeMinFromPrev(0);
            }
        } else {
            dto.setDistanceKmFromPrev(BigDecimal.ZERO);
            dto.setTravelTimeMinFromPrev(0);
        }

        // Tiempo de espera (si llega antes de la ventana horaria)
        dto.setWaitTimeMin(0); // Por ahora 0, se puede calcular basado en ventanas horarias

        if (visit.getVehicle() != null) {
            dto.setVehicleId(visit.getVehicle().getId());
            dto.setVehiclePatente(visit.getVehicle().getPatente());
        }

        return dto;
    }

    /**
     * Construye un StopDTO desde un RouteStop.
     */
    private OptimizeRouteResponse.StopDTO buildStopDTOFromRouteStop(RouteStop stop) {
        OptimizeRouteResponse.StopDTO dto = new OptimizeRouteResponse.StopDTO();
        dto.setOrderId(stop.getOrder().getId());
        dto.setCustomerId(stop.getOrder().getCustomer().getId());
        dto.setCustomerName(stop.getOrder().getCustomer().getNombre());
        dto.setDireccion(stop.getOrder().getCustomer().getDireccion());
        dto.setSequence(stop.getSecuencia());
        dto.setEta(stop.getEta() != null ? stop.getEta().toString() : null);
        dto.setEtd(stop.getEtd() != null ? stop.getEtd().toString() : null);
        dto.setLatitude(stop.getOrder().getCustomer().getLatitud().doubleValue());
        dto.setLongitude(stop.getOrder().getCustomer().getLongitud().doubleValue());
        dto.setDistanceKmFromPrev(stop.getDistanciaKmDesdeAnterior());
        dto.setTravelTimeMinFromPrev(stop.getTiempoViajeMínDesdeAnterior());
        dto.setWaitTimeMin(stop.getTiempoEsperaMin());
        dto.setCargaAcumuladaCantidad(stop.getCargaAcumuladaCantidad());
        dto.setCargaAcumuladaVolumen(stop.getCargaAcumuladaVolumen());
        dto.setCargaAcumuladaPeso(stop.getCargaAcumuladaPeso());
        dto.setCantidad(stop.getOrder().getCantidad());
        dto.setVolumen(stop.getOrder().getVolumen());
        dto.setPeso(stop.getOrder().getPeso());
        dto.setVehicleId(stop.getVehicle().getId());
        dto.setVehiclePatente(stop.getVehicle().getPatente());
        return dto;
    }
}
