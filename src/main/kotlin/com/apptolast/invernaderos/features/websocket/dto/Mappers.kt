package com.apptolast.invernaderos.features.websocket.dto

import com.apptolast.invernaderos.features.alert.Alert
import com.apptolast.invernaderos.features.catalog.ActuatorState
import com.apptolast.invernaderos.features.catalog.AlertSeverity
import com.apptolast.invernaderos.features.catalog.AlertType
import com.apptolast.invernaderos.features.catalog.DataType
import com.apptolast.invernaderos.features.catalog.DeviceCategory
import com.apptolast.invernaderos.features.catalog.DeviceType
import com.apptolast.invernaderos.features.catalog.Unit
import com.apptolast.invernaderos.features.device.Device
import com.apptolast.invernaderos.features.greenhouse.Greenhouse
import com.apptolast.invernaderos.features.sector.Sector
import com.apptolast.invernaderos.features.setting.Setting
import com.apptolast.invernaderos.features.tenant.Tenant
import com.apptolast.invernaderos.features.user.User
import java.time.Instant

/**
 * Extension functions para mapear entidades JPA a DTOs de respuesta del WebSocket.
 * Separa la capa de persistencia de la capa de presentacion.
 */

// --- Catalog mappers ---

fun DeviceCategory.toDto() = DeviceCategoryDto(
    id = id!!,
    name = name
)

fun DeviceType.toDto() = DeviceTypeDto(
    id = id!!,
    name = name,
    description = description,
    dataType = dataType,
    minExpectedValue = minExpectedValue,
    maxExpectedValue = maxExpectedValue,
    controlType = controlType,
    categoryId = categoryId
)

fun Unit.toDto() = UnitDto(
    id = id!!,
    symbol = symbol,
    name = name
)

fun ActuatorState.toDto() = ActuatorStateDto(
    id = id!!,
    name = name,
    description = description,
    isOperational = isOperational,
    displayOrder = displayOrder,
    color = color
)

fun DataType.toDto() = DataTypeDto(
    id = id!!,
    name = name,
    description = description,
    validationRegex = validationRegex,
    exampleValue = exampleValue
)

fun AlertType.toDto() = AlertTypeDto(
    id = id!!,
    name = name,
    description = description
)

fun AlertSeverity.toDto() = AlertSeverityDto(
    id = id!!,
    name = name,
    level = level,
    description = description,
    color = color,
    requiresAction = requiresAction ?: false,
    notificationDelayMinutes = notificationDelayMinutes ?: 0
)

// --- Business entity mappers ---

fun User.toResponse() = UserResponse(
    id = id!!,
    code = code,
    username = username,
    email = email,
    role = role,
    isActive = isActive ?: true,
    lastLogin = lastLogin
)

fun Device.toResponse(currentValue: String?, lastUpdated: Instant?) = DeviceResponse(
    id = id!!,
    code = code,
    name = name,
    isActive = isActive,
    category = category?.toDto(),
    type = type?.toDto(),
    unit = unit?.toDto(),
    clientName = clientName,
    currentValue = currentValue,
    lastUpdated = lastUpdated
)

fun Setting.toResponse(currentValue: String?, lastUpdated: Instant?) = SettingResponse(
    id = id!!,
    code = code,
    description = description,
    configuredValue = value,
    isActive = isActive,
    parameter = parameter?.toDto(),
    actuatorState = actuatorState?.toDto(),
    dataType = dataType?.toDto(),
    clientName = clientName,
    currentValue = currentValue,
    lastUpdated = lastUpdated
)

fun Alert.toResponse() = AlertResponse(
    id = id!!,
    code = code,
    message = message,
    description = description,
    isResolved = isResolved,
    clientName = clientName,
    resolvedAt = resolvedAt,
    createdAt = createdAt,
    alertType = alertType?.toDto(),
    severity = severity?.toDto(),
    resolvedByUser = resolvedByUser?.toResponse()
)

fun Sector.toResponse(
    devices: List<DeviceResponse>,
    settings: List<SettingResponse>,
    alerts: List<AlertResponse>
) = SectorResponse(
    id = id!!,
    code = code,
    name = name,
    devices = devices,
    settings = settings,
    alerts = alerts
)

fun Greenhouse.toResponse(sectors: List<SectorResponse>) = GreenhouseResponse(
    id = id!!,
    code = code,
    name = name,
    location = location,
    areaM2 = areaM2,
    timezone = timezone,
    isActive = isActive,
    sectors = sectors
)

fun Tenant.toResponse(
    users: List<UserResponse>,
    greenhouses: List<GreenhouseResponse>
) = TenantResponse(
    id = id!!,
    code = code,
    name = name,
    email = email,
    province = province,
    country = country,
    phone = phone,
    location = location,
    isActive = isActive ?: true,
    users = users,
    greenhouses = greenhouses
)
