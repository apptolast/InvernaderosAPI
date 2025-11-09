package com.apptolast.invernaderos.config

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.KotlinModule
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary

/**
 * Configuración de Jackson para serialización/deserialización JSON
 *
 * Configura:
 * - JavaTimeModule para tipos de fecha/hora (Instant, LocalDateTime, etc.)
 * - KotlinModule para soporte completo de Kotlin (data classes, nullable types, etc.)
 * - Configuración de formatos de fecha como ISO-8601 en lugar de timestamps numéricos
 *
 * Basado en: https://github.com/FasterXML/jackson-modules-java8
 */
@Configuration
class JacksonConfig {

    /**
     * ObjectMapper principal de la aplicación
     * Se marca como @Primary para que Spring lo use en toda la aplicación
     * (REST controllers, Redis, MQTT, WebSocket, etc.)
     */
    @Bean
    @Primary
    fun objectMapper(): ObjectMapper {
        return ObjectMapper().apply {
            // ===== MÓDULOS =====

            // JavaTimeModule: Soporte para java.time.* (Instant, LocalDateTime, etc.)
            registerModule(JavaTimeModule())

            // KotlinModule: Soporte completo para Kotlin (data classes, nullable types, default values)
            registerModule(KotlinModule.Builder().build())

            // ===== SERIALIZACIÓN =====

            // Desactivar escritura de fechas como timestamps (usar ISO-8601)
            // Sin esto: {"time": 1735896000000}
            // Con esto:  {"time": "2025-01-03T12:00:00Z"}
            disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)

            // Desactivar escritura de duraciones como timestamps
            disable(SerializationFeature.WRITE_DURATIONS_AS_TIMESTAMPS)

            // Pretty print para desarrollo (puedes desactivar en producción)
            // disable(SerializationFeature.INDENT_OUTPUT)

            // No incluir propiedades null en el JSON de salida
            setSerializationInclusion(JsonInclude.Include.NON_NULL)

            // ===== DESERIALIZACIÓN =====

            // No fallar si el JSON tiene propiedades desconocidas
            // Útil cuando el frontend envía campos extra que no usamos
            configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)

            // No fallar si hay propiedades null para tipos primitivos
            configure(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES, false)

            // Aceptar números como strings (ej: "123" se deserializa como 123)
            configure(DeserializationFeature.ACCEPT_FLOAT_AS_INT, false)
        }
    }
}
