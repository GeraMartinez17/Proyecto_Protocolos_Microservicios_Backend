package com.proyecto.microservicio.productos.service;

import com.proyecto.microservicio.productos.dto.ProductoRequest;
import com.proyecto.microservicio.productos.dto.ProductoResponse;

import java.util.List;

public interface ProductoService {
    List<ProductoResponse> listar();
    ProductoResponse obtener(Long id);
    List<ProductoResponse> buscarPorNombre(String nombre);
    ProductoResponse crear(ProductoRequest request);
    ProductoResponse actualizar(Long id, ProductoRequest request);
    void eliminar(Long id);
}
