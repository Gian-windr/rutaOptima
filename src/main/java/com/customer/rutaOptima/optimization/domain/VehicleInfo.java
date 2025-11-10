package com.customer.rutaOptima.optimization.domain;

import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalTime;

/**
 * Representa un vehículo en el problema de optimización
 */
@Data
@NoArgsConstructor
public class VehicleInfo {

    private Long id;
    private String patente;
    private Double capacidadCantidad;
    private Double capacidadVolumen;
    private Double capacidadPeso;
    private Double velocidadKmh;
    private Double costoKm;
    private Double depotLatitud;
    private Double depotLongitud;
    private LocalTime jornadaInicio;
    private LocalTime jornadaFin;

    /**
     * Calcula la distancia desde el depósito a una ubicación
     */
    public double calcularDistanciaDesdeDepotKm(Location location) {
        return calcularDistanciaHaversine(
                depotLatitud, depotLongitud,
                location.getLatitud(), location.getLongitud()
        );
    }

    /**
     * Calcula el tiempo de viaje en minutos entre dos ubicaciones
     */
    public int calcularTiempoViajeMinutos(double distanciaKm) {
        if (velocidadKmh <= 0) {
            return 0;
        }
        double tiempoHoras = distanciaKm / velocidadKmh;
        return (int) Math.ceil(tiempoHoras * 60);
    }

    /**
     * Calcula el tiempo de viaje en minutos considerando factor de tráfico
     */
    public int calcularTiempoViajeMinutos(double distanciaKm, double factorTrafico) {
        if (velocidadKmh <= 0) {
            return 0;
        }
        double tiempoHoras = distanciaKm / velocidadKmh;
        return (int) Math.ceil(tiempoHoras * 60 * factorTrafico);
    }

    /**
     * Fórmula de Haversine
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

    /**
     * Obtiene la duración de la jornada en minutos
     */
    public int getJornadaDuracionMinutos() {
        if (jornadaInicio != null && jornadaFin != null) {
            return (int) java.time.Duration.between(jornadaInicio, jornadaFin).toMinutes();
        }
        return 600; // 10 horas por defecto
    }
}
