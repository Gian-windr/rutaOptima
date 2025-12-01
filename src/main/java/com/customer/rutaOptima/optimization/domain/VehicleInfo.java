package com.customer.rutaOptima.optimization.domain;

import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Información del vehículo para el solver.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class VehicleInfo {
    private Long vehicleId;
    private String vehicleName;
    private BigDecimal capacidadCantidad;
    private BigDecimal capacidadVolumen;
    private BigDecimal capacidadPeso;
    private Location depot;
    private String zona;
    private String conductor;
    private String color;
}
