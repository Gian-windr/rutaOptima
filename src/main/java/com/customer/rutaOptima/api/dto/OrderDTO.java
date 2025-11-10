package com.customer.rutaOptima.api.dto;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

/**
 * DTO para crear/actualizar pedidos
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderDTO {

    private Long id;

    @NotNull(message = "El ID del cliente es obligatorio")
    private Long customerId;

    @NotNull(message = "La fecha de entrega es obligatoria")
    @FutureOrPresent(message = "La fecha de entrega debe ser presente o futura")
    private LocalDate fechaEntrega;

    @NotNull(message = "La cantidad es obligatoria")
    @DecimalMin(value = "0.01", message = "La cantidad debe ser mayor que 0")
    private BigDecimal cantidad;

    @DecimalMin(value = "0.0")
    private BigDecimal volumen;

    @DecimalMin(value = "0.0")
    private BigDecimal peso;

    private String estado = "PENDIENTE";
    private LocalTime ventanaHorariaInicio;
    private LocalTime ventanaHorariaFin;
    private Integer prioridad = 1;
    
    @Min(value = 0, message = "El tiempo de servicio no puede ser negativo")
    private Integer tiempoServicioEstimadoMin = 10;
    
    private String notas;
}
