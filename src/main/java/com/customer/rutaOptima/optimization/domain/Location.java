package com.customer.rutaOptima.optimization.domain;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Representa una ubicación a visitar (cliente con pedido)
 * en el problema de optimización
 */
@Data
@NoArgsConstructor
public class Location {

    private Long orderId;
    private Long customerId;
    private String nombre;
    private Double latitud;
    private Double longitud;
    private Double demandaCantidad;
    private Double demandaVolumen;
    private Double demandaPeso;
    private Instant ventanaInicio;
    private Instant ventanaFin;
    private Integer tiempoServicioMin = 10; // Tiempo de servicio por defecto
    private Integer prioridad = 1;

    /**
     * Verifica si tiene ventana horaria definida
     */
    public boolean tieneVentanaHoraria() {
        return ventanaInicio != null && ventanaFin != null;
    }

    /**
     * Calcula la distancia en kilómetros a otra ubicación usando la fórmula de Haversine
     */
    public double calcularDistanciaKm(Location otra) {
        return calcularDistanciaHaversine(
                this.latitud, this.longitud,
                otra.latitud, otra.longitud
        );
    }

    /**
     * Fórmula de Haversine para calcular distancia entre dos puntos en la Tierra
     */
    private double calcularDistanciaHaversine(double lat1, double lon1, double lat2, double lon2) {
        final int R = 6371; // Radio de la Tierra en km

        double latDistance = Math.toRadians(lat2 - lat1);
        double lonDistance = Math.toRadians(lon2 - lon1);
        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        return R * c;
    }
}