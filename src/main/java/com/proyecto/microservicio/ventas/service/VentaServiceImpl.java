package com.proyecto.microservicio.ventas.service;

import com.proyecto.microservicio.exception.ConflictoNegocioException;
import com.proyecto.microservicio.exception.RecursoNoEncontradoException;
import com.proyecto.microservicio.exception.SolicitudInvalidaException;
import com.proyecto.microservicio.productos.entity.EstadoProducto;
import com.proyecto.microservicio.productos.entity.Producto;
import com.proyecto.microservicio.productos.repository.ProductoRepository;
import com.proyecto.microservicio.ventas.dto.DetalleVentaRequest;
import com.proyecto.microservicio.ventas.dto.DetalleVentaResponse;
import com.proyecto.microservicio.ventas.dto.VentaRequest;
import com.proyecto.microservicio.ventas.dto.VentaResponse;
import com.proyecto.microservicio.ventas.entity.DetalleVenta;
import com.proyecto.microservicio.ventas.entity.EstadoVenta;
import com.proyecto.microservicio.ventas.entity.Venta;
import com.proyecto.microservicio.ventas.repository.VentaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class VentaServiceImpl implements VentaService {

    private final VentaRepository ventaRepository;
    private final ProductoRepository productoRepository;
    private final Clock clock;

    public VentaServiceImpl(VentaRepository ventaRepository, ProductoRepository productoRepository, Clock clock) {
        this.ventaRepository = ventaRepository;
        this.productoRepository = productoRepository;
        this.clock = clock;
    }

    @Override
    public List<VentaResponse> listar() {
        return ventaRepository.findAllConDetalles().stream().map(VentaResponse::desde).toList();
    }

    @Override
    public VentaResponse obtener(Long id) {
        return VentaResponse.desde(buscar(id));
    }

    @Override
    public List<DetalleVentaResponse> detalle(Long id) {
        return buscar(id).getDetalles().stream().map(DetalleVentaResponse::desde).toList();
    }

    @Override
    public List<VentaResponse> porFecha(LocalDate inicio, LocalDate fin) {
        if (inicio.isAfter(fin)) {
            throw new SolicitudInvalidaException("La fecha de inicio no puede ser posterior a la fecha fin");
        }
        return ventaRepository.findEntreFechas(inicio.atStartOfDay(), fin.plusDays(1).atStartOfDay())
                .stream().map(VentaResponse::desde).toList();
    }

    /**
     * Registra una venta de forma atómica:
     * 1) bloquea (SELECT ... FOR UPDATE) las filas de los productos involucrados,
     * 2) valida existencia, estado y stock de TODOS los productos,
     * 3) crea las partidas, calcula subtotales y total y descuenta el inventario.
     * Cualquier excepción provoca rollback: no quedan modificaciones parciales.
     */
    @Override
    @Transactional
    public VentaResponse registrar(VentaRequest request) {
        if (request == null || request.productos() == null || request.productos().isEmpty()) {
            throw new SolicitudInvalidaException("La venta debe incluir al menos un producto");
        }

        // Si un producto aparece varias veces en la solicitud se suman sus cantidades.
        Map<Long, Integer> cantidades = new LinkedHashMap<>();
        for (DetalleVentaRequest d : request.productos()) {
            if (d == null || d.productoId() == null || d.cantidad() == null) {
                throw new SolicitudInvalidaException("Cada partida requiere productoId y cantidad");
            }
            if (d.cantidad() <= 0) {
                throw new SolicitudInvalidaException("La cantidad debe ser mayor que cero");
            }
            cantidades.merge(d.productoId(), d.cantidad(), Math::addExact);
        }

        Map<Long, Producto> productos = productoRepository.findAllByIdParaActualizar(cantidades.keySet())
                .stream().collect(Collectors.toMap(Producto::getId, Function.identity()));

        for (Map.Entry<Long, Integer> e : cantidades.entrySet()) {
            Producto p = productos.get(e.getKey());
            if (p == null) {
                throw new RecursoNoEncontradoException("Producto", e.getKey());
            }
            if (p.getEstado() != EstadoProducto.ACTIVO) {
                throw new ConflictoNegocioException("El producto '" + p.getNombre() + "' está inactivo y no puede venderse");
            }
            if (e.getValue() > p.getStock()) {
                throw new ConflictoNegocioException("Stock insuficiente para '" + p.getNombre()
                        + "': disponible " + p.getStock() + ", solicitado " + e.getValue());
            }
        }

        Venta venta = new Venta(ahora());
        cantidades.forEach((id, cantidad) -> {
            Producto p = productos.get(id);
            venta.agregarDetalle(p, cantidad, p.getPrecio());
            p.descontarStock(cantidad);
        });
        venta.recalcularTotal();

        return VentaResponse.desde(ventaRepository.saveAndFlush(venta));
    }

    /**
     * Cancela una venta (DELETE lógico): cambia su estado a CANCELADA y repone el inventario.
     * La venta se conserva para fines de auditoría.
     */
    @Override
    @Transactional
    public void cancelar(Long id) {
        Venta venta = buscar(id);
        if (venta.getEstado() == EstadoVenta.CANCELADA) {
            throw new ConflictoNegocioException("La venta " + id + " ya está cancelada");
        }
        Set<Long> ids = venta.getDetalles().stream().map(d -> d.getProducto().getId())
                .collect(Collectors.toCollection(TreeSet::new));
        Map<Long, Producto> productos = productoRepository.findAllByIdParaActualizar(ids)
                .stream().collect(Collectors.toMap(Producto::getId, Function.identity()));
        for (DetalleVenta d : venta.getDetalles()) {
            productos.get(d.getProducto().getId()).reponerStock(d.getCantidad());
        }
        venta.cancelar(ahora());
    }

    /** Hora actual truncada a microsegundos: la misma precisión que DATETIME(6) en MySQL. */
    private LocalDateTime ahora() {
        return LocalDateTime.now(clock).truncatedTo(ChronoUnit.MICROS);
    }

    private Venta buscar(Long id) {
        return ventaRepository.findConDetallesById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Venta", id));
    }
}
