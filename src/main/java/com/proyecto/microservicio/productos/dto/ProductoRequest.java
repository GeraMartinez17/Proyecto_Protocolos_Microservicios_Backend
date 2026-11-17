package com.proyecto.microservicio.productos.dto;

import com.proyecto.microservicio.productos.entity.EstadoProducto;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;

@Schema(description = "Datos para registrar o actualizar un producto")
public record ProductoRequest(

        @Schema(example = "Teclado inalámbrico Logitech K380")
        @NotBlank(message = "El nombre es obligatorio")
        @Size(max = 120, message = "El nombre no puede exceder 120 caracteres")
        String nombre,

        @Schema(example = "Teclado Bluetooth multidispositivo")
        @Size(max = 500, message = "La descripción no puede exceder 500 caracteres")
        String descripcion,

        @Schema(example = "649.00")
        @NotNull(message = "El precio es obligatorio")
        @DecimalMin(value = "0.00", message = "El precio no puede ser negativo")
        @Digits(integer = 8, fraction = 2, message = "El precio admite hasta 8 enteros y 2 decimales")
        BigDecimal precio,

        @Schema(example = "25")
        @NotNull(message = "El stock es obligatorio")
        @Min(value = 0, message = "El stock no puede ser negativo")
        @Max(value = 1_000_000, message = "El stock no puede exceder 1,000,000 unidades")
        Integer stock,

        @Schema(example = "ACTIVO", description = "ACTIVO por defecto")
        EstadoProducto estado
) {
}
