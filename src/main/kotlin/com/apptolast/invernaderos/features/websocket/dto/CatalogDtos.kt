package com.apptolast.invernaderos.features.websocket.dto

import java.math.BigDecimal

/**
 * DTOs de catalogo para la respuesta enriquecida del WebSocket.
 * Representan las tablas de referencia de metadata (device_categories, device_types, units, etc.)
 * Se embeben dentro de DeviceResponse y SettingResponse para que el front no necesite hacer lookups.
 */

data class DeviceCategoryDto(
    val id: Short,
    val name: String
)

data class DeviceTypeDto(
    val id: Short,
    val name: String,
    val description: String?,
    val dataType: String?,
    val minExpectedValue: BigDecimal?,
    val maxExpectedValue: BigDecimal?,
    val controlType: String?,
    val categoryId: Short
)

data class UnitDto(
    val id: Short,
    val symbol: String,
    val name: String
)

data class ActuatorStateDto(
    val id: Short,
    val name: String,
    val description: String?,
    val isOperational: Boolean,
    val displayOrder: Short,
    val color: String?
)

data class DataTypeDto(
    val id: Short,
    val name: String,
    val description: String?,
    val validationRegex: String?,
    val exampleValue: String?
)

data class AlertTypeDto(
    val id: Short,
    val name: String,
    val description: String?
)

data class AlertSeverityDto(
    val id: Short,
    val name: String,
    val level: Short,
    val description: String?,
    val color: String?,
    val requiresAction: Boolean,
    val notificationDelayMinutes: Int
)
