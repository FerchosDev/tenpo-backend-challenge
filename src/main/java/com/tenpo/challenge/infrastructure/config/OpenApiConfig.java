package com.tenpo.challenge.infrastructure.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI challengeOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Tenpo Backend Challenge API")
                        .description("Suma dos números y aplica un porcentaje dinámico obtenido de un "
                                + "servicio externo (mockeado), con reintentos, historial asíncrono "
                                + "paginado y rate limiting.")
                        .version("v1"));
    }

}
