package com.apptolast.invernaderos.features.catalog

import com.apptolast.invernaderos.features.alert.AlertRepository
import com.apptolast.invernaderos.features.catalog.dto.AlertTypeCreateRequest
import com.apptolast.invernaderos.features.catalog.dto.AlertTypeResponse
import com.apptolast.invernaderos.features.catalog.dto.AlertTypeUpdateRequest
import com.apptolast.invernaderos.features.catalog.dto.toResponse
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * Service para operaciones CRUD de tipos de alerta.
 * Los tipos de alerta definen las categorías de alertas del sistema
 * (ej: THRESHOLD_EXCEEDED, SENSOR_OFFLINE, ACTUATOR_FAILURE, etc.)
 *
 * @see <a href="https://docs.spring.io/spring-boot/reference/data/sql.html">Spring Boot SQL Data Access</a>
 */
@Service
class AlertTypeService(
    private val alertTypeRepository: AlertTypeRepository,
    private val alertRepository: AlertRepository
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    /**
     * Obtiene todos los tipos de alerta.
     */
    fun findAll(): List<AlertTypeResponse> {
        logger.debug("Obteniendo todos los tipos de alerta")
        return alertTypeRepository.findAll().map { it.toResponse() }
    }

    /**
     * Obtiene un tipo de alerta por su ID.
     * @param id ID del tipo de alerta
     * @return El tipo encontrado o null si no existe
     */
    fun findById(id: Short): AlertTypeResponse? {
        logger.debug("Buscando tipo de alerta con ID: $id")
        return alertTypeRepository.findById(id)
            .map { it.toResponse() }
            .orElse(null)
    }

    /**
     * Obtiene un tipo de alerta por su nombre.
     * @param name Nombre del tipo (ej: "THRESHOLD_EXCEEDED", "SENSOR_OFFLINE")
     * @return El tipo encontrado o null si no existe
     */
    fun findByName(name: String): AlertTypeResponse? {
        logger.debug("Buscando tipo de alerta con nombre: $name")
        return alertTypeRepository.findByName(name)?.toResponse()
    }

    /**
     * Crea un nuevo tipo de alerta.
     * El ID se genera automáticamente por la base de datos.
     *
     * @param request Datos del nuevo tipo de alerta
     * @return El tipo creado con su ID auto-generado
     * @throws IllegalArgumentException si el nombre ya existe
     */
    @Transactional("metadataTransactionManager")
    fun create(request: AlertTypeCreateRequest): AlertTypeResponse {
        logger.info("Creando nuevo tipo de alerta: ${request.name}")

        val normalizedName = request.name.uppercase().trim()

        // Validar que el nombre no exista
        alertTypeRepository.findByName(normalizedName)?.let {
            throw IllegalArgumentException("Ya existe un tipo de alerta con nombre: $normalizedName")
        }

        val alertType = AlertType(
            name = normalizedName,
            description = request.description?.trim()
        )

        val savedType = alertTypeRepository.save(alertType)
        logger.info("Tipo de alerta creado exitosamente: ${savedType.name} (ID: ${savedType.id})")

        return savedType.toResponse()
    }

    /**
     * Actualiza un tipo de alerta existente.
     * @param id ID del tipo de alerta a actualizar
     * @param request Datos a actualizar
     * @return El tipo actualizado o null si no existe
     * @throws IllegalArgumentException si el nuevo nombre ya existe en otro tipo
     */
    @Transactional("metadataTransactionManager")
    fun update(id: Short, request: AlertTypeUpdateRequest): AlertTypeResponse? {
        logger.info("Actualizando tipo de alerta con ID: $id")

        val existingType = alertTypeRepository.findById(id).orElse(null)
            ?: run {
                logger.warn("No se encontró tipo de alerta con ID: $id para actualizar")
                return null
            }

        // Si se proporciona un nuevo nombre, validar que no exista en otro tipo
        request.name?.let { newName ->
            val normalizedName = newName.uppercase().trim()
            alertTypeRepository.findByName(normalizedName)?.let { existing ->
                if (existing.id != id) {
                    throw IllegalArgumentException("Ya existe otro tipo de alerta con nombre: $normalizedName")
                }
            }
        }

        // Crear nueva instancia con los valores actualizados (data class es inmutable)
        val updatedType = AlertType(
            id = existingType.id,
            name = request.name?.uppercase()?.trim() ?: existingType.name,
            description = request.description?.trim() ?: existingType.description
        )

        val savedType = alertTypeRepository.save(updatedType)
        logger.info("Tipo de alerta actualizado exitosamente: ${savedType.name}")

        return savedType.toResponse()
    }

    /**
     * Elimina un tipo de alerta.
     * @param id ID del tipo de alerta a eliminar
     * @return true si se eliminó, false si no existía
     * @throws IllegalStateException si el tipo tiene alertas asociadas
     */
    @Transactional("metadataTransactionManager")
    fun delete(id: Short): Boolean {
        logger.info("Eliminando tipo de alerta con ID: $id")

        if (!alertTypeRepository.existsById(id)) {
            logger.warn("No se encontró tipo de alerta con ID: $id para eliminar")
            return false
        }

        // Verificar si hay alertas asociadas a este tipo
        val associatedAlerts = alertRepository.findByAlertTypeId(id)
        if (associatedAlerts.isNotEmpty()) {
            throw IllegalStateException(
                "No se puede eliminar el tipo de alerta con ID: $id porque tiene " +
                "${associatedAlerts.size} alerta(s) asociada(s)"
            )
        }

        alertTypeRepository.deleteById(id)
        logger.info("Tipo de alerta con ID: $id eliminado exitosamente")

        return true
    }

    /**
     * Verifica si existe un tipo de alerta con el ID especificado.
     */
    fun existsById(id: Short): Boolean {
        return alertTypeRepository.existsById(id)
    }
}
