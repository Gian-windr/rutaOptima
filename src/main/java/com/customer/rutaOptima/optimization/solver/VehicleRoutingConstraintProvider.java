package com.customer.rutaOptima.optimization.solver;

import java.math.BigDecimal;

import org.optaplanner.core.api.score.buildin.hardsoft.HardSoftScore;
import org.optaplanner.core.api.score.stream.Constraint;
import org.optaplanner.core.api.score.stream.ConstraintFactory;
import org.optaplanner.core.api.score.stream.ConstraintProvider;

import com.customer.rutaOptima.optimization.domain.Visit;

/**
 * Define las restricciones del problema de ruteo.
 * - Hard constraints: deben cumplirse (capacidad, zonas)
 * - Soft constraints: se minimizan (distancia, tiempo)
 */
public class VehicleRoutingConstraintProvider implements ConstraintProvider {

    @Override
    public Constraint[] defineConstraints(ConstraintFactory constraintFactory) {
        return new Constraint[] {
            // Hard constraints
            vehicleCapacityCantidad(constraintFactory),
            vehicleCapacityVolumen(constraintFactory),
            vehicleCapacityPeso(constraintFactory),
            vehicleZoneMatch(constraintFactory),
            
            // Soft constraints
            minimizeTotalDistance(constraintFactory),
            minimizeTotalTravelTime(constraintFactory)
        };
    }

    // Hard: La cantidad acumulada no debe exceder la capacidad del vehículo
    Constraint vehicleCapacityCantidad(ConstraintFactory constraintFactory) {
        return constraintFactory.forEach(Visit.class)
            .filter(visit -> visit.getVehicle() != null && visit.getAccumulatedCantidad() != null)
            .filter(visit -> visit.getAccumulatedCantidad().compareTo(
                visit.getVehicle().getCapacidadCantidad()) > 0)
            .penalize(HardSoftScore.ONE_HARD,
                visit -> {
                    BigDecimal excess = visit.getAccumulatedCantidad()
                        .subtract(visit.getVehicle().getCapacidadCantidad());
                    return excess.intValue();
                })
            .asConstraint("Capacidad cantidad del vehículo");
    }

    // Hard: El volumen acumulado no debe exceder la capacidad del vehículo
    Constraint vehicleCapacityVolumen(ConstraintFactory constraintFactory) {
        return constraintFactory.forEach(Visit.class)
            .filter(visit -> visit.getVehicle() != null && visit.getAccumulatedVolumen() != null)
            .filter(visit -> visit.getAccumulatedVolumen().compareTo(
                visit.getVehicle().getCapacidadVolumen()) > 0)
            .penalize(HardSoftScore.ONE_HARD,
                visit -> {
                    BigDecimal excess = visit.getAccumulatedVolumen()
                        .subtract(visit.getVehicle().getCapacidadVolumen());
                    return excess.intValue();
                })
            .asConstraint("Capacidad volumen del vehículo");
    }

    // Hard: El peso acumulado no debe exceder la capacidad del vehículo
    Constraint vehicleCapacityPeso(ConstraintFactory constraintFactory) {
        return constraintFactory.forEach(Visit.class)
            .filter(visit -> visit.getVehicle() != null && visit.getAccumulatedPeso() != null)
            .filter(visit -> visit.getAccumulatedPeso().compareTo(
                visit.getVehicle().getCapacidadPeso()) > 0)
            .penalize(HardSoftScore.ONE_HARD,
                visit -> {
                    BigDecimal excess = visit.getAccumulatedPeso()
                        .subtract(visit.getVehicle().getCapacidadPeso());
                    return excess.intValue();
                })
            .asConstraint("Capacidad peso del vehículo");
    }

    // Hard: Las visitas deben estar en la zona del vehículo
    Constraint vehicleZoneMatch(ConstraintFactory constraintFactory) {
        return constraintFactory.forEach(Visit.class)
            .filter(visit -> visit.getVehicle() != null && visit.getLocation() != null)
            .filter(visit -> {
                String vehicleZone = visit.getVehicle().getZona();
                String visitZone = visit.getLocation().getZona();
                return vehicleZone != null && visitZone != null && !vehicleZone.equalsIgnoreCase(visitZone);
            })
            .penalize(HardSoftScore.ONE_HARD, visit -> 100)
            .asConstraint("Vehículo debe estar en la zona correcta");
    }

    // Soft: Minimizar la distancia total (usando OSRM - ya calculado en shadow variables)
    Constraint minimizeTotalDistance(ConstraintFactory constraintFactory) {
        return constraintFactory.forEach(Visit.class)
            .filter(visit -> visit.getDistanceFromPreviousKm() != null)
            .penalize(HardSoftScore.ONE_SOFT,
                visit -> (int) (visit.getDistanceFromPreviousKm() * 1000)) // metros
            .asConstraint("Minimizar distancia total");
    }

    // Soft: Minimizar el tiempo total de viaje
    Constraint minimizeTotalTravelTime(ConstraintFactory constraintFactory) {
        return constraintFactory.forEach(Visit.class)
            .filter(visit -> visit.getTravelTimeFromPreviousMin() != null)
            .penalize(HardSoftScore.ONE_SOFT,
                Visit::getTravelTimeFromPreviousMin)
            .asConstraint("Minimizar tiempo de viaje");
    }
}
