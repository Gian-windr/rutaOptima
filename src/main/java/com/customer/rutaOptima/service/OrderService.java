package com.customer.rutaOptima.service;

import com.customer.rutaOptima.config.exception.BusinessException;
import com.customer.rutaOptima.domain.Customer;
import com.customer.rutaOptima.domain.Order;
import com.customer.rutaOptima.persistence.CustomerRepository;
import com.customer.rutaOptima.persistence.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * Servicio de negocio para pedidos
 * Implementa la regla de 5 días de anticipación para clientes nuevos
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class OrderService {

    private final OrderRepository orderRepository;
    private final CustomerRepository customerRepository;

    private static final int DIAS_ANTICIPACION_CLIENTE_NUEVO = 5;

    @Transactional
    public List<Order> findAll() {
        return orderRepository.findAll();
    }

    @Transactional
    public Order createOrder(Order order) {
        log.info("Creando pedido para cliente ID: {}, fecha entrega: {}",
                order.getCustomer().getId(), order.getFechaEntrega());

        // Validar que el cliente existe
        Customer customer = customerRepository.findById(order.getCustomer().getId())
                .orElseThrow(() -> new BusinessException(
                        "Cliente con ID " + order.getCustomer().getId() + " no encontrado"));

        order.setCustomer(customer);

        // REGLA DE NEGOCIO CLAVE: Validar 5 días de anticipación para clientes nuevos
        if (customer.getEsNuevo()) {
            validateNewCustomerLeadTime(order.getFechaEntrega(), customer);
        }

        // Validar fecha no sea en el pasado
        if (order.getFechaEntrega().isBefore(Instant.now())) {
            throw new BusinessException(
                    "La fecha de entrega no puede ser en el pasado");
        }

        // Validar ventanas horarias si están presentes
        if (order.getVentanaHorariaInicio() != null && order.getVentanaHorariaFin() != null) {
            if (order.getVentanaHorariaInicio().isAfter(order.getVentanaHorariaFin())) {
                throw new BusinessException(
                        "La ventana horaria de inicio no puede ser posterior a la de fin");
            }
        }

        Order savedOrder = orderRepository.save(order);
        log.info("Pedido creado exitosamente con ID: {}", savedOrder.getId());

        return savedOrder;
    }

    /**
     * Valida que los pedidos de clientes nuevos cumplan con la anticipación mínima de 5 días
     */
    private void validateNewCustomerLeadTime(Instant fechaEntrega, Customer customer) {
        LocalDate fechaActual = LocalDate.now();
        long diasAnticipacion = ChronoUnit.DAYS.between(fechaActual, fechaEntrega);

        log.debug("Cliente nuevo: {}, días de anticipación: {}, mínimo requerido: {}",
                customer.getNombre(), diasAnticipacion, DIAS_ANTICIPACION_CLIENTE_NUEVO);

        if (diasAnticipacion < DIAS_ANTICIPACION_CLIENTE_NUEVO) {
            throw new BusinessException(String.format(
                    "Para clientes nuevos, los pedidos deben realizarse con mínimo %d días de anticipación. " +
                            "Fecha actual: %s, Fecha entrega: %s (solo %d días de anticipación). " +
                            "Cliente: %s",
                    DIAS_ANTICIPACION_CLIENTE_NUEVO,
                    fechaActual,
                    fechaEntrega,
                    diasAnticipacion,
                    customer.getNombre()
            ));
        }
    }

    @Transactional(readOnly = true)
    public List<Order> findByFecha(Instant fecha) {
        return orderRepository.findByFechaEntrega(fecha);
    }

    @Transactional(readOnly = true)
    public List<Order> findByFechaRange(Instant start, Instant end) {
        return orderRepository.findByFechaEntregaBetween(start, end);
    }

    @Transactional(readOnly = true)
    public List<Order> findPendingOrdersByFecha(Instant fecha) {
        return orderRepository.findPendingOrdersWithCustomerByFecha(fecha);
    }

    @Transactional(readOnly = true)
    public Order findById(Long id) {
        return orderRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Pedido con ID " + id + " no encontrado"));
    }

    @Transactional
    public void deleteOrder(Long id) {
        if (!orderRepository.existsById(id)) {
            throw new BusinessException("Pedido con ID " + id + " no encontrado");
        }
        orderRepository.deleteById(id);
        log.info("Pedido con ID {} eliminado", id);
    }
}
