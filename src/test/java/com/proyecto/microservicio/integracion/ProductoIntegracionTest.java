package com.proyecto.microservicio.integracion;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@DisplayName("Integración (Spring Boot + MySQL) - Productos")
class ProductoIntegracionTest extends BaseIntegracionTest {

    private static final String NUEVO = """
            {"nombre":"Teclado Logitech K380","descripcion":"Bluetooth","precio":649.00,"stock":25}""";

    @Test
    @DisplayName("P1 Obtener todos los productos -> 200 con los 10 productos iniciales")
    void p1ObtenerTodos() throws Exception {
        mvc.perform(get("/api/productos")).andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(10)))
                .andExpect(jsonPath("$[0].nombre").value("Laptop Lenovo IdeaPad 3"));
    }

    @Test
    @DisplayName("P2 Consultar un producto existente -> 200")
    void p2Existente() throws Exception {
        mvc.perform(get("/api/productos/4")).andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(4))
                .andExpect(jsonPath("$.precio").value(2899.00))
                .andExpect(jsonPath("$.stock").value(12))
                .andExpect(jsonPath("$.fechaCreacion").exists());
    }

    @Test
    @DisplayName("P3 Consultar un producto inexistente -> 404")
    void p3Inexistente() throws Exception {
        mvc.perform(get("/api/productos/999")).andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("Producto con id 999 no existe"));
    }

    @Test
    @DisplayName("P4 Registrar un producto correctamente -> 201 y se persiste en MySQL")
    void p4Registrar() throws Exception {
        String body = mvc.perform(post("/api/productos").contentType(MediaType.APPLICATION_JSON).content(NUEVO))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", endsWith("/api/productos/11")))
                .andExpect(jsonPath("$.estado").value("ACTIVO"))
                .andReturn().getResponse().getContentAsString();
        int id = JsonPath.read(body, "$.id");
        assertThat(jdbc.queryForObject("select nombre from productos where id=?", String.class, id))
                .isEqualTo("Teclado Logitech K380");
    }

    @Test
    @DisplayName("P5 Registrar un producto con datos inválidos -> 400 y no se persiste")
    void p5Invalido() throws Exception {
        mvc.perform(post("/api/productos").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"\",\"precio\":-10,\"stock\":-1}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores", aMapWithSize(3)));
        assertThat(contar("productos")).isEqualTo(10);
    }

    @Test
    @DisplayName("P6 Actualizar un producto -> 200 y fecha de actualización cambia")
    void p6Actualizar() throws Exception {
        String antes = jdbc.queryForObject("select fecha_actualizacion from productos where id=2", String.class);
        mvc.perform(put("/api/productos/2").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"Mouse inalámbrico Logitech M170\",\"precio\":229.50,\"stock\":50,\"estado\":\"ACTIVO\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.precio").value(229.50))
                .andExpect(jsonPath("$.stock").value(50));
        assertThat(stock(2)).isEqualTo(50);
        String despues = jdbc.queryForObject("select fecha_actualizacion from productos where id=2", String.class);
        assertThat(despues).isNotEqualTo(antes);
    }

    @Test
    @DisplayName("P7 Actualizar un producto inexistente -> 404")
    void p7ActualizarInexistente() throws Exception {
        mvc.perform(put("/api/productos/999").contentType(MediaType.APPLICATION_JSON).content(NUEVO))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("P8 Eliminar un producto sin ventas -> 204; con ventas -> 409")
    void p8Eliminar() throws Exception {
        mvc.perform(delete("/api/productos/8")).andExpect(status().isNoContent());
        mvc.perform(get("/api/productos/8")).andExpect(status().isNotFound());
        mvc.perform(delete("/api/productos/1")).andExpect(status().isConflict());
        assertThat(contar("productos")).isEqualTo(9);
    }

    @Test
    @DisplayName("P9 Buscar productos por nombre (sin distinguir mayúsculas) -> 200")
    void p9Buscar() throws Exception {
        mvc.perform(get("/api/productos/buscar").param("nombre", "LOGITECH")).andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[*].nombre", everyItem(containsStringIgnoringCase("logitech"))));
        mvc.perform(get("/api/productos/buscar").param("nombre", "no-existe")).andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    @DisplayName("P10 Validaciones: nombre duplicado 409, nombre > 120 caracteres 400, JSON mal formado 400")
    void p10Validaciones() throws Exception {
        mvc.perform(post("/api/productos").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"laptop lenovo ideapad 3\",\"precio\":1,\"stock\":1}"))
                .andExpect(status().isConflict());
        mvc.perform(post("/api/productos").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"" + "x".repeat(121) + "\",\"precio\":1,\"stock\":1}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.errores.nombre").exists());
        mvc.perform(post("/api/productos").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"X\",\"precio\":\"caro\",\"stock\":1}"))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/api/productos").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"X\",\"precio\":1,\"stock\":1.5}"))
                .andExpect(status().isBadRequest());
        assertThat(contar("productos")).isEqualTo(10);
    }
}
