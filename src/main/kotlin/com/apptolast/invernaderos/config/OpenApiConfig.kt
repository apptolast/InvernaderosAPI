package com.apptolast.invernaderos.config

import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.info.Contact
import io.swagger.v3.oas.models.info.Info
import io.swagger.v3.oas.models.info.License
import io.swagger.v3.oas.models.servers.Server
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

/**
 * Configuración de OpenAPI/Swagger para documentación de la API REST
 *
 * Basado en springdoc-openapi v2.8.14 para Spring Boot 3.x
 * Documentación oficial: https://springdoc.org/
 *
 * Endpoints disponibles:
 * - Swagger UI: http://localhost:8080/swagger-ui.html
 * - OpenAPI JSON: http://localhost:8080/v3/api-docs
 * - OpenAPI YAML: http://localhost:8080/v3/api-docs.yaml
 */
@Configuration
class OpenApiConfig {

    @Bean
    fun customOpenAPI(): OpenAPI {
        return OpenAPI()
            .info(
                Info()
                    .title("Invernaderos API")
                    .description(
                        """
                        API REST para el sistema de monitoreo y control de invernaderos.

                        **Características principales:**
                        - 🌡️ Monitoreo de sensores en tiempo real (temperatura, humedad, etc.)
                        - 📊 Almacenamiento de series temporales en TimescaleDB
                        - 🔴 Caché de datos en Redis para acceso rápido
                        - 📡 Comunicación MQTT con dispositivos IoT
                        - 🔌 WebSocket para actualizaciones en tiempo real
                        - 📈 Estadísticas y análisis de datos históricos

                        **Stack tecnológico:**
                        - Spring Boot 3.5.7
                        - Kotlin 1.9.25
                        - TimescaleDB (PostgreSQL)
                        - Redis (Lettuce)
                        - MQTT (EMQX)
                        - WebSocket (STOMP)
                        """.trimIndent()
                    )
                    .version("1.0.0")
                    .contact(
                        Contact()
                            .name("AppToLast")
                            .email("info@apptolast.com")
                    )
                    .license(
                        License()
                            .name("Proprietary")
                            .url("https://apptolast.com")
                    )
            )
            .servers(
                listOf(
                    Server()
                        .url("http://localhost:8080")
                        .description("Servidor de desarrollo local"),
                    Server()
                        .url("https://api.apptolast.com")
                        .description("Servidor de producción")
                )
            )
    }
}
