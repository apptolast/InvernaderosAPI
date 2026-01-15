package com.apptolast.invernaderos.features.catalog.dto

import com.apptolast.invernaderos.features.catalog.AlertSeverity
import com.apptolast.invernaderos.features.catalog.AlertType
import com.apptolast.invernaderos.features.catalog.DeviceCategory
import com.apptolast.invernaderos.features.catalog.DeviceType
import com.apptolast.invernaderos.features.catalog.Period
import com.apptolast.invernaderos.features.catalog.Unit
import io.swagger.v3.oas.annotations.media.Schema
import java.math.BigDecimal

@Schema(description = "Categoría de dispositivo (SENSOR o ACTUATOR)")
data class DeviceCategoryResponse(
    @Schema(description = "ID de la categoría", example = "1")
    val id: Short,

    @Schema(description = "Nombre de la categoría", example = "SENSOR")
    val name: String
)

@Schema(description = "Tipo de dispositivo con información completa")
data class DeviceTypeResponse(
    @Schema(description = "ID del tipo", example = "1")
    val id: Short,

    @Schema(description = "Nombre del tipo", example = "TEMPERATURE")
    val name: String,

    @Schema(description = "Descripción del tipo", example = "Sensor de temperatura ambiente")
    val description: String?,

    @Schema(description = "ID de la categoría a la que pertenece (1=SENSOR, 2=ACTUATOR)", example = "1")
    val categoryId: Short,

    @Schema(description = "Nombre de la categoría", example = "SENSOR")
    val categoryName: String?,

    @Schema(description = "ID de la unidad por defecto", example = "1")
    val defaultUnitId: Short?,

    @Schema(description = "Símbolo de la unidad por defecto", example = "°C")
    val defaultUnitSymbol: String?,

    @Schema(description = "Tipo de dato que genera", example = "DECIMAL")
    val dataType: String?,

    @Schema(description = "Valor mínimo esperado físicamente", example = "-50.00")
    val minExpectedValue: BigDecimal?,

    @Schema(description = "Valor máximo esperado físicamente", example = "100.00")
    val maxExpectedValue: BigDecimal?,

    @Schema(description = "Tipo de control para actuadores: BINARY, CONTINUOUS, MULTI_STATE", example = "CONTINUOUS")
    val controlType: String?,

    @Schema(description = "Si el tipo está activo", example = "true")
    val isActive: Boolean
)

@Schema(description = "Unidad de medida")
data class UnitResponse(
    @Schema(description = "ID de la unidad", example = "1")
    val id: Short,

    @Schema(description = "Símbolo de la unidad", example = "°C")
    val symbol: String,

    @Schema(description = "Nombre de la unidad", example = "Grados Celsius")
    val name: String,

    @Schema(description = "Descripción de la unidad", example = "Temperatura en grados Celsius")
    val description: String?,

    @Schema(description = "Si la unidad está activa", example = "true")
    val isActive: Boolean
)

fun DeviceCategory.toResponse() = DeviceCategoryResponse(
    id = this.id ?: throw IllegalStateException("DeviceCategory ID cannot be null"),
    name = this.name
)

fun DeviceType.toResponse() = DeviceTypeResponse(
    id = this.id ?: throw IllegalStateException("DeviceType ID cannot be null"),
    name = this.name,
    description = this.description,
    categoryId = this.categoryId,
    categoryName = this.category?.name,
    defaultUnitId = this.defaultUnitId,
    defaultUnitSymbol = this.defaultUnit?.symbol,
    dataType = this.dataType,
    minExpectedValue = this.minExpectedValue,
    maxExpectedValue = this.maxExpectedValue,
    controlType = this.controlType,
    isActive = this.isActive
)

fun Unit.toResponse() = UnitResponse(
    id = this.id ?: throw IllegalStateException("Unit ID cannot be null"),
    symbol = this.symbol,
    name = this.name,
    description = this.description,
    isActive = this.isActive
)

// ========== Alert Catalog DTOs ==========

@Schema(description = "Tipo de alerta")
data class AlertTypeResponse(
    @Schema(description = "ID del tipo de alerta", example = "1")
    val id: Short,

    @Schema(description = "Nombre del tipo", example = "THRESHOLD_EXCEEDED")
    val name: String,

    @Schema(description = "Descripción del tipo de alerta")
    val description: String?
)

@Schema(description = "Nivel de severidad de alerta")
data class AlertSeverityResponse(
    @Schema(description = "ID de la severidad", example = "1")
    val id: Short,

    @Schema(description = "Nombre de la severidad", example = "INFO")
    val name: String,

    @Schema(description = "Nivel numérico para ordenación (1=bajo, 4=crítico)", example = "1")
    val level: Short,

    @Schema(description = "Descripción de la severidad")
    val description: String?,

    @Schema(description = "Color hexadecimal para UI", example = "#0066FF")
    val color: String?,

    @Schema(description = "Si requiere acción inmediata", example = "false")
    val requiresAction: Boolean,

    @Schema(description = "Minutos de retraso antes de notificar", example = "0")
    val notificationDelayMinutes: Int
)

fun AlertType.toResponse() = AlertTypeResponse(
    id = this.id ?: throw IllegalStateException("AlertType ID cannot be null"),
    name = this.name,
    description = this.description
)

fun AlertSeverity.toResponse() = AlertSeverityResponse(
    id = this.id ?: throw IllegalStateException("AlertSeverity ID cannot be null"),
    name = this.name,
    level = this.level,
    description = this.description,
    color = this.color,
    requiresAction = this.requiresAction,
    notificationDelayMinutes = this.notificationDelayMinutes
)

// ========== Device Category Request DTOs ==========

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

/**
 * DTO para actualizar una categoría de dispositivo existente.
 */
@Schema(description = "Request para actualizar una categoría de dispositivo")
data class DeviceCategoryUpdateRequest(
    @Schema(description = "Nuevo nombre de la categoría (único, máx 20 caracteres)", example = "HYBRID_UPDATED")
    @field:jakarta.validation.constraints.Size(max = 20, message = "El nombre no puede exceder 20 caracteres")
    val name: String?
)

// ========== Device Type Request DTOs ==========

/**
 * DTO para crear un nuevo tipo de dispositivo.
 * @see <a href="https://docs.spring.io/spring-boot/reference/web/servlet.html#web.servlet.spring-mvc.binding">Spring Boot Binding</a>
 */
@Schema(description = "Request para crear un tipo de dispositivo")
data class DeviceTypeCreateRequest(
    @Schema(description = "Nombre único del tipo (máx 30 caracteres)", example = "NITROGEN_SENSOR", required = true)
    @field:jakarta.validation.constraints.NotBlank(message = "El nombre es obligatorio")
    @field:jakarta.validation.constraints.Size(max = 30, message = "El nombre no puede exceder 30 caracteres")
    val name: String,

    @Schema(description = "Descripción del tipo de dispositivo", example = "Sensor de nitrógeno en suelo")
    val description: String? = null,

    @Schema(description = "ID de la categoría (1=SENSOR, 2=ACTUATOR)", example = "1", required = true)
    @field:jakarta.validation.constraints.NotNull(message = "La categoría es obligatoria")
    val categoryId: Short,

    @Schema(description = "ID de la unidad por defecto", example = "5")
    val defaultUnitId: Short? = null,

    @Schema(description = "Tipo de dato: DECIMAL, INTEGER, BOOLEAN, TEXT, JSON", example = "DECIMAL")
    @field:jakarta.validation.constraints.Pattern(
        regexp = "^(DECIMAL|INTEGER|BOOLEAN|TEXT|JSON)$",
        message = "dataType debe ser: DECIMAL, INTEGER, BOOLEAN, TEXT o JSON"
    )
    val dataType: String? = "DECIMAL",

    @Schema(description = "Valor mínimo esperado físicamente", example = "0.00")
    val minExpectedValue: BigDecimal? = null,

    @Schema(description = "Valor máximo esperado físicamente", example = "100.00")
    val maxExpectedValue: BigDecimal? = null,

    @Schema(description = "Tipo de control para actuadores: BINARY, CONTINUOUS, MULTI_STATE", example = "CONTINUOUS")
    @field:jakarta.validation.constraints.Pattern(
        regexp = "^(BINARY|CONTINUOUS|MULTI_STATE)$",
        message = "controlType debe ser: BINARY, CONTINUOUS o MULTI_STATE"
    )
    val controlType: String? = null,

    @Schema(description = "Si el tipo está activo", example = "true")
    val isActive: Boolean = true
)

/**
 * DTO para actualizar un tipo de dispositivo existente.
 */
@Schema(description = "Request para actualizar un tipo de dispositivo")
data class DeviceTypeUpdateRequest(
    @Schema(description = "Nuevo nombre del tipo (máx 30 caracteres)", example = "NITROGEN_SENSOR_V2")
    @field:jakarta.validation.constraints.Size(max = 30, message = "El nombre no puede exceder 30 caracteres")
    val name: String? = null,

    @Schema(description = "Nueva descripción del tipo", example = "Sensor de nitrógeno mejorado")
    val description: String? = null,

    @Schema(description = "Nueva categoría (1=SENSOR, 2=ACTUATOR)", example = "1")
    val categoryId: Short? = null,

    @Schema(description = "Nuevo ID de unidad por defecto", example = "6")
    val defaultUnitId: Short? = null,

    @Schema(description = "Nuevo tipo de dato: DECIMAL, INTEGER, BOOLEAN, TEXT, JSON", example = "DECIMAL")
    @field:jakarta.validation.constraints.Pattern(
        regexp = "^(DECIMAL|INTEGER|BOOLEAN|TEXT|JSON)$",
        message = "dataType debe ser: DECIMAL, INTEGER, BOOLEAN, TEXT o JSON"
    )
    val dataType: String? = null,

    @Schema(description = "Nuevo valor mínimo esperado", example = "0.00")
    val minExpectedValue: BigDecimal? = null,

    @Schema(description = "Nuevo valor máximo esperado", example = "150.00")
    val maxExpectedValue: BigDecimal? = null,

    @Schema(description = "Nuevo tipo de control: BINARY, CONTINUOUS, MULTI_STATE", example = "BINARY")
    @field:jakarta.validation.constraints.Pattern(
        regexp = "^(BINARY|CONTINUOUS|MULTI_STATE)$",
        message = "controlType debe ser: BINARY, CONTINUOUS o MULTI_STATE"
    )
    val controlType: String? = null,

    @Schema(description = "Nuevo estado activo/inactivo", example = "true")
    val isActive: Boolean? = null
)

// ========== Alert Type Request DTOs ==========

/**
 * DTO para crear un nuevo tipo de alerta.
 * El ID se genera automáticamente por la base de datos (IDENTITY strategy).
 *
 * @see <a href="https://docs.spring.io/spring-boot/reference/web/servlet.html#web.servlet.spring-mvc.binding">Spring Boot Binding</a>
 */
@Schema(description = "Request para crear un tipo de alerta")
data class AlertTypeCreateRequest(
    @Schema(description = "Nombre único del tipo de alerta (máx 30 caracteres)", example = "MAINTENANCE_REQUIRED", required = true)
    @field:jakarta.validation.constraints.NotBlank(message = "El nombre es obligatorio")
    @field:jakarta.validation.constraints.Size(max = 30, message = "El nombre no puede exceder 30 caracteres")
    val name: String,

    @Schema(description = "Descripción del tipo de alerta", example = "Se requiere mantenimiento preventivo")
    val description: String? = null
)

/**
 * DTO para actualizar un tipo de alerta existente.
 */
@Schema(description = "Request para actualizar un tipo de alerta")
data class AlertTypeUpdateRequest(
    @Schema(description = "Nuevo nombre del tipo (máx 30 caracteres)", example = "MAINTENANCE_REQUIRED_V2")
    @field:jakarta.validation.constraints.Size(max = 30, message = "El nombre no puede exceder 30 caracteres")
    val name: String? = null,

    @Schema(description = "Nueva descripción del tipo de alerta", example = "Se requiere mantenimiento preventivo del equipo")
    val description: String? = null
)

// ========== Alert Severity Request DTOs ==========

/**
 * DTO para crear un nuevo nivel de severidad de alerta.
 * El ID se genera automáticamente por la base de datos (IDENTITY strategy).
 *
 * @see <a href="https://docs.spring.io/spring-boot/reference/web/servlet.html#web.servlet.spring-mvc.binding">Spring Boot Binding</a>
 */
@Schema(description = "Request para crear un nivel de severidad de alerta")
data class AlertSeverityCreateRequest(
    @Schema(description = "Nombre único de la severidad (máx 20 caracteres)", example = "URGENT", required = true)
    @field:jakarta.validation.constraints.NotBlank(message = "El nombre es obligatorio")
    @field:jakarta.validation.constraints.Size(max = 20, message = "El nombre no puede exceder 20 caracteres")
    val name: String,

    @Schema(description = "Nivel numérico para ordenación (1=más bajo, mayor=más alto)", example = "5", required = true)
    @field:jakarta.validation.constraints.NotNull(message = "El nivel es obligatorio")
    @field:jakarta.validation.constraints.Positive(message = "El nivel debe ser positivo")
    val level: Short,

    @Schema(description = "Descripción de la severidad", example = "Requiere atención urgente")
    val description: String? = null,

    @Schema(description = "Color hexadecimal para UI (7 caracteres con #)", example = "#FF00FF")
    @field:jakarta.validation.constraints.Pattern(
        regexp = "^#[0-9A-Fa-f]{6}$",
        message = "El color debe ser un código hexadecimal válido (ej: #FF0000)"
    )
    val color: String? = null,

    @Schema(description = "Si requiere acción inmediata", example = "true")
    val requiresAction: Boolean = false,

    @Schema(description = "Minutos de retraso antes de notificar", example = "0")
    @field:jakarta.validation.constraints.Min(value = 0, message = "El retraso no puede ser negativo")
    val notificationDelayMinutes: Int = 0
)

/**
 * DTO para actualizar un nivel de severidad existente.
 */
@Schema(description = "Request para actualizar un nivel de severidad de alerta")
data class AlertSeverityUpdateRequest(
    @Schema(description = "Nuevo nombre de la severidad (máx 20 caracteres)", example = "URGENT_V2")
    @field:jakarta.validation.constraints.Size(max = 20, message = "El nombre no puede exceder 20 caracteres")
    val name: String? = null,

    @Schema(description = "Nuevo nivel numérico para ordenación", example = "5")
    @field:jakarta.validation.constraints.Positive(message = "El nivel debe ser positivo")
    val level: Short? = null,

    @Schema(description = "Nueva descripción de la severidad", example = "Requiere atención urgente inmediata")
    val description: String? = null,

    @Schema(description = "Nuevo color hexadecimal para UI", example = "#FF00FF")
    @field:jakarta.validation.constraints.Pattern(
        regexp = "^#[0-9A-Fa-f]{6}$",
        message = "El color debe ser un código hexadecimal válido (ej: #FF0000)"
    )
    val color: String? = null,

    @Schema(description = "Si requiere acción inmediata", example = "true")
    val requiresAction: Boolean? = null,

    @Schema(description = "Minutos de retraso antes de notificar", example = "5")
    @field:jakarta.validation.constraints.Min(value = 0, message = "El retraso no puede ser negativo")
    val notificationDelayMinutes: Int? = null
)

// ========== Period Catalog DTOs ==========

@Schema(description = "Periodo del día para configuraciones (DAY, NIGHT, ALL)")
data class PeriodResponse(
    @Schema(description = "ID del periodo", example = "1")
    val id: Short,

    @Schema(description = "Nombre del periodo", example = "DAY")
    val name: String
)

fun Period.toResponse() = PeriodResponse(
    id = this.id ?: throw IllegalStateException("Period ID cannot be null"),
    name = this.name
)

/**
 * DTO para crear un nuevo periodo.
 * El ID se genera automáticamente por la base de datos (IDENTITY strategy).
 *
 * @see <a href="https://docs.spring.io/spring-boot/reference/web/servlet.html#web.servlet.spring-mvc.binding">Spring Boot Binding</a>
 */
@Schema(description = "Request para crear un periodo")
data class PeriodCreateRequest(
    @Schema(description = "Nombre único del periodo (máx 10 caracteres)", example = "MORNING", required = true)
    @field:jakarta.validation.constraints.NotBlank(message = "El nombre es obligatorio")
    @field:jakarta.validation.constraints.Size(max = 10, message = "El nombre no puede exceder 10 caracteres")
    val name: String
)

/**
 * DTO para actualizar un periodo existente.
 */
@Schema(description = "Request para actualizar un periodo")
data class PeriodUpdateRequest(
    @Schema(description = "Nuevo nombre del periodo (máx 10 caracteres)", example = "MORNING_V2")
    @field:jakarta.validation.constraints.Size(max = 10, message = "El nombre no puede exceder 10 caracteres")
    val name: String?
)

// ========== Unit Request DTOs ==========

/**
 * DTO para crear una nueva unidad de medida.
 * El ID se genera automáticamente por la base de datos (IDENTITY strategy).
 *
 * @see <a href="https://docs.spring.io/spring-boot/reference/web/servlet.html#web.servlet.spring-mvc.binding">Spring Boot Binding</a>
 */
@Schema(description = "Request para crear una unidad de medida")
data class UnitCreateRequest(
    @Schema(description = "Símbolo de la unidad (único, máx 10 caracteres)", example = "mg/L", required = true)
    @field:jakarta.validation.constraints.NotBlank(message = "El símbolo es obligatorio")
    @field:jakarta.validation.constraints.Size(max = 10, message = "El símbolo no puede exceder 10 caracteres")
    val symbol: String,

    @Schema(description = "Nombre de la unidad (máx 50 caracteres)", example = "Miligramos por litro", required = true)
    @field:jakarta.validation.constraints.NotBlank(message = "El nombre es obligatorio")
    @field:jakarta.validation.constraints.Size(max = 50, message = "El nombre no puede exceder 50 caracteres")
    val name: String,

    @Schema(description = "Descripción de la unidad", example = "Concentración de nutrientes")
    val description: String? = null,

    @Schema(description = "Si la unidad está activa", example = "true")
    val isActive: Boolean = true
)

/**
 * DTO para actualizar una unidad de medida existente.
 */
@Schema(description = "Request para actualizar una unidad de medida")
data class UnitUpdateRequest(
    @Schema(description = "Nuevo símbolo de la unidad (máx 10 caracteres)", example = "mg/L")
    @field:jakarta.validation.constraints.Size(max = 10, message = "El símbolo no puede exceder 10 caracteres")
    val symbol: String? = null,

    @Schema(description = "Nuevo nombre de la unidad (máx 50 caracteres)", example = "Miligramos por litro")
    @field:jakarta.validation.constraints.Size(max = 50, message = "El nombre no puede exceder 50 caracteres")
    val name: String? = null,

    @Schema(description = "Nueva descripción de la unidad", example = "Concentración de nutrientes en solución")
    val description: String? = null,

    @Schema(description = "Nuevo estado activo/inactivo", example = "true")
    val isActive: Boolean? = null
)

// ========== Actuator State DTOs ==========

@Schema(description = "Estado de actuador")
data class ActuatorStateResponse(
    @Schema(description = "ID del estado", example = "1")
    val id: Short,

    @Schema(description = "Nombre del estado", example = "ON")
    val name: String,

    @Schema(description = "Descripción del estado", example = "Encendido")
    val description: String?,

    @Schema(description = "Si el actuador está funcionando en este estado", example = "true")
    val isOperational: Boolean,

    @Schema(description = "Orden para mostrar en UI", example = "1")
    val displayOrder: Short,

    @Schema(description = "Color hexadecimal para UI", example = "#00FF00")
    val color: String?
)

fun com.apptolast.invernaderos.features.catalog.ActuatorState.toResponse() = ActuatorStateResponse(
    id = this.id ?: throw IllegalStateException("ActuatorState ID cannot be null"),
    name = this.name,
    description = this.description,
    isOperational = this.isOperational,
    displayOrder = this.displayOrder,
    color = this.color
)

/**
 * DTO para crear un nuevo estado de actuador.
 * El ID se genera automáticamente por la base de datos (IDENTITY strategy).
 *
 * @see <a href="https://docs.spring.io/spring-boot/reference/web/servlet.html#web.servlet.spring-mvc.binding">Spring Boot Binding</a>
 */
@Schema(description = "Request para crear un estado de actuador")
data class ActuatorStateCreateRequest(
    @Schema(description = "Nombre del estado (único, máx 20 caracteres)", example = "WARMING_UP", required = true)
    @field:jakarta.validation.constraints.NotBlank(message = "El nombre es obligatorio")
    @field:jakarta.validation.constraints.Size(max = 20, message = "El nombre no puede exceder 20 caracteres")
    val name: String,

    @Schema(description = "Descripción del estado", example = "Calentando el sistema")
    val description: String? = null,

    @Schema(description = "Si el actuador está funcionando en este estado", example = "true")
    val isOperational: Boolean = false,

    @Schema(description = "Orden para mostrar en UI (mayor = más abajo)", example = "5")
    @field:jakarta.validation.constraints.Min(value = 0, message = "El orden no puede ser negativo")
    val displayOrder: Short = 0,

    @Schema(description = "Color hexadecimal para UI (7 caracteres con #)", example = "#FFA500")
    @field:jakarta.validation.constraints.Pattern(
        regexp = "^#[0-9A-Fa-f]{6}$",
        message = "El color debe ser un código hexadecimal válido (ej: #FF0000)"
    )
    val color: String? = null
)

/**
 * DTO para actualizar un estado de actuador existente.
 */
@Schema(description = "Request para actualizar un estado de actuador")
data class ActuatorStateUpdateRequest(
    @Schema(description = "Nuevo nombre del estado (max 20 caracteres)", example = "WARMING_UP_V2")
    @field:jakarta.validation.constraints.Size(max = 20, message = "El nombre no puede exceder 20 caracteres")
    val name: String? = null,

    @Schema(description = "Nueva descripcion del estado", example = "Calentando el sistema - version 2")
    val description: String? = null,

    @Schema(description = "Nuevo estado operacional", example = "true")
    val isOperational: Boolean? = null,

    @Schema(description = "Nuevo orden para mostrar en UI", example = "6")
    @field:jakarta.validation.constraints.Min(value = 0, message = "El orden no puede ser negativo")
    val displayOrder: Short? = null,

    @Schema(description = "Nuevo color hexadecimal para UI", example = "#FFA500")
    @field:jakarta.validation.constraints.Pattern(
        regexp = "^#[0-9A-Fa-f]{6}$",
        message = "El color debe ser un codigo hexadecimal valido (ej: #FF0000)"
    )
    val color: String? = null
)

// ========== Data Type DTOs ==========

@Schema(description = "Tipo de dato para valores de configuracion")
data class DataTypeResponse(
    @Schema(description = "ID del tipo de dato", example = "1")
    val id: Short,

    @Schema(description = "Nombre del tipo", example = "INTEGER")
    val name: String,

    @Schema(description = "Descripcion del tipo", example = "Numero entero")
    val description: String?,

    @Schema(description = "Expresion regular para validar valores", example = "^-?\\d+$")
    val validationRegex: String?,

    @Schema(description = "Ejemplo de valor valido", example = "25")
    val exampleValue: String?,

    @Schema(description = "Orden para mostrar en UI", example = "1")
    val displayOrder: Short,

    @Schema(description = "Si el tipo esta activo", example = "true")
    val isActive: Boolean
)

fun com.apptolast.invernaderos.features.catalog.DataType.toResponse() = DataTypeResponse(
    id = this.id ?: throw IllegalStateException("DataType ID cannot be null"),
    name = this.name,
    description = this.description,
    validationRegex = this.validationRegex,
    exampleValue = this.exampleValue,
    displayOrder = this.displayOrder,
    isActive = this.isActive
)

/**
 * DTO para crear un nuevo tipo de dato.
 */
@Schema(description = "Request para crear un tipo de dato")
data class DataTypeCreateRequest(
    @Schema(description = "Nombre del tipo (unico, max 20 caracteres)", example = "PERCENTAGE", required = true)
    @field:jakarta.validation.constraints.NotBlank(message = "El nombre es obligatorio")
    @field:jakarta.validation.constraints.Size(max = 20, message = "El nombre no puede exceder 20 caracteres")
    val name: String,

    @Schema(description = "Descripcion del tipo", example = "Valor porcentual 0-100")
    val description: String? = null,

    @Schema(description = "Expresion regular para validar valores", example = "^\\d{1,3}$")
    val validationRegex: String? = null,

    @Schema(description = "Ejemplo de valor valido", example = "75")
    val exampleValue: String? = null,

    @Schema(description = "Orden para mostrar en UI", example = "10")
    @field:jakarta.validation.constraints.Min(value = 0, message = "El orden no puede ser negativo")
    val displayOrder: Short? = 0,

    @Schema(description = "Si el tipo esta activo", example = "true")
    val isActive: Boolean? = true
)

/**
 * DTO para actualizar un tipo de dato existente.
 */
@Schema(description = "Request para actualizar un tipo de dato")
data class DataTypeUpdateRequest(
    @Schema(description = "Nuevo nombre del tipo (max 20 caracteres)", example = "PERCENTAGE_V2")
    @field:jakarta.validation.constraints.Size(max = 20, message = "El nombre no puede exceder 20 caracteres")
    val name: String? = null,

    @Schema(description = "Nueva descripcion del tipo", example = "Valor porcentual 0-100 con decimales")
    val description: String? = null,

    @Schema(description = "Nueva expresion regular para validar valores", example = "^\\d{1,3}(\\.\\d+)?$")
    val validationRegex: String? = null,

    @Schema(description = "Nuevo ejemplo de valor valido", example = "75.5")
    val exampleValue: String? = null,

    @Schema(description = "Nuevo orden para mostrar en UI", example = "11")
    @field:jakarta.validation.constraints.Min(value = 0, message = "El orden no puede ser negativo")
    val displayOrder: Short? = null,

    @Schema(description = "Nuevo estado activo/inactivo", example = "true")
    val isActive: Boolean? = null
)
