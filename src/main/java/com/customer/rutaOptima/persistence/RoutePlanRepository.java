package com.customer.rutaOptima.persistence;

import com.customer.rutaOptima.domain.RoutePlan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Repositorio para la entidad RoutePlan
 */
@Repository
public interface RoutePlanRepository extends JpaRepository<RoutePlan, Long> {

    List<RoutePlan> findByFechaBetween(Instant startInclusive, Instant endExclusive);

    List<RoutePlan> findByEstado(RoutePlan.Estado estado);

    List<RoutePlan> findByFechaBetweenAndEstado(Instant startInclusive, Instant endExclusive, RoutePlan.Estado estado);

    @Query("SELECT rp FROM RoutePlan rp WHERE rp.fecha >= :start AND rp.fecha < :end ORDER BY rp.createdAt DESC")
    List<RoutePlan> findByFechaBetweenOrderedByCreatedDesc(@Param("start") Instant start, @Param("end") Instant end);

    @Query("SELECT rp FROM RoutePlan rp LEFT JOIN FETCH rp.stops WHERE rp.id = :id")
    Optional<RoutePlan> findByIdWithStops(@Param("id") Long id);

    @Query("SELECT rp FROM RoutePlan rp WHERE rp.fecha >= :start AND rp.fecha < :end AND rp.estado = com.customer.rutaOptima.domain.RoutePlan$Estado.OPTIMIZED ORDER BY rp.createdAt DESC")
    List<RoutePlan> findOptimizedPlansByFechaRange(@Param("start") Instant start, @Param("end") Instant end);

    // Queries con JOIN FETCH para evitar LazyInitializationException
    @Query("SELECT DISTINCT rp FROM RoutePlan rp LEFT JOIN FETCH rp.stops ORDER BY rp.id DESC")
    List<RoutePlan> findAllWithStops();

    @Query("SELECT DISTINCT rp FROM RoutePlan rp LEFT JOIN FETCH rp.stops WHERE rp.estado = :estado ORDER BY rp.id DESC")
    List<RoutePlan> findByEstadoWithStops(@Param("estado") RoutePlan.Estado estado);

    @Query("SELECT DISTINCT rp FROM RoutePlan rp LEFT JOIN FETCH rp.stops WHERE rp.fecha >= :start AND rp.fecha < :end ORDER BY rp.id DESC")
    List<RoutePlan> findByFechaBetweenWithStops(@Param("start") Instant start, @Param("end") Instant end);

    @Query("SELECT DISTINCT rp FROM RoutePlan rp LEFT JOIN FETCH rp.stops WHERE rp.fecha >= :start AND rp.fecha < :end AND rp.estado = :estado ORDER BY rp.id DESC")
    List<RoutePlan> findByFechaBetweenAndEstadoWithStops(@Param("start") Instant start, @Param("end") Instant end,
            @Param("estado") RoutePlan.Estado estado);
}
