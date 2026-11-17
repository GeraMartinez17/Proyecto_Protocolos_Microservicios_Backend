package com.proyecto.microservicio.ventas.repository;

import com.proyecto.microservicio.ventas.entity.DetalleVenta;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DetalleVentaRepository extends JpaRepository<DetalleVenta, Long> {

    boolean existsByProductoId(Long productoId);

    @EntityGraph(attributePaths = "producto")
    List<DetalleVenta> findByVentaIdOrderByIdAsc(Long ventaId);
}
