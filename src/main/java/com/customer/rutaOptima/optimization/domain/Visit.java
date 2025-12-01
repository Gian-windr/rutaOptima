package com.customer.rutaOptima.optimization.domain;

import java.math.BigDecimal;
import java.time.Instant;

import org.optaplanner.core.api.domain.entity.PlanningEntity;
import org.optaplanner.core.api.domain.lookup.PlanningId;
import org.optaplanner.core.api.domain.variable.PlanningVariable;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Entidad de planificación: una visita a un cliente.
 * OptaPlanner optimiza asignando vehículo y secuencia.
 */
@PlanningEntity
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Visit {

    @PlanningId
    private Long id;

    private Long orderId;
    private Location location;
    private BigDecimal cantidad;
    private BigDecimal volumen;
    private BigDecimal peso;
    private Integer prioridad;

    // Variables de planificación que OptaPlanner cambia
    @PlanningVariable(valueRangeProviderRefs = "vehicleRange")
    private VehicleInfo vehicle;

    // Estas variables se calculan DESPUÉS del solving, no durante
    private Visit previousVisit;
    private Instant arrivalTime;
    private BigDecimal accumulatedCantidad;
    private BigDecimal accumulatedVolumen;
    private BigDecimal accumulatedPeso;
    private Double distanceFromPreviousKm;
    private Integer travelTimeFromPreviousMin;

    public Visit(Long id, Long orderId, Location location, BigDecimal cantidad, BigDecimal volumen, 
                 BigDecimal peso, Integer prioridad) {
        this.id = id;
        this.orderId = orderId;
        this.location = location;
        this.cantidad = cantidad;
        this.volumen = volumen;
        this.peso = peso;
        this.prioridad = prioridad;
    }

    /**
     * Verifica si esta visita es el anchor (primera visita de un vehículo).
     */
    public boolean isAnchor() {
        return previousVisit == null && vehicle != null;
    }

    /**
     * Obtiene la próxima visita en la cadena.
     */
    public Visit getNextVisit() {
        // Implementado mediante VariableListener en el solver
        return null;
    }
}
