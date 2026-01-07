package com.apptolast.invernaderos.features.catalog

import com.apptolast.invernaderos.features.catalog.dto.UnitCreateRequest
import com.apptolast.invernaderos.features.catalog.dto.UnitResponse
import com.apptolast.invernaderos.features.catalog.dto.UnitUpdateRequest
import com.apptolast.invernaderos.features.catalog.dto.toResponse
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

/**
 * Service para operaciones CRUD de unidades de medida.
 * Las unidades definen las medidas de los sensores y actuadores (°C, %, hPa, etc.)
 *
 * @see <a href="https://docs.spring.io/spring-boot/reference/data/sql.html">Spring Boot SQL Data Access</a>
 */
@Service
class UnitService(
    private val unitRepository: UnitRepository,
    private val deviceTypeRepository: DeviceTypeRepository
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    /**
     * Obtiene todas las unidades de medida.
     * @param activeOnly Si es true, solo retorna unidades activas
     */
    fun findAll(activeOnly: Boolean = true): List<UnitResponse> {
        logger.debug("Obteniendo todas las unidades de medida (activeOnly=$activeOnly)")
        val units = if (activeOnly) {
            unitRepository.findByIsActive(true)
        } else {
            unitRepository.findAll()
        }
        return units.map { it.toResponse() }
    }

    /**
     * Obtiene una unidad por su ID.
     * @param id ID de la unidad
     * @return La unidad encontrada o null si no existe
     */
    fun findById(id: Short): UnitResponse? {
        logger.debug("Buscando unidad con ID: $id")
        return unitRepository.findById(id)
            .map { it.toResponse() }
            .orElse(null)
    }

    /**
     * Obtiene una unidad por su símbolo.
     * @param symbol Símbolo de la unidad (ej: "°C", "%", "hPa")
     * @return La unidad encontrada o null si no existe
     */
    fun findBySymbol(symbol: String): UnitResponse? {
        logger.debug("Buscando unidad con símbolo: $symbol")
        return unitRepository.findBySymbol(symbol)?.toResponse()
    }

    /**
     * Crea una nueva unidad de medida.
     * El ID se genera automáticamente por la base de datos.
     *
     * @param request Datos de la nueva unidad
     * @return La unidad creada con su ID auto-generado
     * @throws IllegalArgumentException si el símbolo ya existe
     */
    @Transactional("metadataTransactionManager")
    fun create(request: UnitCreateRequest): UnitResponse {
        logger.info("Creando nueva unidad de medida: ${request.symbol}")

        // Validar que el símbolo no exista
        unitRepository.findBySymbol(request.symbol)?.let {
            throw IllegalArgumentException("Ya existe una unidad con símbolo: ${request.symbol}")
        }

        val unit = Unit(
            symbol = request.symbol.trim(),
            name = request.name.trim(),
            description = request.description?.trim(),
            isActive = request.isActive,
            createdAt = Instant.now()
        )

        val savedUnit = unitRepository.save(unit)
        logger.info("Unidad creada exitosamente: ${savedUnit.symbol} (ID: ${savedUnit.id})")

        return savedUnit.toResponse()
    }

    /**
     * Actualiza una unidad de medida existente.
     * @param id ID de la unidad a actualizar
     * @param request Datos a actualizar
     * @return La unidad actualizada o null si no existe
     * @throws IllegalArgumentException si el nuevo símbolo ya existe en otra unidad
     */
    @Transactional("metadataTransactionManager")
    fun update(id: Short, request: UnitUpdateRequest): UnitResponse? {
        logger.info("Actualizando unidad con ID: $id")

        val existingUnit = unitRepository.findById(id).orElse(null)
            ?: run {
                logger.warn("No se encontró unidad con ID: $id para actualizar")
                return null
            }

        // Si se proporciona un nuevo símbolo, validar que no exista en otra unidad
        request.symbol?.let { newSymbol ->
            unitRepository.findBySymbol(newSymbol)?.let { existing ->
                if (existing.id != id) {
                    throw IllegalArgumentException("Ya existe otra unidad con símbolo: $newSymbol")
                }
            }
        }

        // Crear nueva instancia con los valores actualizados (data class es inmutable)
        val updatedUnit = Unit(
            id = existingUnit.id,
            symbol = request.symbol?.trim() ?: existingUnit.symbol,
            name = request.name?.trim() ?: existingUnit.name,
            description = request.description?.trim() ?: existingUnit.description,
            isActive = request.isActive ?: existingUnit.isActive,
            createdAt = existingUnit.createdAt
        )

        val savedUnit = unitRepository.save(updatedUnit)
        logger.info("Unidad actualizada exitosamente: ${savedUnit.symbol}")

        return savedUnit.toResponse()
    }

    /**
     * Elimina una unidad de medida.
     * @param id ID de la unidad a eliminar
     * @return true si se eliminó, false si no existía
     * @throws IllegalStateException si la unidad está siendo usada por tipos de dispositivo
     */
    @Transactional("metadataTransactionManager")
    fun delete(id: Short): Boolean {
        logger.info("Eliminando unidad con ID: $id")

        if (!unitRepository.existsById(id)) {
            logger.warn("No se encontró unidad con ID: $id para eliminar")
            return false
        }

        // Verificar si hay tipos de dispositivo usando esta unidad
        val associatedTypes = deviceTypeRepository.findAll().filter { it.defaultUnitId == id }
        if (associatedTypes.isNotEmpty()) {
            throw IllegalStateException(
                "No se puede eliminar la unidad con ID: $id porque está siendo usada por " +
                "${associatedTypes.size} tipo(s) de dispositivo: " +
                associatedTypes.take(5).joinToString { it.name }
            )
        }

        unitRepository.deleteById(id)
        logger.info("Unidad con ID: $id eliminada exitosamente")

        return true
    }

    /**
     * Activa una unidad de medida.
     */
    @Transactional("metadataTransactionManager")
    fun activate(id: Short): UnitResponse? {
        logger.info("Activando unidad con ID: $id")
        return update(id, UnitUpdateRequest(isActive = true))
    }

    /**
     * Desactiva una unidad de medida.
     */
    @Transactional("metadataTransactionManager")
    fun deactivate(id: Short): UnitResponse? {
        logger.info("Desactivando unidad con ID: $id")
        return update(id, UnitUpdateRequest(isActive = false))
    }

    /**
     * Verifica si existe una unidad con el ID especificado.
     */
    fun existsById(id: Short): Boolean {
        return unitRepository.existsById(id)
    }
}
