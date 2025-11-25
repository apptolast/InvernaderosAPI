package com.apptolast.invernaderos.core.config

import io.swagger.v3.oas.models.Components
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.info.Contact
import io.swagger.v3.oas.models.info.Info
import io.swagger.v3.oas.models.info.License
import io.swagger.v3.oas.models.security.SecurityRequirement
import io.swagger.v3.oas.models.security.SecurityScheme
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class OpenApiConfig {

        @Bean
        fun customOpenAPI(): OpenAPI {
                val securitySchemeName = "bearerAuth"
                return OpenAPI()
                        .info(
                                Info().title("Invernaderos API")
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
                                                        .name("AppToLast Support")
                                                        .email("support@apptolast.com")
                                                        .url("https://apptolast.com")
                                        )
                                        .license(
                                                License()
                                                        .name("Apache 2.0")
                                                        .url(
                                                                "https://www.apache.org/licenses/LICENSE-2.0"
                                                        )
                                        )
                        )
                        .addSecurityItem(SecurityRequirement().addList(securitySchemeName))
                        .components(
                                Components()
                                        .addSecuritySchemes(
                                                securitySchemeName,
                                                SecurityScheme()
                                                        .name(securitySchemeName)
                                                        .type(SecurityScheme.Type.HTTP)
                                                        .scheme("bearer")
                                                        .bearerFormat("JWT")
                                        )
                        )
                        .servers(
                                listOf(
                                        io.swagger.v3.oas.models.servers.Server()
                                                .url("http://localhost:8080")
                                                .description("Servidor de desarrollo local"),
                                        io.swagger.v3.oas.models.servers.Server()
                                                .url("https://api.apptolast.com")
                                                .description("Servidor de producción")
                                )
                        )
        }
}
