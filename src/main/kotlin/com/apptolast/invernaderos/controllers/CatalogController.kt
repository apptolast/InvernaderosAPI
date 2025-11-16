package com.apptolast.invernaderos.controllers

import com.apptolast.invernaderos.entities.metadata.catalog.*
import com.apptolast.invernaderos.repositories.metadata.*
import org.slf4j.LoggerFactory
import org.springframework.cache.annotation.Cacheable
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

/**
 * REST Controller para acceder a catálogos de referencia
 *
 * Los catálogos son tablas pequeñas (< 100 registros) con datos de referencia
 * que cambian muy poco, por lo que se cachean agresivamente.
 *
 * Endpoints disponibles:
 * - GET /api/catalog/units - Unidades de medida
 * - GET /api/catalog/sensor-types - Tipos de sensores
 * - GET /api/catalog/actuator-types - Tipos de actuadores
 * - GET /api/catalog/actuator-states - Estados de actuadores
 * - GET /api/catalog/alert-severities - Niveles de severidad
 * - GET /api/catalog/alert-types - Tipos de alertas
 */
@RestController
@RequestMapping("/api/catalog")
@CrossOrigin(origins = ["*"])
class CatalogController(
    private val unitRepository: UnitRepository,
    private val sensorTypeRepository: SensorTypeRepository,
    private val actuatorTypeRepository: ActuatorTypeRepository,
    private val actuatorStateRepository: ActuatorStateRepository,
    private val alertSeverityRepository: AlertSeverityRepository,
    private val alertTypeRepository: AlertTypeRepository
) {

    private val logger = LoggerFactory.getLogger(CatalogController::class.java)

    /**
     * GET /api/catalog/units
     *
     * Obtiene todas las unidades de medida (°C, %, ppm, etc.)
     *
     * Response: List<Unit>
     * Cache: 1 hora (los catálogos cambian muy poco)
     */
    @GetMapping("/units")
    @Cacheable("units", unless = "#result == null")
    fun getAllUnits(
        @RequestParam(required = false, defaultValue = "false") activeOnly: Boolean
    ): ResponseEntity<List<Unit>> {
        logger.debug("GET /api/catalog/units?activeOnly={}", activeOnly)

        return try {
            val units = if (activeOnly) {
                unitRepository.findByIsActiveTrue()
            } else {
                unitRepository.findAll()
            }
            ResponseEntity.ok(units)
        } catch (e: Exception) {
            logger.error("Error obteniendo units", e)
            ResponseEntity.internalServerError().build()
        }
    }

    /**
     * GET /api/catalog/units/{id}
     *
     * Obtiene una unidad específica por ID
     */
    @GetMapping("/units/{id}")
    fun getUnitById(@PathVariable id: Short): ResponseEntity<Unit> {
        logger.debug("GET /api/catalog/units/{}", id)

        return unitRepository.findById(id)
            .map { ResponseEntity.ok(it) }
            .orElse(ResponseEntity.notFound().build())
    }

    /**
     * GET /api/catalog/sensor-types
     *
     * Obtiene todos los tipos de sensores (TEMPERATURE, HUMIDITY, etc.)
     *
     * Response: List<SensorType>
     */
    @GetMapping("/sensor-types")
    @Cacheable("sensorTypes", unless = "#result == null")
    fun getAllSensorTypes(
        @RequestParam(required = false, defaultValue = "false") activeOnly: Boolean
    ): ResponseEntity<List<SensorType>> {
        logger.debug("GET /api/catalog/sensor-types?activeOnly={}", activeOnly)

        return try {
            val types = if (activeOnly) {
                sensorTypeRepository.findByIsActiveTrue()
            } else {
                sensorTypeRepository.findAll()
            }
            ResponseEntity.ok(types)
        } catch (e: Exception) {
            logger.error("Error obteniendo sensor types", e)
            ResponseEntity.internalServerError().build()
        }
    }

    /**
     * GET /api/catalog/sensor-types/{id}
     *
     * Obtiene un tipo de sensor específico por ID
     */
    @GetMapping("/sensor-types/{id}")
    fun getSensorTypeById(@PathVariable id: Short): ResponseEntity<SensorType> {
        logger.debug("GET /api/catalog/sensor-types/{}", id)

        return sensorTypeRepository.findById(id)
            .map { ResponseEntity.ok(it) }
            .orElse(ResponseEntity.notFound().build())
    }

    /**
     * GET /api/catalog/actuator-types
     *
     * Obtiene todos los tipos de actuadores (VENTILATOR, HEATER, etc.)
     *
     * Response: List<ActuatorType>
     */
    @GetMapping("/actuator-types")
    @Cacheable("actuatorTypes", unless = "#result == null")
    fun getAllActuatorTypes(
        @RequestParam(required = false, defaultValue = "false") activeOnly: Boolean
    ): ResponseEntity<List<ActuatorType>> {
        logger.debug("GET /api/catalog/actuator-types?activeOnly={}", activeOnly)

        return try {
            val types = if (activeOnly) {
                actuatorTypeRepository.findByIsActiveTrue()
            } else {
                actuatorTypeRepository.findAll()
            }
            ResponseEntity.ok(types)
        } catch (e: Exception) {
            logger.error("Error obteniendo actuator types", e)
            ResponseEntity.internalServerError().build()
        }
    }

    /**
     * GET /api/catalog/actuator-types/{id}
     *
     * Obtiene un tipo de actuador específico por ID
     */
    @GetMapping("/actuator-types/{id}")
    fun getActuatorTypeById(@PathVariable id: Short): ResponseEntity<ActuatorType> {
        logger.debug("GET /api/catalog/actuator-types/{}", id)

        return actuatorTypeRepository.findById(id)
            .map { ResponseEntity.ok(it) }
            .orElse(ResponseEntity.notFound().build())
    }

    /**
     * GET /api/catalog/actuator-states
     *
     * Obtiene todos los estados de actuadores (OFF, ON, AUTO, etc.)
     *
     * Response: List<ActuatorState> ordenados por display_order
     */
    @GetMapping("/actuator-states")
    @Cacheable("actuatorStates", unless = "#result == null")
    fun getAllActuatorStates(
        @RequestParam(required = false, defaultValue = "false") operationalOnly: Boolean
    ): ResponseEntity<List<ActuatorState>> {
        logger.debug("GET /api/catalog/actuator-states?operationalOnly={}", operationalOnly)

        return try {
            val states = if (operationalOnly) {
                actuatorStateRepository.findByIsOperationalTrue()
            } else {
                actuatorStateRepository.findAllOrderedByDisplay()
            }
            ResponseEntity.ok(states)
        } catch (e: Exception) {
            logger.error("Error obteniendo actuator states", e)
            ResponseEntity.internalServerError().build()
        }
    }

    /**
     * GET /api/catalog/actuator-states/{id}
     *
     * Obtiene un estado de actuador específico por ID
     */
    @GetMapping("/actuator-states/{id}")
    fun getActuatorStateById(@PathVariable id: Short): ResponseEntity<ActuatorState> {
        logger.debug("GET /api/catalog/actuator-states/{}", id)

        return actuatorStateRepository.findById(id)
            .map { ResponseEntity.ok(it) }
            .orElse(ResponseEntity.notFound().build())
    }

    /**
     * GET /api/catalog/alert-severities
     *
     * Obtiene todas las severidades de alertas (INFO, WARNING, ERROR, CRITICAL)
     *
     * Response: List<AlertSeverity> ordenados por nivel (1-4)
     */
    @GetMapping("/alert-severities")
    @Cacheable("alertSeverities", unless = "#result == null")
    fun getAllAlertSeverities(): ResponseEntity<List<AlertSeverity>> {
        logger.debug("GET /api/catalog/alert-severities")

        return try {
            val severities = alertSeverityRepository.findAllOrderedByLevel()
            ResponseEntity.ok(severities)
        } catch (e: Exception) {
            logger.error("Error obteniendo alert severities", e)
            ResponseEntity.internalServerError().build()
        }
    }

    /**
     * GET /api/catalog/alert-severities/{id}
     *
     * Obtiene una severidad específica por ID
     */
    @GetMapping("/alert-severities/{id}")
    fun getAlertSeverityById(@PathVariable id: Short): ResponseEntity<AlertSeverity> {
        logger.debug("GET /api/catalog/alert-severities/{}", id)

        return alertSeverityRepository.findById(id)
            .map { ResponseEntity.ok(it) }
            .orElse(ResponseEntity.notFound().build())
    }

    /**
     * GET /api/catalog/alert-types
     *
     * Obtiene todos los tipos de alertas
     *
     * Query params:
     * - activeOnly: Solo tipos activos (default: false)
     * - category: Filtrar por categoría (SENSOR, ACTUATOR, SYSTEM, etc.)
     *
     * Response: List<AlertType>
     */
    @GetMapping("/alert-types")
    @Cacheable("alertTypes", unless = "#result == null")
    fun getAllAlertTypes(
        @RequestParam(required = false, defaultValue = "false") activeOnly: Boolean,
        @RequestParam(required = false) category: String?
    ): ResponseEntity<List<AlertType>> {
        logger.debug("GET /api/catalog/alert-types?activeOnly={}&category={}", activeOnly, category)

        return try {
            val types = when {
                category != null && activeOnly ->
                    alertTypeRepository.findByCategoryAndIsActiveTrue(category)
                category != null ->
                    alertTypeRepository.findByCategory(category)
                activeOnly ->
                    alertTypeRepository.findByIsActiveTrue()
                else ->
                    alertTypeRepository.findAll()
            }
            ResponseEntity.ok(types)
        } catch (e: Exception) {
            logger.error("Error obteniendo alert types", e)
            ResponseEntity.internalServerError().build()
        }
    }

    /**
     * GET /api/catalog/alert-types/{id}
     *
     * Obtiene un tipo de alerta específico por ID
     */
    @GetMapping("/alert-types/{id}")
    fun getAlertTypeById(@PathVariable id: Short): ResponseEntity<AlertType> {
        logger.debug("GET /api/catalog/alert-types/{}", id)

        return alertTypeRepository.findById(id)
            .map { ResponseEntity.ok(it) }
            .orElse(ResponseEntity.notFound().build())
    }
}
