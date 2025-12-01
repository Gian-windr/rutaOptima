package com.customer.rutaOptima.persistence;

import com.customer.rutaOptima.domain.RoutePlan;
import com.customer.rutaOptima.domain.RouteStop;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repositorio para la entidad RouteStop
 */
@Repository
public interface RouteStopRepository extends JpaRepository<RouteStop, Long> {

    List<RouteStop> findByRoutePlanId(Long routePlanId);

    @Query("SELECT rs FROM RouteStop rs WHERE rs.routePlan.id = :routePlanId ORDER BY rs.vehicle.id, rs.secuencia")
    List<RouteStop> findByRoutePlanIdOrderedByVehicleAndSequence(@Param("routePlanId") Long routePlanId);

    @Query("SELECT rs FROM RouteStop rs WHERE rs.routePlan.id = :routePlanId AND rs.vehicle.id = :vehicleId ORDER BY rs.secuencia")
    List<RouteStop> findByRoutePlanIdAndVehicleIdOrdered(@Param("routePlanId") Long routePlanId, @Param("vehicleId") Long vehicleId);

    void deleteByRoutePlanId(Long routePlanId);
    void deleteByRoutePlan(RoutePlan routePlan);


}
