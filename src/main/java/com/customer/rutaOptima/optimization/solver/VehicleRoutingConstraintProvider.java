package com.customer.rutaOptima.optimization.solver;

import com.customer.rutaOptima.optimization.domain.Visit;
import org.optaplanner.core.api.score.buildin.hardsoft.HardSoftScore;
import org.optaplanner.core.api.score.stream.Constraint;
import org.optaplanner.core.api.score.stream.ConstraintFactory;
import org.optaplanner.core.api.score.stream.ConstraintProvider;

import java.time.Duration;
import java.time.Instant;

/**
 * Define las restricciones del problema de optimización de rutas.
 * OptaPlanner usará estas reglas para evaluar soluciones y buscar la óptima.
 */
public class VehicleRoutingConstraintProvider implements ConstraintProvider {

        @Override
        public Constraint[] defineConstraints(ConstraintFactory constraintFactory) {
                return new Constraint[] {
                                // Hard constraints (no pueden violarse)
                                allVisitsMustBeAssigned(constraintFactory), // NUEVO: Todas las visitas deben asignarse
                                vehicleCapacityCantidad(constraintFactory),
                                vehicleCapacityVolumen(constraintFactory),
                                vehicleCapacityPeso(constraintFactory),
                                timeWindowStart(constraintFactory),
                                timeWindowEnd(constraintFactory),
                                workShiftEnd(constraintFactory),

                                // Soft constraints (minimizar/optimizar)
                                minimizeDistance(constraintFactory),
                                minimizeTravelTime(constraintFactory),
                                minimizeVehicles(constraintFactory),
                                prioritizeHighPriorityOrders(constraintFactory)
                };
        }

        // ========== HARD CONSTRAINTS ==========

        /**
         * CRÍTICO: Todas las visitas DEBEN ser asignadas a un vehículo.
         * Sin esta constraint, OptaPlanner puede dejar visitas sin asignar con score
         * 0hard/0soft.
         */
        protected Constraint allVisitsMustBeAssigned(ConstraintFactory constraintFactory) {
                return constraintFactory.forEach(Visit.class)
                                .filter(visit -> visit.getVehicle() == null)
                                .penalize(HardSoftScore.ofHard(1000000)) // Penalización MUY alta
                                .asConstraint("Todas las visitas deben ser asignadas");
        }

        /**
         * La carga acumulada de cantidad no puede exceder la capacidad del vehículo.
         */
        protected Constraint vehicleCapacityCantidad(ConstraintFactory constraintFactory) {
                return constraintFactory.forEach(Visit.class)
                                .filter(visit -> visit.getVehicle() != null)
                                .filter(visit -> visit.getAccumulatedCantidad() != null)
                                .filter(visit -> visit.getAccumulatedCantidad() > visit.getVehicle()
                                                .getCapacidadCantidad())
                                .penalize(HardSoftScore.ONE_HARD,
                                                visit -> (int) (visit.getAccumulatedCantidad()
                                                                - visit.getVehicle().getCapacidadCantidad()))
                                .asConstraint("Capacidad cantidad del vehículo excedida");
        }

        /**
         * La carga acumulada de volumen no puede exceder la capacidad del vehículo.
         */
        protected Constraint vehicleCapacityVolumen(ConstraintFactory constraintFactory) {
                return constraintFactory.forEach(Visit.class)
                                .filter(visit -> visit.getVehicle() != null)
                                .filter(visit -> visit.getAccumulatedVolumen() != null)
                                .filter(visit -> visit.getAccumulatedVolumen() > visit.getVehicle()
                                                .getCapacidadVolumen())
                                .penalize(HardSoftScore.ONE_HARD,
                                                visit -> (int) (visit.getAccumulatedVolumen()
                                                                - visit.getVehicle().getCapacidadVolumen()))
                                .asConstraint("Capacidad volumen del vehículo excedida");
        }

        /**
         * La carga acumulada de peso no puede exceder la capacidad del vehículo.
         */
        protected Constraint vehicleCapacityPeso(ConstraintFactory constraintFactory) {
                return constraintFactory.forEach(Visit.class)
                                .filter(visit -> visit.getVehicle() != null)
                                .filter(visit -> visit.getAccumulatedPeso() != null)
                                .filter(visit -> visit.getAccumulatedPeso() > visit.getVehicle().getCapacidadPeso())
                                .penalize(HardSoftScore.ONE_HARD,
                                                visit -> (int) (visit.getAccumulatedPeso()
                                                                - visit.getVehicle().getCapacidadPeso()))
                                .asConstraint("Capacidad peso del vehículo excedida");
        }

        /**
         * No se puede llegar antes de la ventana horaria del cliente.
         */
        protected Constraint timeWindowStart(ConstraintFactory constraintFactory) {
                return constraintFactory.forEach(Visit.class)
                                .filter(visit -> visit.getArrivalTime() != null)
                                .filter(visit -> visit.getLocation().getVentanaInicio() != null)
                                .filter(visit -> visit.getArrivalTime()
                                                .isBefore(visit.getLocation().getVentanaInicio()))
                                .penalize(HardSoftScore.ONE_HARD,
                                                visit -> (int) java.time.Duration.between(
                                                                visit.getArrivalTime(),
                                                                visit.getLocation().getVentanaInicio()).toMinutes())
                                .asConstraint("Llegada antes de ventana horaria");
        }

        /**
         * No se puede llegar después de la ventana horaria del cliente.
         */
        protected Constraint timeWindowEnd(ConstraintFactory constraintFactory) {
                return constraintFactory.forEach(Visit.class)
                                .filter(visit -> visit.getArrivalTime() != null)
                                .filter(visit -> visit.getLocation().getVentanaFin() != null)
                                .filter(visit -> visit.getArrivalTime().isAfter(visit.getLocation().getVentanaFin()))
                                .penalize(HardSoftScore.ONE_HARD,
                                                visit -> (int) java.time.Duration.between(
                                                                visit.getLocation().getVentanaFin(),
                                                                visit.getArrivalTime()).toMinutes())
                                .asConstraint("Llegada después de ventana horaria");
        }

        /**
         * El vehículo debe terminar su ruta antes del fin de jornada.
         */
        protected Constraint workShiftEnd(ConstraintFactory constraintFactory) {
                return constraintFactory.forEach(Visit.class)
                                .filter(visit -> visit.getVehicle() != null)
                                .filter(visit -> visit.getArrivalTime() != null)
                                .filter(visit -> {
                                        Instant departureTime = visit.getArrivalTime()
                                                        .plus(Duration.ofMinutes(
                                                                        visit.getLocation().getTiempoServicioMin()));
                                        return departureTime.isAfter(visit.getVehicle().getJornadaFin());
                                })
                                .penalize(HardSoftScore.ONE_HARD,
                                                visit -> {
                                                        Instant departureTime = visit.getArrivalTime()
                                                                        .plus(Duration.ofMinutes(visit.getLocation()
                                                                                        .getTiempoServicioMin()));
                                                        return (int) java.time.Duration.between(
                                                                        visit.getVehicle().getJornadaFin(),
                                                                        departureTime).toMinutes();
                                                })
                                .asConstraint("Excede jornada laboral del vehículo");
        }

        // ========== SOFT CONSTRAINTS ==========

        /**
         * Minimizar la distancia total recorrida.
         */
        protected Constraint minimizeDistance(ConstraintFactory constraintFactory) {
                return constraintFactory.forEach(Visit.class)
                                .filter(visit -> visit.getVehicle() != null)
                                .penalize(HardSoftScore.ONE_SOFT,
                                                Visit::getDistanciaDesdeAnteriorMetros)
                                .asConstraint("Minimizar distancia total");
        }

        /**
         * Minimizar el tiempo total de viaje.
         */
        protected Constraint minimizeTravelTime(ConstraintFactory constraintFactory) {
                return constraintFactory.forEach(Visit.class)
                                .filter(visit -> visit.getVehicle() != null)
                                .penalize(HardSoftScore.ONE_SOFT,
                                                Visit::getTiempoViajeDesdeAnteriorMinutos)
                                .asConstraint("Minimizar tiempo de viaje");
        }

        /**
         * Minimizar la cantidad de vehículos utilizados.
         */
        protected Constraint minimizeVehicles(ConstraintFactory constraintFactory) {
                return constraintFactory.forEach(Visit.class)
                                .filter(visit -> visit.getVehicle() != null)
                                .filter(visit -> visit.getPreviousVisit() == null) // Primera visita de cada vehículo
                                .penalize(HardSoftScore.ofSoft(1000))
                                .asConstraint("Minimizar cantidad de vehículos");
        }

        /**
         * Priorizar pedidos de alta prioridad (asignarlos primero).
         */
        protected Constraint prioritizeHighPriorityOrders(ConstraintFactory constraintFactory) {
                return constraintFactory.forEach(Visit.class)
                                .filter(visit -> visit.getVehicle() != null)
                                .filter(visit -> visit.getLocation().getPrioridad() > 1)
                                .reward(HardSoftScore.ONE_SOFT,
                                                visit -> visit.getLocation().getPrioridad() * 100)
                                .asConstraint("Priorizar pedidos importantes");
        }
}
