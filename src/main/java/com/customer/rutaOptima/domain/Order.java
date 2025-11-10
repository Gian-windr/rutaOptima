package com.customer.rutaOptima.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * Entidad que representa un pedido a entregar
 */
@Entity
@Table(name = "orders", indexes = {
        @Index(name = "idx_orders_customer_id", columnList = "customer_id"),
        @Index(name = "idx_orders_fecha_entrega", columnList = "fecha_entrega"),
        @Index(name = "idx_orders_estado", columnList = "estado"),
        @Index(name = "idx_orders_fecha_customer", columnList = "fecha_entrega, customer_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false, foreignKey = @ForeignKey(name = "fk_order_customer"))
    @NotNull(message = "El cliente es obligatorio")
    private Customer customer;

    @NotNull(message = "La fecha de entrega es obligatoria")
    @FutureOrPresent(message = "La fecha de entrega no puede ser en el pasado")
    @Column(name = "fecha_entrega", nullable = false)
    private LocalDate fechaEntrega;

    @NotNull(message = "La cantidad es obligatoria")
    @DecimalMin(value = "0.01", message = "La cantidad debe ser mayor que 0")
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal cantidad;

    @DecimalMin(value = "0.0", message = "El volumen no puede ser negativo")
    @Column(precision = 10, scale = 2)
    private BigDecimal volumen;

    @DecimalMin(value = "0.0", message = "El peso no puede ser negativo")
    @Column(precision = 10, scale = 2)
    private BigDecimal peso;

    @NotBlank(message = "El estado es obligatorio")
    @Column(nullable = false, length = 50)
    @Builder.Default
    private String estado = "PENDIENTE";

    @Column(name = "ventana_horaria_inicio")
    private LocalTime ventanaHorariaInicio;

    @Column(name = "ventana_horaria_fin")
    private LocalTime ventanaHorariaFin;

    @Min(value = 1, message = "La prioridad debe ser al menos 1")
    @Column
    @Builder.Default
    private Integer prioridad = 1;

    @Min(value = 0, message = "El tiempo de servicio no puede ser negativo")
    @Column(name = "tiempo_servicio_estimado_min")
    @Builder.Default
    private Integer tiempoServicioEstimadoMin = 10; // Tiempo estimado de descarga en minutos

    @Column(columnDefinition = "TEXT")
    private String notas;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (estado == null) {
            estado = "PENDIENTE";
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    /**
     * Verifica si el pedido tiene ventana horaria definida
     */
    public boolean tieneVentanaHoraria() {
        return ventanaHorariaInicio != null && ventanaHorariaFin != null;
    }

    /**
     * Usa la ventana horaria del pedido, si no existe usa la del cliente
     */
    public LocalDateTime getVentanaHorariaInicioEfectiva() {
        if (ventanaHorariaInicio != null) {
            return ventanaHorariaInicio.atDate(LocalDate.now()); // Convierte LocalTime a LocalDateTime
        }
        if (customer != null && customer.getVentanaHorariaInicio() != null) {
            return customer.getVentanaHorariaInicio().atDate(LocalDate.now()); // Convierte LocalTime a LocalDateTime
        }
        return null;
    }

    public LocalDateTime getVentanaHorariaFinEfectiva() {
        if (ventanaHorariaFin != null) {
            return ventanaHorariaFin.atDate(LocalDate.now()); // Convierte LocalTime a LocalDateTime
        }
        if (customer != null && customer.getVentanaHorariaFin() != null) {
            return customer.getVentanaHorariaFin().atDate(LocalDate.now()); // Convierte LocalTime a LocalDateTime
        }
        return null;
    }
}
