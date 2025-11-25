package com.apptolast.invernaderos.features.catalog

import com.apptolast.invernaderos.features.catalog.catalog.ActuatorState
import com.apptolast.invernaderos.features.catalog.catalog.ActuatorType
import com.apptolast.invernaderos.features.catalog.catalog.AlertSeverity
import com.apptolast.invernaderos.features.catalog.catalog.AlertType
import com.apptolast.invernaderos.features.catalog.catalog.SensorType
import com.apptolast.invernaderos.features.catalog.catalog.Unit as CatalogUnit
import org.slf4j.LoggerFactory
import org.springframework.cache.annotation.Cacheable
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

/**
 * REST Controller para acceder a catálogos de referencia
 *
 * Los catálogos son tablas pequeñas (< 100 registros) con datos de referencia que cambian muy poco,
 * por lo que se cachean agresivamente.
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
class CatalogController(private val catalogService: CatalogService) {

    private val logger = LoggerFactory.getLogger(CatalogController::class.java)

    @GetMapping("/units")
    @Cacheable("units", unless = "#result == null")
    fun getAllUnits(
            @RequestParam(required = false, defaultValue = "false") activeOnly: Boolean
    ): ResponseEntity<List<CatalogUnit>> {
        logger.debug("GET /api/catalog/units?activeOnly={}", activeOnly)
        return try {
            val units = catalogService.getAllUnits(activeOnly)
            ResponseEntity.ok(units)
        } catch (e: Exception) {
            logger.error("Error obteniendo units", e)
            ResponseEntity.internalServerError().build()
        }
    }

    @GetMapping("/units/{id}")
    fun getUnitById(@PathVariable id: Short): ResponseEntity<CatalogUnit> {
        logger.debug("GET /api/catalog/units/{}", id)
        return catalogService
                .getUnitById(id)
                .map { ResponseEntity.ok(it) }
                .orElse(ResponseEntity.notFound().build())
    }

    @GetMapping("/sensor-types")
    @Cacheable("sensorTypes", unless = "#result == null")
    fun getAllSensorTypes(
            @RequestParam(required = false, defaultValue = "false") activeOnly: Boolean
    ): ResponseEntity<List<SensorType>> {
        logger.debug("GET /api/catalog/sensor-types?activeOnly={}", activeOnly)
        return try {
            val types = catalogService.getAllSensorTypes(activeOnly)
            ResponseEntity.ok(types)
        } catch (e: Exception) {
            logger.error("Error obteniendo sensor types", e)
            ResponseEntity.internalServerError().build()
        }
    }

    @GetMapping("/sensor-types/{id}")
    fun getSensorTypeById(@PathVariable id: Short): ResponseEntity<SensorType> {
        logger.debug("GET /api/catalog/sensor-types/{}", id)
        return catalogService
                .getSensorTypeById(id)
                .map { ResponseEntity.ok(it) }
                .orElse(ResponseEntity.notFound().build())
    }

    @GetMapping("/actuator-types")
    @Cacheable("actuatorTypes", unless = "#result == null")
    fun getAllActuatorTypes(
            @RequestParam(required = false, defaultValue = "false") activeOnly: Boolean
    ): ResponseEntity<List<ActuatorType>> {
        logger.debug("GET /api/catalog/actuator-types?activeOnly={}", activeOnly)
        return try {
            val types = catalogService.getAllActuatorTypes(activeOnly)
            ResponseEntity.ok(types)
        } catch (e: Exception) {
            logger.error("Error obteniendo actuator types", e)
            ResponseEntity.internalServerError().build()
        }
    }

    @GetMapping("/actuator-types/{id}")
    fun getActuatorTypeById(@PathVariable id: Short): ResponseEntity<ActuatorType> {
        logger.debug("GET /api/catalog/actuator-types/{}", id)
        return catalogService
                .getActuatorTypeById(id)
                .map { ResponseEntity.ok(it) }
                .orElse(ResponseEntity.notFound().build())
    }

    @GetMapping("/actuator-states")
    @Cacheable("actuatorStates", unless = "#result == null")
    fun getAllActuatorStates(
            @RequestParam(required = false, defaultValue = "false") operationalOnly: Boolean
    ): ResponseEntity<List<ActuatorState>> {
        logger.debug("GET /api/catalog/actuator-states?operationalOnly={}", operationalOnly)
        return try {
            val states = catalogService.getAllActuatorStates(operationalOnly)
            ResponseEntity.ok(states)
        } catch (e: Exception) {
            logger.error("Error obteniendo actuator states", e)
            ResponseEntity.internalServerError().build()
        }
    }

    @GetMapping("/actuator-states/{id}")
    fun getActuatorStateById(@PathVariable id: Short): ResponseEntity<ActuatorState> {
        logger.debug("GET /api/catalog/actuator-states/{}", id)
        return catalogService
                .getActuatorStateById(id)
                .map { ResponseEntity.ok(it) }
                .orElse(ResponseEntity.notFound().build())
    }

    @GetMapping("/alert-severities")
    @Cacheable("alertSeverities", unless = "#result == null")
    fun getAllAlertSeverities(): ResponseEntity<List<AlertSeverity>> {
        logger.debug("GET /api/catalog/alert-severities")
        return try {
            val severities = catalogService.getAllAlertSeverities()
            ResponseEntity.ok(severities)
        } catch (e: Exception) {
            logger.error("Error obteniendo alert severities", e)
            ResponseEntity.internalServerError().build()
        }
    }

    @GetMapping("/alert-severities/{id}")
    fun getAlertSeverityById(@PathVariable id: Short): ResponseEntity<AlertSeverity> {
        logger.debug("GET /api/catalog/alert-severities/{}", id)
        return catalogService
                .getAlertSeverityById(id)
                .map { ResponseEntity.ok(it) }
                .orElse(ResponseEntity.notFound().build())
    }

    @GetMapping("/alert-types")
    @Cacheable("alertTypes", unless = "#result == null")
    fun getAllAlertTypes(
            @RequestParam(required = false, defaultValue = "false") activeOnly: Boolean,
            @RequestParam(required = false) category: String?
    ): ResponseEntity<List<AlertType>> {
        logger.debug("GET /api/catalog/alert-types?activeOnly={}&category={}", activeOnly, category)
        return try {
            val types = catalogService.getAllAlertTypes(activeOnly, category)
            ResponseEntity.ok(types)
        } catch (e: Exception) {
            logger.error("Error obteniendo alert types", e)
            ResponseEntity.internalServerError().build()
        }
    }

    @GetMapping("/alert-types/{id}")
    fun getAlertTypeById(@PathVariable id: Short): ResponseEntity<AlertType> {
        logger.debug("GET /api/catalog/alert-types/{}", id)
        return catalogService
                .getAlertTypeById(id)
                .map { ResponseEntity.ok(it) }
                .orElse(ResponseEntity.notFound().build())
    }
}
