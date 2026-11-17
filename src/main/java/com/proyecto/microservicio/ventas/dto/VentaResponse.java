package com.proyecto.microservicio.ventas.dto;

import com.proyecto.microservicio.ventas.entity.EstadoVenta;
import com.proyecto.microservicio.ventas.entity.Venta;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record VentaResponse(
        Long id,
        LocalDateTime fecha,
        BigDecimal total,
        EstadoVenta estado,
        LocalDateTime fechaCancelacion,
        int totalArticulos,
        List<DetalleVentaResponse> detalles
) {
    public static VentaResponse desde(Venta v) {
        return new VentaResponse(v.getId(), v.getFecha(), v.getTotal(), v.getEstado(), v.getFechaCancelacion(),
                v.getTotalArticulos(), v.getDetalles().stream().map(DetalleVentaResponse::desde).toList());
    }
}
