    package com.customer.rutaOptima.api.controller;

import com.customer.rutaOptima.api.dto.OptimizeRouteRequest;
import com.customer.rutaOptima.api.dto.OptimizeRouteResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * Controlador MOCK para demostración - Devuelve resultados simulados
 * TODO: Usar RoutePlanController real cuando OptaPlanner esté configurado
 */
@RestController
@RequiredArgsConstructor
@Slf4j
public class MockOptimizationController {

    @PostMapping("/api/route-plans-demo/optimize")
    public ResponseEntity<OptimizeRouteResponse> optimizeRoutesMock(
            @RequestBody OptimizeRouteRequest request) {
        
        log.info("POST /api/route-plans-demo/optimize - Generando datos simulados para fecha: {}", 
                request.getFecha());
        log.info("Vehicle IDs recibidos: {}", request.getVehicleIds());

        // Obtener la fecha base del request (formato: "2025-11-12")
        String fechaBase = request.getFecha() != null ? request.getFecha() : "2025-11-29";

        OptimizeRouteResponse response = new OptimizeRouteResponse();
        response.setRoutePlanId(1L);
        response.setStatus("OPTIMIZED");
        response.setScore("-87hard/-4532soft");
        response.setTiempoOptimizacionSeg(20);

        // Métricas simuladas
        OptimizeRouteResponse.MetricsDTO metrics = new OptimizeRouteResponse.MetricsDTO();
        metrics.setTotalKm(java.math.BigDecimal.valueOf(287.5));
        metrics.setTotalTimeMin(542);
        metrics.setTotalCost(java.math.BigDecimal.valueOf(687.50));
        metrics.setVehiculosUtilizados(request.getVehicleIds().size());
        metrics.setPedidosAsignados(35);
        metrics.setPedidosNoAsignados(0);
        response.setMetrics(metrics);

        // Crear rutas simuladas - Aceptar cualquier ID de vehículo
        List<OptimizeRouteResponse.VehicleRouteDTO> routes = new ArrayList<>();

        // Generar rutas para cada vehículo solicitado
        for (Long vehicleId : request.getVehicleIds()) {
            routes.add(createRouteForVehicle(vehicleId, fechaBase));
        }

        response.setVehicleRoutes(routes);
        response.setStops(extractAllStops(routes));

        log.info("Respuesta generada: {} rutas, {} paradas", 
                routes.size(), response.getStops().size());

        return ResponseEntity.ok(response);
    }

    /**
     * Crea una ruta simulada para cualquier vehículo
     */
    private OptimizeRouteResponse.VehicleRouteDTO createRouteForVehicle(Long vehicleId, String fechaBase) {
        // Datos simulados basados en el ID del vehículo
        String[] nombres = {"Camión Norte", "Furgoneta Sur", "Furgoneta Este", "Moto Centro"};
        String[] conductores = {"Juan Pérez", "María García", "Carlos López", "Ana Torres"};
        String[] zonas = {"Norte", "Sur", "Este", "Centro"};
        String[] colores = {"#3B82F6", "#10B981", "#8B5CF6", "#F59E0B"};
        
        int index = (int) ((vehicleId - 1) % 4); // Ciclar entre 0-3
        
        OptimizeRouteResponse.VehicleRouteDTO route = new OptimizeRouteResponse.VehicleRouteDTO();
        route.setVehicleId(vehicleId);
        route.setVehicleName(nombres[index] + " " + vehicleId);
        route.setConductor(conductores[index]);
        route.setZona(zonas[index]);
        route.setColor(colores[index]);
        route.setTotalKm(java.math.BigDecimal.valueOf(50 + (vehicleId * 10)));
        route.setTotalTimeMin(120 + (int)(vehicleId * 15));

        // Crear paradas simuladas
        List<OptimizeRouteResponse.StopDTO> stops = new ArrayList<>();
        int numStops = 3 + (int)(vehicleId % 4);
        for (int i = 1; i <= numStops; i++) {
            stops.add(createStop(
                i, 
                vehicleId * 10 + i, 
                "Cliente " + (vehicleId * 10 + i), 
                -12.05 + (i * 0.01), 
                -77.03 + (i * 0.01), 
                fechaBase + "T" + String.format("%02d", 8 + i) + ":00:00", 
                fechaBase + "T" + String.format("%02d", 8 + i) + ":15:00", 
                15.0 + i
            ));
        }
        
        route.setStops(stops);
        return route;
    }

    @Deprecated
    private OptimizeRouteResponse.VehicleRouteDTO createRoute1(String fechaBase) {
        OptimizeRouteResponse.VehicleRouteDTO route = new OptimizeRouteResponse.VehicleRouteDTO();
        route.setVehicleId(1L);
        route.setVehicleName("Camión Norte");
        route.setConductor("Juan Pérez");
        route.setZona("Norte");
        route.setColor("#3B82F6"); // Azul
        route.setTotalKm(java.math.BigDecimal.valueOf(78.3));
        route.setTotalTimeMin(156);

        List<OptimizeRouteResponse.StopDTO> stops = new ArrayList<>();
        stops.add(createStop(1, 1L, "Bodega San Juan", -12.11965, -77.03411, fechaBase + "T08:15:00", fechaBase + "T08:25:00", 12.0));
        stops.add(createStop(2, 2L, "Minimarket El Sol", -12.04665, -77.03012, fechaBase + "T08:42:00", fechaBase + "T08:52:00", 18.5));
        stops.add(createStop(3, 4L, "Comercial Pérez", -12.07890, -77.06980, fechaBase + "T09:18:00", fechaBase + "T09:28:00", 25.0));
        stops.add(createStop(4, 5L, "Bodega Central", -12.05480, -77.11890, fechaBase + "T09:54:00", fechaBase + "T10:04:00", 30.0));
        stops.add(createStop(5, 14L, "Bodega Los Andes", -12.05240, -77.03110, fechaBase + "T10:28:00", fechaBase + "T10:38:00", 20.0));
        stops.add(createStop(6, 15L, "Comercial Lima", -12.08980, -77.00120, fechaBase + "T11:05:00", fechaBase + "T11:15:00", 28.0));

        route.setStops(stops);
        return route;
    }

    private OptimizeRouteResponse.VehicleRouteDTO createRoute2(String fechaBase) {
        OptimizeRouteResponse.VehicleRouteDTO route = new OptimizeRouteResponse.VehicleRouteDTO();
        route.setVehicleId(2L);
        route.setVehicleName("Furgoneta Sur");
        route.setConductor("María García");
        route.setZona("Sur");
        route.setColor("#10B981"); // Verde
        route.setTotalKm(java.math.BigDecimal.valueOf(92.1));
        route.setTotalTimeMin(178);

        List<OptimizeRouteResponse.StopDTO> stops = new ArrayList<>();
        stops.add(createStop(1, 6L, "Market Express", -11.98350, -77.06120, fechaBase + "T08:20:00", fechaBase + "T08:30:00", 15.0));
        stops.add(createStop(2, 7L, "Tienda Familia", -12.11420, -77.01980, fechaBase + "T09:05:00", fechaBase + "T09:15:00", 10.0));
        stops.add(createStop(3, 8L, "Bodega Don José", -12.12780, -77.02560, fechaBase + "T09:35:00", fechaBase + "T09:45:00", 14.0));
        stops.add(createStop(4, 9L, "Minimarket La Esquina", -12.12250, -77.02940, fechaBase + "T10:05:00", fechaBase + "T10:15:00", 16.5));
        stops.add(createStop(5, 10L, "Comercial Rojas", -12.14490, -77.01930, fechaBase + "T10:40:00", fechaBase + "T10:50:00", 22.0));
        stops.add(createStop(6, 11L, "Bodega Santa Rosa", -12.06350, -77.02870, fechaBase + "T11:25:00", fechaBase + "T11:35:00", 16.5));

        route.setStops(stops);
        return route;
    }

    private OptimizeRouteResponse.VehicleRouteDTO createRoute3(String fechaBase) {
        OptimizeRouteResponse.VehicleRouteDTO route = new OptimizeRouteResponse.VehicleRouteDTO();
        route.setVehicleId(3L);
        route.setVehicleName("Furgoneta Este");
        route.setConductor("Carlos López");
        route.setZona("Este");
        route.setColor("#8B5CF6"); // Morado
        route.setTotalKm(java.math.BigDecimal.valueOf(95.8));
        route.setTotalTimeMin(184);

        List<OptimizeRouteResponse.StopDTO> stops = new ArrayList<>();
        stops.add(createStop(1, 12L, "Tienda Del Pueblo", -11.99900, -77.05220, fechaBase + "T08:10:00", fechaBase + "T08:20:00", 19.0));
        stops.add(createStop(2, 13L, "Market Popular", -12.06780, -77.01720, fechaBase + "T08:55:00", fechaBase + "T09:05:00", 14.5));
        stops.add(createStop(3, 16L, "Minimarket Surco", -12.15560, -77.00670, fechaBase + "T09:45:00", fechaBase + "T09:55:00", 17.0));
        stops.add(createStop(4, 17L, "Bodega El Pino", -12.07740, -77.09210, fechaBase + "T10:32:00", fechaBase + "T10:42:00", 13.5));
        stops.add(createStop(5, 18L, "Tienda Norte", -11.93890, -77.04880, fechaBase + "T11:20:00", fechaBase + "T11:30:00", 11.0));
        stops.add(createStop(6, 19L, "Market Sur", -12.21890, -76.93340, fechaBase + "T12:25:00", fechaBase + "T12:35:00", 16.0));

        route.setStops(stops);
        return route;
    }

    private OptimizeRouteResponse.VehicleRouteDTO createRoute4(String fechaBase) {
        OptimizeRouteResponse.VehicleRouteDTO route = new OptimizeRouteResponse.VehicleRouteDTO();
        route.setVehicleId(4L);
        route.setVehicleName("Moto Centro");
        route.setConductor("Ana Torres");
        route.setZona("Centro");
        route.setColor("#F59E0B"); // Naranja
        route.setTotalKm(java.math.BigDecimal.valueOf(21.3));
        route.setTotalTimeMin(24);

        List<OptimizeRouteResponse.StopDTO> stops = new ArrayList<>();
        stops.add(createStop(1, 20L, "Comercial Este", -12.02980, -76.95790, fechaBase + "T08:05:00", fechaBase + "T08:15:00", 8.0));
        stops.add(createStop(2, 21L, "Bodega Nueva Victoria", -12.07230, -77.01680, fechaBase + "T08:35:00", fechaBase + "T08:45:00", 6.8));

        route.setStops(stops);
        return route;
    }

    private OptimizeRouteResponse.StopDTO createStop(int sequence, Long customerId, String nombre, 
                                                        double lat, double lng, String arrival, 
                                                        String departure, double carga) {
        OptimizeRouteResponse.StopDTO stop = new OptimizeRouteResponse.StopDTO();
        stop.setSequence(sequence);
        stop.setCustomerId(customerId);
        stop.setCustomerName(nombre);
        stop.setLatitude(lat);
        stop.setLongitude(lng);
        stop.setEta(arrival);
        stop.setEtd(departure);
        stop.setCantidad(java.math.BigDecimal.valueOf(carga));
        return stop;
    }

    private List<OptimizeRouteResponse.StopDTO> extractAllStops(List<OptimizeRouteResponse.VehicleRouteDTO> routes) {
        List<OptimizeRouteResponse.StopDTO> allStops = new ArrayList<>();
        for (OptimizeRouteResponse.VehicleRouteDTO route : routes) {
            allStops.addAll(route.getStops());
        }
        return allStops;
    }

    /**
     * Endpoint alternativo compatible con frontend que envía fechaBase en formato ISO
     */
    @PostMapping("/api/route-plans/mock/optimize")
    public ResponseEntity<OptimizeRouteResponse> optimizeRoutesMockAlt(
            @RequestBody Map<String, Object> request) {
        
        // Extraer fechaBase y convertir a fecha simple
        String fechaBase = (String) request.get("fechaBase");
        String fecha = fechaBase != null ? fechaBase.substring(0, 10) : "2025-11-29";
        
        log.info("POST /api/route-plans/mock/optimize - fechaBase: {}, fecha parseada: {}", fechaBase, fecha);
        
        // Convertir vehicleIds de Integer a Long
        List<Long> vehicleIds = ((List<?>) request.get("vehicleIds")).stream()
                .map(id -> id instanceof Integer ? ((Integer) id).longValue() : (Long) id)
                .collect(java.util.stream.Collectors.toList());
        
        // Crear request con formato esperado
        OptimizeRouteRequest normalizedRequest = new OptimizeRouteRequest();
        normalizedRequest.setFecha(fecha);
        normalizedRequest.setVehicleIds(vehicleIds);
        normalizedRequest.setObjective("MINIMIZE_DISTANCE");
        
        return optimizeRoutesMock(normalizedRequest);
    }
}
