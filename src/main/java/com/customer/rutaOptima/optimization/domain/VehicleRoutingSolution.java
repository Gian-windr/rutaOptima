package com.customer.rutaOptima.optimization.domain;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.optaplanner.core.api.domain.solution.PlanningEntityCollectionProperty;
import org.optaplanner.core.api.domain.solution.PlanningScore;
import org.optaplanner.core.api.domain.solution.PlanningSolution;
import org.optaplanner.core.api.domain.solution.ProblemFactCollectionProperty;
import org.optaplanner.core.api.domain.valuerange.ValueRangeProvider;
import org.optaplanner.core.api.score.buildin.hardsoft.HardSoftScore;

import java.util.ArrayList;
import java.util.List;

/**
 * Planning Solution: representa el problema completo de optimización de rutas (VRPTW)
 * Contiene todas las visitas a planificar y todos los vehículos disponibles
 */
@PlanningSolution
@Data
@NoArgsConstructor
public class VehicleRoutingSolution {

    /**
     * Lista de vehículos disponibles (problem facts - no cambian durante la optimización)
     */
    @ProblemFactCollectionProperty
    @ValueRangeProvider(id = "vehicleRange")
    private List<VehicleInfo> vehicles = new ArrayList<>();

    /**
     * Lista de visitas a planificar (planning entities - OptaPlanner las modificará)
     */
    @PlanningEntityCollectionProperty
    @ValueRangeProvider(id = "visitRange")
    private List<Visit> visits = new ArrayList<>();

    /**
     * Score de la solución: mide qué tan buena es
     * - Hard score: violaciones de restricciones duras (capacidad, ventanas horarias)
     * - Soft score: objetivo de optimización (minimizar distancia/costo)
     */
    @PlanningScore
    private HardSoftScore score;

    /**
     * Factor de tráfico global (1.0 = normal, >1.0 = tráfico lento)
     */
    private double factorTrafico = 1.0;

    /**
     * Objetivo de optimización
     */
    private String objetivo = "MINIMIZE_DISTANCE";

    /**
     * Permitir violaciones suaves de ventanas horarias
     */
    private boolean allowSoftTimeWindowViolations = false;

    /**
     * Constructor
     */
    public VehicleRoutingSolution(List<VehicleInfo> vehicles, List<Visit> visits) {
        this.vehicles = vehicles != null ? vehicles : new ArrayList<>();
        this.visits = visits != null ? visits : new ArrayList<>();
    }

    /**
     * Calcula el total de km de todas las rutas
     */
    public double getTotalKilometros() {
        return visits == null ? 0.0 : visits.stream()
                .filter(v -> v.getVehicle() != null)
                .mapToDouble(Visit::getDistanciaDesdeAnteriorKm)
                .sum();
    }

    /**
     * Calcula el total de minutos de todas las rutas
     */
    public int getTotalMinutos() {
        return visits == null ? 0 : visits.stream()
                .filter(v -> v.getVehicle() != null)
                .mapToInt(v -> v.getTiempoViajeDesdeAnteriorMinutos() + 
                    (v.getLocation().getTiempoServicioMin() != null ? v.getLocation().getTiempoServicioMin() : 0))
                .sum();
    }

    /**
     * Cuenta cuántas visitas no están asignadas
     */
    public long getVisitasNoAsignadas() {
        return visits == null ? 0 : visits.stream()
                .filter(v -> v.getVehicle() == null)
                .count();
    }

    /**
     * Cuenta cuántos vehículos están siendo utilizados
     */
    public long getVehiculosUtilizados() {
        return visits == null ? 0 : visits.stream()
                .map(Visit::getVehicle)
                .filter(v -> v != null)
                .distinct()
                .count();
    }
}
