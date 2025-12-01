package com.customer.rutaOptima.optimization.domain;

import java.math.BigDecimal;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Ubicación geográfica para optimización.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Location {
    private BigDecimal latitud;
    private BigDecimal longitud;
    private Long customerId;
    private String customerName;
    private String zona; // Zona del cliente (Norte, Sur, Este, Centro)
    private Integer tiempoServicioMin;
    private BigDecimal demanda;
}
