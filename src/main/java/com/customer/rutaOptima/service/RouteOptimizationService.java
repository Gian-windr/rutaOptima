package com.customer.rutaOptima.service;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

import org.optaplanner.core.api.solver.SolverJob;
import org.optaplanner.core.api.solver.SolverManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.customer.rutaOptima.api.dto.OptimizeRouteRequest;
import com.customer.rutaOptima.api.dto.OptimizeRouteResponse;
import com.customer.rutaOptima.config.exception.BusinessException;
import com.customer.rutaOptima.domain.Customer;
import com.customer.rutaOptima.domain.Order;
import com.customer.rutaOptima.domain.RoutePlan;
import com.customer.rutaOptima.domain.RouteStop;
import com.customer.rutaOptima.domain.Vehicle;
import com.customer.rutaOptima.optimization.domain.Location;
import com.customer.rutaOptima.optimization.domain.VehicleInfo;
import com.customer.rutaOptima.optimization.domain.VehicleRoutingSolution;
import com.customer.rutaOptima.optimization.domain.Visit;
import com.customer.rutaOptima.persistence.OrderRepository;
import com.customer.rutaOptima.persistence.RoutePlanRepository;
import com.customer.rutaOptima.persistence.RouteStopRepository;
import com.customer.rutaOptima.persistence.VehicleRepository;
import com.customer.rutaOptima.service.DistanceMatrixService.RouteInfo;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Servicio de optimización de rutas usando OptaPlanner + OSRM.
 * OptaPlanner optimiza asignación y secuencia, OSRM provee distancias reales.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class RouteOptimizationService {

    private final OrderRepository orderRepository;
    private final VehicleRepository vehicleRepository;
    private final RoutePlanRepository routePlanRepository;
    private final RouteStopRepository routeStopRepository;
    private final DistanceMatrixService distanceMatrixService;
    private final SolverManager<VehicleRoutingSolution, Long> solverManager;

    /**
     * Optimiza rutas usando OptaPlanner (metaheuristics) + OSRM (distancias reales)
     */
    @Transactional
    public OptimizeRouteResponse optimizeRoutesWithRealDistances(OptimizeRouteRequest request) {
        log.info("Optimizando rutas con OptaPlanner para fecha: {}", request.getFecha());

        // 1. Parsear fecha
        LocalDate fecha = LocalDate.parse(request.getFecha());
        ZoneId zone = ZoneId.systemDefault();
        Instant startOfDay = fecha.atStartOfDay(zone).toInstant();
        Instant endOfDay = fecha.plusDays(1).atStartOfDay(zone).toInstant();

        // 2. Obtener datos
        List<Order> orders = orderRepository.findByFechaEntregaBetweenAndEstado(
            startOfDay, endOfDay, "PENDIENTE"
        );

        if (orders.isEmpty()) {
            throw new BusinessException("No hay pedidos pendientes para la fecha especificada");
        }

        // Filtrar por orderIds específicos si se proporcionan
        if (request.getOrderIds() != null && !request.getOrderIds().isEmpty()) {
            Set<Long> requestedOrderIds = new HashSet<>(request.getOrderIds());
            orders = orders.stream()
                .filter(order -> requestedOrderIds.contains(order.getId()))
                .collect(Collectors.toList());
            
            if (orders.isEmpty()) {
                throw new BusinessException("Ninguna de las órdenes especificadas está pendiente para la fecha");
            }
            log.info("Filtrando {} órdenes específicas de {} disponibles", 
                orders.size(), requestedOrderIds.size());
        }

        List<Vehicle> vehicles = vehicleRepository.findAllById(request.getVehicleIds());

        if (vehicles.isEmpty()) {
            throw new BusinessException("No hay vehículos seleccionados");
        }

        log.info("Datos cargados: {} pedidos, {} vehículos", orders.size(), vehicles.size());

        // 3. Crear plan de rutas
        RoutePlan routePlan = new RoutePlan();
        routePlan.setFecha(startOfDay);
        routePlan.setObjetivo(request.getObjective());
        routePlan.setEstado(RoutePlan.Estado.OPTIMIZING);
        routePlan = routePlanRepository.save(routePlan);

        // 4. Construir problema para OptaPlanner
        VehicleRoutingSolution problem = buildOptaPlannerProblem(orders, vehicles);
        
        log.info("Problema construido: {} visitas, {} vehículos", problem.getVisits().size(), problem.getVehicles().size());

        // 5. Resolver con OptaPlanner (30 segundos máximo)
        VehicleRoutingSolution solution;
        try {
            SolverJob<VehicleRoutingSolution, Long> solverJob = solverManager.solve(
                routePlan.getId(), problem
            );
            solution = solverJob.getFinalBestSolution();
            log.info("OptaPlanner finalizado. Score: {}", solution.getScore());
        } catch (InterruptedException | ExecutionException e) {
            log.error("Error en OptaPlanner", e);
            throw new BusinessException("Error al optimizar rutas: " + e.getMessage());
        }

        // 6. Extraer rutas optimizadas y calcular distancias REALES con OSRM
        List<RouteStop> allStops = extractRouteStopsWithOSRM(routePlan, solution, orders, vehicles);

        // 7. Calcular métricas
        BigDecimal totalKm = allStops.stream()
            .map(RouteStop::getDistanciaKmDesdeAnterior)
            .filter(Objects::nonNull)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        int totalTimeMin = allStops.stream()
            .map(RouteStop::getTiempoViajeMínDesdeAnterior)
            .filter(Objects::nonNull)
            .mapToInt(Integer::intValue)
            .sum();

        Map<Long, List<RouteStop>> stopsByVehicle = allStops.stream()
            .collect(Collectors.groupingBy(stop -> stop.getVehicle().getId()));

        BigDecimal totalCost = BigDecimal.ZERO;
        for (Map.Entry<Long, List<RouteStop>> entry : stopsByVehicle.entrySet()) {
            Vehicle vehicle = vehicles.stream()
                .filter(v -> v.getId().equals(entry.getKey()))
                .findFirst()
                .orElse(null);
            
            if (vehicle != null) {
                BigDecimal routeKm = entry.getValue().stream()
                    .map(RouteStop::getDistanciaKmDesdeAnterior)
                    .filter(Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
                totalCost = totalCost.add(routeKm.multiply(vehicle.getCostoKm()));
            }
        }

        // 8. Guardar stops
        routeStopRepository.saveAll(allStops);

        // 9. Actualizar plan
        routePlan.setEstado(RoutePlan.Estado.OPTIMIZED);
        routePlan.setKmsTotales(totalKm);
        routePlan.setTiempoEstimadoMin(totalTimeMin);
        routePlan.setCostoTotal(totalCost);
        routePlan.setVehiculosUtilizados(stopsByVehicle.size());
        routePlan.setPedidosAsignados(orders.size());
        routePlan.setScore(solution.getScore().toString());
        routePlanRepository.save(routePlan);

        log.info("Optimización completada: {} km, {} min, ${}, score: {}", 
            totalKm, totalTimeMin, totalCost, solution.getScore());

        // 10. Construir respuesta
        return buildResponse(routePlan, allStops, vehicles);
    }

    /**
     * Construye el problema de optimización para OptaPlanner
     */
    private VehicleRoutingSolution buildOptaPlannerProblem(List<Order> orders, List<Vehicle> vehicles) {
        // Crear VehicleInfo para cada vehículo
        List<VehicleInfo> vehicleInfos = new ArrayList<>();
        for (Vehicle vehicle : vehicles) {
            Location depot = new Location(
                vehicle.getDepotLatitud(),
                vehicle.getDepotLongitud(),
                null, // customerId
                "Depot " + vehicle.getNombre(),
                vehicle.getZona(), // zona del vehículo
                0, // tiempoServicio
                BigDecimal.ZERO // demanda
            );

            VehicleInfo vehicleInfo = new VehicleInfo(
                vehicle.getId(),
                vehicle.getNombre(),
                vehicle.getCapacidadCantidad(),
                vehicle.getCapacidadVolumen(),
                vehicle.getCapacidadPeso(),
                depot,
                vehicle.getZona(),
                vehicle.getConductor(),
                vehicle.getColor()
            );

            vehicleInfos.add(vehicleInfo);
        }

        // Crear Visit para cada orden
        List<Visit> visits = new ArrayList<>();
        for (Order order : orders) {
            Customer customer = order.getCustomer();
            
            Location location = new Location(
                customer.getLatitud(),
                customer.getLongitud(),
                customer.getId(),
                customer.getNombre(),
                customer.getZona(), // zona del cliente
                Objects.requireNonNullElse(order.getTiempoServicioEstimadoMin(), 10),
                order.getCantidad()
            );

            Visit visit = new Visit(
                order.getId(),
                order.getId(),
                location,
                order.getCantidad(),
                order.getVolumen(),
                order.getPeso(),
                order.getPrioridad()
            );

            visits.add(visit);
        }

        // Crear solución inicial sin asignación
        VehicleRoutingSolution solution = new VehicleRoutingSolution();
        solution.setVehicles(vehicleInfos);
        solution.setVisits(visits);

        return solution;
    }

    /**
     * Extrae los stops optimizados y calcula distancias REALES con OSRM
     */
    private List<RouteStop> extractRouteStopsWithOSRM(
            RoutePlan routePlan,
            VehicleRoutingSolution solution,
            List<Order> orders,
            List<Vehicle> vehicles) {

        List<RouteStop> allStops = new ArrayList<>();

        // Agrupar visitas por vehículo
        Map<Long, List<Visit>> visitsByVehicle = solution.getVisits().stream()
            .filter(visit -> visit.getVehicle() != null)
            .collect(Collectors.groupingBy(visit -> visit.getVehicle().getVehicleId()));

        for (Map.Entry<Long, List<Visit>> entry : visitsByVehicle.entrySet()) {
            Long vehicleId = entry.getKey();
            List<Visit> vehicleVisits = entry.getValue();

            // Encontrar vehículo
            Vehicle vehicle = vehicles.stream()
                .filter(v -> v.getId().equals(vehicleId))
                .findFirst()
                .orElse(null);

            if (vehicle == null) continue;

            // Ordenar visitas por cadena (previousVisit)
            List<Visit> orderedVisits = orderVisitsByChain(vehicleVisits);

            // Crear stops con distancias OSRM reales
            DistanceMatrixService.Location currentLocation = new DistanceMatrixService.Location(
                vehicle.getDepotLatitud(), vehicle.getDepotLongitud()
            );
            Instant currentTime = routePlan.getFecha();

            for (int i = 0; i < orderedVisits.size(); i++) {
                Visit visit = orderedVisits.get(i);
                
                // Encontrar orden correspondiente
                Order order = orders.stream()
                    .filter(o -> o.getId().equals(visit.getOrderId()))
                    .findFirst()
                    .orElse(null);

                if (order == null) continue;

                Customer customer = order.getCustomer();
                DistanceMatrixService.Location customerLocation = new DistanceMatrixService.Location(
                    customer.getLatitud(), customer.getLongitud()
                );

                // Obtener distancia y tiempo REAL desde OSRM
                RouteInfo routeInfo = distanceMatrixService.getRouteInfo(currentLocation, customerLocation);

                // Calcular llegada
                currentTime = currentTime.plusSeconds(routeInfo.getDurationSeconds());

                RouteStop stop = new RouteStop();
                stop.setRoutePlan(routePlan);
                stop.setOrder(order);
                stop.setVehicle(vehicle);
                stop.setSecuencia(i + 1);
                stop.setEta(currentTime);
                stop.setDistanciaKmDesdeAnterior(BigDecimal.valueOf(routeInfo.getDistanceMeters() / 1000.0));
                stop.setTiempoViajeMínDesdeAnterior((int) (routeInfo.getDurationSeconds() / 60));

                // Tiempo de servicio
                int serviceTime = Objects.requireNonNullElse(order.getTiempoServicioEstimadoMin(), 10);
                currentTime = currentTime.plusSeconds(serviceTime * 60L);
                stop.setEtd(currentTime);

                allStops.add(stop);
                currentLocation = customerLocation;
            }
        }

        return allStops;
    }

    /**
     * Ordena visitas por proximidad geográfica (algoritmo nearest neighbor)
     * Ya que no usamos chained variables, ordenamos por distancia desde el depot.
     */
    private List<Visit> orderVisitsByChain(List<Visit> visits) {
        if (visits.isEmpty()) {
            return new ArrayList<>();
        }

        // SIMPLIFICADO: Ya que OptaPlanner solo asigna vehículos (no secuencia),
        // ordenamos las visitas por proximidad desde el depot del vehículo.
        
        // Obtener depot del vehículo (todas las visitas tienen el mismo vehículo aquí)
        VehicleInfo vehicle = visits.get(0).getVehicle();
        if (vehicle == null) {
            return visits; // Sin vehículo asignado, devolver como está
        }

        Location depot = vehicle.getDepot();
        List<Visit> ordered = new ArrayList<>();
        List<Visit> remaining = new ArrayList<>(visits);
        
        // Algoritmo Nearest Neighbor: desde depot, siempre elegir la visita más cercana
        Location currentLocation = depot;
        
        while (!remaining.isEmpty()) {
            // Encontrar la visita más cercana a la ubicación actual
            Visit nearest = null;
            double minDistance = Double.MAX_VALUE;
            
            for (Visit visit : remaining) {
                double distance = calculateHaversineDistance(
                    currentLocation.getLatitud(),
                    currentLocation.getLongitud(),
                    visit.getLocation().getLatitud(),
                    visit.getLocation().getLongitud()
                );
                
                if (distance < minDistance) {
                    minDistance = distance;
                    nearest = visit;
                }
            }
            
            if (nearest != null) {
                ordered.add(nearest);
                remaining.remove(nearest);
                currentLocation = nearest.getLocation();
            } else {
                break; // No debería ocurrir, pero por seguridad
            }
        }
        
        log.debug("Ordenadas {} visitas para vehículo {} usando nearest neighbor", 
            ordered.size(), vehicle.getVehicleName());
        
        return ordered;
    }
    
    /**
     * Calcula distancia Haversine entre dos puntos (en km)
     */
    private double calculateHaversineDistance(BigDecimal lat1, BigDecimal lon1, 
                                              BigDecimal lat2, BigDecimal lon2) {
        final int R = 6371; // Radio de la Tierra en km
        
        double latDistance = Math.toRadians(lat2.doubleValue() - lat1.doubleValue());
        double lonDistance = Math.toRadians(lon2.doubleValue() - lon1.doubleValue());
        
        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(lat1.doubleValue())) * Math.cos(Math.toRadians(lat2.doubleValue()))
                * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
        
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        
        return R * c;
    }

    private OptimizeRouteResponse buildResponse(RoutePlan routePlan, List<RouteStop> allStops, List<Vehicle> vehicles) {
        OptimizeRouteResponse response = new OptimizeRouteResponse();
        response.setRoutePlanId(routePlan.getId());
        response.setStatus(routePlan.getEstado().name());
        response.setScore(routePlan.getScore());

        // Agrupar stops por vehículo
        Map<Long, List<RouteStop>> stopsByVehicle = allStops.stream()
            .collect(Collectors.groupingBy(stop -> stop.getVehicle().getId()));

        List<OptimizeRouteResponse.VehicleRouteDTO> routes = new ArrayList<>();
        
        int totalTravelTime = 0;
        int totalServiceTime = 0;
        int totalWaitTime = 0;

        for (Vehicle vehicle : vehicles) {
            List<RouteStop> vehicleStops = stopsByVehicle.getOrDefault(vehicle.getId(), Collections.emptyList());
            if (vehicleStops.isEmpty()) continue;

            // Calcular métricas del vehículo
            BigDecimal routeKm = vehicleStops.stream()
                .map(RouteStop::getDistanciaKmDesdeAnterior)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
                
            int routeTravelTime = vehicleStops.stream()
                .map(RouteStop::getTiempoViajeMínDesdeAnterior)
                .filter(Objects::nonNull)
                .mapToInt(Integer::intValue)
                .sum();
                
            int routeServiceTime = vehicleStops.stream()
                .map(stop -> stop.getOrder().getTiempoServicioEstimadoMin())
                .filter(Objects::nonNull)
                .mapToInt(Integer::intValue)
                .sum();
                
            int routeWaitTime = 0; // Por ahora, no calculamos esperas

            // Calcular retorno al depot
            RouteStop lastStop = vehicleStops.get(vehicleStops.size() - 1);
            Customer lastCustomer = lastStop.getOrder().getCustomer();
            DistanceMatrixService.Location lastLocation = new DistanceMatrixService.Location(
                lastCustomer.getLatitud(), lastCustomer.getLongitud()
            );
            DistanceMatrixService.Location depotLocation = new DistanceMatrixService.Location(
                vehicle.getDepotLatitud(), vehicle.getDepotLongitud()
            );
            RouteInfo returnInfo = distanceMatrixService.getRouteInfo(lastLocation, depotLocation);
            
            BigDecimal returnKm = BigDecimal.valueOf(returnInfo.getDistanceMeters() / 1000.0);
            int returnTimeMin = returnInfo.getDurationSeconds() / 60;

            // Construir geometría completa de la ruta
            List<List<Double>> fullRouteCoordinates = new ArrayList<>();
            
            // Empezar desde depot
            fullRouteCoordinates.add(Arrays.asList(
                vehicle.getDepotLongitud().doubleValue(), 
                vehicle.getDepotLatitud().doubleValue()
            ));
            
            // Agregar geometrías de cada segmento
            DistanceMatrixService.Location currentLoc = depotLocation;
            for (RouteStop stop : vehicleStops) {
                Customer customer = stop.getOrder().getCustomer();
                DistanceMatrixService.Location nextLoc = new DistanceMatrixService.Location(
                    customer.getLatitud(), customer.getLongitud()
                );
                
                RouteInfo segmentInfo = distanceMatrixService.getRouteInfo(currentLoc, nextLoc);
                if (segmentInfo.getGeometry() != null) {
                    for (double[] coord : segmentInfo.getGeometry()) {
                        fullRouteCoordinates.add(Arrays.asList(coord[0], coord[1]));
                    }
                }
                currentLoc = nextLoc;
            }
            
            // Agregar geometría de regreso al depot
            if (returnInfo.getGeometry() != null) {
                for (double[] coord : returnInfo.getGeometry()) {
                    fullRouteCoordinates.add(Arrays.asList(coord[0], coord[1]));
                }
            }

            OptimizeRouteResponse.VehicleRouteDTO route = new OptimizeRouteResponse.VehicleRouteDTO();
            route.setVehicleId(vehicle.getId());
            route.setVehicleName(vehicle.getNombre());
            route.setConductor(vehicle.getConductor());
            route.setZona(vehicle.getZona());
            route.setColor(vehicle.getColor());
            route.setTotalKm(routeKm.add(returnKm));
            route.setTotalTravelTimeMin(routeTravelTime + returnTimeMin);
            route.setTotalServiceTimeMin(routeServiceTime);
            route.setTotalWaitTimeMin(routeWaitTime);
            route.setTotalTimeMin(routeTravelTime + routeServiceTime + routeWaitTime + returnTimeMin);
            route.setReturnToDepotKm(returnKm);
            route.setReturnToDepotTimeMin(returnTimeMin);

            // Geometría de la ruta
            OptimizeRouteResponse.RouteGeometry geometry = new OptimizeRouteResponse.RouteGeometry();
            geometry.setType("LineString");
            geometry.setCoordinates(fullRouteCoordinates);
            route.setRouteGeometry(geometry);

            // Convertir stops
            List<OptimizeRouteResponse.StopDTO> stopDTOs = vehicleStops.stream()
                .map(this::toStopDTO)
                .collect(Collectors.toList());
            route.setStops(stopDTOs);
            
            routes.add(route);
            
            totalTravelTime += routeTravelTime + returnTimeMin;
            totalServiceTime += routeServiceTime;
            totalWaitTime += routeWaitTime;
        }

        response.setVehicleRoutes(routes);

        // Métricas globales
        OptimizeRouteResponse.MetricsDTO metrics = new OptimizeRouteResponse.MetricsDTO();
        metrics.setTotalKm(routePlan.getKmsTotales());
        metrics.setTotalTravelTimeMin(totalTravelTime);
        metrics.setTotalServiceTimeMin(totalServiceTime);
        metrics.setTotalWaitTimeMin(totalWaitTime);
        metrics.setTotalTimeMin(totalTravelTime + totalServiceTime + totalWaitTime);
        metrics.setTotalCost(routePlan.getCostoTotal());
        metrics.setVehiculosUtilizados(routePlan.getVehiculosUtilizados());
        metrics.setPedidosAsignados(routePlan.getPedidosAsignados());
        metrics.setPedidosNoAsignados(0);
        response.setMetrics(metrics);

        return response;
    }

    private OptimizeRouteResponse.StopDTO toStopDTO(RouteStop stop) {
        Customer customer = stop.getOrder().getCustomer();

        OptimizeRouteResponse.StopDTO dto = new OptimizeRouteResponse.StopDTO();
        dto.setOrderId(stop.getOrder().getId());
        dto.setCustomerId(customer.getId());
        dto.setCustomerName(customer.getNombre());
        dto.setDireccion(customer.getDireccion());
        dto.setSequence(stop.getSecuencia());
        dto.setEta(stop.getEta().toString());
        dto.setEtd(stop.getEtd().toString());
        dto.setLatitude(customer.getLatitud().doubleValue());
        dto.setLongitude(customer.getLongitud().doubleValue());
        dto.setDistanceKmFromPrev(stop.getDistanciaKmDesdeAnterior());
        dto.setTravelTimeMinFromPrev(stop.getTiempoViajeMínDesdeAnterior());
        dto.setServiceTimeMin(Objects.requireNonNullElse(stop.getOrder().getTiempoServicioEstimadoMin(), 10));
        dto.setWaitTimeMin(0); // Por ahora no hay esperas
        dto.setCantidad(stop.getOrder().getCantidad());
        dto.setVolumen(stop.getOrder().getVolumen());
        dto.setPeso(stop.getOrder().getPeso());
        dto.setVehicleId(stop.getVehicle().getId());
        dto.setVehiclePatente(stop.getVehicle().getPatente());

        return dto;
    }
}
