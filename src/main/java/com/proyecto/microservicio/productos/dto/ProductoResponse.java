package com.proyecto.microservicio.productos.dto;

import com.proyecto.microservicio.productos.entity.EstadoProducto;
import com.proyecto.microservicio.productos.entity.Producto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ProductoResponse(
        Long id,
        String nombre,
        String descripcion,
        BigDecimal precio,
        Integer stock,
        EstadoProducto estado,
        LocalDateTime fechaCreacion,
        LocalDateTime fechaActualizacion
) {
    public static ProductoResponse desde(Producto p) {
        return new ProductoResponse(p.getId(), p.getNombre(), p.getDescripcion(), p.getPrecio(),
                p.getStock(), p.getEstado(), p.getFechaCreacion(), p.getFechaActualizacion());
    }
}
