package com.customer.rutaOptima.domain;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

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
    private Instant fechaEntrega;

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
    private Instant ventanaHorariaInicio;

    @Column(name = "ventana_horaria_fin")
    private Instant ventanaHorariaFin;

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
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
        updatedAt = Instant.now();
        if (estado == null) {
            estado = "PENDIENTE";
        }
        
        // Validar que la fecha de entrega sea al menos 3 días después
        if (fechaEntrega != null) {
            Instant minimoFecha = Instant.now().plus(3, java.time.temporal.ChronoUnit.DAYS);
            if (fechaEntrega.isBefore(minimoFecha)) {
                throw new IllegalArgumentException(
                    "La fecha de entrega debe ser al menos 3 días después de la fecha actual"
                );
            }
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
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
    public Instant getVentanaHorariaInicioEfectiva() {
        if (ventanaHorariaInicio != null) {
            return ventanaHorariaInicio;
        }
        return resolveCustomerTime("getVentanaHorariaInicio");
    }

    public Instant getVentanaHorariaFinEfectiva() {
        if (ventanaHorariaFin != null) {
            return ventanaHorariaFin;
        }
        return resolveCustomerTime("getVentanaHorariaFin");
    }

    private Instant resolveCustomerTime(String getterName) {
        if (customer == null) {
            return null;
        }
        try {
            Method m = customer.getClass().getMethod(getterName);
            Object val = m.invoke(customer);
            if (val == null) {
                return null;
            }
            // Si el customer ya devuelve Instant
            if (val instanceof Instant instant) {
                return instant;
            }
            // Si devuelve LocalDateTime
            if (val instanceof LocalDateTime localDateTime) {
                return localDateTime.atZone(ZoneId.systemDefault()).toInstant();
            }
            // Si devuelve LocalDate (usar inicio del día)
            if (val instanceof LocalDate localDate) {
                return localDate.atStartOfDay(ZoneId.systemDefault()).toInstant();
            }
            // Si devuelve LocalTime, combinar con la fecha de entrega (o hoy)
            if (val instanceof LocalTime localTime) {
                LocalDate date = (fechaEntrega != null)
                        ? LocalDateTime.ofInstant(fechaEntrega, ZoneId.systemDefault()).toLocalDate()
                        : LocalDate.now(ZoneId.systemDefault());
                LocalDateTime ldt = LocalDateTime.of(date, (LocalTime) val);
                return ldt.atZone(ZoneId.systemDefault()).toInstant();
            }
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            // Si falla la resolución por reflexión, devolver null silenciosamente
        }
        return null;
    }
}
