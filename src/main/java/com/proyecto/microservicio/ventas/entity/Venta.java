package com.proyecto.microservicio.ventas.entity;

import com.proyecto.microservicio.productos.entity.Producto;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "ventas")
public class Venta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private LocalDateTime fecha;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal total = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoVenta estado = EstadoVenta.REGISTRADA;

    @Column(name = "fecha_cancelacion")
    private LocalDateTime fechaCancelacion;

    @OneToMany(mappedBy = "venta", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id ASC")
    private List<DetalleVenta> detalles = new ArrayList<>();

    protected Venta() {
    }

    public Venta(LocalDateTime fecha) {
        this.fecha = fecha;
    }

    /** Agrega una partida; su subtotal se calcula en DetalleVenta. */
    public DetalleVenta agregarDetalle(Producto producto, int cantidad, BigDecimal precioUnitario) {
        DetalleVenta d = new DetalleVenta(this, producto, cantidad, precioUnitario);
        detalles.add(d);
        recalcularTotal();
        return d;
    }

    /** Total = suma de (cantidad_i * precioUnitario_i). */
    public void recalcularTotal() {
        this.total = detalles.stream()
                .map(DetalleVenta::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public void cancelar(LocalDateTime cuando) {
        this.estado = EstadoVenta.CANCELADA;
        this.fechaCancelacion = cuando;
    }

    public int getTotalArticulos() {
        return detalles.stream().mapToInt(DetalleVenta::getCantidad).sum();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public LocalDateTime getFecha() { return fecha; }
    public BigDecimal getTotal() { return total; }
    public EstadoVenta getEstado() { return estado; }
    public LocalDateTime getFechaCancelacion() { return fechaCancelacion; }
    public List<DetalleVenta> getDetalles() { return detalles; }
}
