package com.apptolast.invernaderos.features.catalog

import com.apptolast.invernaderos.features.catalog.dto.*
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

/**
 * Controller para endpoints de catálogos.
 * Proporciona operaciones CRUD para categorías de dispositivos, tipos de dispositivos,
 * unidades, tipos de alerta, severidades y periodos.
 *
 * @see <a href="https://docs.spring.io/spring-boot/reference/web/servlet.html">Spring Boot Web Documentation</a>
 */
@RestController
@RequestMapping("/api/v1/catalog")
@Tag(name = "Catalog", description = "CRUD de catálogos del sistema (categorías, tipos, unidades, alertas, periodos)")
class CatalogController(
    private val deviceCategoryRepository: DeviceCategoryRepository,
    private val deviceTypeRepository: DeviceTypeRepository,
    private val unitRepository: UnitRepository,
    private val alertTypeRepository: AlertTypeRepository,
    private val alertSeverityRepository: AlertSeverityRepository,
    private val periodRepository: PeriodRepository,
    private val deviceCategoryService: DeviceCategoryService,
    private val deviceTypeService: DeviceTypeService,
    private val alertTypeService: AlertTypeService,
    private val alertSeverityService: AlertSeverityService,
    private val periodService: PeriodService
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

    // ========== Device Category CRUD Endpoints ==========

    @GetMapping("/device-categories/{id}")
    @Operation(
        summary = "Obtener una categoría por ID",
        description = "Retorna la categoría de dispositivo con el ID especificado"
    )
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "Categoría encontrada"),
        ApiResponse(responseCode = "404", description = "Categoría no encontrada")
    )
    fun getDeviceCategoryById(
        @Parameter(description = "ID de la categoría", example = "1")
        @PathVariable id: Short
    ): ResponseEntity<DeviceCategoryResponse> {
        val category = deviceCategoryService.findById(id)
            ?: return ResponseEntity.notFound().build()
        return ResponseEntity.ok(category)
    }

    @PostMapping("/device-categories")
    @Operation(
        summary = "Crear nueva categoría de dispositivo",
        description = "Crea una nueva categoría. El ID es obligatorio y debe ser único."
    )
    @ApiResponses(
        ApiResponse(responseCode = "201", description = "Categoría creada exitosamente"),
        ApiResponse(responseCode = "400", description = "Datos inválidos o ID/nombre duplicado")
    )
    fun createDeviceCategory(
        @Valid @RequestBody request: DeviceCategoryCreateRequest
    ): ResponseEntity<DeviceCategoryResponse> {
        return try {
            val created = deviceCategoryService.create(request)
            ResponseEntity.status(HttpStatus.CREATED).body(created)
        } catch (e: IllegalArgumentException) {
            ResponseEntity.badRequest().build()
        }
    }

    @PutMapping("/device-categories/{id}")
    @Operation(
        summary = "Actualizar categoría de dispositivo",
        description = "Actualiza una categoría existente. Solo se actualizan los campos proporcionados."
    )
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "Categoría actualizada exitosamente"),
        ApiResponse(responseCode = "400", description = "Datos inválidos o nombre duplicado"),
        ApiResponse(responseCode = "404", description = "Categoría no encontrada")
    )
    fun updateDeviceCategory(
        @Parameter(description = "ID de la categoría a actualizar", example = "1")
        @PathVariable id: Short,
        @Valid @RequestBody request: DeviceCategoryUpdateRequest
    ): ResponseEntity<DeviceCategoryResponse> {
        return try {
            val updated = deviceCategoryService.update(id, request)
                ?: return ResponseEntity.notFound().build()
            ResponseEntity.ok(updated)
        } catch (e: IllegalArgumentException) {
            ResponseEntity.badRequest().build()
        }
    }

    @DeleteMapping("/device-categories/{id}")
    @Operation(
        summary = "Eliminar categoría de dispositivo",
        description = "Elimina una categoría. No se puede eliminar si tiene tipos de dispositivo asociados."
    )
    @ApiResponses(
        ApiResponse(responseCode = "204", description = "Categoría eliminada exitosamente"),
        ApiResponse(responseCode = "404", description = "Categoría no encontrada"),
        ApiResponse(responseCode = "409", description = "Conflicto: la categoría tiene tipos asociados")
    )
    fun deleteDeviceCategory(
        @Parameter(description = "ID de la categoría a eliminar", example = "3")
        @PathVariable id: Short
    ): ResponseEntity<Void> {
        return try {
            val deleted = deviceCategoryService.delete(id)
            if (deleted) {
                ResponseEntity.noContent().build()
            } else {
                ResponseEntity.notFound().build()
            }
        } catch (e: IllegalStateException) {
            ResponseEntity.status(HttpStatus.CONFLICT).build()
        }
    }

    // ========== Device Type CRUD Endpoints ==========

    @GetMapping("/device-types/{id}")
    @Operation(
        summary = "Obtener un tipo de dispositivo por ID",
        description = "Retorna el tipo de dispositivo con el ID especificado, incluyendo información de la categoría y unidad"
    )
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "Tipo encontrado"),
        ApiResponse(responseCode = "404", description = "Tipo no encontrado")
    )
    fun getDeviceTypeById(
        @Parameter(description = "ID del tipo de dispositivo", example = "1")
        @PathVariable id: Short
    ): ResponseEntity<DeviceTypeResponse> {
        val type = deviceTypeService.findById(id)
            ?: return ResponseEntity.notFound().build()
        return ResponseEntity.ok(type)
    }

    @PostMapping("/device-types")
    @Operation(
        summary = "Crear nuevo tipo de dispositivo",
        description = "Crea un nuevo tipo de dispositivo. El nombre debe ser único y la categoría debe existir."
    )
    @ApiResponses(
        ApiResponse(responseCode = "201", description = "Tipo creado exitosamente"),
        ApiResponse(responseCode = "400", description = "Datos inválidos, nombre duplicado, o categoría/unidad inexistente")
    )
    fun createDeviceType(
        @Valid @RequestBody request: DeviceTypeCreateRequest
    ): ResponseEntity<DeviceTypeResponse> {
        return try {
            val created = deviceTypeService.create(request)
            ResponseEntity.status(HttpStatus.CREATED).body(created)
        } catch (e: IllegalArgumentException) {
            ResponseEntity.badRequest().build()
        }
    }

    @PutMapping("/device-types/{id}")
    @Operation(
        summary = "Actualizar tipo de dispositivo",
        description = "Actualiza un tipo existente. Solo se actualizan los campos proporcionados."
    )
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "Tipo actualizado exitosamente"),
        ApiResponse(responseCode = "400", description = "Datos inválidos, nombre duplicado, o categoría/unidad inexistente"),
        ApiResponse(responseCode = "404", description = "Tipo no encontrado")
    )
    fun updateDeviceType(
        @Parameter(description = "ID del tipo a actualizar", example = "1")
        @PathVariable id: Short,
        @Valid @RequestBody request: DeviceTypeUpdateRequest
    ): ResponseEntity<DeviceTypeResponse> {
        return try {
            val updated = deviceTypeService.update(id, request)
                ?: return ResponseEntity.notFound().build()
            ResponseEntity.ok(updated)
        } catch (e: IllegalArgumentException) {
            ResponseEntity.badRequest().build()
        }
    }

    @DeleteMapping("/device-types/{id}")
    @Operation(
        summary = "Eliminar tipo de dispositivo",
        description = "Elimina un tipo de dispositivo. No se puede eliminar si tiene dispositivos asociados."
    )
    @ApiResponses(
        ApiResponse(responseCode = "204", description = "Tipo eliminado exitosamente"),
        ApiResponse(responseCode = "404", description = "Tipo no encontrado"),
        ApiResponse(responseCode = "409", description = "Conflicto: el tipo tiene dispositivos asociados")
    )
    fun deleteDeviceType(
        @Parameter(description = "ID del tipo a eliminar", example = "28")
        @PathVariable id: Short
    ): ResponseEntity<Void> {
        return try {
            val deleted = deviceTypeService.delete(id)
            if (deleted) {
                ResponseEntity.noContent().build()
            } else {
                ResponseEntity.notFound().build()
            }
        } catch (e: IllegalStateException) {
            ResponseEntity.status(HttpStatus.CONFLICT).build()
        }
    }

    @PatchMapping("/device-types/{id}/deactivate")
    @Operation(
        summary = "Desactivar tipo de dispositivo",
        description = "Desactiva un tipo de dispositivo (soft delete). Los dispositivos existentes no se ven afectados."
    )
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "Tipo desactivado exitosamente"),
        ApiResponse(responseCode = "404", description = "Tipo no encontrado")
    )
    fun deactivateDeviceType(
        @Parameter(description = "ID del tipo a desactivar", example = "1")
        @PathVariable id: Short
    ): ResponseEntity<DeviceTypeResponse> {
        val deactivated = deviceTypeService.deactivate(id)
            ?: return ResponseEntity.notFound().build()
        return ResponseEntity.ok(deactivated)
    }

    @PatchMapping("/device-types/{id}/activate")
    @Operation(
        summary = "Activar tipo de dispositivo",
        description = "Activa un tipo de dispositivo previamente desactivado."
    )
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "Tipo activado exitosamente"),
        ApiResponse(responseCode = "404", description = "Tipo no encontrado")
    )
    fun activateDeviceType(
        @Parameter(description = "ID del tipo a activar", example = "1")
        @PathVariable id: Short
    ): ResponseEntity<DeviceTypeResponse> {
        val activated = deviceTypeService.activate(id)
            ?: return ResponseEntity.notFound().build()
        return ResponseEntity.ok(activated)
    }

    // ========== Alert Type CRUD Endpoints ==========

    @GetMapping("/alert-types/{id}")
    @Operation(
        summary = "Obtener un tipo de alerta por ID",
        description = "Retorna el tipo de alerta con el ID especificado"
    )
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "Tipo de alerta encontrado"),
        ApiResponse(responseCode = "404", description = "Tipo de alerta no encontrado")
    )
    fun getAlertTypeById(
        @Parameter(description = "ID del tipo de alerta", example = "1")
        @PathVariable id: Short
    ): ResponseEntity<AlertTypeResponse> {
        val type = alertTypeService.findById(id)
            ?: return ResponseEntity.notFound().build()
        return ResponseEntity.ok(type)
    }

    @PostMapping("/alert-types")
    @Operation(
        summary = "Crear nuevo tipo de alerta",
        description = "Crea un nuevo tipo de alerta. El ID y nombre deben ser únicos."
    )
    @ApiResponses(
        ApiResponse(responseCode = "201", description = "Tipo de alerta creado exitosamente"),
        ApiResponse(responseCode = "400", description = "Datos inválidos o ID/nombre duplicado")
    )
    fun createAlertType(
        @Valid @RequestBody request: AlertTypeCreateRequest
    ): ResponseEntity<AlertTypeResponse> {
        return try {
            val created = alertTypeService.create(request)
            ResponseEntity.status(HttpStatus.CREATED).body(created)
        } catch (e: IllegalArgumentException) {
            ResponseEntity.badRequest().build()
        }
    }

    @PutMapping("/alert-types/{id}")
    @Operation(
        summary = "Actualizar tipo de alerta",
        description = "Actualiza un tipo de alerta existente. Solo se actualizan los campos proporcionados."
    )
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "Tipo de alerta actualizado exitosamente"),
        ApiResponse(responseCode = "400", description = "Datos inválidos o nombre duplicado"),
        ApiResponse(responseCode = "404", description = "Tipo de alerta no encontrado")
    )
    fun updateAlertType(
        @Parameter(description = "ID del tipo de alerta a actualizar", example = "1")
        @PathVariable id: Short,
        @Valid @RequestBody request: AlertTypeUpdateRequest
    ): ResponseEntity<AlertTypeResponse> {
        return try {
            val updated = alertTypeService.update(id, request)
                ?: return ResponseEntity.notFound().build()
            ResponseEntity.ok(updated)
        } catch (e: IllegalArgumentException) {
            ResponseEntity.badRequest().build()
        }
    }

    @DeleteMapping("/alert-types/{id}")
    @Operation(
        summary = "Eliminar tipo de alerta",
        description = "Elimina un tipo de alerta. No se puede eliminar si tiene alertas asociadas."
    )
    @ApiResponses(
        ApiResponse(responseCode = "204", description = "Tipo de alerta eliminado exitosamente"),
        ApiResponse(responseCode = "404", description = "Tipo de alerta no encontrado"),
        ApiResponse(responseCode = "409", description = "Conflicto: el tipo tiene alertas asociadas")
    )
    fun deleteAlertType(
        @Parameter(description = "ID del tipo de alerta a eliminar", example = "7")
        @PathVariable id: Short
    ): ResponseEntity<Void> {
        return try {
            val deleted = alertTypeService.delete(id)
            if (deleted) {
                ResponseEntity.noContent().build()
            } else {
                ResponseEntity.notFound().build()
            }
        } catch (e: IllegalStateException) {
            ResponseEntity.status(HttpStatus.CONFLICT).build()
        }
    }

    // ========== Alert Severity CRUD Endpoints ==========

    @GetMapping("/alert-severities/{id}")
    @Operation(
        summary = "Obtener un nivel de severidad por ID",
        description = "Retorna el nivel de severidad con el ID especificado"
    )
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "Severidad encontrada"),
        ApiResponse(responseCode = "404", description = "Severidad no encontrada")
    )
    fun getAlertSeverityById(
        @Parameter(description = "ID del nivel de severidad", example = "1")
        @PathVariable id: Short
    ): ResponseEntity<AlertSeverityResponse> {
        val severity = alertSeverityService.findById(id)
            ?: return ResponseEntity.notFound().build()
        return ResponseEntity.ok(severity)
    }

    @PostMapping("/alert-severities")
    @Operation(
        summary = "Crear nuevo nivel de severidad",
        description = "Crea un nuevo nivel de severidad. El ID y nombre deben ser únicos."
    )
    @ApiResponses(
        ApiResponse(responseCode = "201", description = "Severidad creada exitosamente"),
        ApiResponse(responseCode = "400", description = "Datos inválidos o ID/nombre duplicado")
    )
    fun createAlertSeverity(
        @Valid @RequestBody request: AlertSeverityCreateRequest
    ): ResponseEntity<AlertSeverityResponse> {
        return try {
            val created = alertSeverityService.create(request)
            ResponseEntity.status(HttpStatus.CREATED).body(created)
        } catch (e: IllegalArgumentException) {
            ResponseEntity.badRequest().build()
        }
    }

    @PutMapping("/alert-severities/{id}")
    @Operation(
        summary = "Actualizar nivel de severidad",
        description = "Actualiza un nivel de severidad existente. Solo se actualizan los campos proporcionados."
    )
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "Severidad actualizada exitosamente"),
        ApiResponse(responseCode = "400", description = "Datos inválidos o nombre duplicado"),
        ApiResponse(responseCode = "404", description = "Severidad no encontrada")
    )
    fun updateAlertSeverity(
        @Parameter(description = "ID del nivel de severidad a actualizar", example = "1")
        @PathVariable id: Short,
        @Valid @RequestBody request: AlertSeverityUpdateRequest
    ): ResponseEntity<AlertSeverityResponse> {
        return try {
            val updated = alertSeverityService.update(id, request)
                ?: return ResponseEntity.notFound().build()
            ResponseEntity.ok(updated)
        } catch (e: IllegalArgumentException) {
            ResponseEntity.badRequest().build()
        }
    }

    @DeleteMapping("/alert-severities/{id}")
    @Operation(
        summary = "Eliminar nivel de severidad",
        description = "Elimina un nivel de severidad. No se puede eliminar si tiene alertas asociadas."
    )
    @ApiResponses(
        ApiResponse(responseCode = "204", description = "Severidad eliminada exitosamente"),
        ApiResponse(responseCode = "404", description = "Severidad no encontrada"),
        ApiResponse(responseCode = "409", description = "Conflicto: la severidad tiene alertas asociadas")
    )
    fun deleteAlertSeverity(
        @Parameter(description = "ID del nivel de severidad a eliminar", example = "5")
        @PathVariable id: Short
    ): ResponseEntity<Void> {
        return try {
            val deleted = alertSeverityService.delete(id)
            if (deleted) {
                ResponseEntity.noContent().build()
            } else {
                ResponseEntity.notFound().build()
            }
        } catch (e: IllegalStateException) {
            ResponseEntity.status(HttpStatus.CONFLICT).build()
        }
    }

    // ========== Period CRUD Endpoints ==========

    @GetMapping("/periods")
    @Operation(
        summary = "Obtener todos los periodos",
        description = "Retorna los periodos disponibles: DAY (1), NIGHT (2), ALL (3)"
    )
    fun getAllPeriods(): ResponseEntity<List<PeriodResponse>> {
        val periods = periodRepository.findAll().map { it.toResponse() }
        return ResponseEntity.ok(periods)
    }

    @GetMapping("/periods/{id}")
    @Operation(
        summary = "Obtener un periodo por ID",
        description = "Retorna el periodo con el ID especificado"
    )
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "Periodo encontrado"),
        ApiResponse(responseCode = "404", description = "Periodo no encontrado")
    )
    fun getPeriodById(
        @Parameter(description = "ID del periodo", example = "1")
        @PathVariable id: Short
    ): ResponseEntity<PeriodResponse> {
        val period = periodService.findById(id)
            ?: return ResponseEntity.notFound().build()
        return ResponseEntity.ok(period)
    }

    @PostMapping("/periods")
    @Operation(
        summary = "Crear nuevo periodo",
        description = "Crea un nuevo periodo. El ID y nombre deben ser únicos."
    )
    @ApiResponses(
        ApiResponse(responseCode = "201", description = "Periodo creado exitosamente"),
        ApiResponse(responseCode = "400", description = "Datos inválidos o ID/nombre duplicado")
    )
    fun createPeriod(
        @Valid @RequestBody request: PeriodCreateRequest
    ): ResponseEntity<PeriodResponse> {
        return try {
            val created = periodService.create(request)
            ResponseEntity.status(HttpStatus.CREATED).body(created)
        } catch (e: IllegalArgumentException) {
            ResponseEntity.badRequest().build()
        }
    }

    @PutMapping("/periods/{id}")
    @Operation(
        summary = "Actualizar periodo",
        description = "Actualiza un periodo existente. Solo se actualizan los campos proporcionados."
    )
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "Periodo actualizado exitosamente"),
        ApiResponse(responseCode = "400", description = "Datos inválidos o nombre duplicado"),
        ApiResponse(responseCode = "404", description = "Periodo no encontrado")
    )
    fun updatePeriod(
        @Parameter(description = "ID del periodo a actualizar", example = "1")
        @PathVariable id: Short,
        @Valid @RequestBody request: PeriodUpdateRequest
    ): ResponseEntity<PeriodResponse> {
        return try {
            val updated = periodService.update(id, request)
                ?: return ResponseEntity.notFound().build()
            ResponseEntity.ok(updated)
        } catch (e: IllegalArgumentException) {
            ResponseEntity.badRequest().build()
        }
    }

    @DeleteMapping("/periods/{id}")
    @Operation(
        summary = "Eliminar periodo",
        description = "Elimina un periodo. No se puede eliminar si tiene settings asociados."
    )
    @ApiResponses(
        ApiResponse(responseCode = "204", description = "Periodo eliminado exitosamente"),
        ApiResponse(responseCode = "404", description = "Periodo no encontrado"),
        ApiResponse(responseCode = "409", description = "Conflicto: el periodo tiene settings asociados")
    )
    fun deletePeriod(
        @Parameter(description = "ID del periodo a eliminar", example = "4")
        @PathVariable id: Short
    ): ResponseEntity<Void> {
        return try {
            val deleted = periodService.delete(id)
            if (deleted) {
                ResponseEntity.noContent().build()
            } else {
                ResponseEntity.notFound().build()
            }
        } catch (e: IllegalStateException) {
            ResponseEntity.status(HttpStatus.CONFLICT).build()
        }
    }
}
