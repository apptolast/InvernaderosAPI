package com.apptolast.invernaderos.features.catalog

import com.apptolast.invernaderos.features.catalog.dto.DeviceTypeCreateRequest
import com.apptolast.invernaderos.features.catalog.dto.DeviceTypeResponse
import com.apptolast.invernaderos.features.catalog.dto.DeviceTypeUpdateRequest
import com.apptolast.invernaderos.features.catalog.dto.toResponse
import com.apptolast.invernaderos.features.device.DeviceRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

/**
 * Service para operaciones CRUD de tipos de dispositivos.
 * Los tipos definen las características específicas de sensores y actuadores
 * (ej: TEMPERATURE, HUMIDITY, VALVE, PUMP, etc.)
 *
 * @see <a href="https://docs.spring.io/spring-boot/reference/data/sql.html">Spring Boot SQL Data Access</a>
 */
@Service
class DeviceTypeService(
    private val deviceTypeRepository: DeviceTypeRepository,
    private val deviceCategoryRepository: DeviceCategoryRepository,
    private val unitRepository: UnitRepository,
    private val deviceRepository: DeviceRepository
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    /**
     * Obtiene todos los tipos de dispositivos.
     * @param categoryId Filtrar por categoría (opcional)
     * @param activeOnly Filtrar solo activos (por defecto true)
     */
    fun findAll(categoryId: Short? = null, activeOnly: Boolean = true): List<DeviceTypeResponse> {
        logger.debug("Obteniendo tipos de dispositivos. categoryId=$categoryId, activeOnly=$activeOnly")

        val types = when {
            categoryId != null && activeOnly -> deviceTypeRepository.findByCategoryIdAndIsActive(categoryId, true)
            categoryId != null -> deviceTypeRepository.findByCategoryId(categoryId)
            activeOnly -> deviceTypeRepository.findByIsActive(true)
            else -> deviceTypeRepository.findAll()
        }

        return types.map { it.toResponse() }
    }

    /**
     * Obtiene un tipo por su ID.
     * @param id ID del tipo
     * @return El tipo encontrado o null si no existe
     */
    fun findById(id: Short): DeviceTypeResponse? {
        logger.debug("Buscando tipo de dispositivo con ID: $id")
        return deviceTypeRepository.findById(id)
            .map { it.toResponse() }
            .orElse(null)
    }

    /**
     * Obtiene un tipo por su nombre.
     * @param name Nombre del tipo (ej: "TEMPERATURE", "HUMIDITY")
     * @return El tipo encontrado o null si no existe
     */
    fun findByName(name: String): DeviceTypeResponse? {
        logger.debug("Buscando tipo de dispositivo con nombre: $name")
        return deviceTypeRepository.findByName(name)?.toResponse()
    }

    /**
     * Obtiene todos los tipos de sensores activos.
     */
    fun findSensorTypes(): List<DeviceTypeResponse> {
        logger.debug("Obteniendo tipos de sensores activos")
        return deviceTypeRepository.findByCategoryIdAndIsActive(DeviceCategory.SENSOR, true)
            .map { it.toResponse() }
    }

    /**
     * Obtiene todos los tipos de actuadores activos.
     */
    fun findActuatorTypes(): List<DeviceTypeResponse> {
        logger.debug("Obteniendo tipos de actuadores activos")
        return deviceTypeRepository.findByCategoryIdAndIsActive(DeviceCategory.ACTUATOR, true)
            .map { it.toResponse() }
    }

    /**
     * Crea un nuevo tipo de dispositivo.
     * @param request Datos del nuevo tipo
     * @return El tipo creado
     * @throws IllegalArgumentException si el nombre ya existe o la categoría/unidad no existe
     */
    @Transactional("metadataTransactionManager")
    fun create(request: DeviceTypeCreateRequest): DeviceTypeResponse {
        logger.info("Creando nuevo tipo de dispositivo: ${request.name}")

        // Validar que el nombre no exista
        deviceTypeRepository.findByName(request.name.uppercase().trim())?.let {
            throw IllegalArgumentException("Ya existe un tipo de dispositivo con nombre: ${request.name}")
        }

        // Validar que la categoría exista
        if (!deviceCategoryRepository.existsById(request.categoryId)) {
            throw IllegalArgumentException("No existe la categoría con ID: ${request.categoryId}")
        }

        // Validar que la unidad exista (si se proporciona)
        request.defaultUnitId?.let { unitId ->
            if (!unitRepository.existsById(unitId)) {
                throw IllegalArgumentException("No existe la unidad con ID: $unitId")
            }
        }

        // Validar que minExpectedValue <= maxExpectedValue si ambos están presentes
        if (request.minExpectedValue != null && request.maxExpectedValue != null) {
            if (request.minExpectedValue > request.maxExpectedValue) {
                throw IllegalArgumentException(
                    "El valor mínimo (${request.minExpectedValue}) no puede ser mayor que el máximo (${request.maxExpectedValue})"
                )
            }
        }

        val deviceType = DeviceType(
            name = request.name.uppercase().trim(),
            description = request.description?.trim(),
            categoryId = request.categoryId,
            defaultUnitId = request.defaultUnitId,
            dataType = request.dataType ?: "DECIMAL",
            minExpectedValue = request.minExpectedValue,
            maxExpectedValue = request.maxExpectedValue,
            controlType = request.controlType,
            isActive = request.isActive,
            createdAt = Instant.now()
        )

        val savedType = deviceTypeRepository.save(deviceType)

        // Recargar con EntityGraph para obtener las relaciones
        val reloadedType = deviceTypeRepository.findById(savedType.id!!).orElse(savedType)

        logger.info("Tipo de dispositivo creado exitosamente: ${reloadedType.name} (ID: ${reloadedType.id})")
        return reloadedType.toResponse()
    }

    /**
     * Actualiza un tipo de dispositivo existente.
     * @param id ID del tipo a actualizar
     * @param request Datos a actualizar
     * @return El tipo actualizado o null si no existe
     * @throws IllegalArgumentException si el nuevo nombre ya existe o la categoría/unidad no existe
     */
    @Transactional("metadataTransactionManager")
    fun update(id: Short, request: DeviceTypeUpdateRequest): DeviceTypeResponse? {
        logger.info("Actualizando tipo de dispositivo con ID: $id")

        val existingType = deviceTypeRepository.findById(id).orElse(null)
            ?: run {
                logger.warn("No se encontró tipo de dispositivo con ID: $id para actualizar")
                return null
            }

        // Si se proporciona un nuevo nombre, validar que no exista en otro tipo
        request.name?.let { newName ->
            val normalizedName = newName.uppercase().trim()
            deviceTypeRepository.findByName(normalizedName)?.let { existing ->
                if (existing.id != id) {
                    throw IllegalArgumentException("Ya existe otro tipo con nombre: $normalizedName")
                }
            }
        }

        // Validar que la nueva categoría exista (si se proporciona)
        request.categoryId?.let { catId ->
            if (!deviceCategoryRepository.existsById(catId)) {
                throw IllegalArgumentException("No existe la categoría con ID: $catId")
            }
        }

        // Validar que la nueva unidad exista (si se proporciona)
        request.defaultUnitId?.let { unitId ->
            if (!unitRepository.existsById(unitId)) {
                throw IllegalArgumentException("No existe la unidad con ID: $unitId")
            }
        }

        // Validar rango de valores
        val newMin = request.minExpectedValue ?: existingType.minExpectedValue
        val newMax = request.maxExpectedValue ?: existingType.maxExpectedValue
        if (newMin != null && newMax != null && newMin > newMax) {
            throw IllegalArgumentException(
                "El valor mínimo ($newMin) no puede ser mayor que el máximo ($newMax)"
            )
        }

        // Crear nueva instancia con los valores actualizados
        val updatedType = DeviceType(
            id = existingType.id,
            name = request.name?.uppercase()?.trim() ?: existingType.name,
            description = request.description?.trim() ?: existingType.description,
            categoryId = request.categoryId ?: existingType.categoryId,
            defaultUnitId = request.defaultUnitId ?: existingType.defaultUnitId,
            dataType = request.dataType ?: existingType.dataType,
            minExpectedValue = request.minExpectedValue ?: existingType.minExpectedValue,
            maxExpectedValue = request.maxExpectedValue ?: existingType.maxExpectedValue,
            controlType = request.controlType ?: existingType.controlType,
            isActive = request.isActive ?: existingType.isActive,
            createdAt = existingType.createdAt
        )

        val savedType = deviceTypeRepository.save(updatedType)

        // Recargar con EntityGraph para obtener las relaciones actualizadas
        val reloadedType = deviceTypeRepository.findById(savedType.id!!).orElse(savedType)

        logger.info("Tipo de dispositivo actualizado exitosamente: ${reloadedType.name}")
        return reloadedType.toResponse()
    }

    /**
     * Elimina un tipo de dispositivo.
     * @param id ID del tipo a eliminar
     * @return true si se eliminó, false si no existía
     * @throws IllegalStateException si el tipo tiene dispositivos asociados
     */
    @Transactional("metadataTransactionManager")
    fun delete(id: Short): Boolean {
        logger.info("Eliminando tipo de dispositivo con ID: $id")

        if (!deviceTypeRepository.existsById(id)) {
            logger.warn("No se encontró tipo de dispositivo con ID: $id para eliminar")
            return false
        }

        // Verificar si hay dispositivos asociados a este tipo
        val associatedDevices = deviceRepository.findByTypeId(id)
        if (associatedDevices.isNotEmpty()) {
            throw IllegalStateException(
                "No se puede eliminar el tipo con ID: $id porque tiene " +
                "${associatedDevices.size} dispositivo(s) asociados"
            )
        }

        deviceTypeRepository.deleteById(id)
        logger.info("Tipo de dispositivo con ID: $id eliminado exitosamente")

        return true
    }

    /**
     * Desactiva un tipo de dispositivo (soft delete).
     * @param id ID del tipo a desactivar
     * @return El tipo desactivado o null si no existe
     */
    @Transactional("metadataTransactionManager")
    fun deactivate(id: Short): DeviceTypeResponse? {
        logger.info("Desactivando tipo de dispositivo con ID: $id")
        return update(id, DeviceTypeUpdateRequest(isActive = false))
    }

    /**
     * Activa un tipo de dispositivo.
     * @param id ID del tipo a activar
     * @return El tipo activado o null si no existe
     */
    @Transactional("metadataTransactionManager")
    fun activate(id: Short): DeviceTypeResponse? {
        logger.info("Activando tipo de dispositivo con ID: $id")
        return update(id, DeviceTypeUpdateRequest(isActive = true))
    }

    /**
     * Verifica si existe un tipo con el ID especificado.
     */
    fun existsById(id: Short): Boolean {
        return deviceTypeRepository.existsById(id)
    }
}
