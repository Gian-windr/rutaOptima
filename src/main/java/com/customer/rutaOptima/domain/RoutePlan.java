package com.customer.rutaOptima.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Entidad que representa un plan de ruta optimizado
 */
@Entity
@Table(name = "route_plan", indexes = {
        @Index(name = "idx_route_plan_fecha", columnList = "fecha"),
        @Index(name = "idx_route_plan_estado", columnList = "estado"),
        @Index(name = "idx_route_plan_fecha_estado", columnList = "fecha, estado")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RoutePlan {

    /**
     * Estados posibles de un plan de ruta
     */
    public enum Estado {
        CREATED,      // Recién creado
        OPTIMIZING,   // En proceso de optimización
        OPTIMIZED,    // Optimización completada exitosamente
        FAILED        // Falló la optimización
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull(message = "La fecha es obligatoria")
    @Column(nullable = false)
    private Instant fecha;

    @NotNull(message = "El estado es obligatorio")
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    @Builder.Default
    private Estado estado = Estado.CREATED;

    @NotBlank(message = "El objetivo es obligatorio")
    @Column(nullable = false, length = 50)
    @Builder.Default
    private String objetivo = "MINIMIZE_DISTANCE";

    @Column(name = "allow_soft_time_window_violations", nullable = false)
    @Builder.Default
    private Boolean allowSoftTimeWindowViolations = false;

    @Column(name = "kms_totales", precision = 10, scale = 2)
    private BigDecimal kmsTotales;

    @Column(name = "tiempo_estimado_min")
    private Integer tiempoEstimadoMin;

    @Column(name = "costo_total", precision = 10, scale = 2)
    private BigDecimal costoTotal;

    @Column(name = "vehiculos_utilizados")
    @Builder.Default
    private Integer vehiculosUtilizados = 0;

    @Column(name = "pedidos_asignados")
    @Builder.Default
    private Integer pedidosAsignados = 0;

    @Column(name = "pedidos_no_asignados")
    @Builder.Default
    private Integer pedidosNoAsignados = 0;

    @Column(length = 255)
    private String score;

    @Column(name = "tiempo_optimizacion_seg")
    private Integer tiempoOptimizacionSeg;

    @Column(name = "max_optimization_time_seconds")
    private Integer maxOptimizationTimeSeconds;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @OneToMany(mappedBy = "routePlan", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<RouteStop> stops = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
        updatedAt = Instant.now();
        if (estado == null) {
            estado = Estado.CREATED;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }

    /**
     * Marca el plan como optimizado
     */
    public void markAsOptimized() {
        this.estado = Estado.OPTIMIZED;
    }

    /**
     * Marca el plan como fallido
     */
    public void markAsFailed() {
        this.estado = Estado.FAILED;
    }

    /**
     * Marca el plan como en proceso de optimización
     */
    public void markAsOptimizing() {
        this.estado = Estado.OPTIMIZING;
    }

    /**
     * Calcula las métricas del plan basándose en las paradas
     */
    public void calculateMetrics() {
        if (stops == null || stops.isEmpty()) {
            return;
        }

        BigDecimal totalKm = BigDecimal.ZERO;
        BigDecimal totalCosto = BigDecimal.ZERO;
        int totalTiempo = 0;
        int pedidosAsignados = 0;
        long vehiculosUnicos = stops.stream()
                .map(RouteStop::getVehicle)
                .distinct()
                .count();

        for (RouteStop stop : stops) {
            if (stop.getDistanciaKmDesdeAnterior() != null) {
                totalKm = totalKm.add(stop.getDistanciaKmDesdeAnterior());
            }
            if (stop.getTiempoViajeMínDesdeAnterior() != null) {
                totalTiempo += stop.getTiempoViajeMínDesdeAnterior();
            }
            if (stop.getTiempoEsperaMin() != null) {
                totalTiempo += stop.getTiempoEsperaMin();
            }
            pedidosAsignados++;

            // Calcular costo basado en km y vehículo
            if (stop.getDistanciaKmDesdeAnterior() != null && stop.getVehicle() != null) {
                BigDecimal costoVehiculo = stop.getVehicle().getCostoKm();
                totalCosto = totalCosto.add(stop.getDistanciaKmDesdeAnterior().multiply(costoVehiculo));
            }
        }

        this.kmsTotales = totalKm;
        this.tiempoEstimadoMin = totalTiempo;
        this.costoTotal = totalCosto;
        this.vehiculosUtilizados = (int) vehiculosUnicos;
        this.pedidosAsignados = pedidosAsignados;
    }

    // Métodos helper para compatibilidad con DTOs
    public Double getTotalKm() {
        return kmsTotales != null ? kmsTotales.doubleValue() : null;
    }

    public Integer getTotalTimeMin() {
        return tiempoEstimadoMin;
    }

    public Double getTotalCost() {
        return costoTotal != null ? costoTotal.doubleValue() : null;
    }

    public Integer getMaxOptimizationTimeSeconds() {
        return tiempoOptimizacionSeg;
    }
}
