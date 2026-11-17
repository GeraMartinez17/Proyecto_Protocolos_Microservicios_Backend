package com.proyecto.microservicio.ventas.service;

import com.proyecto.microservicio.ventas.dto.DetalleVentaResponse;
import com.proyecto.microservicio.ventas.dto.VentaRequest;
import com.proyecto.microservicio.ventas.dto.VentaResponse;

import java.time.LocalDate;
import java.util.List;

public interface VentaService {
    List<VentaResponse> listar();
    VentaResponse obtener(Long id);
    List<DetalleVentaResponse> detalle(Long id);
    List<VentaResponse> porFecha(LocalDate inicio, LocalDate fin);
    VentaResponse registrar(VentaRequest request);
    void cancelar(Long id);
}
