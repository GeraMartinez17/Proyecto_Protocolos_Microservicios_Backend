package com.proyecto.microservicio.unit;

import com.proyecto.microservicio.exception.ConflictoNegocioException;
import com.proyecto.microservicio.exception.RecursoNoEncontradoException;
import com.proyecto.microservicio.productos.dto.ProductoRequest;
import com.proyecto.microservicio.productos.dto.ProductoResponse;
import com.proyecto.microservicio.productos.entity.EstadoProducto;
import com.proyecto.microservicio.productos.entity.Producto;
import com.proyecto.microservicio.productos.repository.ProductoRepository;
import com.proyecto.microservicio.productos.service.ProductoServiceImpl;
import com.proyecto.microservicio.ventas.repository.DetalleVentaRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Sort;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Unitarias (Mockito) - ProductoService")
class ProductoServiceTest {

    @Mock ProductoRepository productoRepository;
    @Mock DetalleVentaRepository detalleVentaRepository;
    @InjectMocks ProductoServiceImpl service;

    private static Producto producto(long id, String nombre, int stock) {
        Producto p = new Producto(nombre, "desc", new BigDecimal("100.00"), stock, EstadoProducto.ACTIVO);
        p.setId(id);
        return p;
    }

    private static ProductoRequest req(String nombre) {
        return new ProductoRequest(nombre, " Descripción ", new BigDecimal("50.00"), 10, null);
    }

    @Test
    @DisplayName("Listar devuelve todos los productos")
    void listar() {
        when(productoRepository.findAll(any(Sort.class))).thenReturn(List.of(producto(1, "A", 1), producto(2, "B", 2)));
        assertThat(service.listar()).extracting(ProductoResponse::nombre).containsExactly("A", "B");
    }

    @Test
    @DisplayName("Obtener un producto existente")
    void obtenerExistente() {
        when(productoRepository.findById(1L)).thenReturn(Optional.of(producto(1, "A", 5)));
        assertThat(service.obtener(1L).stock()).isEqualTo(5);
    }

    @Test
    @DisplayName("Obtener un producto inexistente lanza RecursoNoEncontrado")
    void obtenerInexistente() {
        when(productoRepository.findById(99L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.obtener(99L))
                .isInstanceOf(RecursoNoEncontradoException.class).hasMessageContaining("99");
    }

    @Test
    @DisplayName("Crear normaliza espacios, asigna estado ACTIVO y guarda")
    void crear() {
        when(productoRepository.existsByNombreIgnoreCase("Nuevo")).thenReturn(false);
        when(productoRepository.saveAndFlush(any(Producto.class))).thenAnswer(inv -> inv.getArgument(0));
        ProductoResponse r = service.crear(req("  Nuevo  "));
        assertThat(r.nombre()).isEqualTo("Nuevo");
        assertThat(r.descripcion()).isEqualTo("Descripción");
        assertThat(r.estado()).isEqualTo(EstadoProducto.ACTIVO);
    }

    @Test
    @DisplayName("Crear con nombre duplicado lanza Conflicto")
    void crearDuplicado() {
        when(productoRepository.existsByNombreIgnoreCase("A")).thenReturn(true);
        assertThatThrownBy(() -> service.crear(req("A"))).isInstanceOf(ConflictoNegocioException.class);
        verify(productoRepository, never()).saveAndFlush(any());
    }

    @Test
    @DisplayName("Actualizar modifica los campos del producto")
    void actualizar() {
        Producto p = producto(1, "A", 5);
        when(productoRepository.findById(1L)).thenReturn(Optional.of(p));
        when(productoRepository.saveAndFlush(p)).thenReturn(p);
        ProductoResponse r = service.actualizar(1L, req("A2"));
        assertThat(r.nombre()).isEqualTo("A2");
        assertThat(r.precio()).isEqualByComparingTo("50.00");
        assertThat(r.stock()).isEqualTo(10);
    }

    @Test
    @DisplayName("Actualizar un producto inexistente lanza RecursoNoEncontrado")
    void actualizarInexistente() {
        when(productoRepository.findById(99L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.actualizar(99L, req("X"))).isInstanceOf(RecursoNoEncontradoException.class);
    }

    @Test
    @DisplayName("Eliminar un producto sin ventas")
    void eliminar() {
        Producto p = producto(1, "A", 5);
        when(productoRepository.findById(1L)).thenReturn(Optional.of(p));
        when(detalleVentaRepository.existsByProductoId(1L)).thenReturn(false);
        service.eliminar(1L);
        verify(productoRepository).delete(p);
    }

    @Test
    @DisplayName("Eliminar un producto con ventas lanza Conflicto")
    void eliminarConVentas() {
        when(productoRepository.findById(1L)).thenReturn(Optional.of(producto(1, "A", 5)));
        when(detalleVentaRepository.existsByProductoId(1L)).thenReturn(true);
        assertThatThrownBy(() -> service.eliminar(1L)).isInstanceOf(ConflictoNegocioException.class);
        verify(productoRepository, never()).delete(any());
    }

    @Test
    @DisplayName("Buscar por nombre delega en el repositorio")
    void buscar() {
        when(productoRepository.findByNombreContainingIgnoreCaseOrderByNombreAsc("logi"))
                .thenReturn(List.of(producto(2, "Mouse Logitech", 4)));
        assertThat(service.buscarPorNombre(" logi ")).hasSize(1);
    }
}
