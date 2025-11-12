package com.customer.rutaOptima.api.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/**
 * DTO para solicitar la optimización de rutas
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OptimizeRouteRequest {

    @NotBlank(message = "La fecha es obligatoria")
    @Pattern(regexp = "^\\d{4}-\\d{2}-\\d{2}$", message = "La fecha debe tener formato YYYY-MM-DD")
    private String fecha;

    @NotEmpty(message = "Debe especificar al menos un vehículo")
    private List<Long> vehicleIds;

    @NotBlank(message = "El objetivo es obligatorio")
    @Pattern(regexp = "MINIMIZE_DISTANCE|MINIMIZE_TIME|MINIMIZE_COST",
            message = "El objetivo debe ser MINIMIZE_DISTANCE, MINIMIZE_TIME o MINIMIZE_COST")
    private String objective;

    @Builder.Default
    private Boolean allowSoftTimeWindowViolations = false;

    @Min(value = 5, message = "El tiempo mínimo de optimización es 5 segundos")
    @Max(value = 300, message = "El tiempo máximo de optimización es 300 segundos")
    @Builder.Default
    private Integer maxOptimizationTimeSeconds = 20;
}
