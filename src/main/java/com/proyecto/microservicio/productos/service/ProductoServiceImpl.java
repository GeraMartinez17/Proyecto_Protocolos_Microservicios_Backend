package com.proyecto.microservicio.productos.service;

import com.proyecto.microservicio.exception.ConflictoNegocioException;
import com.proyecto.microservicio.exception.RecursoNoEncontradoException;
import com.proyecto.microservicio.exception.SolicitudInvalidaException;
import com.proyecto.microservicio.productos.dto.ProductoRequest;
import com.proyecto.microservicio.productos.dto.ProductoResponse;
import com.proyecto.microservicio.productos.entity.EstadoProducto;
import com.proyecto.microservicio.productos.entity.Producto;
import com.proyecto.microservicio.productos.repository.ProductoRepository;
import com.proyecto.microservicio.ventas.repository.DetalleVentaRepository;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class ProductoServiceImpl implements ProductoService {

    private final ProductoRepository productoRepository;
    private final DetalleVentaRepository detalleVentaRepository;

    public ProductoServiceImpl(ProductoRepository productoRepository,
                               DetalleVentaRepository detalleVentaRepository) {
        this.productoRepository = productoRepository;
        this.detalleVentaRepository = detalleVentaRepository;
    }

    @Override
    public List<ProductoResponse> listar() {
        return productoRepository.findAll(Sort.by("id")).stream().map(ProductoResponse::desde).toList();
    }

    @Override
    public ProductoResponse obtener(Long id) {
        return ProductoResponse.desde(buscar(id));
    }

    @Override
    public List<ProductoResponse> buscarPorNombre(String nombre) {
        if (nombre == null || nombre.isBlank()) {
            throw new SolicitudInvalidaException("El parámetro 'nombre' es obligatorio");
        }
        return productoRepository.findByNombreContainingIgnoreCaseOrderByNombreAsc(nombre.trim())
                .stream().map(ProductoResponse::desde).toList();
    }

    @Override
    @Transactional
    public ProductoResponse crear(ProductoRequest r) {
        String nombre = r.nombre().trim();
        if (productoRepository.existsByNombreIgnoreCase(nombre)) {
            throw new ConflictoNegocioException("Ya existe un producto con el nombre '" + nombre + "'");
        }
        Producto p = new Producto(nombre, limpiar(r.descripcion()), r.precio(), r.stock(), r.estado());
        return ProductoResponse.desde(productoRepository.saveAndFlush(p));
    }

    @Override
    @Transactional
    public ProductoResponse actualizar(Long id, ProductoRequest r) {
        Producto p = buscar(id);
        String nombre = r.nombre().trim();
        if (productoRepository.existsByNombreIgnoreCaseAndIdNot(nombre, id)) {
            throw new ConflictoNegocioException("Ya existe otro producto con el nombre '" + nombre + "'");
        }
        p.setNombre(nombre);
        p.setDescripcion(limpiar(r.descripcion()));
        p.setPrecio(r.precio());
        p.setStock(r.stock());
        p.setEstado(r.estado() == null ? EstadoProducto.ACTIVO : r.estado());
        return ProductoResponse.desde(productoRepository.saveAndFlush(p));
    }

    @Override
    @Transactional
    public void eliminar(Long id) {
        Producto p = buscar(id);
        if (detalleVentaRepository.existsByProductoId(id)) {
            throw new ConflictoNegocioException("El producto '" + p.getNombre()
                    + "' tiene ventas registradas y no puede eliminarse; márquelo como INACTIVO");
        }
        productoRepository.delete(p);
    }

    private Producto buscar(Long id) {
        return productoRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Producto", id));
    }

    private static String limpiar(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }
}
