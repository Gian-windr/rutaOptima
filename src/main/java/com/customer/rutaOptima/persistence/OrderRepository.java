package com.customer.rutaOptima.persistence;

import com.customer.rutaOptima.domain.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

/**
 * Repositorio para la entidad Order
 */
@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {

    List<Order> findByFechaEntrega(LocalDate fechaEntrega);

    List<Order> findByFechaEntregaAndEstado(LocalDate fechaEntrega, String estado);

    List<Order> findByCustomerId(Long customerId);

    @Query("SELECT o FROM Order o WHERE o.fechaEntrega = :fecha AND o.estado = 'PENDIENTE' ORDER BY o.prioridad DESC, o.id")
    List<Order> findPendingOrdersByFecha(@Param("fecha") LocalDate fecha);

    @Query("SELECT o FROM Order o JOIN FETCH o.customer c WHERE o.fechaEntrega = :fecha AND o.estado = 'PENDIENTE'")
    List<Order> findPendingOrdersWithCustomerByFecha(@Param("fecha") LocalDate fecha);

    @Query("SELECT COUNT(o) FROM Order o WHERE o.customer.id = :customerId")
    long countByCustomerId(@Param("customerId") Long customerId);

    @Query("SELECT o FROM Order o WHERE o.customer.esNuevo = true AND o.fechaEntrega BETWEEN :fechaInicio AND :fechaFin")
    List<Order> findOrdersFromNewCustomersInRange(@Param("fechaInicio") LocalDate fechaInicio, @Param("fechaFin") LocalDate fechaFin);
}
