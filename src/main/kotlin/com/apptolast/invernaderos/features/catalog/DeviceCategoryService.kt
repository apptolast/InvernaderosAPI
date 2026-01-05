package com.apptolast.invernaderos.features.catalog

import com.apptolast.invernaderos.features.catalog.dto.DeviceCategoryCreateRequest
import com.apptolast.invernaderos.features.catalog.dto.DeviceCategoryResponse
import com.apptolast.invernaderos.features.catalog.dto.DeviceCategoryUpdateRequest
import com.apptolast.invernaderos.features.catalog.dto.toResponse
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * Service para operaciones CRUD de categorías de dispositivos.
 * Las categorías son catálogos fijos (SENSOR=1, ACTUATOR=2) pero se permite
 * crear nuevas categorías si el negocio lo requiere.
 *
 * @see <a href="https://docs.spring.io/spring-boot/reference/data/sql.html">Spring Boot SQL Data Access</a>
 */
@Service
class DeviceCategoryService(
    private val deviceCategoryRepository: DeviceCategoryRepository,
    private val deviceTypeRepository: DeviceTypeRepository
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    /**
     * Obtiene todas las categorías de dispositivos.
     */
    fun findAll(): List<DeviceCategoryResponse> {
        logger.debug("Obteniendo todas las categorías de dispositivos")
        return deviceCategoryRepository.findAll().map { it.toResponse() }
    }

    /**
     * Obtiene una categoría por su ID.
     * @param id ID de la categoría
     * @return La categoría encontrada o null si no existe
     */
    fun findById(id: Short): DeviceCategoryResponse? {
        logger.debug("Buscando categoría con ID: $id")
        return deviceCategoryRepository.findById(id)
            .map { it.toResponse() }
            .orElse(null)
    }

    /**
     * Obtiene una categoría por su nombre.
     * @param name Nombre de la categoría (ej: "SENSOR", "ACTUATOR")
     * @return La categoría encontrada o null si no existe
     */
    fun findByName(name: String): DeviceCategoryResponse? {
        logger.debug("Buscando categoría con nombre: $name")
        return deviceCategoryRepository.findByName(name)?.toResponse()
    }

    /**
     * Crea una nueva categoría de dispositivo.
     * @param request Datos de la nueva categoría
     * @return La categoría creada
     * @throws IllegalArgumentException si el ID o nombre ya existen
     */
    @Transactional("metadataTransactionManager")
    fun create(request: DeviceCategoryCreateRequest): DeviceCategoryResponse {
        logger.info("Creando nueva categoría de dispositivo: ${request.name} (ID: ${request.id})")

        // Validar que el ID no exista
        if (deviceCategoryRepository.existsById(request.id)) {
            throw IllegalArgumentException("Ya existe una categoría con ID: ${request.id}")
        }

        // Validar que el nombre no exista
        deviceCategoryRepository.findByName(request.name)?.let {
            throw IllegalArgumentException("Ya existe una categoría con nombre: ${request.name}")
        }

        val category = DeviceCategory(
            id = request.id,
            name = request.name.uppercase().trim()
        )

        val savedCategory = deviceCategoryRepository.save(category)
        logger.info("Categoría creada exitosamente: ${savedCategory.name} (ID: ${savedCategory.id})")

        return savedCategory.toResponse()
    }

    /**
     * Actualiza una categoría existente.
     * @param id ID de la categoría a actualizar
     * @param request Datos a actualizar
     * @return La categoría actualizada o null si no existe
     * @throws IllegalArgumentException si el nuevo nombre ya existe en otra categoría
     */
    @Transactional("metadataTransactionManager")
    fun update(id: Short, request: DeviceCategoryUpdateRequest): DeviceCategoryResponse? {
        logger.info("Actualizando categoría con ID: $id")

        val existingCategory = deviceCategoryRepository.findById(id).orElse(null)
            ?: run {
                logger.warn("No se encontró categoría con ID: $id para actualizar")
                return null
            }

        // Si se proporciona un nuevo nombre, validar que no exista en otra categoría
        request.name?.let { newName ->
            val normalizedName = newName.uppercase().trim()
            deviceCategoryRepository.findByName(normalizedName)?.let { existing ->
                if (existing.id != id) {
                    throw IllegalArgumentException("Ya existe otra categoría con nombre: $normalizedName")
                }
            }
        }

        // Crear nueva instancia con los valores actualizados (data class es inmutable)
        val updatedCategory = DeviceCategory(
            id = existingCategory.id,
            name = request.name?.uppercase()?.trim() ?: existingCategory.name
        )

        val savedCategory = deviceCategoryRepository.save(updatedCategory)
        logger.info("Categoría actualizada exitosamente: ${savedCategory.name}")

        return savedCategory.toResponse()
    }

    /**
     * Elimina una categoría de dispositivo.
     * @param id ID de la categoría a eliminar
     * @return true si se eliminó, false si no existía
     * @throws IllegalStateException si la categoría tiene tipos de dispositivo asociados
     */
    @Transactional("metadataTransactionManager")
    fun delete(id: Short): Boolean {
        logger.info("Eliminando categoría con ID: $id")

        if (!deviceCategoryRepository.existsById(id)) {
            logger.warn("No se encontró categoría con ID: $id para eliminar")
            return false
        }

        // Verificar si hay tipos de dispositivo asociados a esta categoría
        val associatedTypes = deviceTypeRepository.findByCategoryId(id)
        if (associatedTypes.isNotEmpty()) {
            throw IllegalStateException(
                "No se puede eliminar la categoría con ID: $id porque tiene " +
                "${associatedTypes.size} tipo(s) de dispositivo asociados: " +
                associatedTypes.take(5).joinToString { it.name }
            )
        }

        deviceCategoryRepository.deleteById(id)
        logger.info("Categoría con ID: $id eliminada exitosamente")

        return true
    }

    /**
     * Verifica si existe una categoría con el ID especificado.
     */
    fun existsById(id: Short): Boolean {
        return deviceCategoryRepository.existsById(id)
    }
}
