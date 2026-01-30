package com.apptolast.invernaderos.features.catalog

import com.apptolast.invernaderos.features.alert.AlertRepository
import com.apptolast.invernaderos.features.catalog.dto.AlertSeverityCreateRequest
import com.apptolast.invernaderos.features.catalog.dto.AlertSeverityResponse
import com.apptolast.invernaderos.features.catalog.dto.AlertSeverityUpdateRequest
import com.apptolast.invernaderos.features.catalog.dto.toResponse
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

/**
 * Service para operaciones CRUD de niveles de severidad de alertas.
 * Los niveles de severidad definen la importancia de las alertas
 * (ej: INFO, WARNING, ERROR, CRITICAL)
 *
 * @see <a href="https://docs.spring.io/spring-boot/reference/data/sql.html">Spring Boot SQL Data Access</a>
 */
@Service
class AlertSeverityService(
    private val alertSeverityRepository: AlertSeverityRepository,
    private val alertRepository: AlertRepository
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    /**
     * Obtiene todos los niveles de severidad ordenados por nivel ascendente.
     */
    fun findAll(): List<AlertSeverityResponse> {
        logger.debug("Obteniendo todos los niveles de severidad")
        return alertSeverityRepository.findAllByOrderByLevelAsc().map { it.toResponse() }
    }

    /**
     * Obtiene un nivel de severidad por su ID.
     * @param id ID de la severidad
     * @return La severidad encontrada o null si no existe
     */
    fun findById(id: Short): AlertSeverityResponse? {
        logger.debug("Buscando severidad con ID: $id")
        return alertSeverityRepository.findById(id)
            .map { it.toResponse() }
            .orElse(null)
    }

    /**
     * Obtiene un nivel de severidad por su nombre.
     * @param name Nombre de la severidad (ej: "INFO", "WARNING", "ERROR", "CRITICAL")
     * @return La severidad encontrada o null si no existe
     */
    fun findByName(name: String): AlertSeverityResponse? {
        logger.debug("Buscando severidad con nombre: $name")
        return alertSeverityRepository.findByName(name)?.toResponse()
    }

    /**
     * Obtiene los niveles de severidad que requieren acción inmediata.
     */
    fun findRequiringAction(): List<AlertSeverityResponse> {
        logger.debug("Obteniendo severidades que requieren acción")
        return alertSeverityRepository.findByRequiresAction(true).map { it.toResponse() }
    }

    /**
     * Crea un nuevo nivel de severidad.
     * El ID se genera automáticamente por la base de datos.
     *
     * @param request Datos del nuevo nivel de severidad
     * @return El nivel creado con su ID auto-generado
     * @throws IllegalArgumentException si el nombre ya existe
     */
    @Transactional("metadataTransactionManager")
    fun create(request: AlertSeverityCreateRequest): AlertSeverityResponse {
        logger.info("Creando nuevo nivel de severidad: ${request.name} (Level: ${request.level})")

        val normalizedName = request.name.uppercase().trim()

        // Validar que el nombre no exista
        alertSeverityRepository.findByName(normalizedName)?.let {
            throw IllegalArgumentException("Ya existe un nivel de severidad con nombre: $normalizedName")
        }

        val alertSeverity = AlertSeverity(
            name = normalizedName,
            level = request.level,
            description = request.description?.trim(),
            color = request.color?.uppercase(),
            requiresAction = request.requiresAction,
            notificationDelayMinutes = request.notificationDelayMinutes,
            createdAt = Instant.now()
        )

        val savedSeverity = alertSeverityRepository.save(alertSeverity)
        logger.info("Nivel de severidad creado exitosamente: ${savedSeverity.name} (ID: ${savedSeverity.id})")

        return savedSeverity.toResponse()
    }

    /**
     * Actualiza un nivel de severidad existente.
     * @param id ID del nivel de severidad a actualizar
     * @param request Datos a actualizar
     * @return El nivel actualizado o null si no existe
     * @throws IllegalArgumentException si el nuevo nombre ya existe en otro nivel
     */
    @Transactional("metadataTransactionManager")
    fun update(id: Short, request: AlertSeverityUpdateRequest): AlertSeverityResponse? {
        logger.info("Actualizando nivel de severidad con ID: $id")

        val existingSeverity = alertSeverityRepository.findById(id).orElse(null)
            ?: run {
                logger.warn("No se encontró nivel de severidad con ID: $id para actualizar")
                return null
            }

        // Si se proporciona un nuevo nombre, validar que no exista en otro nivel
        request.name?.let { newName ->
            val normalizedName = newName.uppercase().trim()
            alertSeverityRepository.findByName(normalizedName)?.let { existing ->
                if (existing.id != id) {
                    throw IllegalArgumentException("Ya existe otro nivel de severidad con nombre: $normalizedName")
                }
            }
        }

        // Crear nueva instancia con los valores actualizados (data class es inmutable)
        val updatedSeverity = AlertSeverity(
            id = existingSeverity.id,
            name = request.name?.uppercase()?.trim() ?: existingSeverity.name,
            level = request.level ?: existingSeverity.level,
            description = request.description?.trim() ?: existingSeverity.description,
            color = request.color?.uppercase() ?: existingSeverity.color,
            requiresAction = request.requiresAction ?: existingSeverity.requiresAction,
            notificationDelayMinutes = request.notificationDelayMinutes ?: existingSeverity.notificationDelayMinutes,
            createdAt = existingSeverity.createdAt
        )

        val savedSeverity = alertSeverityRepository.save(updatedSeverity)
        logger.info("Nivel de severidad actualizado exitosamente: ${savedSeverity.name}")

        return savedSeverity.toResponse()
    }

    /**
     * Elimina un nivel de severidad.
     * @param id ID del nivel de severidad a eliminar
     * @return true si se eliminó, false si no existía
     * @throws IllegalStateException si el nivel tiene alertas asociadas
     */
    @Transactional("metadataTransactionManager")
    fun delete(id: Short): Boolean {
        logger.info("Eliminando nivel de severidad con ID: $id")

        if (!alertSeverityRepository.existsById(id)) {
            logger.warn("No se encontró nivel de severidad con ID: $id para eliminar")
            return false
        }

        // Verificar si hay alertas asociadas a este nivel de severidad
        val associatedAlerts = alertRepository.findBySeverityId(id)
        if (associatedAlerts.isNotEmpty()) {
            throw IllegalStateException(
                "No se puede eliminar el nivel de severidad con ID: $id porque tiene " +
                "${associatedAlerts.size} alerta(s) asociada(s)"
            )
        }

        alertSeverityRepository.deleteById(id)
        logger.info("Nivel de severidad con ID: $id eliminado exitosamente")

        return true
    }

    /**
     * Verifica si existe un nivel de severidad con el ID especificado.
     */
    fun existsById(id: Short): Boolean {
        return alertSeverityRepository.existsById(id)
    }
}
