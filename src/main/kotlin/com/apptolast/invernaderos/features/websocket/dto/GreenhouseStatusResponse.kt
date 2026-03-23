package com.apptolast.invernaderos.features.websocket.dto

import com.apptolast.invernaderos.features.tenant.LocationDto
import java.math.BigDecimal
import java.time.Instant

/**
 * DTO raiz de la respuesta del WebSocket.
 * Contiene el timestamp de la consulta y la jerarquia completa de todos los tenants.
 */
data class GreenhouseStatusResponse(
    val timestamp: Instant,
    val tenants: List<TenantResponse>
)

/**
 * Datos del tenant con sus usuarios e invernaderos.
 * Corresponde a metadata.tenants + relaciones.
 */
data class TenantResponse(
    val id: Long,
    val code: String,
    val name: String,
    val email: String,
    val province: String?,
    val country: String?,
    val phone: String?,
    val location: LocationDto?,
    val isActive: Boolean,
    val users: List<UserResponse>,
    val greenhouses: List<GreenhouseResponse>
)

/**
 * Datos basicos del usuario (sin password_hash ni tokens de reset).
 * Corresponde a metadata.users.
 */
data class UserResponse(
    val id: Long,
    val code: String,
    val username: String,
    val email: String,
    val role: String,
    val isActive: Boolean,
    val lastLogin: Instant?
)

/**
 * Datos del invernadero con sus sectores.
 * Corresponde a metadata.greenhouses + relaciones.
 */
data class GreenhouseResponse(
    val id: Long,
    val code: String,
    val name: String,
    val location: LocationDto?,
    val areaM2: BigDecimal?,
    val timezone: String?,
    val isActive: Boolean,
    val sectors: List<SectorResponse>
)

/**
 * Datos del sector con dispositivos, settings y alertas.
 * Corresponde a metadata.sectors + relaciones.
 */
data class SectorResponse(
    val id: Long,
    val code: String,
    val name: String?,
    val devices: List<DeviceResponse>,
    val settings: List<SettingResponse>,
    val alerts: List<AlertResponse>
)

/**
 * Dispositivo con catalogo embebido y valor actual de TimescaleDB.
 * Corresponde a metadata.devices + iot.sensor_readings (ultimo valor por code).
 *
 * @property currentValue Ultimo valor reportado por el hardware (de TimescaleDB). Null si no hay datos.
 * @property lastUpdated Timestamp del ultimo registro en TimescaleDB para este code. Null si no hay datos.
 */
data class DeviceResponse(
    val id: Long,
    val code: String,
    val name: String?,
    val isActive: Boolean,
    val category: DeviceCategoryDto?,
    val type: DeviceTypeDto?,
    val unit: UnitDto?,
    val clientName: String?,
    val currentValue: String?,
    val lastUpdated: Instant?
)

/**
 * Setting/consigna con catalogo embebido y valor actual de TimescaleDB.
 * Corresponde a metadata.settings + iot.sensor_readings (ultimo valor por code).
 *
 * @property configuredValue Valor configurado en PostgreSQL (metadata.settings.value)
 * @property currentValue Ultimo valor reportado por el hardware (de TimescaleDB). Null si no hay datos.
 * @property lastUpdated Timestamp del ultimo registro en TimescaleDB para este code. Null si no hay datos.
 */
data class SettingResponse(
    val id: Long,
    val code: String,
    val description: String?,
    val configuredValue: String?,
    val isActive: Boolean,
    val parameter: DeviceTypeDto?,
    val actuatorState: ActuatorStateDto?,
    val dataType: DataTypeDto?,
    val clientName: String?,
    val currentValue: String?,
    val lastUpdated: Instant?
)

/**
 * Alerta con catalogo embebido.
 * Corresponde a metadata.alerts + relaciones con alert_types, alert_severities, users.
 */
data class AlertResponse(
    val id: Long,
    val code: String,
    val message: String?,
    val description: String?,
    val isResolved: Boolean,
    val clientName: String?,
    val resolvedAt: Instant?,
    val createdAt: Instant,
    val alertType: AlertTypeDto?,
    val severity: AlertSeverityDto?,
    val resolvedByUser: UserResponse?
)
