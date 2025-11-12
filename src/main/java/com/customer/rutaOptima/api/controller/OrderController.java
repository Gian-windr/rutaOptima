package com.customer.rutaOptima.api.controller;

import com.customer.rutaOptima.api.dto.OrderDTO;
import com.customer.rutaOptima.domain.Customer;
import com.customer.rutaOptima.domain.Order;
import com.customer.rutaOptima.service.CustomerService;
import com.customer.rutaOptima.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.*;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Controlador REST para pedidos
 * Implementa validación de la regla de 5 días para clientes nuevos
 */
@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;
    private final CustomerService customerService;

    @GetMapping
    public ResponseEntity<List<OrderDTO>> getOrders(
            @RequestParam String fecha,
            @RequestParam(required = false) String estado) {

        if (fecha == null || fecha.isBlank()) {
            return ResponseEntity.ok(List.of());
        }

        ZoneId zone = ZoneId.systemDefault();

        Instant startInstant;
        Instant endInstant;

        if (fecha.matches("^\\d{4}-\\d{2}-\\d{2}$")) {
            LocalDate ld = LocalDate.parse(fecha);
            startInstant = ld.atStartOfDay(zone).toInstant();
            endInstant = ld.plusDays(1).atStartOfDay(zone).toInstant();
        } else {
            Instant parsedInstant;
            try {
                parsedInstant = Instant.parse(fecha); // e.g. 2025-11-12T10:15:30Z
            } catch (DateTimeParseException ex1) {
                try {
                    parsedInstant = OffsetDateTime.parse(fecha).toInstant(); // con offset
                } catch (DateTimeParseException ex2) {
                    // sin offset -> LocalDateTime
                    LocalDateTime ldt = LocalDateTime.parse(fecha);
                    parsedInstant = ldt.atZone(zone).toInstant();
                }
            }
            // Para "con hora" tomar la hora (rango de 1 hora)
            startInstant = parsedInstant.truncatedTo(ChronoUnit.HOURS);
            endInstant = startInstant.plus(1, ChronoUnit.HOURS);
        }

        List<Order> orders = orderService.findByFechaRange(startInstant, endInstant);

        if (estado != null && !estado.isBlank()) {
            orders = orders.stream()
                    .filter(o -> estado.equalsIgnoreCase(o.getEstado()))
                    .collect(Collectors.toList());
        }

        List<OrderDTO> dtos = orders.stream()
                .map(this::toDTO)
                .collect(Collectors.toList());

        return ResponseEntity.ok(dtos);
    }

    @GetMapping("/{id}")
    public ResponseEntity<OrderDTO> getOrderById(@PathVariable Long id) {
        Order order = orderService.findById(id);
        return ResponseEntity.ok(toDTO(order));
    }

    /**
     * Crear pedido - valida automáticamente la regla de 5 días para clientes nuevos
     */
    @PostMapping
    public ResponseEntity<OrderDTO> createOrder(@Valid @RequestBody OrderDTO dto) {
        Order order = toEntity(dto);
        Order created = orderService.createOrder(order); // Valida regla de 5 días internamente
        return ResponseEntity.status(HttpStatus.CREATED).body(toDTO(created));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteOrder(@PathVariable Long id) {
        orderService.deleteOrder(id);
        return ResponseEntity.noContent().build();
    }

    private OrderDTO toDTO(Order order) {
        return OrderDTO.builder()
                .id(order.getId())
                .customerId(order.getCustomer().getId())
                .fechaEntrega(order.getFechaEntrega())
                .cantidad(order.getCantidad())
                .volumen(order.getVolumen())
                .peso(order.getPeso())
                .estado(order.getEstado())
                .ventanaHorariaInicio(order.getVentanaHorariaInicio())
                .ventanaHorariaFin(order.getVentanaHorariaFin())
                .prioridad(order.getPrioridad())
                .notas(order.getNotas())
                .build();
    }

    private Order toEntity(OrderDTO dto) {
        Customer customer = customerService.findById(dto.getCustomerId());

        return Order.builder()
                .customer(customer)
                .fechaEntrega(dto.getFechaEntrega())
                .cantidad(dto.getCantidad())
                .volumen(dto.getVolumen())
                .peso(dto.getPeso())
                .estado(dto.getEstado() != null ? dto.getEstado() : "PENDIENTE")
                .ventanaHorariaInicio(dto.getVentanaHorariaInicio())
                .ventanaHorariaFin(dto.getVentanaHorariaFin())
                .prioridad(Integer.valueOf(dto.getPrioridad() != null ? dto.getPrioridad() : 1))
                .notas(dto.getNotas())
                .build();
    }
}
