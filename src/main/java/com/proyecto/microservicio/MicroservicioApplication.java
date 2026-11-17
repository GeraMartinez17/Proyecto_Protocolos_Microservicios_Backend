package com.proyecto.microservicio;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Microservicio modular de productos y ventas.
 * Un único servicio Spring Boot con dos módulos funcionales: productos y ventas.
 */
@SpringBootApplication
public class MicroservicioApplication {

    public static void main(String[] args) {
        SpringApplication.run(MicroservicioApplication.class, args);
    }
}
