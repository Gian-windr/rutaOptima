package com.customer.rutaOptima.api.dto;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;

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
    @Builder.Default
    private BigDecimal velocidadKmh = new BigDecimal("40.0");

    @NotNull
    @DecimalMin(value = "0.0")
    @Builder.Default
    private BigDecimal costoKm = new BigDecimal("1.5");

    @Builder.Default
    private Boolean activo = (Boolean) true;

    @NotBlank(message = "El nombre del conductor es obligatorio")
    private String conductor;

    private String zona;

    @NotBlank(message = "El color es obligatorio")
    @Pattern(regexp = "^#[0-9A-Fa-f]{6}$", message = "El color debe ser un código hexadecimal válido (ej: #3B82F6)")
    @Builder.Default
    private String color = "#3B82F6";

    @NotNull(message = "La latitud del depósito es obligatoria")
    @DecimalMin(value = "-90.0")
    @DecimalMax(value = "90.0")
    private BigDecimal depotLatitud;

    @NotNull(message = "La longitud del depósito es obligatoria")
    @DecimalMin(value = "-180.0")
    @DecimalMax(value = "180.0")
    private BigDecimal depotLongitud;

    @Builder.Default
    private Instant jornadaInicio = LocalDate.now(ZoneId.systemDefault())
            .atTime(LocalTime.of(8, 0))
            .atZone(ZoneId.systemDefault())
            .toInstant();

    @Builder.Default
    private Instant jornadaFin = LocalDate.now(ZoneId.systemDefault())
            .atTime(LocalTime.of(18, 0))
            .atZone(ZoneId.systemDefault())
            .toInstant();
}
