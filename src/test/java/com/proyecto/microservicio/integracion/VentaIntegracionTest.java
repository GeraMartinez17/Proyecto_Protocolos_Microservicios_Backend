package com.proyecto.microservicio.integracion;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@DisplayName("Integración (Spring Boot + MySQL) - Ventas")
class VentaIntegracionTest extends BaseIntegracionTest {

    private String postVenta(String json, int esperado) throws Exception {
        return mvc.perform(post("/api/ventas").contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().is(esperado)).andReturn().getResponse().getContentAsString();
    }

    @Test
    @DisplayName("V1 Obtener todas las ventas -> 200, más recientes primero")
    void v1Todas() throws Exception {
        mvc.perform(get("/api/ventas")).andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(3)))
                .andExpect(jsonPath("$[0].id").value(3))
                .andExpect(jsonPath("$[0].detalles", hasSize(2)));
    }

    @Test
    @DisplayName("V2 Consultar una venta existente -> 200; inexistente -> 404")
    void v2Existente() throws Exception {
        mvc.perform(get("/api/ventas/2")).andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(4015.00))
                .andExpect(jsonPath("$.totalArticulos").value(5));
        mvc.perform(get("/api/ventas/999")).andExpect(status().isNotFound());
        mvc.perform(get("/api/ventas/2/detalle")).andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(3)))
                .andExpect(jsonPath("$[0].nombreProducto").value("Monitor Samsung 24\""));
    }

    @Test
    @DisplayName("V3 Registrar una venta correctamente -> 201 y se persiste con su detalle")
    void v3Registrar() throws Exception {
        String r = postVenta("{\"productos\":[{\"productoId\":3,\"cantidad\":2},{\"productoId\":6,\"cantidad\":1}]}", 201);
        assertThat((String) JsonPath.read(r, "$.estado")).isEqualTo("REGISTRADA");
        assertThat(contar("ventas")).isEqualTo(4);
        assertThat(contar("detalle_ventas")).isEqualTo(9);
    }

    @Test
    @DisplayName("V4 Registrar una venta sin productos -> 400 (lista vacía y campo ausente)")
    void v4SinProductos() throws Exception {
        postVenta("{\"productos\":[]}", 400);
        postVenta("{}", 400);
        assertThat(contar("ventas")).isEqualTo(3);
    }

    @Test
    @DisplayName("V5 Vender un producto inexistente -> 404")
    void v5Inexistente() throws Exception {
        String r = postVenta("{\"productos\":[{\"productoId\":999,\"cantidad\":1}]}", 404);
        assertThat((String) JsonPath.read(r, "$.detail")).contains("999");
    }

    @Test
    @DisplayName("V6 Vender más unidades de las disponibles -> 409; producto agotado -> 409")
    void v6StockInsuficiente() throws Exception {
        String r = postVenta("{\"productos\":[{\"productoId\":1,\"cantidad\":9}]}", 409);
        assertThat((String) JsonPath.read(r, "$.detail")).contains("disponible 8, solicitado 9");
        postVenta("{\"productos\":[{\"productoId\":8,\"cantidad\":1}]}", 409);
        assertThat(stock(1)).isEqualTo(8);
    }

    @Test
    @DisplayName("V7 Cálculo de subtotales = cantidad x precio unitario")
    void v7Subtotales() throws Exception {
        String r = postVenta("{\"productos\":[{\"productoId\":5,\"cantidad\":7},{\"productoId\":2,\"cantidad\":3}]}", 201);
        List<Double> subtotales = JsonPath.read(r, "$.detalles[*].subtotal");
        assertThat(subtotales).containsExactly(973.00, 747.00);   // 7*139 y 3*249
        List<Double> precios = JsonPath.read(r, "$.detalles[*].precioUnitario");
        assertThat(precios).containsExactly(139.00, 249.00);
    }

    @Test
    @DisplayName("V8 Cálculo del total = suma de subtotales (también en BD)")
    void v8Total() throws Exception {
        String r = postVenta("{\"productos\":[{\"productoId\":5,\"cantidad\":7},{\"productoId\":2,\"cantidad\":3}]}", 201);
        assertThat(((Number) JsonPath.read(r, "$.total")).doubleValue()).isEqualTo(1720.00);
        int id = JsonPath.read(r, "$.id");
        BigDecimal total = jdbc.queryForObject("select total from ventas where id=?", BigDecimal.class, id);
        BigDecimal suma = jdbc.queryForObject("select sum(subtotal) from detalle_ventas where venta_id=?", BigDecimal.class, id);
        assertThat(total).isEqualByComparingTo("1720.00").isEqualByComparingTo(suma);
    }

    @Test
    @DisplayName("V9 Se descuenta el inventario de cada producto vendido")
    void v9DescuentaInventario() throws Exception {
        postVenta("{\"productos\":[{\"productoId\":4,\"cantidad\":2},{\"productoId\":7,\"cantidad\":15}]}", 201);
        assertThat(stock(4)).isEqualTo(10);   // 12 - 2
        assertThat(stock(7)).isZero();        // 15 - 15
        mvc.perform(get("/api/productos/7")).andExpect(jsonPath("$.stock").value(0));
    }

    @Test
    @DisplayName("V10 Una venta fallida (2ª partida sin stock) no modifica el inventario ni crea registros")
    void v10VentaFallidaAtomica() throws Exception {
        int ventas = contar("ventas"), detalles = contar("detalle_ventas");
        postVenta("{\"productos\":[{\"productoId\":2,\"cantidad\":5},{\"productoId\":1,\"cantidad\":50}]}", 409);
        postVenta("{\"productos\":[{\"productoId\":2,\"cantidad\":5},{\"productoId\":999,\"cantidad\":1}]}", 404);
        assertThat(stock(2)).isEqualTo(45);
        assertThat(stock(1)).isEqualTo(8);
        assertThat(contar("ventas")).isEqualTo(ventas);
        assertThat(contar("detalle_ventas")).isEqualTo(detalles);
    }

    @Test
    @DisplayName("V11 Consultar ventas por intervalo de fechas -> 200; inicio > fin -> 400")
    void v11PorFecha() throws Exception {
        mvc.perform(get("/api/ventas/fecha").param("inicio", "2026-11-01").param("fin", "2026-11-05"))
                .andExpect(status().isOk()).andExpect(jsonPath("$[*].id", contains(1, 2)));
        mvc.perform(get("/api/ventas/fecha").param("inicio", "2026-11-10").param("fin", "2026-11-10"))
                .andExpect(status().isOk()).andExpect(jsonPath("$[*].id", contains(3)));
        mvc.perform(get("/api/ventas/fecha").param("inicio", "2026-12-01").param("fin", "2026-12-31"))
                .andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(0)));
        mvc.perform(get("/api/ventas/fecha").param("inicio", "2026-11-30").param("fin", "2026-11-01"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("V12 Cancelar una venta -> 204, repone inventario; segunda cancelación -> 409; inexistente -> 404")
    void v12Cancelar() throws Exception {
        mvc.perform(delete("/api/ventas/2")).andExpect(status().isNoContent());
        assertThat(stock(4)).isEqualTo(13);   // 12 + 1
        assertThat(stock(5)).isEqualTo(100);  // 97 + 3
        assertThat(stock(3)).isEqualTo(21);   // 20 + 1
        mvc.perform(get("/api/ventas/2")).andExpect(jsonPath("$.estado").value("CANCELADA"))
                .andExpect(jsonPath("$.fechaCancelacion").isNotEmpty());
        mvc.perform(delete("/api/ventas/2")).andExpect(status().isConflict());
        assertThat(stock(4)).isEqualTo(13);
        mvc.perform(delete("/api/ventas/999")).andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("V13 Concurrencia: 20 ventas simultáneas de 1 unidad sobre un stock de 8 -> exactamente 8 éxitos")
    void v13Concurrencia() throws Exception {
        int hilos = 20;
        ExecutorService pool = Executors.newFixedThreadPool(hilos);
        CountDownLatch salida = new CountDownLatch(1);
        List<Future<Integer>> resultados = new ArrayList<>();
        for (int i = 0; i < hilos; i++) {
            resultados.add(pool.submit(() -> {
                salida.await();
                return mvc.perform(post("/api/ventas").contentType(MediaType.APPLICATION_JSON)
                                .content("{\"productos\":[{\"productoId\":1,\"cantidad\":1}]}"))
                        .andReturn().getResponse().getStatus();
            }));
        }
        salida.countDown();
        int exitos = 0, conflictos = 0;
        for (Future<Integer> f : resultados) {
            int s = f.get(60, TimeUnit.SECONDS);
            if (s == 201) exitos++;
            else if (s == 409) conflictos++;
        }
        pool.shutdown();
        assertThat(exitos).isEqualTo(8);
        assertThat(conflictos).isEqualTo(12);
        assertThat(stock(1)).isZero();
        assertThat(jdbc.queryForObject("select coalesce(sum(cantidad),0) from detalle_ventas d join ventas v on v.id=d.venta_id "
                + "where d.producto_id=1 and v.id > 3", Integer.class)).isEqualTo(8);
    }
}
