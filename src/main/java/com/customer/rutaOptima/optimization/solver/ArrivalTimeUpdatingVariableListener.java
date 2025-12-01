package com.customer.rutaOptima.optimization.solver;

import java.math.BigDecimal;
import java.time.Instant;

import org.optaplanner.core.api.domain.variable.VariableListener;
import org.optaplanner.core.api.score.director.ScoreDirector;

import com.customer.rutaOptima.optimization.domain.Location;
import com.customer.rutaOptima.optimization.domain.VehicleRoutingSolution;
import com.customer.rutaOptima.optimization.domain.Visit;

/**
 * Listener que actualiza las shadow variables de Visit cuando cambian las planning variables.
 * Las distancias se calculan usando Haversine (OSRM se usa fuera del solver para resultados finales).
 */
public class ArrivalTimeUpdatingVariableListener implements VariableListener<VehicleRoutingSolution, Visit> {

    @Override
    public void beforeEntityAdded(ScoreDirector<VehicleRoutingSolution> scoreDirector, Visit visit) {
        // No action needed
    }

    @Override
    public void afterEntityAdded(ScoreDirector<VehicleRoutingSolution> scoreDirector, Visit visit) {
        updateVisit(scoreDirector, visit);
    }

    @Override
    public void beforeVariableChanged(ScoreDirector<VehicleRoutingSolution> scoreDirector, Visit visit) {
        // No action needed
    }

    @Override
    public void afterVariableChanged(ScoreDirector<VehicleRoutingSolution> scoreDirector, Visit visit) {
        updateVisit(scoreDirector, visit);
    }

    @Override
    public void beforeEntityRemoved(ScoreDirector<VehicleRoutingSolution> scoreDirector, Visit visit) {
        // No action needed
    }

    @Override
    public void afterEntityRemoved(ScoreDirector<VehicleRoutingSolution> scoreDirector, Visit visit) {
        // No action needed
    }

    /**
     * Actualiza todas las shadow variables de la visita y propaga a las siguientes.
     */
    private void updateVisit(ScoreDirector<VehicleRoutingSolution> scoreDirector, Visit sourceVisit) {
        Visit visit = sourceVisit;
        if (visit.getVehicle() == null) {
            return; // No asignado todavía
        }
        
        Location previousLocation = visit.isAnchor() ? 
            visit.getVehicle().getDepot() : 
            (visit.getPreviousVisit() != null ? visit.getPreviousVisit().getLocation() : null);

        // Calcular distancia y tiempo desde ubicación anterior
        if (previousLocation != null && visit.getLocation() != null) {
            double distance = calculateHaversineDistance(previousLocation, visit.getLocation());
            int duration = (int) (distance * 2); // estimación: 30 km/h promedio
            
            scoreDirector.beforeVariableChanged(visit, "distanceFromPreviousKm");
            visit.setDistanceFromPreviousKm(distance);
            scoreDirector.afterVariableChanged(visit, "distanceFromPreviousKm");
            
            scoreDirector.beforeVariableChanged(visit, "travelTimeFromPreviousMin");
            visit.setTravelTimeFromPreviousMin(duration);
            scoreDirector.afterVariableChanged(visit, "travelTimeFromPreviousMin");
        } else {
            scoreDirector.beforeVariableChanged(visit, "distanceFromPreviousKm");
            visit.setDistanceFromPreviousKm(0.0);
            scoreDirector.afterVariableChanged(visit, "distanceFromPreviousKm");
            
            scoreDirector.beforeVariableChanged(visit, "travelTimeFromPreviousMin");
            visit.setTravelTimeFromPreviousMin(0);
            scoreDirector.afterVariableChanged(visit, "travelTimeFromPreviousMin");
        }

        // Calcular tiempo de llegada
        Instant arrivalTime;
        if (visit.isAnchor()) {
            arrivalTime = Instant.now(); // primera visita empieza ahora
        } else {
            Visit prev = visit.getPreviousVisit();
            if (prev != null && prev.getArrivalTime() != null && visit.getTravelTimeFromPreviousMin() != null) {
                Integer tiempoServicio = prev.getLocation().getTiempoServicioMin();
                int serviceTime = tiempoServicio != null ? tiempoServicio : 10;
                arrivalTime = prev.getArrivalTime()
                    .plusSeconds((visit.getTravelTimeFromPreviousMin() + serviceTime) * 60L);
            } else {
                arrivalTime = null;
            }
        }
        
        scoreDirector.beforeVariableChanged(visit, "arrivalTime");
        visit.setArrivalTime(arrivalTime);
        scoreDirector.afterVariableChanged(visit, "arrivalTime");

        // Calcular cargas acumuladas
        BigDecimal accCantidad, accVolumen, accPeso;
        BigDecimal demanda = visit.getLocation().getDemanda() != null ? 
            visit.getLocation().getDemanda() : BigDecimal.ZERO;
            
        if (visit.isAnchor()) {
            accCantidad = demanda;
            accVolumen = demanda; // simplificación
            accPeso = demanda;
        } else {
            Visit prev = visit.getPreviousVisit();
            if (prev != null && prev.getAccumulatedCantidad() != null) {
                accCantidad = prev.getAccumulatedCantidad().add(demanda);
                accVolumen = prev.getAccumulatedVolumen().add(demanda);
                accPeso = prev.getAccumulatedPeso().add(demanda);
            } else {
                accCantidad = demanda;
                accVolumen = demanda;
                accPeso = demanda;
            }
        }
        
        scoreDirector.beforeVariableChanged(visit, "accumulatedCantidad");
        visit.setAccumulatedCantidad(accCantidad);
        scoreDirector.afterVariableChanged(visit, "accumulatedCantidad");
        
        scoreDirector.beforeVariableChanged(visit, "accumulatedVolumen");
        visit.setAccumulatedVolumen(accVolumen);
        scoreDirector.afterVariableChanged(visit, "accumulatedVolumen");
        
        scoreDirector.beforeVariableChanged(visit, "accumulatedPeso");
        visit.setAccumulatedPeso(accPeso);
        scoreDirector.afterVariableChanged(visit, "accumulatedPeso");

        // Propagar cambios a la siguiente visita (si hay cadena)
        Visit nextVisit = findNextVisit(scoreDirector, visit);
        if (nextVisit != null) {
            updateVisit(scoreDirector, nextVisit);
        }
    }

    /**
     * Encuentra la siguiente visita en la cadena.
     */
    private Visit findNextVisit(ScoreDirector<VehicleRoutingSolution> scoreDirector, Visit visit) {
        for (Visit otherVisit : scoreDirector.getWorkingSolution().getVisits()) {
            if (otherVisit.getPreviousVisit() != null && 
                otherVisit.getPreviousVisit().equals(visit)) {
                return otherVisit;
            }
        }
        return null;
    }

    private String getCacheKey(Location from, Location to) {
        return String.format("%f,%f->%f,%f", 
            from.getLatitud(), from.getLongitud(),
            to.getLatitud(), to.getLongitud());
    }

    private double calculateHaversineDistance(Location from, Location to) {
        double lat1 = from.getLatitud().doubleValue();
        double lon1 = from.getLongitud().doubleValue();
        double lat2 = to.getLatitud().doubleValue();
        double lon2 = to.getLongitud().doubleValue();

        double R = 6371; // Radio de la Tierra en km
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                   Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                   Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c;
    }
}
