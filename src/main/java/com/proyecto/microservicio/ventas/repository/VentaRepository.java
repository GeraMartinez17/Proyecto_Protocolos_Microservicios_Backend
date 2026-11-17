package com.proyecto.microservicio.ventas.repository;

import com.proyecto.microservicio.ventas.entity.Venta;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface VentaRepository extends JpaRepository<Venta, Long> {

    /** EntityGraph: carga detalles y productos en una sola consulta (evita N+1). */
    @EntityGraph(attributePaths = {"detalles", "detalles.producto"})
    @Query("select v from Venta v order by v.fecha desc, v.id desc")
    List<Venta> findAllConDetalles();

    @EntityGraph(attributePaths = {"detalles", "detalles.producto"})
    @Query("select v from Venta v where v.id = :id")
    Optional<Venta> findConDetallesById(Long id);

    @EntityGraph(attributePaths = {"detalles", "detalles.producto"})
    @Query("select v from Venta v where v.fecha >= :desde and v.fecha < :hasta order by v.fecha asc, v.id asc")
    List<Venta> findEntreFechas(LocalDateTime desde, LocalDateTime hasta);
}
