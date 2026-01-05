package com.apptolast.invernaderos.features.catalog

import com.apptolast.invernaderos.features.catalog.dto.PeriodCreateRequest
import com.apptolast.invernaderos.features.catalog.dto.PeriodResponse
import com.apptolast.invernaderos.features.catalog.dto.PeriodUpdateRequest
import com.apptolast.invernaderos.features.catalog.dto.toResponse
import com.apptolast.invernaderos.features.setting.SettingRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * Service para operaciones CRUD de periodos.
 * Los periodos definen el momento del día para aplicar configuraciones
 * (ej: DAY, NIGHT, ALL)
 *
 * @see <a href="https://docs.spring.io/spring-boot/reference/data/sql.html">Spring Boot SQL Data Access</a>
 */
@Service
class PeriodService(
    private val periodRepository: PeriodRepository,
    private val settingRepository: SettingRepository
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    /**
     * Obtiene todos los periodos.
     */
    fun findAll(): List<PeriodResponse> {
        logger.debug("Obteniendo todos los periodos")
        return periodRepository.findAll().map { it.toResponse() }
    }

    /**
     * Obtiene un periodo por su ID.
     * @param id ID del periodo
     * @return El periodo encontrado o null si no existe
     */
    fun findById(id: Short): PeriodResponse? {
        logger.debug("Buscando periodo con ID: $id")
        return periodRepository.findById(id)
            .map { it.toResponse() }
            .orElse(null)
    }

    /**
     * Obtiene un periodo por su nombre.
     * @param name Nombre del periodo (ej: "DAY", "NIGHT", "ALL")
     * @return El periodo encontrado o null si no existe
     */
    fun findByName(name: String): PeriodResponse? {
        logger.debug("Buscando periodo con nombre: $name")
        return periodRepository.findByName(name)?.toResponse()
    }

    /**
     * Crea un nuevo periodo.
     * @param request Datos del nuevo periodo
     * @return El periodo creado
     * @throws IllegalArgumentException si el ID o nombre ya existen
     */
    @Transactional("metadataTransactionManager")
    fun create(request: PeriodCreateRequest): PeriodResponse {
        logger.info("Creando nuevo periodo: ${request.name} (ID: ${request.id})")

        // Validar que el ID no exista
        if (periodRepository.existsById(request.id)) {
            throw IllegalArgumentException("Ya existe un periodo con ID: ${request.id}")
        }

        // Validar que el nombre no exista
        periodRepository.findByName(request.name.uppercase().trim())?.let {
            throw IllegalArgumentException("Ya existe un periodo con nombre: ${request.name}")
        }

        val period = Period(
            id = request.id,
            name = request.name.uppercase().trim()
        )

        val savedPeriod = periodRepository.save(period)
        logger.info("Periodo creado exitosamente: ${savedPeriod.name} (ID: ${savedPeriod.id})")

        return savedPeriod.toResponse()
    }

    /**
     * Actualiza un periodo existente.
     * @param id ID del periodo a actualizar
     * @param request Datos a actualizar
     * @return El periodo actualizado o null si no existe
     * @throws IllegalArgumentException si el nuevo nombre ya existe en otro periodo
     */
    @Transactional("metadataTransactionManager")
    fun update(id: Short, request: PeriodUpdateRequest): PeriodResponse? {
        logger.info("Actualizando periodo con ID: $id")

        val existingPeriod = periodRepository.findById(id).orElse(null)
            ?: run {
                logger.warn("No se encontró periodo con ID: $id para actualizar")
                return null
            }

        // Si se proporciona un nuevo nombre, validar que no exista en otro periodo
        request.name?.let { newName ->
            val normalizedName = newName.uppercase().trim()
            periodRepository.findByName(normalizedName)?.let { existing ->
                if (existing.id != id) {
                    throw IllegalArgumentException("Ya existe otro periodo con nombre: $normalizedName")
                }
            }
        }

        // Crear nueva instancia con los valores actualizados (data class es inmutable)
        val updatedPeriod = Period(
            id = existingPeriod.id,
            name = request.name?.uppercase()?.trim() ?: existingPeriod.name
        )

        val savedPeriod = periodRepository.save(updatedPeriod)
        logger.info("Periodo actualizado exitosamente: ${savedPeriod.name}")

        return savedPeriod.toResponse()
    }

    /**
     * Elimina un periodo.
     * @param id ID del periodo a eliminar
     * @return true si se eliminó, false si no existía
     * @throws IllegalStateException si el periodo tiene settings asociados
     */
    @Transactional("metadataTransactionManager")
    fun delete(id: Short): Boolean {
        logger.info("Eliminando periodo con ID: $id")

        if (!periodRepository.existsById(id)) {
            logger.warn("No se encontró periodo con ID: $id para eliminar")
            return false
        }

        // Verificar si hay settings asociados a este periodo
        val associatedSettings = settingRepository.findAll().filter { it.periodId == id }
        if (associatedSettings.isNotEmpty()) {
            throw IllegalStateException(
                "No se puede eliminar el periodo con ID: $id porque tiene " +
                "${associatedSettings.size} configuración(es) asociada(s)"
            )
        }

        periodRepository.deleteById(id)
        logger.info("Periodo con ID: $id eliminado exitosamente")

        return true
    }

    /**
     * Verifica si existe un periodo con el ID especificado.
     */
    fun existsById(id: Short): Boolean {
        return periodRepository.existsById(id)
    }
}
