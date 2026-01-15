package com.apptolast.invernaderos.features.device.dto

import com.apptolast.invernaderos.features.device.Device
import io.swagger.v3.oas.annotations.media.Schema
import java.time.Instant

@Schema(description = "Respuesta que representa un Dispositivo (Sensor o Actuador)")
data class DeviceResponse(
    @Schema(description = "ID único del dispositivo") val id: Long,
    @Schema(description = "Código único por tenant del dispositivo", example = "DEV-00001") val code: String,
    @Schema(description = "ID del tenant propietario") val tenantId: Long,
    @Schema(description = "ID del sector al que pertenece") val sectorId: Long,
    @Schema(description = "Código del sector") val sectorCode: String?,
    @Schema(description = "Nombre legible del dispositivo", example = "Sensor Temperatura Invernadero 1") val name: String?,
    @Schema(description = "ID de la categoría (1=SENSOR, 2=ACTUATOR)") val categoryId: Short?,
    @Schema(description = "Nombre de la categoría") val categoryName: String?,
    @Schema(description = "ID del tipo de dispositivo") val typeId: Short?,
    @Schema(description = "Nombre del tipo de dispositivo") val typeName: String?,
    @Schema(description = "ID de la unidad de medida") val unitId: Short?,
    @Schema(description = "Símbolo de la unidad") val unitSymbol: String?,
    @Schema(description = "Si el dispositivo está activo") val isActive: Boolean,
    @Schema(description = "Fecha de creación") val createdAt: Instant,
    @Schema(description = "Fecha de última actualización") val updatedAt: Instant
)

@Schema(description = "Solicitud para crear un nuevo Dispositivo")
data class DeviceCreateRequest(
    @Schema(description = "ID del sector donde se instalará el dispositivo", required = true)
    val sectorId: Long,

    @Schema(description = "Nombre legible del dispositivo (máx 100 caracteres)", example = "Sensor Temperatura Invernadero 1")
    @field:jakarta.validation.constraints.Size(max = 100, message = "El nombre no puede exceder 100 caracteres")
    val name: String? = null,

    @Schema(description = "ID de la categoría (1=SENSOR, 2=ACTUATOR)", example = "1")
    val categoryId: Short? = null,

    @Schema(description = "ID del tipo de dispositivo (ej: 1=TEMPERATURE, 2=HUMIDITY)", example = "1")
    val typeId: Short? = null,

    @Schema(description = "ID de la unidad de medida", example = "1")
    val unitId: Short? = null,

    @Schema(description = "Si el dispositivo está activo", example = "true")
    val isActive: Boolean? = true
)

@Schema(description = "Solicitud para actualizar un Dispositivo existente")
data class DeviceUpdateRequest(
    @Schema(description = "Nombre legible del dispositivo (máx 100 caracteres)", example = "Sensor Temperatura Invernadero 1")
    @field:jakarta.validation.constraints.Size(max = 100, message = "El nombre no puede exceder 100 caracteres")
    val name: String? = null,

    @Schema(description = "ID de la categoría (1=SENSOR, 2=ACTUATOR)")
    val categoryId: Short? = null,

    @Schema(description = "ID del tipo de dispositivo")
    val typeId: Short? = null,

    @Schema(description = "ID de la unidad de medida")
    val unitId: Short? = null,

    @Schema(description = "Si el dispositivo está activo")
    val isActive: Boolean? = null
)

fun Device.toResponse() = DeviceResponse(
    id = this.id ?: throw IllegalStateException("Device ID cannot be null"),
    code = this.code,
    tenantId = this.tenantId,
    sectorId = this.sectorId,
    sectorCode = this.sector?.code,
    name = this.name,
    categoryId = this.categoryId,
    categoryName = this.category?.name,
    typeId = this.typeId,
    typeName = this.type?.name,
    unitId = this.unitId,
    unitSymbol = this.unit?.symbol,
    isActive = this.isActive,
    createdAt = this.createdAt,
    updatedAt = this.updatedAt
)
