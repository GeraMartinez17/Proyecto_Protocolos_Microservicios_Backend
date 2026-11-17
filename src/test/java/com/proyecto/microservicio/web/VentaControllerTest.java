package com.proyecto.microservicio.web;

import com.proyecto.microservicio.exception.ConflictoNegocioException;
import com.proyecto.microservicio.exception.RecursoNoEncontradoException;
import com.proyecto.microservicio.ventas.controller.VentaController;
import com.proyecto.microservicio.ventas.service.VentaService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(VentaController.class)
@DisplayName("Capa web (MockMvc + Mockito) - VentaController")
class VentaControllerTest {

    @Autowired MockMvc mvc;
    @MockitoBean VentaService service;

    @Test
    @DisplayName("POST sin productos -> 400 y no llama al servicio")
    void sinProductos() throws Exception {
        mvc.perform(post("/api/ventas").contentType(MediaType.APPLICATION_JSON).content("{\"productos\":[]}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores.productos").value("La venta debe incluir al menos un producto"));
        verifyNoInteractions(service);
    }

    @Test
    @DisplayName("POST con cantidad 0 o negativa -> 400")
    void cantidadInvalida() throws Exception {
        mvc.perform(post("/api/ventas").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"productos\":[{\"productoId\":1,\"cantidad\":0}]}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores['productos[0].cantidad']").value("La cantidad debe ser mayor que cero"));
        mvc.perform(post("/api/ventas").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"productos\":[{\"productoId\":1,\"cantidad\":-3}]}"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(service);
    }

    @Test
    @DisplayName("POST con producto inexistente -> 404")
    void productoInexistente() throws Exception {
        when(service.registrar(any())).thenThrow(new RecursoNoEncontradoException("Producto", 99L));
        mvc.perform(post("/api/ventas").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"productos\":[{\"productoId\":99,\"cantidad\":1}]}"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("POST con stock insuficiente -> 409")
    void stockInsuficiente() throws Exception {
        when(service.registrar(any())).thenThrow(new ConflictoNegocioException("Stock insuficiente"));
        mvc.perform(post("/api/ventas").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"productos\":[{\"productoId\":1,\"cantidad\":1000}]}"))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.status").value(409));
    }

    @Test
    @DisplayName("GET /fecha sin parámetros o con fecha mal formada -> 400")
    void fechaInvalida() throws Exception {
        mvc.perform(get("/api/ventas/fecha")).andExpect(status().isBadRequest());
        mvc.perform(get("/api/ventas/fecha").param("inicio", "16/11/2026").param("fin", "2026-11-30"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("DELETE -> 204; DELETE venta ya cancelada -> 409")
    void cancelar() throws Exception {
        mvc.perform(delete("/api/ventas/1")).andExpect(status().isNoContent());
        doThrow(new ConflictoNegocioException("ya cancelada")).when(service).cancelar(2L);
        mvc.perform(delete("/api/ventas/2")).andExpect(status().isConflict());
    }
}
