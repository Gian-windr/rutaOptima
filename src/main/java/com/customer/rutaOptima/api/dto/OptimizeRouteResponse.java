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
        private Double latitude;
        private Double longitude;
        private BigDecimal distanceKmFromPrev;
        private Integer travelTimeMinFromPrev;
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
