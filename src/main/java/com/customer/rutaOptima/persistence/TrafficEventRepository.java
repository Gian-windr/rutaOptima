package com.customer.rutaOptima.persistence;

import com.customer.rutaOptima.domain.TrafficEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Repositorio para la entidad TrafficEvent
 */
@Repository
public interface TrafficEventRepository extends JpaRepository<TrafficEvent, Long> {

    List<TrafficEvent> findByActivoTrue();

    @Query("SELECT te FROM TrafficEvent te WHERE te.activo = true AND te.fechaHora >= :desde ORDER BY te.fechaHora DESC")
    List<TrafficEvent> findActiveEventsSince(@Param("desde") LocalDateTime desde);

    @Query("SELECT te FROM TrafficEvent te WHERE te.activo = true AND te.fechaHora BETWEEN :desde AND :hasta")
    List<TrafficEvent> findActiveEventsInRange(@Param("desde") LocalDateTime desde, @Param("hasta") LocalDateTime hasta);
}
