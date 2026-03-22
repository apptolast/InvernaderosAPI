package com.apptolast.invernaderos.features.catalog.dto.request

import io.swagger.v3.oas.annotations.media.Schema

/**
 * DTO para crear una nueva categoría de dispositivo.
 * El ID se genera automáticamente por la base de datos (IDENTITY strategy).
 *
 * @see <a href="https://docs.spring.io/spring-boot/reference/web/servlet.html#web.servlet.spring-mvc.binding">Spring Boot Binding</a>
 */
@Schema(description = "Request para crear una categoría de dispositivo")
data class DeviceCategoryCreateRequest(
    @Schema(description = "Nombre de la categoría (único, máx 20 caracteres)", example = "HYBRID", required = true)
    @field:jakarta.validation.constraints.NotBlank(message = "El nombre es obligatorio")
    @field:jakarta.validation.constraints.Size(max = 20, message = "El nombre no puede exceder 20 caracteres")
    val name: String
)
