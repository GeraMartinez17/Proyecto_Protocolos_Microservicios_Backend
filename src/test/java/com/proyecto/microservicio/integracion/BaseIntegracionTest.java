package com.proyecto.microservicio.integracion;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Pruebas de integración contra MySQL real (base db_microservicios_test).
 * Antes de cada prueba se limpia el esquema y se reaplican las migraciones Flyway
 * (V1 tablas + V2 datos iniciales), de modo que cada prueba parte del mismo estado.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
abstract class BaseIntegracionTest {

    @Autowired protected MockMvc mvc;
    @Autowired protected JdbcTemplate jdbc;
    @Autowired private Flyway flyway;

    @BeforeEach
    void reiniciarBaseDeDatos() {
        flyway.clean();
        flyway.migrate();
    }

    protected int stock(long productoId) {
        return jdbc.queryForObject("select stock from productos where id = ?", Integer.class, productoId);
    }

    protected int contar(String tabla) {
        return jdbc.queryForObject("select count(*) from " + tabla, Integer.class);
    }
}
