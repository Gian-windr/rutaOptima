package com.customer.rutaOptima.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * Entidad que representa una parada individual en una ruta
 */
@Entity
@Table(name = "route_stop", indexes = {
        @Index(name = "idx_route_stop_route_plan", columnList = "route_plan_id"),
        @Index(name = "idx_route_stop_vehicle", columnList = "vehicle_id"),
        @Index(name = "idx_route_stop_order", columnList = "order_id"),
        @Index(name = "idx_route_stop_route_vehicle_seq", columnList = "route_plan_id, vehicle_id, secuencia")
}, uniqueConstraints = {
        @UniqueConstraint(name = "idx_route_stop_unique_order_per_plan", columnNames = {"route_plan_id", "order_id"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RouteStop {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "route_plan_id", nullable = false, foreignKey = @ForeignKey(name = "fk_route_stop_route_plan"))
    @NotNull(message = "El plan de ruta es obligatorio")
    private RoutePlan routePlan;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vehicle_id", nullable = false, foreignKey = @ForeignKey(name = "fk_route_stop_vehicle"))
    @NotNull(message = "El vehículo es obligatorio")
    private Vehicle vehicle;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false, foreignKey = @ForeignKey(name = "fk_route_stop_order"))
    @NotNull(message = "El pedido es obligatorio")
    private Order order;

    @NotNull(message = "La secuencia es obligatoria")
    @Min(value = 1, message = "La secuencia debe ser al menos 1")
    @Column(nullable = false)
    private Integer secuencia;

    /**
     * Estimated Time of Arrival - hora estimada de llegada
     */
    @Column
    private LocalDateTime eta;

    /**
     * Estimated Time of Departure - hora estimada de salida
     */
    @Column
    private LocalDateTime etd;

    @Column(name = "distancia_km_desde_anterior", precision = 10, scale = 2)
    private BigDecimal distanciaKmDesdeAnterior;

    @Column(name = "tiempo_viaje_min_desde_anterior")
    private Integer tiempoViajeMínDesdeAnterior;

    @Column(name = "carga_acumulada_cantidad", precision = 10, scale = 2)
    private BigDecimal cargaAcumuladaCantidad;

    @Column(name = "carga_acumulada_volumen", precision = 10, scale = 2)
    private BigDecimal cargaAcumuladaVolumen;

    @Column(name = "carga_acumulada_peso", precision = 10, scale = 2)
    private BigDecimal cargaAcumuladaPeso;

    @Column(name = "tiempo_espera_min")
    @Builder.Default
    private Integer tiempoEsperaMin = 0;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    /**
     * Verifica si esta parada viola la ventana horaria
     */
    public boolean violatesTimeWindow() {
        if (eta == null || order == null) {
            return false;
        }

        var ventanaInicio = order.getVentanaHorariaInicioEfectiva();
        var ventanaFin = order.getVentanaHorariaFinEfectiva();

        if (ventanaInicio == null || ventanaFin == null) {
            return false;
        }

        var horaLlegada = eta.toLocalTime();
        return horaLlegada.isBefore(LocalTime.from(ventanaInicio)) || horaLlegada.isAfter(LocalTime.from(ventanaFin));
    }

    /**
     * Calcula los minutos de violación de ventana horaria
     */
    public int getTimeWindowViolationMinutes() {
        if (eta == null || order == null) {
            return 0;
        }

        var ventanaInicio = order.getVentanaHorariaInicioEfectiva();
        var ventanaFin = order.getVentanaHorariaFinEfectiva();

        if (ventanaInicio == null || ventanaFin == null) {
            return 0;
        }

        var horaLlegada = eta.toLocalTime();

        if (horaLlegada.isBefore(LocalTime.from(ventanaInicio))) {
            return (int) java.time.Duration.between(horaLlegada, ventanaInicio).toMinutes();
        } else if (horaLlegada.isAfter(LocalTime.from(ventanaFin))) {
            return (int) java.time.Duration.between(ventanaFin, horaLlegada).toMinutes();
        }

        return 0;
    }
}
