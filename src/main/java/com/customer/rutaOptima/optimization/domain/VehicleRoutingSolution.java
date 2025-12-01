package com.customer.rutaOptima.optimization.domain;

import org.optaplanner.core.api.domain.solution.PlanningEntityCollectionProperty;
import org.optaplanner.core.api.domain.solution.PlanningScore;
import org.optaplanner.core.api.domain.solution.PlanningSolution;
import org.optaplanner.core.api.domain.solution.ProblemFactCollectionProperty;
import org.optaplanner.core.api.domain.valuerange.ValueRangeProvider;
import org.optaplanner.core.api.score.buildin.hardsoft.HardSoftScore;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Solución del problema de ruteo de vehículos.
 * OptaPlanner optimiza esta clase asignando vehículos y secuencias a las visitas.
 */
@PlanningSolution
@Data
@NoArgsConstructor
@AllArgsConstructor
public class VehicleRoutingSolution {

    @ProblemFactCollectionProperty
    @ValueRangeProvider(id = "vehicleRange")
    private List<VehicleInfo> vehicles;

    @PlanningEntityCollectionProperty
    @ValueRangeProvider(id = "visitRange")
    private List<Visit> visits;

    @PlanningScore
    private HardSoftScore score;

    /**
     * Calcula la distancia total de todas las rutas.
     */
    public double getTotalDistance() {
        if (visits == null) return 0.0;
        
        return visits.stream()
            .filter(v -> v.getDistanceFromPreviousKm() != null)
            .mapToDouble(Visit::getDistanceFromPreviousKm)
            .sum();
    }

    /**
     * Calcula el tiempo total de viaje.
     */
    public int getTotalTravelTime() {
        if (visits == null) return 0;
        
        return visits.stream()
            .filter(v -> v.getTravelTimeFromPreviousMin() != null)
            .mapToInt(Visit::getTravelTimeFromPreviousMin)
            .sum();
    }

    /**
     * Cuenta vehículos utilizados.
     */
    public long getVehiclesUsed() {
        if (visits == null) return 0;
        
        return visits.stream()
            .map(Visit::getVehicle)
            .filter(v -> v != null)
            .distinct()
            .count();
    }
}
