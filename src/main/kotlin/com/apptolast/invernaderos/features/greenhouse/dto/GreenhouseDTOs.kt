package com.apptolast.invernaderos.features.greenhouse.dto

import com.apptolast.invernaderos.features.greenhouse.Greenhouse
import com.apptolast.invernaderos.features.tenant.LocationDto
import io.swagger.v3.oas.annotations.media.Schema
import java.math.BigDecimal
import java.time.Instant

@Schema(description = "Respuesta que representa un Invernadero")
data class GreenhouseResponse(
    @Schema(description = "ID único del invernadero") val id: Long,
    @Schema(description = "Nombre del invernadero") val name: String,
    @Schema(description = "ID del tenant propietario") val tenantId: Long,
    @Schema(description = "Ubicación geográfica") val location: LocationDto?,
    @Schema(description = "Área en metros cuadrados") val areaM2: BigDecimal?,
    @Schema(description = "Zona horaria") val timezone: String?,
    @Schema(description = "Si el invernadero está activo") val isActive: Boolean,
    @Schema(description = "Fecha de creación") val createdAt: Instant,
    @Schema(description = "Fecha de última actualización") val updatedAt: Instant
)

@Schema(description = "Solicitud para crear un nuevo Invernadero")
data class GreenhouseCreateRequest(
    @Schema(description = "Nombre del invernadero", example = "Invernadero Principal")
    val name: String,

    @Schema(description = "Ubicación geográfica")
    val location: LocationDto? = null,

    @Schema(description = "Área en metros cuadrados", example = "1500.50")
    val areaM2: BigDecimal? = null,

    @Schema(description = "Zona horaria", example = "Europe/Madrid")
    val timezone: String? = "Europe/Madrid",

    @Schema(description = "Si el invernadero está activo", example = "true")
    val isActive: Boolean? = true
)

@Schema(description = "Solicitud para actualizar un Invernadero existente")
data class GreenhouseUpdateRequest(
    @Schema(description = "Nombre del invernadero")
    val name: String? = null,

    @Schema(description = "Ubicación geográfica")
    val location: LocationDto? = null,

    @Schema(description = "Área en metros cuadrados")
    val areaM2: BigDecimal? = null,

    @Schema(description = "Zona horaria")
    val timezone: String? = null,

    @Schema(description = "Si el invernadero está activo")
    val isActive: Boolean? = null
)

fun Greenhouse.toResponse() = GreenhouseResponse(
    id = this.id ?: throw IllegalStateException("Greenhouse ID cannot be null"),
    name = this.name,
    tenantId = this.tenantId,
    location = this.location,
    areaM2 = this.areaM2,
    timezone = this.timezone,
    isActive = this.isActive,
    createdAt = this.createdAt,
    updatedAt = this.updatedAt
)
