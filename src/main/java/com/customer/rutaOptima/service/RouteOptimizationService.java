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
    public OptimizeRouteResponse optimizeRoutes(OptimizeRouteRequest request, Instant startInclusive, Instant endExclusive) {
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
        RoutePlan routePlan = createRoutePlan(request, startInclusive);

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
                    problem
            );

            // Esperar a que termine la optimización (síncrono)
            VehicleRoutingSolution solution = solverJob.getFinalBestSolution();

            // 5. Guardar resultados
            saveOptimizationResults(routePlan, solution);

            log.info("Optimización completada. Score: {}", solution.getScore());

            return buildResponse(routePlan, solution);

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
                existingPlan.getCreatedAt()
        );

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
                    problem
            );

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
                    inactiveVehicles.stream().map(Vehicle::getId).map(String::valueOf).collect(Collectors.joining(", ")));
        }

        return vehicles;
    }

    /**
     * Crea una entidad RoutePlan inicial.
     */
    private RoutePlan createRoutePlan(OptimizeRouteRequest request, Instant startInclusive) {
        RoutePlan plan = new RoutePlan();
        plan.setFecha(startInclusive);
        plan.setObjetivo(request.getObjective());
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

        // Convertir vehículos
        List<VehicleInfo> vehicleInfos = vehicles.stream()
                .map(this::toVehicleInfo)
                .collect(Collectors.toList());

        // Convertir pedidos a visitas
        List<Visit> visits = orders.stream()
                .map(order -> toVisit(order, routePlanId))
                .collect(Collectors.toList());

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

        // Crear paradas para cada visita asignada
        for (Visit visit : solution.getVisits()) {
            if (visit.getVehicle() != null) {
                RouteStop stop = createRouteStop(visit, routePlan);
                routeStopRepository.save(stop);
            }
        }

        // Actualizar métricas del plan
        routePlan.calculateMetrics();
        routePlan.setScore(solution.getScore() != null ? solution.getScore().toString() : "N/A");
        routePlan.setEstado(RoutePlan.Estado.OPTIMIZED);
        routePlanRepository.save(routePlan);
    }

    /**
     * Crea una RouteStop desde una Visit.
     */
    private RouteStop createRouteStop(Visit visit, RoutePlan routePlan) {
        RouteStop stop = new RouteStop();
        stop.setRoutePlan(routePlan);
        
        // Buscar el pedido y vehículo reales
        Order order = orderRepository.findById(visit.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Order not found: " + visit.getId()));
        Vehicle vehicle = vehicleRepository.findById(visit.getVehicle().getId())
                .orElseThrow(() -> new ResourceNotFoundException("Vehicle not found: " + visit.getVehicle().getId()));
        
        stop.setOrder(order);
        stop.setVehicle(vehicle);
        stop.setSecuencia(calculateSequence(visit));
        stop.setEta(visit.getArrivalTime());
        stop.setEtd(visit.getArrivalTime() != null ?
                visit.getArrivalTime().plus(Duration.ofMinutes(visit.getLocation().getTiempoServicioMin())) : null);
        stop.setCargaAcumuladaCantidad(BigDecimal.valueOf(visit.getAccumulatedCantidad() != null ? visit.getAccumulatedCantidad() : 0.0));
        stop.setCargaAcumuladaVolumen(BigDecimal.valueOf(visit.getAccumulatedVolumen() != null ? visit.getAccumulatedVolumen() : 0.0));
        stop.setCargaAcumuladaPeso(BigDecimal.valueOf(visit.getAccumulatedPeso() != null ? visit.getAccumulatedPeso() : 0.0));
        
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
        OptimizeRouteResponse.MetricsDTO metrics = new OptimizeRouteResponse.MetricsDTO();
        metrics.setTotalKm(BigDecimal.valueOf(routePlan.getTotalKm()));  // Convierte Double a BigDecimal
        metrics.setTotalTimeMin(routePlan.getTotalTimeMin());
        metrics.setTotalCost(BigDecimal.valueOf(routePlan.getTotalCost()));  // Convierte Double a BigDecimal
        metrics.setVehiculosUtilizados(routePlan.getVehiculosUtilizados());
        metrics.setPedidosAsignados(routePlan.getPedidosAsignados());
        response.setMetrics(metrics);
        
        // Rutas por vehículo
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
    private OptimizeRouteResponse buildResponseFromPlan(RoutePlan routePlan) {
        OptimizeRouteResponse response = new OptimizeRouteResponse();
        response.setRoutePlanId(routePlan.getId());
        response.setStatus(routePlan.getEstado().name());
        response.setScore(routePlan.getScore());
        
        // Métricas
        OptimizeRouteResponse.MetricsDTO metrics = new OptimizeRouteResponse.MetricsDTO();
        metrics.setTotalKm(BigDecimal.valueOf(routePlan.getTotalKm()));  // Convierte Double a BigDecimal
        metrics.setTotalTimeMin(routePlan.getTotalTimeMin());
        metrics.setTotalCost(BigDecimal.valueOf(routePlan.getTotalCost()));
        metrics.setVehiculosUtilizados(routePlan.getVehiculosUtilizados());
        metrics.setPedidosAsignados(routePlan.getPedidosAsignados());
        response.setMetrics(metrics);
        
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
        dto.setCustomerName(visit.getLocation().getNombre());
        dto.setSequence(calculateSequence(visit));
        dto.setEta(String.valueOf(visit.getArrivalTime()));
        dto.setEtd(visit.getArrivalTime() != null ?
                String.valueOf(visit.getArrivalTime().plus(Duration.ofMinutes(visit.getLocation().getTiempoServicioMin()))) : null);
        dto.setLatitude(visit.getLocation().getLatitud());
        dto.setLongitude(visit.getLocation().getLongitud());
        return dto;
    }

    /**
     * Construye un StopDTO desde un RouteStop.
     */
    private OptimizeRouteResponse.StopDTO buildStopDTOFromRouteStop(RouteStop stop) {
        OptimizeRouteResponse.StopDTO dto = new OptimizeRouteResponse.StopDTO();
        dto.setOrderId(stop.getOrder().getId());
        dto.setCustomerName(stop.getOrder().getCustomer().getNombre());
        dto.setSequence(stop.getSecuencia());
        dto.setEta(String.valueOf(stop.getEta()));
        dto.setEtd(String.valueOf(stop.getEtd()));
        dto.setLatitude(stop.getOrder().getCustomer().getLatitud().doubleValue());
        dto.setLongitude(stop.getOrder().getCustomer().getLongitud().doubleValue());
        return dto;
    }
}
