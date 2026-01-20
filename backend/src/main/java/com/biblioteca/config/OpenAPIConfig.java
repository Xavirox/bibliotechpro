package com.biblioteca.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenAPIConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Biblioteca Web API")
                        .version("1.0.0")
                        .description("API REST para gestión de biblioteca digital. " +
                                "Permite la consulta del catálogo, gestión de préstamos, " +
                                "reservas y recomendaciones inteligentes con IA.")
                        .contact(new Contact()
                                .name("Equipo Biblioteca")
                                .email("soporte@biblioteca.com"))
                        .license(new License()
                                .name("MIT License")
                                .url("https://opensource.org/licenses/MIT")))
                .servers(List.of(
                        new Server()
                                .url("http://localhost:9091")
                                .description("Servidor de desarrollo")));
    }
}
