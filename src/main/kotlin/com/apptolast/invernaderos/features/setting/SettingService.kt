package com.apptolast.invernaderos.features.setting

import com.apptolast.invernaderos.config.CodeGeneratorService
import com.apptolast.invernaderos.features.catalog.DeviceTypeRepository
import com.apptolast.invernaderos.features.catalog.PeriodRepository
import com.apptolast.invernaderos.features.greenhouse.GreenhouseRepository
import com.apptolast.invernaderos.features.setting.dto.SettingCreateRequest
import com.apptolast.invernaderos.features.setting.dto.SettingResponse
import com.apptolast.invernaderos.features.setting.dto.SettingUpdateRequest
import com.apptolast.invernaderos.features.setting.dto.toResponse
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

/**
 * Service para operaciones CRUD de settings (configuraciones de parámetros).
 * Los settings definen rangos min/max para cada tipo de parámetro (sensor)
 * por invernadero y periodo del día.
 *
 * @see <a href="https://docs.spring.io/spring-boot/reference/data/sql.html">Spring Boot SQL Data Access</a>
 */
@Service
class SettingService(
    private val settingRepository: SettingRepository,
    private val greenhouseRepository: GreenhouseRepository,
    private val deviceTypeRepository: DeviceTypeRepository,
    private val periodRepository: PeriodRepository,
    private val codeGeneratorService: CodeGeneratorService
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    /**
     * Obtiene todas las configuraciones de un tenant.
     */
    fun findAllByTenantId(tenantId: Long): List<SettingResponse> {
        logger.debug("Obteniendo settings para tenant: $tenantId")
        return settingRepository.findByTenantId(tenantId).map { it.toResponse() }
    }

    /**
     * Obtiene todas las configuraciones de un invernadero.
     */
    fun findAllByGreenhouseId(greenhouseId: Long): List<SettingResponse> {
        logger.debug("Obteniendo settings para invernadero: $greenhouseId")
        return settingRepository.findByGreenhouseId(greenhouseId).map { it.toResponse() }
    }

    /**
     * Obtiene las configuraciones activas de un invernadero.
     */
    fun findActiveByGreenhouseId(greenhouseId: Long): List<SettingResponse> {
        logger.debug("Obteniendo settings activos para invernadero: $greenhouseId")
        return settingRepository.findByGreenhouseIdAndIsActive(greenhouseId, true).map { it.toResponse() }
    }

    /**
     * Obtiene las configuraciones de un invernadero filtradas por parámetro.
     */
    fun findByGreenhouseIdAndParameterId(greenhouseId: Long, parameterId: Short): List<SettingResponse> {
        logger.debug("Obteniendo settings para invernadero: $greenhouseId, parámetro: $parameterId")
        return settingRepository.findByGreenhouseIdAndParameterId(greenhouseId, parameterId)
            .map { it.toResponse() }
    }

    /**
     * Obtiene las configuraciones de un invernadero filtradas por periodo.
     */
    fun findByGreenhouseIdAndPeriodId(greenhouseId: Long, periodId: Short): List<SettingResponse> {
        logger.debug("Obteniendo settings para invernadero: $greenhouseId, periodo: $periodId")
        return settingRepository.findByGreenhouseIdAndPeriodId(greenhouseId, periodId)
            .map { it.toResponse() }
    }

    /**
     * Obtiene una configuración específica por invernadero, parámetro y periodo.
     */
    fun findByGreenhouseParameterAndPeriod(
        greenhouseId: Long,
        parameterId: Short,
        periodId: Short
    ): SettingResponse? {
        logger.debug("Buscando setting: invernadero=$greenhouseId, parámetro=$parameterId, periodo=$periodId")
        return settingRepository.findByGreenhouseIdAndParameterIdAndPeriodId(
            greenhouseId, parameterId, periodId
        )?.toResponse()
    }

    /**
     * Obtiene una configuración por ID y tenant.
     */
    fun findByIdAndTenantId(id: Long, tenantId: Long): SettingResponse? {
        logger.debug("Buscando setting con ID: $id para tenant: $tenantId")
        val setting = settingRepository.findById(id).orElse(null) ?: return null
        if (setting.tenantId != tenantId) return null
        return setting.toResponse()
    }

    /**
     * Crea una nueva configuración.
     * @param tenantId ID del tenant propietario
     * @param request Datos de la configuración
     * @return La configuración creada
     * @throws IllegalArgumentException si el invernadero no existe, no pertenece al tenant,
     *         o si ya existe una configuración con la misma combinación greenhouse/parameter/period
     */
    @Transactional("metadataTransactionManager")
    fun create(tenantId: Long, request: SettingCreateRequest): SettingResponse {
        logger.info("Creando setting para invernadero: ${request.greenhouseId}, parámetro: ${request.parameterId}, periodo: ${request.periodId}")

        // Validar que el invernadero existe y pertenece al tenant
        val greenhouse = greenhouseRepository.findById(request.greenhouseId).orElse(null)
            ?: throw IllegalArgumentException("No existe el invernadero con ID: ${request.greenhouseId}")

        if (greenhouse.tenantId != tenantId) {
            throw IllegalArgumentException("El invernadero no pertenece al tenant especificado")
        }

        // Validar que el tipo de parámetro existe
        if (!deviceTypeRepository.existsById(request.parameterId)) {
            throw IllegalArgumentException("No existe el tipo de parámetro con ID: ${request.parameterId}")
        }

        // Validar que el periodo existe
        if (!periodRepository.existsById(request.periodId)) {
            throw IllegalArgumentException("No existe el periodo con ID: ${request.periodId}")
        }

        // Validar que no existe ya una configuración con esta combinación
        settingRepository.findByGreenhouseIdAndParameterIdAndPeriodId(
            request.greenhouseId, request.parameterId, request.periodId
        )?.let {
            throw IllegalArgumentException(
                "Ya existe una configuración para invernadero=${request.greenhouseId}, " +
                "parámetro=${request.parameterId}, periodo=${request.periodId}"
            )
        }

        // Validar que minValue <= maxValue si ambos están presentes
        if (request.minValue != null && request.maxValue != null) {
            if (request.minValue > request.maxValue) {
                throw IllegalArgumentException(
                    "El valor mínimo (${request.minValue}) no puede ser mayor que el máximo (${request.maxValue})"
                )
            }
        }

        val setting = Setting(
            code = codeGeneratorService.generateSettingCode(),
            greenhouseId = request.greenhouseId,
            tenantId = tenantId,
            parameterId = request.parameterId,
            periodId = request.periodId,
            minValue = request.minValue,
            maxValue = request.maxValue,
            isActive = request.isActive
        )

        val savedSetting = settingRepository.save(setting)

        // Recargar para obtener las relaciones
        val reloadedSetting = settingRepository.findById(savedSetting.id!!).orElse(savedSetting)

        logger.info("Setting creado exitosamente con ID: ${reloadedSetting.id}")
        return reloadedSetting.toResponse()
    }

    /**
     * Actualiza una configuración existente.
     * @param id ID de la configuración
     * @param tenantId ID del tenant propietario
     * @param request Datos a actualizar
     * @return La configuración actualizada o null si no existe
     * @throws IllegalArgumentException si los nuevos valores son inválidos
     */
    @Transactional("metadataTransactionManager")
    fun update(id: Long, tenantId: Long, request: SettingUpdateRequest): SettingResponse? {
        logger.info("Actualizando setting con ID: $id")

        val existingSetting = settingRepository.findById(id).orElse(null)
            ?: run {
                logger.warn("No se encontró setting con ID: $id")
                return null
            }

        if (existingSetting.tenantId != tenantId) {
            logger.warn("El setting con ID: $id no pertenece al tenant: $tenantId")
            return null
        }

        // Validar que el nuevo tipo de parámetro existe (si se proporciona)
        request.parameterId?.let { paramId ->
            if (!deviceTypeRepository.existsById(paramId)) {
                throw IllegalArgumentException("No existe el tipo de parámetro con ID: $paramId")
            }
        }

        // Validar que el nuevo periodo existe (si se proporciona)
        request.periodId?.let { perIdValue ->
            if (!periodRepository.existsById(perIdValue)) {
                throw IllegalArgumentException("No existe el periodo con ID: $perIdValue")
            }
        }

        // Validar rango de valores
        val newMin = request.minValue ?: existingSetting.minValue
        val newMax = request.maxValue ?: existingSetting.maxValue
        if (newMin != null && newMax != null && newMin > newMax) {
            throw IllegalArgumentException(
                "El valor mínimo ($newMin) no puede ser mayor que el máximo ($newMax)"
            )
        }

        // Si se cambia parameter o period, verificar que no exista otra config con esa combinación
        val newParameterId = request.parameterId ?: existingSetting.parameterId
        val newPeriodId = request.periodId ?: existingSetting.periodId

        if (newParameterId != existingSetting.parameterId || newPeriodId != existingSetting.periodId) {
            settingRepository.findByGreenhouseIdAndParameterIdAndPeriodId(
                existingSetting.greenhouseId, newParameterId, newPeriodId
            )?.let { existing ->
                if (existing.id != id) {
                    throw IllegalArgumentException(
                        "Ya existe otra configuración para invernadero=${existingSetting.greenhouseId}, " +
                        "parámetro=$newParameterId, periodo=$newPeriodId"
                    )
                }
            }
        }

        val updatedSetting = existingSetting.copy(
            parameterId = newParameterId,
            periodId = newPeriodId,
            minValue = request.minValue ?: existingSetting.minValue,
            maxValue = request.maxValue ?: existingSetting.maxValue,
            isActive = request.isActive ?: existingSetting.isActive,
            updatedAt = Instant.now()
        )

        settingRepository.save(updatedSetting)

        // Recargar para obtener las relaciones actualizadas
        val reloadedSetting = settingRepository.findById(id).orElse(updatedSetting)

        logger.info("Setting actualizado exitosamente: ${reloadedSetting.id}")
        return reloadedSetting.toResponse()
    }

    /**
     * Elimina una configuración.
     * @param id ID de la configuración
     * @param tenantId ID del tenant propietario
     * @return true si se eliminó, false si no existía o no pertenece al tenant
     */
    @Transactional("metadataTransactionManager")
    fun delete(id: Long, tenantId: Long): Boolean {
        logger.info("Eliminando setting con ID: $id")

        val setting = settingRepository.findById(id).orElse(null)
            ?: run {
                logger.warn("No se encontró setting con ID: $id para eliminar")
                return false
            }

        if (setting.tenantId != tenantId) {
            logger.warn("El setting con ID: $id no pertenece al tenant: $tenantId")
            return false
        }

        settingRepository.delete(setting)
        logger.info("Setting con ID: $id eliminado exitosamente")

        return true
    }
}
