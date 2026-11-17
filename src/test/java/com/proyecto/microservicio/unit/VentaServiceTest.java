package com.proyecto.microservicio.unit;

import com.proyecto.microservicio.exception.ConflictoNegocioException;
import com.proyecto.microservicio.exception.RecursoNoEncontradoException;
import com.proyecto.microservicio.exception.SolicitudInvalidaException;
import com.proyecto.microservicio.productos.entity.EstadoProducto;
import com.proyecto.microservicio.productos.entity.Producto;
import com.proyecto.microservicio.productos.repository.ProductoRepository;
import com.proyecto.microservicio.ventas.dto.DetalleVentaRequest;
import com.proyecto.microservicio.ventas.dto.VentaRequest;
import com.proyecto.microservicio.ventas.dto.VentaResponse;
import com.proyecto.microservicio.ventas.entity.EstadoVenta;
import com.proyecto.microservicio.ventas.entity.Venta;
import com.proyecto.microservicio.ventas.repository.VentaRepository;
import com.proyecto.microservicio.ventas.service.VentaServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.*;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Unitarias (Mockito) - VentaService")
class VentaServiceTest {

    @Mock VentaRepository ventaRepository;
    @Mock ProductoRepository productoRepository;
    VentaServiceImpl service;

    final Clock reloj = Clock.fixed(Instant.parse("2026-11-16T18:00:00Z"), ZoneId.of("America/Mexico_City"));
    Producto laptop;
    Producto mouse;

    @BeforeEach
    void setUp() {
        service = new VentaServiceImpl(ventaRepository, productoRepository, reloj);
        laptop = producto(1, "Laptop", "11499.00", 8);
        mouse = producto(2, "Mouse", "249.00", 45);
    }

    private static Producto producto(long id, String nombre, String precio, int stock) {
        Producto p = new Producto(nombre, null, new BigDecimal(precio), stock, EstadoProducto.ACTIVO);
        p.setId(id);
        return p;
    }

    private static VentaRequest venta(DetalleVentaRequest... d) {
        return new VentaRequest(List.of(d));
    }

    @Test
    @DisplayName("Registrar calcula subtotales y total y descuenta inventario")
    void registrar() {
        when(productoRepository.findAllByIdParaActualizar(anyCollection())).thenReturn(List.of(laptop, mouse));
        when(ventaRepository.saveAndFlush(any(Venta.class))).thenAnswer(inv -> inv.getArgument(0));

        VentaResponse r = service.registrar(venta(new DetalleVentaRequest(1L, 1), new DetalleVentaRequest(2L, 2)));

        assertThat(r.detalles()).extracting(d -> d.subtotal().toPlainString()).containsExactly("11499.00", "498.00");
        assertThat(r.total()).isEqualByComparingTo("11997.00");
        assertThat(r.fecha()).isEqualTo(LocalDateTime.of(2026, 11, 16, 12, 0));
        assertThat(laptop.getStock()).isEqualTo(7);
        assertThat(mouse.getStock()).isEqualTo(43);
    }

    @Test
    @DisplayName("Partidas repetidas del mismo producto se suman")
    void partidasRepetidas() {
        when(productoRepository.findAllByIdParaActualizar(anyCollection())).thenReturn(List.of(mouse));
        when(ventaRepository.saveAndFlush(any(Venta.class))).thenAnswer(inv -> inv.getArgument(0));
        VentaResponse r = service.registrar(venta(new DetalleVentaRequest(2L, 1), new DetalleVentaRequest(2L, 3)));
        assertThat(r.detalles()).hasSize(1);
        assertThat(r.detalles().getFirst().cantidad()).isEqualTo(4);
        assertThat(mouse.getStock()).isEqualTo(41);
    }

    @Test
    @DisplayName("Venta sin productos lanza SolicitudInvalida")
    void sinProductos() {
        assertThatThrownBy(() -> service.registrar(new VentaRequest(List.of())))
                .isInstanceOf(SolicitudInvalidaException.class);
        verifyNoInteractions(productoRepository, ventaRepository);
    }

    @Test
    @DisplayName("Cantidad <= 0 lanza SolicitudInvalida")
    void cantidadCero() {
        assertThatThrownBy(() -> service.registrar(venta(new DetalleVentaRequest(1L, 0))))
                .isInstanceOf(SolicitudInvalidaException.class);
    }

    @Test
    @DisplayName("Producto inexistente lanza RecursoNoEncontrado y no guarda")
    void productoInexistente() {
        when(productoRepository.findAllByIdParaActualizar(anyCollection())).thenReturn(List.of(laptop));
        assertThatThrownBy(() -> service.registrar(venta(new DetalleVentaRequest(1L, 1), new DetalleVentaRequest(99L, 1))))
                .isInstanceOf(RecursoNoEncontradoException.class).hasMessageContaining("99");
        assertThat(laptop.getStock()).isEqualTo(8);
        verify(ventaRepository, never()).saveAndFlush(any());
    }

    @Test
    @DisplayName("Stock insuficiente lanza Conflicto sin tocar el inventario de ningún producto")
    void stockInsuficiente() {
        when(productoRepository.findAllByIdParaActualizar(anyCollection())).thenReturn(List.of(laptop, mouse));
        assertThatThrownBy(() -> service.registrar(venta(new DetalleVentaRequest(2L, 5), new DetalleVentaRequest(1L, 9))))
                .isInstanceOf(ConflictoNegocioException.class).hasMessageContaining("Stock insuficiente");
        assertThat(mouse.getStock()).isEqualTo(45);
        assertThat(laptop.getStock()).isEqualTo(8);
        verify(ventaRepository, never()).saveAndFlush(any());
    }

    @Test
    @DisplayName("Producto inactivo lanza Conflicto")
    void productoInactivo() {
        laptop.setEstado(EstadoProducto.INACTIVO);
        when(productoRepository.findAllByIdParaActualizar(anyCollection())).thenReturn(List.of(laptop));
        assertThatThrownBy(() -> service.registrar(venta(new DetalleVentaRequest(1L, 1))))
                .isInstanceOf(ConflictoNegocioException.class).hasMessageContaining("inactivo");
    }

    @Test
    @DisplayName("Cancelar repone el inventario y marca la venta como CANCELADA")
    void cancelar() {
        Venta v = new Venta(LocalDateTime.now(reloj));
        v.setId(5L);
        v.agregarDetalle(mouse, 3, mouse.getPrecio());
        when(ventaRepository.findConDetallesById(5L)).thenReturn(Optional.of(v));
        when(productoRepository.findAllByIdParaActualizar(anyCollection())).thenReturn(List.of(mouse));

        service.cancelar(5L);

        assertThat(v.getEstado()).isEqualTo(EstadoVenta.CANCELADA);
        assertThat(v.getFechaCancelacion()).isNotNull();
        assertThat(mouse.getStock()).isEqualTo(48);
    }

    @Test
    @DisplayName("Cancelar una venta ya cancelada lanza Conflicto")
    void cancelarDosVeces() {
        Venta v = new Venta(LocalDateTime.now(reloj));
        v.cancelar(LocalDateTime.now(reloj));
        when(ventaRepository.findConDetallesById(5L)).thenReturn(Optional.of(v));
        assertThatThrownBy(() -> service.cancelar(5L)).isInstanceOf(ConflictoNegocioException.class);
    }

    @Test
    @DisplayName("Consultar por fecha con inicio > fin lanza SolicitudInvalida")
    void fechasInvertidas() {
        assertThatThrownBy(() -> service.porFecha(LocalDate.of(2026, 11, 10), LocalDate.of(2026, 11, 1)))
                .isInstanceOf(SolicitudInvalidaException.class);
    }

    @Test
    @DisplayName("Consultar por fecha usa [inicio 00:00, fin+1 00:00)")
    void rangoFechas() {
        when(ventaRepository.findEntreFechas(any(), any())).thenReturn(List.of());
        service.porFecha(LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 7));
        verify(ventaRepository).findEntreFechas(LocalDateTime.of(2026, 11, 1, 0, 0), LocalDateTime.of(2026, 11, 8, 0, 0));
    }
}
