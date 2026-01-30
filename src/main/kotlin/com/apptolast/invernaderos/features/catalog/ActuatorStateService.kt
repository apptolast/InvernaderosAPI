package com.apptolast.invernaderos.features.catalog

import com.apptolast.invernaderos.features.catalog.dto.ActuatorStateCreateRequest
import com.apptolast.invernaderos.features.catalog.dto.ActuatorStateResponse
import com.apptolast.invernaderos.features.catalog.dto.ActuatorStateUpdateRequest
import com.apptolast.invernaderos.features.catalog.dto.toResponse
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

/**
 * Service para operaciones CRUD de estados de actuadores.
 * Los estados definen las condiciones posibles de un actuador (ON, OFF, AUTO, ERROR, etc.)
 *
 * @see <a href="https://docs.spring.io/spring-boot/reference/data/sql.html">Spring Boot SQL Data Access</a>
 */
@Service
class ActuatorStateService(
    private val actuatorStateRepository: ActuatorStateRepository
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    /**
     * Obtiene todos los estados de actuadores ordenados por displayOrder.
     */
    fun findAll(): List<ActuatorStateResponse> {
        logger.debug("Obteniendo todos los estados de actuadores")
        return actuatorStateRepository.findAllByOrderByDisplayOrderAsc().map { it.toResponse() }
    }

    /**
     * Obtiene solo los estados operacionales (donde el actuador está funcionando).
     */
    fun findOperational(): List<ActuatorStateResponse> {
        logger.debug("Obteniendo estados operacionales")
        return actuatorStateRepository.findByIsOperational(true).map { it.toResponse() }
    }

    /**
     * Obtiene un estado por su ID.
     * @param id ID del estado
     * @return El estado encontrado o null si no existe
     */
    fun findById(id: Short): ActuatorStateResponse? {
        logger.debug("Buscando estado con ID: $id")
        return actuatorStateRepository.findById(id)
            .map { it.toResponse() }
            .orElse(null)
    }

    /**
     * Obtiene un estado por su nombre.
     * @param name Nombre del estado (ej: "ON", "OFF", "AUTO")
     * @return El estado encontrado o null si no existe
     */
    fun findByName(name: String): ActuatorStateResponse? {
        logger.debug("Buscando estado con nombre: $name")
        return actuatorStateRepository.findByName(name)?.toResponse()
    }

    /**
     * Crea un nuevo estado de actuador.
     * El ID se genera automáticamente por la base de datos.
     *
     * @param request Datos del nuevo estado
     * @return El estado creado con su ID auto-generado
     * @throws IllegalArgumentException si el nombre ya existe
     */
    @Transactional("metadataTransactionManager")
    fun create(request: ActuatorStateCreateRequest): ActuatorStateResponse {
        logger.info("Creando nuevo estado de actuador: ${request.name}")

        val normalizedName = request.name.uppercase().trim()

        // Validar que el nombre no exista
        actuatorStateRepository.findByName(normalizedName)?.let {
            throw IllegalArgumentException("Ya existe un estado con nombre: $normalizedName")
        }

        val actuatorState = ActuatorState(
            name = normalizedName,
            description = request.description?.trim(),
            isOperational = request.isOperational,
            displayOrder = request.displayOrder,
            color = request.color?.uppercase(),
            createdAt = Instant.now()
        )

        val savedState = actuatorStateRepository.save(actuatorState)
        logger.info("Estado creado exitosamente: ${savedState.name} (ID: ${savedState.id})")

        return savedState.toResponse()
    }

    /**
     * Actualiza un estado de actuador existente.
     * @param id ID del estado a actualizar
     * @param request Datos a actualizar
     * @return El estado actualizado o null si no existe
     * @throws IllegalArgumentException si el nuevo nombre ya existe en otro estado
     */
    @Transactional("metadataTransactionManager")
    fun update(id: Short, request: ActuatorStateUpdateRequest): ActuatorStateResponse? {
        logger.info("Actualizando estado con ID: $id")

        val existingState = actuatorStateRepository.findById(id).orElse(null)
            ?: run {
                logger.warn("No se encontró estado con ID: $id para actualizar")
                return null
            }

        // Si se proporciona un nuevo nombre, validar que no exista en otro estado
        request.name?.let { newName ->
            val normalizedName = newName.uppercase().trim()
            actuatorStateRepository.findByName(normalizedName)?.let { existing ->
                if (existing.id != id) {
                    throw IllegalArgumentException("Ya existe otro estado con nombre: $normalizedName")
                }
            }
        }

        // Crear nueva instancia con los valores actualizados (data class es inmutable)
        val updatedState = ActuatorState(
            id = existingState.id,
            name = request.name?.uppercase()?.trim() ?: existingState.name,
            description = request.description?.trim() ?: existingState.description,
            isOperational = request.isOperational ?: existingState.isOperational,
            displayOrder = request.displayOrder ?: existingState.displayOrder,
            color = request.color?.uppercase() ?: existingState.color,
            createdAt = existingState.createdAt
        )

        val savedState = actuatorStateRepository.save(updatedState)
        logger.info("Estado actualizado exitosamente: ${savedState.name}")

        return savedState.toResponse()
    }

    /**
     * Elimina un estado de actuador.
     * @param id ID del estado a eliminar
     * @return true si se eliminó, false si no existía
     */
    @Transactional("metadataTransactionManager")
    fun delete(id: Short): Boolean {
        logger.info("Eliminando estado con ID: $id")

        if (!actuatorStateRepository.existsById(id)) {
            logger.warn("No se encontró estado con ID: $id para eliminar")
            return false
        }

        // TODO: Verificar si hay actuadores usando este estado antes de eliminar

        actuatorStateRepository.deleteById(id)
        logger.info("Estado con ID: $id eliminado exitosamente")

        return true
    }

    /**
     * Verifica si existe un estado con el ID especificado.
     */
    fun existsById(id: Short): Boolean {
        return actuatorStateRepository.existsById(id)
    }
}
