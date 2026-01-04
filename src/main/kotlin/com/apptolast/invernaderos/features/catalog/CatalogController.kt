package com.apptolast.invernaderos.features.catalog

import com.apptolast.invernaderos.features.catalog.dto.AlertSeverityResponse
import com.apptolast.invernaderos.features.catalog.dto.AlertTypeResponse
import com.apptolast.invernaderos.features.catalog.dto.DeviceCategoryResponse
import com.apptolast.invernaderos.features.catalog.dto.DeviceTypeResponse
import com.apptolast.invernaderos.features.catalog.dto.UnitResponse
import com.apptolast.invernaderos.features.catalog.dto.toResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

/**
 * Controller para endpoints de catálogos.
 * Proporciona datos de referencia para formularios del frontend.
 *
 * @see <a href="https://docs.spring.io/spring-boot/docs/current/reference/html/web.html">Spring Boot Web Documentation</a>
 */
@RestController
@RequestMapping("/api/v1/catalog")
@Tag(name = "Catalog", description = "Endpoints para obtener catálogos del sistema (categorías, tipos, unidades)")
class CatalogController(
    private val deviceCategoryRepository: DeviceCategoryRepository,
    private val deviceTypeRepository: DeviceTypeRepository,
    private val unitRepository: UnitRepository,
    private val alertTypeRepository: AlertTypeRepository,
    private val alertSeverityRepository: AlertSeverityRepository
) {

    @GetMapping("/device-categories")
    @Operation(
        summary = "Obtener todas las categorías de dispositivos",
        description = "Retorna las categorías disponibles: SENSOR (id=1) y ACTUATOR (id=2)"
    )
    fun getAllDeviceCategories(): ResponseEntity<List<DeviceCategoryResponse>> {
        val categories = deviceCategoryRepository.findAll().map { it.toResponse() }
        return ResponseEntity.ok(categories)
    }

    @GetMapping("/device-types")
    @Operation(
        summary = "Obtener tipos de dispositivos",
        description = "Retorna todos los tipos o filtrados por categoría. Incluye información de la unidad por defecto."
    )
    fun getDeviceTypes(
        @Parameter(description = "Filtrar por categoría: 1=SENSOR, 2=ACTUATOR")
        @RequestParam(required = false) categoryId: Short?,

        @Parameter(description = "Filtrar solo tipos activos")
        @RequestParam(required = false, defaultValue = "true") activeOnly: Boolean
    ): ResponseEntity<List<DeviceTypeResponse>> {
        val types = when {
            categoryId != null && activeOnly -> deviceTypeRepository.findByCategoryIdAndIsActive(categoryId, true)
            categoryId != null -> deviceTypeRepository.findByCategoryId(categoryId)
            activeOnly -> deviceTypeRepository.findByIsActive(true)
            else -> deviceTypeRepository.findAll()
        }
        return ResponseEntity.ok(types.map { it.toResponse() })
    }

    @GetMapping("/device-types/sensors")
    @Operation(
        summary = "Obtener tipos de sensores",
        description = "Atajo para obtener solo tipos de categoría SENSOR (categoryId=1)"
    )
    fun getSensorTypes(): ResponseEntity<List<DeviceTypeResponse>> {
        val types = deviceTypeRepository.findByCategoryIdAndIsActive(DeviceCategory.SENSOR, true)
        return ResponseEntity.ok(types.map { it.toResponse() })
    }

    @GetMapping("/device-types/actuators")
    @Operation(
        summary = "Obtener tipos de actuadores",
        description = "Atajo para obtener solo tipos de categoría ACTUATOR (categoryId=2)"
    )
    fun getActuatorTypes(): ResponseEntity<List<DeviceTypeResponse>> {
        val types = deviceTypeRepository.findByCategoryIdAndIsActive(DeviceCategory.ACTUATOR, true)
        return ResponseEntity.ok(types.map { it.toResponse() })
    }

    @GetMapping("/units")
    @Operation(
        summary = "Obtener todas las unidades de medida",
        description = "Retorna las unidades disponibles (°C, %, hPa, ppm, etc.)"
    )
    fun getAllUnits(
        @Parameter(description = "Filtrar solo unidades activas")
        @RequestParam(required = false, defaultValue = "true") activeOnly: Boolean
    ): ResponseEntity<List<UnitResponse>> {
        val units = if (activeOnly) {
            unitRepository.findByIsActive(true)
        } else {
            unitRepository.findAll()
        }
        return ResponseEntity.ok(units.map { it.toResponse() })
    }

    // ========== Alert Catalog Endpoints ==========

    @GetMapping("/alert-types")
    @Operation(
        summary = "Obtener todos los tipos de alerta",
        description = "Retorna los tipos de alerta disponibles: THRESHOLD_EXCEEDED, SENSOR_OFFLINE, ACTUATOR_FAILURE, SYSTEM_ERROR, etc."
    )
    fun getAllAlertTypes(): ResponseEntity<List<AlertTypeResponse>> {
        val types = alertTypeRepository.findAll().map { it.toResponse() }
        return ResponseEntity.ok(types)
    }

    @GetMapping("/alert-severities")
    @Operation(
        summary = "Obtener todos los niveles de severidad",
        description = "Retorna los niveles de severidad ordenados por nivel: INFO (1), WARNING (2), ERROR (3), CRITICAL (4)"
    )
    fun getAllAlertSeverities(): ResponseEntity<List<AlertSeverityResponse>> {
        val severities = alertSeverityRepository.findAllByOrderByLevelAsc().map { it.toResponse() }
        return ResponseEntity.ok(severities)
    }

    @GetMapping("/alert-severities/critical")
    @Operation(
        summary = "Obtener severidades que requieren acción",
        description = "Retorna solo los niveles de severidad que requieren acción inmediata (requiresAction=true)"
    )
    fun getCriticalSeverities(): ResponseEntity<List<AlertSeverityResponse>> {
        val severities = alertSeverityRepository.findByRequiresAction(true).map { it.toResponse() }
        return ResponseEntity.ok(severities)
    }
}
