package com.customer.rutaOptima.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

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
    private List<VehicleRouteDTO> routes;
    private List<VehicleRouteDTO> vehicleRoutes;
    private String score;
    private Integer tiempoOptimizacionSeg;

    public void setVehicleRoutes(List<VehicleRouteDTO> vehicleRoutes) {
        this.vehicleRoutes = vehicleRoutes;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class MetricsDTO {
        private BigDecimal totalKm;
        private Integer totalTimeMin;
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
        private List<StopDTO> stops;
        private BigDecimal totalKm;
        private Integer totalTimeMin;
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
        private BigDecimal distanceKmFromPrev;
        private Integer travelTimeMinFromPrev;
        private BigDecimal cargaAcumuladaCantidad;
        private Double latitude;  // Puedes cambiar a Double si prefieres usar Double
        private Double longitude; // Lo mismo aquí: Double si prefieres
    }
}
