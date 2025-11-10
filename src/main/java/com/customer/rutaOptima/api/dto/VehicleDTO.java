package com.customer.rutaOptima.api.dto;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalTime;

/**
 * DTO para vehículos
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VehicleDTO {

    private Long id;

    @NotBlank(message = "El nombre es obligatorio")
    private String nombre;

    @NotBlank(message = "La patente es obligatoria")
    private String patente;

    @NotBlank(message = "El tipo es obligatorio")
    private String tipo;

    @NotNull(message = "La capacidad de cantidad es obligatoria")
    @DecimalMin(value = "0.01")
    private BigDecimal capacidadCantidad;

    @DecimalMin(value = "0.0")
    private BigDecimal capacidadVolumen;

    @DecimalMin(value = "0.0")
    private BigDecimal capacidadPeso;

    @NotNull
    @DecimalMin(value = "0.1")
    private BigDecimal velocidadKmh = new BigDecimal("40.0");

    @NotNull
    @DecimalMin(value = "0.0")
    private BigDecimal costoKm = new BigDecimal("1.5");

    private Boolean activo = true;

    @NotNull(message = "La latitud del depósito es obligatoria")
    @DecimalMin(value = "-90.0")
    @DecimalMax(value = "90.0")
    private BigDecimal depotLatitud;

    @NotNull(message = "La longitud del depósito es obligatoria")
    @DecimalMin(value = "-180.0")
    @DecimalMax(value = "180.0")
    private BigDecimal depotLongitud;

    private LocalTime jornadaInicio = LocalTime.of(8, 0);
    private LocalTime jornadaFin = LocalTime.of(18, 0);
}
