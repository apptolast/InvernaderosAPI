package com.apptolast.invernaderos.features.setting

import com.apptolast.invernaderos.config.CodeGeneratorService
import com.apptolast.invernaderos.features.catalog.ActuatorStateRepository
import com.apptolast.invernaderos.features.catalog.DataTypeRepository
import com.apptolast.invernaderos.features.catalog.DataTypeService
import com.apptolast.invernaderos.features.catalog.DeviceTypeRepository
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
 * Service para operaciones CRUD de settings (configuraciones de parametros).
 * Los settings definen valores de configuracion para cada tipo de parametro (sensor)
 * por invernadero y estado del actuador.
 *
 * @see <a href="https://docs.spring.io/spring-boot/reference/data/sql.html">Spring Boot SQL Data Access</a>
 */
@Service
class SettingService(
    private val settingRepository: SettingRepository,
    private val greenhouseRepository: GreenhouseRepository,
    private val deviceTypeRepository: DeviceTypeRepository,
    private val actuatorStateRepository: ActuatorStateRepository,
    private val dataTypeRepository: DataTypeRepository,
    private val dataTypeService: DataTypeService,
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
     * Obtiene las configuraciones de un invernadero filtradas por parametro.
     */
    fun findByGreenhouseIdAndParameterId(greenhouseId: Long, parameterId: Short): List<SettingResponse> {
        logger.debug("Obteniendo settings para invernadero: $greenhouseId, parametro: $parameterId")
        return settingRepository.findByGreenhouseIdAndParameterId(greenhouseId, parameterId)
            .map { it.toResponse() }
    }

    /**
     * Obtiene las configuraciones de un invernadero filtradas por estado de actuador.
     */
    fun findByGreenhouseIdAndActuatorStateId(greenhouseId: Long, actuatorStateId: Short): List<SettingResponse> {
        logger.debug("Obteniendo settings para invernadero: $greenhouseId, actuatorState: $actuatorStateId")
        return settingRepository.findByGreenhouseIdAndActuatorStateId(greenhouseId, actuatorStateId)
            .map { it.toResponse() }
    }

    /**
     * Obtiene una configuracion especifica por invernadero, parametro y estado de actuador.
     */
    fun findByGreenhouseParameterAndActuatorState(
        greenhouseId: Long,
        parameterId: Short,
        actuatorStateId: Short
    ): SettingResponse? {
        logger.debug("Buscando setting: invernadero=$greenhouseId, parametro=$parameterId, actuatorState=$actuatorStateId")
        return settingRepository.findByGreenhouseIdAndParameterIdAndActuatorStateId(
            greenhouseId, parameterId, actuatorStateId
        )?.toResponse()
    }

    /**
     * Obtiene una configuracion por ID y tenant.
     */
    fun findByIdAndTenantId(id: Long, tenantId: Long): SettingResponse? {
        logger.debug("Buscando setting con ID: $id para tenant: $tenantId")
        val setting = settingRepository.findById(id).orElse(null) ?: return null
        if (setting.tenantId != tenantId) return null
        return setting.toResponse()
    }

    /**
     * Crea una nueva configuracion.
     * @param tenantId ID del tenant propietario
     * @param request Datos de la configuracion
     * @return La configuracion creada
     * @throws IllegalArgumentException si el invernadero no existe, no pertenece al tenant,
     *         o si ya existe una configuracion con la misma combinacion greenhouse/parameter/actuatorState
     */
    @Transactional("metadataTransactionManager")
    fun create(tenantId: Long, request: SettingCreateRequest): SettingResponse {
        logger.info("Creando setting para invernadero: ${request.greenhouseId}, parametro: ${request.parameterId}, actuatorState: ${request.actuatorStateId}")

        // Validar que el invernadero existe y pertenece al tenant
        val greenhouse = greenhouseRepository.findById(request.greenhouseId).orElse(null)
            ?: throw IllegalArgumentException("No existe el invernadero con ID: ${request.greenhouseId}")

        if (greenhouse.tenantId != tenantId) {
            throw IllegalArgumentException("El invernadero no pertenece al tenant especificado")
        }

        // Validar que el tipo de parametro existe
        if (!deviceTypeRepository.existsById(request.parameterId)) {
            throw IllegalArgumentException("No existe el tipo de parametro con ID: ${request.parameterId}")
        }

        // Validar que el estado de actuador existe (si se proporciona)
        request.actuatorStateId?.let { stateId ->
            if (!actuatorStateRepository.existsById(stateId)) {
                throw IllegalArgumentException("No existe el estado de actuador con ID: $stateId")
            }
        }

        // Validar que el tipo de dato existe (si se proporciona)
        request.dataTypeId?.let { typeId ->
            if (!dataTypeRepository.existsById(typeId)) {
                throw IllegalArgumentException("No existe el tipo de dato con ID: $typeId")
            }
        }

        // Validar el valor segun el tipo de dato (si ambos se proporcionan)
        if (request.dataTypeId != null && request.value != null) {
            if (!dataTypeService.validateValue(request.dataTypeId, request.value)) {
                val dataType = dataTypeRepository.findById(request.dataTypeId).orElse(null)
                throw IllegalArgumentException(
                    "El valor '${request.value}' no es valido para el tipo de dato ${dataType?.name ?: request.dataTypeId}. " +
                    "Ejemplo valido: ${dataType?.exampleValue ?: "N/A"}"
                )
            }
        }

        // Validar que no existe ya una configuracion con esta combinacion
        if (request.actuatorStateId != null) {
            settingRepository.findByGreenhouseIdAndParameterIdAndActuatorStateId(
                request.greenhouseId, request.parameterId, request.actuatorStateId
            )?.let {
                throw IllegalArgumentException(
                    "Ya existe una configuracion para invernadero=${request.greenhouseId}, " +
                    "parametro=${request.parameterId}, actuatorState=${request.actuatorStateId}"
                )
            }
        }

        val setting = Setting(
            code = codeGeneratorService.generateSettingCode(),
            greenhouseId = request.greenhouseId,
            tenantId = tenantId,
            parameterId = request.parameterId,
            actuatorStateId = request.actuatorStateId,
            dataTypeId = request.dataTypeId,
            value = request.value,
            isActive = request.isActive
        )

        val savedSetting = settingRepository.save(setting)

        // Recargar para obtener las relaciones
        val reloadedSetting = settingRepository.findById(savedSetting.id!!).orElse(savedSetting)

        logger.info("Setting creado exitosamente con ID: ${reloadedSetting.id}")
        return reloadedSetting.toResponse()
    }

    /**
     * Actualiza una configuracion existente.
     * @param id ID de la configuracion
     * @param tenantId ID del tenant propietario
     * @param request Datos a actualizar
     * @return La configuracion actualizada o null si no existe
     * @throws IllegalArgumentException si los nuevos valores son invalidos
     */
    @Transactional("metadataTransactionManager")
    fun update(id: Long, tenantId: Long, request: SettingUpdateRequest): SettingResponse? {
        logger.info("Actualizando setting con ID: $id")

        val existingSetting = settingRepository.findById(id).orElse(null)
            ?: run {
                logger.warn("No se encontro setting con ID: $id")
                return null
            }

        if (existingSetting.tenantId != tenantId) {
            logger.warn("El setting con ID: $id no pertenece al tenant: $tenantId")
            return null
        }

        // Validar que el nuevo tipo de parametro existe (si se proporciona)
        request.parameterId?.let { paramId ->
            if (!deviceTypeRepository.existsById(paramId)) {
                throw IllegalArgumentException("No existe el tipo de parametro con ID: $paramId")
            }
        }

        // Validar que el nuevo estado de actuador existe (si se proporciona)
        request.actuatorStateId?.let { stateId ->
            if (!actuatorStateRepository.existsById(stateId)) {
                throw IllegalArgumentException("No existe el estado de actuador con ID: $stateId")
            }
        }

        // Validar que el nuevo tipo de dato existe (si se proporciona)
        request.dataTypeId?.let { typeId ->
            if (!dataTypeRepository.existsById(typeId)) {
                throw IllegalArgumentException("No existe el tipo de dato con ID: $typeId")
            }
        }

        // Validar el valor segun el tipo de dato
        val newDataTypeId = request.dataTypeId ?: existingSetting.dataTypeId
        val newValue = request.value ?: existingSetting.value
        if (newDataTypeId != null && newValue != null) {
            if (!dataTypeService.validateValue(newDataTypeId, newValue)) {
                val dataType = dataTypeRepository.findById(newDataTypeId).orElse(null)
                throw IllegalArgumentException(
                    "El valor '$newValue' no es valido para el tipo de dato ${dataType?.name ?: newDataTypeId}. " +
                    "Ejemplo valido: ${dataType?.exampleValue ?: "N/A"}"
                )
            }
        }

        // Si se cambia parameter o actuatorState, verificar que no exista otra config con esa combinacion
        val newParameterId = request.parameterId ?: existingSetting.parameterId
        val newActuatorStateId = request.actuatorStateId ?: existingSetting.actuatorStateId

        if (newActuatorStateId != null &&
            (newParameterId != existingSetting.parameterId || newActuatorStateId != existingSetting.actuatorStateId)) {
            settingRepository.findByGreenhouseIdAndParameterIdAndActuatorStateId(
                existingSetting.greenhouseId, newParameterId, newActuatorStateId
            )?.let { existing ->
                if (existing.id != id) {
                    throw IllegalArgumentException(
                        "Ya existe otra configuracion para invernadero=${existingSetting.greenhouseId}, " +
                        "parametro=$newParameterId, actuatorState=$newActuatorStateId"
                    )
                }
            }
        }

        val updatedSetting = existingSetting.copy(
            parameterId = newParameterId,
            actuatorStateId = newActuatorStateId,
            dataTypeId = newDataTypeId,
            value = newValue,
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
     * Elimina una configuracion.
     * @param id ID de la configuracion
     * @param tenantId ID del tenant propietario
     * @return true si se elimino, false si no existia o no pertenece al tenant
     */
    @Transactional("metadataTransactionManager")
    fun delete(id: Long, tenantId: Long): Boolean {
        logger.info("Eliminando setting con ID: $id")

        val setting = settingRepository.findById(id).orElse(null)
            ?: run {
                logger.warn("No se encontro setting con ID: $id para eliminar")
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
