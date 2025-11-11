package com.customer.rutaOptima.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;

/**
 * Entidad que representa un vehículo de la flota
 */
@Entity
@Table(name = "vehicle", indexes = {
        @Index(name = "idx_vehicle_activo", columnList = "activo"),
        @Index(name = "idx_vehicle_depot", columnList = "depot_latitud, depot_longitud")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Vehicle {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "El nombre del vehículo es obligatorio")
    @Column(nullable = false, length = 255)
    private String nombre;

    @NotBlank(message = "La patente del vehículo es obligatoria")
    @Column(nullable = false, unique = true, length = 20)
    private String patente;

    @NotBlank(message = "El tipo de vehículo es obligatorio")
    @Column(nullable = false, length = 100)
    private String tipo;

    @NotNull(message = "La capacidad de cantidad es obligatoria")
    @DecimalMin(value = "0.01", message = "La capacidad de cantidad debe ser mayor que 0")
    @Column(name = "capacidad_cantidad", nullable = false, precision = 10, scale = 2)
    private BigDecimal capacidadCantidad;

    @DecimalMin(value = "0.0", message = "La capacidad de volumen no puede ser negativa")
    @Column(name = "capacidad_volumen", precision = 10, scale = 2)
    private BigDecimal capacidadVolumen;

    @DecimalMin(value = "0.0", message = "La capacidad de peso no puede ser negativa")
    @Column(name = "capacidad_peso", precision = 10, scale = 2)
    private BigDecimal capacidadPeso;

    @NotNull(message = "La velocidad es obligatoria")
    @DecimalMin(value = "0.1", message = "La velocidad debe ser mayor que 0")
    @Column(name = "velocidad_kmh", nullable = false, precision = 5, scale = 2)
    @Builder.Default
    private BigDecimal velocidadKmh = new BigDecimal("40.0");

    @NotNull(message = "El costo por km es obligatorio")
    @DecimalMin(value = "0.0", message = "El costo por km no puede ser negativo")
    @Column(name = "costo_km", nullable = false, precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal costoKm = new BigDecimal("1.5");

    @Column(nullable = false)
    @Builder.Default
    private Boolean activo = true;

    @NotNull(message = "La latitud del depósito es obligatoria")
    @DecimalMin(value = "-90.0", message = "La latitud debe estar entre -90 y 90")
    @DecimalMax(value = "90.0", message = "La latitud debe estar entre -90 y 90")
    @Column(name = "depot_latitud", nullable = false, precision = 10, scale = 8)
    private BigDecimal depotLatitud;

    @NotNull(message = "La longitud del depósito es obligatoria")
    @DecimalMin(value = "-180.0", message = "La longitud debe estar entre -180 y 180")
    @DecimalMax(value = "180.0", message = "La longitud debe estar entre -180 y 180")
    @Column(name = "depot_longitud", nullable = false, precision = 11, scale = 8)
    private BigDecimal depotLongitud;

    @Column(name = "jornada_inicio")
    @Builder.Default
    private Instant jornadaInicio = LocalDate.now(ZoneId.systemDefault())
            .atTime(LocalTime.of(8, 0))
            .atZone(ZoneId.systemDefault())
            .toInstant();

    @Column(name = "jornada_fin")
    @Builder.Default
    private Instant jornadaFin = LocalDate.now(ZoneId.systemDefault())
            .atTime(LocalTime.of(18, 0))
            .atZone(ZoneId.systemDefault())
            .toInstant();
    
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
        updatedAt = Instant.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
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
