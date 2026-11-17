package com.proyecto.microservicio.ventas.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

@Schema(description = "Venta a registrar. El precio, los subtotales y el total los calcula el servidor")
public record VentaRequest(
        @NotEmpty(message = "La venta debe incluir al menos un producto")
        List<@Valid @NotNull(message = "La partida no puede ser nula") DetalleVentaRequest> productos
) {
}
