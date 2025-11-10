package com.customer.rutaOptima.persistence;

import com.customer.rutaOptima.domain.RoutePlan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Repositorio para la entidad RoutePlan
 */
@Repository
public interface RoutePlanRepository extends JpaRepository<RoutePlan, Long> {

    List<RoutePlan> findByFecha(LocalDate fecha);

    List<RoutePlan> findByEstado(RoutePlan.Estado estado);

    List<RoutePlan> findByFechaAndEstado(LocalDate fecha, RoutePlan.Estado estado);

    @Query("SELECT rp FROM RoutePlan rp WHERE rp.fecha = :fecha ORDER BY rp.createdAt DESC")
    List<RoutePlan> findByFechaOrderedByCreatedDesc(@Param("fecha") LocalDate fecha);

    @Query("SELECT rp FROM RoutePlan rp LEFT JOIN FETCH rp.stops WHERE rp.id = :id")
    Optional<RoutePlan> findByIdWithStops(@Param("id") Long id);

    @Query("SELECT rp FROM RoutePlan rp WHERE rp.fecha = :fecha AND rp.estado = com.customer.rutaOptima.domain.RoutePlan$Estado.OPTIMIZED ORDER BY rp.createdAt DESC")
    List<RoutePlan> findOptimizedPlansByFecha(@Param("fecha") LocalDate fecha);
}
