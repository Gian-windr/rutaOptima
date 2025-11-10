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

import java.time.LocalDate;
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
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha,
            @RequestParam(required = false) String estado) {
        
        List<Order> orders;
        if (fecha != null) {
            orders = (estado != null) 
                    ? orderService.findByFecha(fecha).stream()
                        .filter(o -> o.getEstado().equals(estado))
                        .collect(Collectors.toList())
                    : orderService.findByFecha(fecha);
        } else {
            orders = List.of(); // O implementar un findAll() si es necesario
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
                .prioridad(dto.getPrioridad() != null ? dto.getPrioridad() : 1)
                .notas(dto.getNotas())
                .build();
    }
}
