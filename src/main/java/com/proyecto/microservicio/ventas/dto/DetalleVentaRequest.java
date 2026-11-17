package com.proyecto.microservicio.ventas.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

@Schema(description = "Producto y cantidad a vender")
public record DetalleVentaRequest(
        @Schema(example = "2")
        @NotNull(message = "El id del producto es obligatorio")
        Long productoId,

        @Schema(example = "3")
        @NotNull(message = "La cantidad es obligatoria")
        @Positive(message = "La cantidad debe ser mayor que cero")
        @Max(value = 10_000, message = "La cantidad no puede exceder 10,000 unidades por partida")
        Integer cantidad
) {
}
