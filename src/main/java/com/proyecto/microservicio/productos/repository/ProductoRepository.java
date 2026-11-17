package com.proyecto.microservicio.productos.repository;

import com.proyecto.microservicio.productos.entity.Producto;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface ProductoRepository extends JpaRepository<Producto, Long> {

    List<Producto> findByNombreContainingIgnoreCaseOrderByNombreAsc(String nombre);

    boolean existsByNombreIgnoreCase(String nombre);

    boolean existsByNombreIgnoreCaseAndIdNot(String nombre, Long id);

    /**
     * Bloqueo pesimista (SELECT ... FOR UPDATE) de los productos de una venta.
     * Se ordenan por id para que todas las transacciones adquieran los bloqueos
     * en el mismo orden y se eviten interbloqueos (deadlocks).
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Producto p where p.id in :ids order by p.id")
    List<Producto> findAllByIdParaActualizar(@Param("ids") Collection<Long> ids);
}
