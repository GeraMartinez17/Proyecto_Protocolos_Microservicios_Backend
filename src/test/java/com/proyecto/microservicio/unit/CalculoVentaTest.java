package com.proyecto.microservicio.unit;

import com.proyecto.microservicio.productos.entity.EstadoProducto;
import com.proyecto.microservicio.productos.entity.Producto;
import com.proyecto.microservicio.ventas.entity.DetalleVenta;
import com.proyecto.microservicio.ventas.entity.Venta;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Unitarias - cálculo de subtotales, total e inventario")
class CalculoVentaTest {

    @Test
    @DisplayName("Subtotal = cantidad x precio unitario")
    void subtotal() {
        assertThat(DetalleVenta.calcularSubtotal(3, new BigDecimal("139.00"))).isEqualByComparingTo("417.00");
        assertThat(DetalleVenta.calcularSubtotal(7, new BigDecimal("0.15"))).isEqualByComparingTo("1.05");
    }

    @Test
    @DisplayName("Total = suma de subtotales")
    void total() {
        Venta v = new Venta(LocalDateTime.now());
        v.agregarDetalle(producto(10), 1, new BigDecimal("11499.00"));
        v.agregarDetalle(producto(10), 2, new BigDecimal("249.00"));
        assertThat(v.getTotal()).isEqualByComparingTo("11997.00");
        assertThat(v.getTotalArticulos()).isEqualTo(3);
    }

    @Test
    @DisplayName("Una partida con cantidad <= 0 se rechaza")
    void cantidadInvalida() {
        Venta v = new Venta(LocalDateTime.now());
        assertThatThrownBy(() -> v.agregarDetalle(producto(10), 0, BigDecimal.TEN))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("El stock no puede quedar negativo")
    void stockNoNegativo() {
        Producto p = producto(2);
        assertThatThrownBy(() -> p.descontarStock(3)).isInstanceOf(IllegalStateException.class);
        assertThat(p.getStock()).isEqualTo(2);
        p.descontarStock(2);
        assertThat(p.getStock()).isZero();
        p.reponerStock(5);
        assertThat(p.getStock()).isEqualTo(5);
    }

    private static Producto producto(int stock) {
        return new Producto("P", null, BigDecimal.ONE, stock, EstadoProducto.ACTIVO);
    }
}
