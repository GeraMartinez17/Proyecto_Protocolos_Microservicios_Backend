package com.proyecto.microservicio.web;

import com.proyecto.microservicio.exception.ConflictoNegocioException;
import com.proyecto.microservicio.exception.RecursoNoEncontradoException;
import com.proyecto.microservicio.productos.controller.ProductoController;
import com.proyecto.microservicio.productos.dto.ProductoResponse;
import com.proyecto.microservicio.productos.entity.EstadoProducto;
import com.proyecto.microservicio.productos.service.ProductoService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ProductoController.class)
@DisplayName("Capa web (MockMvc + Mockito) - ProductoController")
class ProductoControllerTest {

    @Autowired MockMvc mvc;
    @MockitoBean ProductoService service;

    private static ProductoResponse resp(long id) {
        return new ProductoResponse(id, "Mouse", null, new BigDecimal("249.00"), 10, EstadoProducto.ACTIVO,
                LocalDateTime.now(), LocalDateTime.now());
    }

    @Test
    @DisplayName("GET /api/productos -> 200 y arreglo JSON")
    void listar() throws Exception {
        when(service.listar()).thenReturn(List.of(resp(1), resp(2)));
        mvc.perform(get("/api/productos")).andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    @DisplayName("GET /api/productos/{id} inexistente -> 404 Problem Details")
    void noEncontrado() throws Exception {
        when(service.obtener(9L)).thenThrow(new RecursoNoEncontradoException("Producto", 9L));
        mvc.perform(get("/api/productos/9")).andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.detail").value("Producto con id 9 no existe"));
    }

    @Test
    @DisplayName("GET /api/productos/abc -> 400 (id no numérico)")
    void idNoNumerico() throws Exception {
        mvc.perform(get("/api/productos/abc")).andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST válido -> 201 con cabecera Location")
    void crear() throws Exception {
        when(service.crear(any())).thenReturn(resp(11));
        mvc.perform(post("/api/productos").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"Mouse\",\"precio\":249.00,\"stock\":10}"))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "http://localhost/api/productos/11"))
                .andExpect(jsonPath("$.id").value(11));
    }

    @Test
    @DisplayName("POST con nombre vacío, precio y stock negativos -> 400 con errores por campo")
    void crearInvalido() throws Exception {
        mvc.perform(post("/api/productos").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\" \",\"precio\":-1,\"stock\":-5}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores.nombre").value("El nombre es obligatorio"))
                .andExpect(jsonPath("$.errores.precio").value("El precio no puede ser negativo"))
                .andExpect(jsonPath("$.errores.stock").value("El stock no puede ser negativo"));
        verify(service, never()).crear(any());
    }

    @Test
    @DisplayName("POST con precio con 3 decimales y estado desconocido -> 400")
    void crearTiposIncorrectos() throws Exception {
        mvc.perform(post("/api/productos").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"X\",\"precio\":1.999,\"stock\":1}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.errores.precio").exists());
        mvc.perform(post("/api/productos").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"X\",\"precio\":1,\"stock\":1,\"estado\":\"OTRO\"}"))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/api/productos").contentType(MediaType.APPLICATION_JSON).content("{no es json"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST con nombre duplicado -> 409")
    void crearDuplicado() throws Exception {
        when(service.crear(any())).thenThrow(new ConflictoNegocioException("Ya existe"));
        mvc.perform(post("/api/productos").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"Mouse\",\"precio\":1,\"stock\":1}"))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("PUT inexistente -> 404")
    void actualizarInexistente() throws Exception {
        when(service.actualizar(eq(9L), any())).thenThrow(new RecursoNoEncontradoException("Producto", 9L));
        mvc.perform(put("/api/productos/9").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"Mouse\",\"precio\":1,\"stock\":1}"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("DELETE -> 204 sin cuerpo")
    void eliminar() throws Exception {
        mvc.perform(delete("/api/productos/1")).andExpect(status().isNoContent())
                .andExpect(content().string(""));
        verify(service).eliminar(1L);
    }

    @Test
    @DisplayName("GET /buscar sin parámetro nombre -> 400")
    void buscarSinParametro() throws Exception {
        mvc.perform(get("/api/productos/buscar")).andExpect(status().isBadRequest());
    }
}
