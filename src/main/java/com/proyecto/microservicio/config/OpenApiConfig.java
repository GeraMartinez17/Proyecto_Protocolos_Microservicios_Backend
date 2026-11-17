package com.proyecto.microservicio.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Documentación OpenAPI 3 disponible en /swagger-ui.html y /v3/api-docs. */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI apiInfo() {
        return new OpenAPI().info(new Info()
                .title("Microservicio de Productos y Ventas")
                .version("1.0.0")
                .description("API REST para la gestión de productos e inventario y el registro de ventas. "
                        + "Los errores se devuelven con el formato Problem Details (RFC 9457)."));
    }
}
