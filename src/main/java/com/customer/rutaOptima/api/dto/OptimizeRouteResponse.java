package com.customer.rutaOptima.api.dto;

import java.math.BigDecimal;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO para la respuesta de optimización de rutas
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OptimizeRouteResponse {
    private Long routePlanId;
    private String status;
    private MetricsDTO metrics;
    private List<VehicleRouteDTO> vehicleRoutes;
    private String score;
    private Integer tiempoOptimizacionSeg;
    private List<StopDTO> stops;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class MetricsDTO {
        private BigDecimal totalKm;
        private Integer totalTimeMin;  // Tiempo total: travel + service + wait + return
        private Integer totalTravelTimeMin;  // Solo tiempo de viaje
        private Integer totalServiceTimeMin;  // Solo tiempo de servicio en clientes
        private Integer totalWaitTimeMin;  // Solo tiempo de espera
        private BigDecimal totalCost;
        private Integer vehiculosUtilizados;
        private Integer pedidosAsignados;
        private Integer pedidosNoAsignados;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class VehicleRouteDTO {
        private Long vehicleId;
        private String vehicleName;
        private String conductor;
        private String zona;
        private String color;
        private List<StopDTO> stops;
        private RouteGeometry routeGeometry;  // Waypoints de OSRM para dibujar ruta en mapa
        private BigDecimal totalKm;
        private Integer totalTimeMin;  // Tiempo total incluyendo return to depot
        private Integer totalTravelTimeMin;  // Solo viaje
        private Integer totalServiceTimeMin;  // Solo servicio
        private Integer totalWaitTimeMin;  // Solo espera
        private BigDecimal returnToDepotKm;  // Distancia de regreso al depot
        private Integer returnToDepotTimeMin;  // Tiempo de regreso al depot
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class RouteGeometry {
        private String type;  // "LineString"
        private List<List<Double>> coordinates;  // [[lng, lat], [lng, lat], ...]
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class StopDTO {
        private Long orderId;
        private Long customerId;
        private String customerName;
        private String direccion;
        private Integer sequence;
        private String eta;
        private String etd;
        private Double latitude;
        private Double longitude;
        private BigDecimal distanceKmFromPrev;
        private Integer travelTimeMinFromPrev;
        private Integer serviceTimeMin;  // Tiempo de servicio/descarga en este stop
        private Integer waitTimeMin;
        private BigDecimal cargaAcumuladaCantidad;
        private BigDecimal cargaAcumuladaVolumen;
        private BigDecimal cargaAcumuladaPeso;
        private BigDecimal cantidad;
        private BigDecimal volumen;
        private BigDecimal peso;
        private Long vehicleId;
        private String vehiclePatente;
    }
}
