package com.customer.rutaOptima.persistence;

import com.customer.rutaOptima.domain.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

/**
 * Repositorio para la entidad Order
 */
@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {

    List<Order> findByFechaEntrega(Instant fechaEntrega);

    List<Order> findByFechaEntregaAndEstado(Instant fechaEntrega, String estado);

    List<Order> findByFechaEntregaBetween(Instant start, Instant end);

    List<Order> findByFechaEntregaBetweenAndEstado(Instant start, Instant end, String estado);

    List<Order> findByCustomerId(Long customerId);

    @Query("SELECT o FROM Order o WHERE o.fechaEntrega = :fecha AND o.estado = 'PENDIENTE' ORDER BY o.prioridad DESC, o.id")
    List<Order> findPendingOrdersByFecha(@Param("fecha") Instant fecha);

    @Query("SELECT o FROM Order o JOIN FETCH o.customer c WHERE o.fechaEntrega = :fecha AND o.estado = 'PENDIENTE'")
    List<Order> findPendingOrdersWithCustomerByFecha(@Param("fecha") Instant fecha);

    @Query("SELECT o FROM Order o JOIN FETCH o.customer c WHERE o.fechaEntrega >= :start AND o.fechaEntrega < :end AND o.estado = 'PENDIENTE' ORDER BY o.prioridad DESC, o.id")
    List<Order> findPendingOrdersWithCustomerBetween(@Param("start") Instant start, @Param("end") Instant end);

    @Query("SELECT COUNT(o) FROM Order o WHERE o.customer.id = :customerId")
    long countByCustomerId(@Param("customerId") Long customerId);

    @Query("SELECT o FROM Order o WHERE o.customer.esNuevo = true AND o.fechaEntrega BETWEEN :fechaInicio AND :fechaFin")
    List<Order> findOrdersFromNewCustomersInRange(@Param("fechaInicio") Instant fechaInicio, @Param("fechaFin") Instant fechaFin);
}
