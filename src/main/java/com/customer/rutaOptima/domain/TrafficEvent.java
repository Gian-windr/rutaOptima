package com.customer.rutaOptima.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Entidad que representa un evento de tráfico para re-optimización
 */
@Entity
@Table(name = "traffic_event", indexes = {
        @Index(name = "idx_traffic_event_fecha_hora", columnList = "fecha_hora"),
        @Index(name = "idx_traffic_event_activo", columnList = "activo"),
        @Index(name = "idx_traffic_event_location", columnList = "latitud, longitud")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TrafficEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull(message = "La fecha y hora del evento es obligatoria")
    @Column(name = "fecha_hora", nullable = false)
    @Builder.Default
    private LocalDateTime fechaHora = LocalDateTime.now();

    @NotBlank(message = "La descripción del evento es obligatoria")
    @Column(nullable = false, columnDefinition = "TEXT")
    private String descripcion;

    /**
     * Factor de retraso: 1.0 = normal, 1.5 = 50% más lento, 2.0 = el doble de tiempo
     */
    @NotNull(message = "El factor de retraso es obligatorio")
    @DecimalMin(value = "1.0", message = "El factor de retraso debe ser al menos 1.0")
    @Column(name = "factor_retraso", nullable = false, precision = 5, scale = 2)
    @Builder.Default
    private BigDecimal factorRetraso = BigDecimal.ONE;

    @DecimalMin(value = "-90.0", message = "La latitud debe estar entre -90 y 90")
    @DecimalMax(value = "90.0", message = "La latitud debe estar entre -90 y 90")
    @Column(precision = 10, scale = 8)
    private BigDecimal latitud;

    @DecimalMin(value = "-180.0", message = "La longitud debe estar entre -180 y 180")
    @DecimalMax(value = "180.0", message = "La longitud debe estar entre -180 y 180")
    @Column(precision = 11, scale = 8)
    private BigDecimal longitud;

    @DecimalMin(value = "0.0", message = "El radio de afectación no puede ser negativo")
    @Column(name = "radio_afectacion_km", precision = 10, scale = 2)
    private BigDecimal radioAfectacionKm;

    @Column(nullable = false)
    @Builder.Default
    private Boolean activo = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (fechaHora == null) {
            fechaHora = LocalDateTime.now();
        }
    }

    /**
     * Verifica si este evento afecta a una ubicación específica
     */
    public boolean afectaUbicacion(BigDecimal lat, BigDecimal lon) {
        if (latitud == null || longitud == null || radioAfectacionKm == null) {
            return false;
        }

        // Calcular distancia usando fórmula de Haversine simplificada
        double distanciaKm = calcularDistanciaHaversine(
                latitud.doubleValue(), longitud.doubleValue(),
                lat.doubleValue(), lon.doubleValue()
        );

        return distanciaKm <= radioAfectacionKm.doubleValue();
    }

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
