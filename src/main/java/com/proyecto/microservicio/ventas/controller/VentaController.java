package com.proyecto.microservicio.ventas.controller;

import com.proyecto.microservicio.ventas.dto.DetalleVentaResponse;
import com.proyecto.microservicio.ventas.dto.VentaRequest;
import com.proyecto.microservicio.ventas.dto.VentaResponse;
import com.proyecto.microservicio.ventas.service.VentaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import org.springframework.http.ProblemDetail;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/ventas")
@Tag(name = "Ventas", description = "Registro, consulta y cancelación de ventas")
public class VentaController {

    private final VentaService service;

    public VentaController(VentaService service) {
        this.service = service;
    }

    @GetMapping
    @Operation(summary = "Consultar todas las ventas (más recientes primero)")
    public List<VentaResponse> listar() {
        return service.listar();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Consultar una venta")
    @ApiResponse(responseCode = "200", description = "Venta encontrada")
    @ApiResponse(responseCode = "404", description = "La venta no existe", content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    public VentaResponse obtener(@PathVariable Long id) {
        return service.obtener(id);
    }

    @GetMapping("/{id}/detalle")
    @Operation(summary = "Consultar las partidas (detalle) de una venta")
    @ApiResponse(responseCode = "404", description = "La venta no existe", content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    public List<DetalleVentaResponse> detalle(@PathVariable Long id) {
        return service.detalle(id);
    }

    @GetMapping("/fecha")
    @Operation(summary = "Consultar ventas en un intervalo de fechas (ambos días incluidos)")
    @ApiResponse(responseCode = "400", description = "Fechas ausentes, mal formadas o inicio > fin", content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    public List<VentaResponse> porFecha(
            @Parameter(example = "2026-11-01") @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate inicio,
            @Parameter(example = "2026-11-30") @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fin) {
        return service.porFecha(inicio, fin);
    }

    @PostMapping
    @Operation(summary = "Registrar una venta (descuenta inventario de forma atómica)")
    @ApiResponse(responseCode = "201", description = "Venta registrada")
    @ApiResponse(responseCode = "400", description = "Sin productos o cantidades inválidas", content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "404", description = "Algún producto no existe", content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "409", description = "Stock insuficiente o producto inactivo", content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    public ResponseEntity<VentaResponse> registrar(@Valid @RequestBody VentaRequest request) {
        VentaResponse venta = service.registrar(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}").buildAndExpand(venta.id()).toUri();
        return ResponseEntity.created(location).body(venta);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Cancelar una venta y reponer el inventario")
    @ApiResponse(responseCode = "204", description = "Venta cancelada", content = @Content)
    @ApiResponse(responseCode = "404", description = "La venta no existe", content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "409", description = "La venta ya estaba cancelada", content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    public ResponseEntity<Void> cancelar(@PathVariable Long id) {
        service.cancelar(id);
        return ResponseEntity.noContent().build();
    }
}
