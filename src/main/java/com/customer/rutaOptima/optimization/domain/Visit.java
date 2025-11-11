// Visit.java
package com.customer.rutaOptima.optimization.domain;

import com.customer.rutaOptima.optimization.domain.Location;
import com.customer.rutaOptima.optimization.domain.VehicleInfo;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.optaplanner.core.api.domain.entity.PlanningEntity;
import org.optaplanner.core.api.domain.lookup.PlanningId;
import org.optaplanner.core.api.domain.variable.PlanningVariable;

import java.time.Duration;
import java.time.Instant;

@PlanningEntity
@Data
@NoArgsConstructor
public class Visit {

    @PlanningId
    private Long id;

    private Long routePlanId;  // ID del plan de rutas al que pertenece
    private Location location;

    @PlanningVariable(valueRangeProviderRefs = "vehicleRange", nullable = true)
    private VehicleInfo vehicle;  // Vehicle assigned to this visit
    
    private Visit previousVisit;  // Previous visit in the route (calculated from sequence)
    private Visit nextVisit;  // Next visit in the route

    private Double accumulatedCantidad = 0.0;
    private Double accumulatedVolumen = 0.0;
    private Double accumulatedPeso = 0.0;

    private Instant arrivalTime;  // Arrival time at the location

    private int servicioMinutos = 10;  // Default service time in minutes (delivery)

    // Constructor
    public Visit(Long id, Location location) {
        this.id = id;
        this.location = location;
    }

    // Calculate distance from previous visit (or depot if first visit)
    public double getDistanciaDesdeAnteriorKm() {
        if (vehicle == null) {
            return 0.0;
        }

        if (previousVisit == null) {
            return vehicle.calcularDistanciaDesdeDepotKm(location);  // First visit, distance from depot
        } else {
            return previousVisit.getLocation().calcularDistanciaKm(location);  // Distance from previous visit
        }
    }

    // Calculate travel time from previous visit in minutes
    public int getTiempoViajeDesdeAnteriorMinutos() {
        if (vehicle == null) {
            return 0;
        }

        double distanciaKm = getDistanciaDesdeAnteriorKm();
        return vehicle.calcularTiempoViajeMinutos(distanciaKm);
    }

    // Calculate distance in meters
    public int getDistanciaDesdeAnteriorMetros() {
        return (int) (getDistanciaDesdeAnteriorKm() * 1000);
    }

    // Calculate travel time from previous visit in minutes, considering traffic factor
    public int getTiempoViajeDesdeAnteriorMinutos(double factorTrafico) {
        if (vehicle == null) {
            return 0;
        }

        double distanciaKm = getDistanciaDesdeAnteriorKm();
        return vehicle.calcularTiempoViajeMinutos(distanciaKm, factorTrafico);
    }

    // Check if the visit violates the time window
    public boolean violatesTimeWindow() {
        if (arrivalTime == null || !location.tieneVentanaHoraria()) {
            return false;
        }

        return arrivalTime.isBefore(location.getVentanaInicio()) || arrivalTime.isAfter(location.getVentanaFin());
    }

    // Calculate minutes of time window violation
    public int getTimeWindowViolationMinutes() {
        if (arrivalTime == null || !location.tieneVentanaHoraria()) {
            return 0;
        }

        if (arrivalTime.isBefore(location.getVentanaInicio())) {
            return (int) java.time.Duration.between(arrivalTime, location.getVentanaInicio()).toMinutes();
        } else if (arrivalTime.isAfter(location.getVentanaFin())) {
            return (int) java.time.Duration.between(location.getVentanaFin(), arrivalTime).toMinutes();
        }

        return 0;
    }

    // Get departure time (after service)
    public Instant getDepartureTime() {
        if (arrivalTime == null) {
            return null;
        }

        Instant effectiveArrival = arrivalTime;

        // If we arrive before the time window, wait
        if (location.tieneVentanaHoraria() && arrivalTime.isBefore(location.getVentanaInicio())) {
            effectiveArrival = location.getVentanaInicio();
        }

        return effectiveArrival.plus(Duration.ofMinutes(servicioMinutos));
    }
}
