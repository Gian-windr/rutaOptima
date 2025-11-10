package com.customer.rutaOptima.api.controller;

import com.customer.rutaOptima.api.dto.OptimizeRouteRequest;
import com.customer.rutaOptima.api.dto.OptimizeRouteResponse;
import com.customer.rutaOptima.domain.RoutePlan;
import com.customer.rutaOptima.domain.RouteStop;
import com.customer.rutaOptima.config.exception.ResourceNotFoundException;
import com.customer.rutaOptima.persistence.RoutePlanRepository;
import com.customer.rutaOptima.service.RouteOptimizationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/route-plans")
@RequiredArgsConstructor
@Slf4j
public class RoutePlanController {

    private final RouteOptimizationService optimizationService;
    private final RoutePlanRepository routePlanRepository;

    /**
     * Optimiza rutas para una fecha específica.
     * 
     * POST /api/route-plans/optimize
     */
    @PostMapping("/optimize")
    public ResponseEntity<OptimizeRouteResponse> optimizeRoutes(
            @Valid @RequestBody OptimizeRouteRequest request) {
        
        log.info("POST /api/route-plans/optimize - Fecha: {}, Objetivo: {}", 
                request.getFecha(), request.getObjective());
        
        OptimizeRouteResponse response = optimizationService.optimizeRoutes(request);
        
        return ResponseEntity.ok(response);
    }

    /**
     * Re-optimiza un plan existente considerando eventos de tráfico.
     * 
     * POST /api/route-plans/{id}/reoptimize
     */
    @PostMapping("/{id}/reoptimize")
    public ResponseEntity<OptimizeRouteResponse> reoptimizeRoutes(@PathVariable Long id) {
        log.info("POST /api/route-plans/{}/reoptimize", id);
        
        OptimizeRouteResponse response = optimizationService.reoptimizeRoutes(id);
        
        return ResponseEntity.ok(response);
    }

    /**
     * Obtiene un plan de rutas con todas sus paradas.
     * 
     * GET /api/route-plans/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<RoutePlanDTO> getRoutePlanById(@PathVariable Long id) {
        log.info("GET /api/route-plans/{}", id);
        
        RoutePlan routePlan = routePlanRepository.findByIdWithStops(id)
                .orElseThrow(() -> new ResourceNotFoundException("Plan de rutas no encontrado: " + id));
        
        RoutePlanDTO dto = toDTO(routePlan);
        
        return ResponseEntity.ok(dto);
    }

    /**
     * Lista planes de rutas con filtros opcionales.
     * 
     * GET /api/route-plans?fecha=2025-06-15&estado=OPTIMIZED
     */
    @GetMapping
    public ResponseEntity<List<RoutePlanDTO>> listRoutePlans(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha,
            @RequestParam(required = false) RoutePlan.Estado estado) {
        
        log.info("GET /api/route-plans - Fecha: {}, Estado: {}", fecha, estado);
        
        List<RoutePlan> plans;
        
        if (fecha != null && estado != null) {
            plans = routePlanRepository.findByFechaAndEstado(fecha, estado);
        } else if (fecha != null) {
            plans = routePlanRepository.findByFecha(fecha);
        } else if (estado != null) {
            plans = routePlanRepository.findByEstado(estado);
        } else {
            plans = routePlanRepository.findAll();
        }
        
        List<RoutePlanDTO> dtos = plans.stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
        
        return ResponseEntity.ok(dtos);
    }

    /**
     * Elimina un plan de rutas (soft delete - cambia estado a CANCELLED).
     * 
     * DELETE /api/route-plans/{id}
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteRoutePlan(@PathVariable Long id) {
        log.info("DELETE /api/route-plans/{}", id);
        
        RoutePlan routePlan = routePlanRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Plan de rutas no encontrado: " + id));
        
        // En lugar de eliminar, cambiar estado
        routePlan.setEstado(RoutePlan.Estado.FAILED); // Reutilizamos FAILED como cancelado
        routePlanRepository.save(routePlan);
        
        return ResponseEntity.noContent().build();
    }

    /**
     * Convierte RoutePlan a DTO.
     */
    private RoutePlanDTO toDTO(RoutePlan plan) {
        RoutePlanDTO dto = new RoutePlanDTO();
        dto.setId(plan.getId());
        dto.setFecha(plan.getFecha());
        dto.setObjetivo(plan.getObjetivo());
        dto.setEstado(plan.getEstado());
        dto.setScore(plan.getScore());
        dto.setTotalKm(plan.getTotalKm());
        dto.setTotalTimeMin(plan.getTotalTimeMin());
        dto.setTotalCost(plan.getTotalCost());
        dto.setVehiculosUtilizados(plan.getVehiculosUtilizados());
        dto.setPedidosAsignados(plan.getPedidosAsignados());
        dto.setMaxOptimizationTimeSeconds(plan.getMaxOptimizationTimeSeconds());
        dto.setCreatedAt(plan.getCreatedAt());
        dto.setUpdatedAt(plan.getUpdatedAt());
        
        if (plan.getStops() != null) {
            List<RouteStopDTO> stops = plan.getStops().stream()
                    .map(this::toStopDTO)
                    .collect(Collectors.toList());
            dto.setStops(stops);
        }
        
        return dto;
    }

    /**
     * Convierte RouteStop a DTO.
     */
    private RouteStopDTO toStopDTO(RouteStop stop) {
        RouteStopDTO dto = new RouteStopDTO();
        dto.setId(stop.getId());
        dto.setOrderId(stop.getOrder().getId());
        dto.setVehicleId(stop.getVehicle().getId());
        dto.setVehiclePatente(stop.getVehicle().getPatente());
        dto.setCustomerName(stop.getOrder().getCustomer().getNombre());
        dto.setCustomerAddress(stop.getOrder().getCustomer().getDireccion());
        dto.setLatitud(stop.getOrder().getCustomer().getLatitud().doubleValue());
        dto.setLongitud(stop.getOrder().getCustomer().getLongitud().doubleValue());
        dto.setSecuencia(stop.getSecuencia());
        dto.setEta(stop.getEta());
        dto.setEtd(stop.getEtd());
        dto.setCargaAcumuladaCantidad(stop.getCargaAcumuladaCantidad().doubleValue());
        dto.setCargaAcumuladaVolumen(stop.getCargaAcumuladaVolumen().doubleValue());
        dto.setCargaAcumuladaPeso(stop.getCargaAcumuladaPeso().doubleValue());
        dto.setCantidad(stop.getOrder().getCantidad().doubleValue());
        dto.setVolumen(stop.getOrder().getVolumen().doubleValue());
        dto.setPeso(stop.getOrder().getPeso().doubleValue());
        return dto;
    }

    // DTOs internos del controlador

    @lombok.Data
    public static class RoutePlanDTO {
        private Long id;
        private LocalDate fecha;
        private String objetivo;
        private RoutePlan.Estado estado;
        private String score;
        private Double totalKm;
        private Integer totalTimeMin;
        private Double totalCost;
        private Integer vehiculosUtilizados;
        private Integer pedidosAsignados;
        private Integer maxOptimizationTimeSeconds;
        private java.time.LocalDateTime createdAt;
        private java.time.LocalDateTime updatedAt;
        private List<RouteStopDTO> stops;
    }

    @lombok.Data
    public static class RouteStopDTO {
        private Long id;
        private Long orderId;
        private Long vehicleId;
        private String vehiclePatente;
        private String customerName;
        private String customerAddress;
        private Double latitud;
        private Double longitud;
        private Integer secuencia;
        private java.time.LocalDateTime eta;
        private java.time.LocalDateTime etd;
        private Double cargaAcumuladaCantidad;
        private Double cargaAcumuladaVolumen;
        private Double cargaAcumuladaPeso;
        private Double cantidad;
        private Double volumen;
        private Double peso;
    }
}
