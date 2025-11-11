package com.customer.rutaOptima.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Entidad que representa un cliente que recibe entregas
 */
@Entity
@Table(name = "customer", indexes = {
        @Index(name = "idx_customer_es_nuevo", columnList = "es_nuevo"),
        @Index(name = "idx_customer_activo", columnList = "activo"),
        @Index(name = "idx_customer_latitud_longitud", columnList = "latitud, longitud")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Customer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "El nombre del cliente es obligatorio")
    @Column(nullable = false, length = 255)
    private String nombre;

    @NotBlank(message = "La dirección es obligatoria")
    @Column(nullable = false, columnDefinition = "TEXT")
    private String direccion;

    @NotNull(message = "La latitud es obligatoria")
    @DecimalMin(value = "-90.0", message = "La latitud debe estar entre -90 y 90")
    @DecimalMax(value = "90.0", message = "La latitud debe estar entre -90 y 90")
    @Column(nullable = false, precision = 10, scale = 8)
    private BigDecimal latitud;

    @NotNull(message = "La longitud es obligatoria")
    @DecimalMin(value = "-180.0", message = "La longitud debe estar entre -180 y 180")
    @DecimalMax(value = "180.0", message = "La longitud debe estar entre -180 y 180")
    @Column(nullable = false, precision = 11, scale = 8)
    private BigDecimal longitud;

    /**
     * Indica si es un cliente nuevo (requiere 5 días de anticipación para el primer pedido)
     */
    @Column(name = "es_nuevo", nullable = false)
    @Builder.Default
    private Boolean esNuevo = true;

    @Column(name = "ventana_horaria_inicio")
    private Instant ventanaHorariaInicio;

    @Column(name = "ventana_horaria_fin")
    private Instant ventanaHorariaFin;

    /**
     * Demanda promedio semanal (para previsión de temporadas)
     */
    @Column(name = "demanda_promedio_semanal", precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal demandaPromedioSemanal = BigDecimal.ZERO;

    /**
     * Factor de estacionalidad (1.0 = normal, >1.0 = alta temporada, <1.0 = baja temporada)
     */
    @Column(name = "factor_estacionalidad", precision = 5, scale = 2)
    @Builder.Default
    private BigDecimal factorEstacionalidad = BigDecimal.ONE;

    @Column(length = 50)
    private String telefono;

    @Email(message = "El email debe ser válido")
    @Column(length = 255)
    private String email;

    @Column(nullable = false)
    @Builder.Default
    private Boolean activo = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @OneToMany(mappedBy = "customer", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<Order> orders = new ArrayList<>();

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
     * Método auxiliar para verificar si tiene ventana horaria definida
     */
    public boolean tieneVentanaHoraria() {
        return ventanaHorariaInicio != null && ventanaHorariaFin != null;
    }
}
