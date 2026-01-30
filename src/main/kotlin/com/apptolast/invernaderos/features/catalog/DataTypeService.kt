package com.apptolast.invernaderos.features.catalog

import com.apptolast.invernaderos.features.catalog.dto.DataTypeCreateRequest
import com.apptolast.invernaderos.features.catalog.dto.DataTypeResponse
import com.apptolast.invernaderos.features.catalog.dto.DataTypeUpdateRequest
import com.apptolast.invernaderos.features.catalog.dto.toResponse
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * Service para operaciones CRUD de tipos de datos.
 * Los tipos de datos definen como interpretar los valores en Settings
 * (ej: INTEGER, BOOLEAN, STRING, DOUBLE, DATE, etc.)
 */
@Service
class DataTypeService(
    private val dataTypeRepository: DataTypeRepository
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    /**
     * Obtiene todos los tipos de datos ordenados por displayOrder.
     */
    fun findAll(): List<DataTypeResponse> {
        logger.debug("Obteniendo todos los tipos de datos")
        return dataTypeRepository.findAllByOrderByDisplayOrderAsc().map { it.toResponse() }
    }

    /**
     * Obtiene solo los tipos de datos activos.
     */
    fun findAllActive(): List<DataTypeResponse> {
        logger.debug("Obteniendo tipos de datos activos")
        return dataTypeRepository.findByIsActive(true).map { it.toResponse() }
    }

    /**
     * Obtiene un tipo de dato por ID.
     */
    fun findById(id: Short): DataTypeResponse? {
        logger.debug("Buscando tipo de dato con ID: $id")
        return dataTypeRepository.findById(id).orElse(null)?.toResponse()
    }

    /**
     * Obtiene un tipo de dato por nombre.
     */
    fun findByName(name: String): DataTypeResponse? {
        logger.debug("Buscando tipo de dato con nombre: $name")
        return dataTypeRepository.findByName(name.uppercase())?.toResponse()
    }

    /**
     * Crea un nuevo tipo de dato.
     */
    @Transactional("metadataTransactionManager")
    fun create(request: DataTypeCreateRequest): DataTypeResponse {
        logger.info("Creando tipo de dato: ${request.name}")

        // Validar que no exista ya
        if (dataTypeRepository.existsByName(request.name.uppercase())) {
            throw IllegalArgumentException("Ya existe un tipo de dato con nombre: ${request.name}")
        }

        // Validar regex si se proporciona
        request.validationRegex?.let { regex ->
            try {
                Regex(regex)
            } catch (e: Exception) {
                throw IllegalArgumentException("La expresion regular no es valida: $regex")
            }
        }

        val dataType = DataType(
            name = request.name.uppercase(),
            description = request.description,
            validationRegex = request.validationRegex,
            exampleValue = request.exampleValue,
            displayOrder = request.displayOrder ?: 0,
            isActive = request.isActive ?: true
        )

        val saved = dataTypeRepository.save(dataType)
        logger.info("Tipo de dato creado con ID: ${saved.id}")

        return saved.toResponse()
    }

    /**
     * Actualiza un tipo de dato existente.
     */
    @Transactional("metadataTransactionManager")
    fun update(id: Short, request: DataTypeUpdateRequest): DataTypeResponse? {
        logger.info("Actualizando tipo de dato con ID: $id")

        val existing = dataTypeRepository.findById(id).orElse(null)
            ?: run {
                logger.warn("No se encontro tipo de dato con ID: $id")
                return null
            }

        // Validar nombre unico si se cambia
        request.name?.let { newName ->
            val normalizedName = newName.uppercase()
            if (normalizedName != existing.name && dataTypeRepository.existsByName(normalizedName)) {
                throw IllegalArgumentException("Ya existe otro tipo de dato con nombre: $newName")
            }
        }

        // Validar regex si se proporciona
        request.validationRegex?.let { regex ->
            try {
                Regex(regex)
            } catch (e: Exception) {
                throw IllegalArgumentException("La expresion regular no es valida: $regex")
            }
        }

        val updated = existing.copy(
            name = request.name?.uppercase() ?: existing.name,
            description = request.description ?: existing.description,
            validationRegex = request.validationRegex ?: existing.validationRegex,
            exampleValue = request.exampleValue ?: existing.exampleValue,
            displayOrder = request.displayOrder ?: existing.displayOrder,
            isActive = request.isActive ?: existing.isActive
        )

        val saved = dataTypeRepository.save(updated)
        logger.info("Tipo de dato actualizado: ${saved.id}")

        return saved.toResponse()
    }

    /**
     * Elimina un tipo de dato.
     * No permite eliminar tipos basicos del sistema.
     */
    @Transactional("metadataTransactionManager")
    fun delete(id: Short): Boolean {
        logger.info("Eliminando tipo de dato con ID: $id")

        val dataType = dataTypeRepository.findById(id).orElse(null)
            ?: run {
                logger.warn("No se encontro tipo de dato con ID: $id para eliminar")
                return false
            }

        // No permitir eliminar tipos basicos (IDs 1-9)
        if (id <= 9) {
            throw IllegalStateException("No se puede eliminar el tipo de dato '${dataType.name}' porque es un tipo basico del sistema")
        }

        dataTypeRepository.delete(dataType)
        logger.info("Tipo de dato con ID: $id eliminado exitosamente")

        return true
    }

    /**
     * Verifica si existe un tipo de dato con el ID especificado.
     */
    fun existsById(id: Short): Boolean {
        return dataTypeRepository.existsById(id)
    }

    /**
     * Valida si un valor es valido para un tipo de dato especifico.
     * @param dataTypeId ID del tipo de dato
     * @param value Valor a validar
     * @return true si el valor es valido, false si no
     */
    fun validateValue(dataTypeId: Short, value: String): Boolean {
        val dataType = dataTypeRepository.findById(dataTypeId).orElse(null) ?: return false

        return when (dataType.name) {
            "INTEGER" -> value.toIntOrNull() != null
            "LONG" -> value.toLongOrNull() != null
            "DOUBLE" -> value.toDoubleOrNull() != null
            "BOOLEAN" -> value.lowercase() in listOf("true", "false", "1", "0")
            "STRING" -> true // Cualquier string es valido
            "DATE" -> isValidDate(value)
            "TIME" -> isValidTime(value)
            "DATETIME" -> isValidDateTime(value)
            "JSON" -> isValidJson(value)
            else -> {
                // Usar regex si esta definida
                dataType.validationRegex?.let { regex ->
                    try {
                        Regex(regex).matches(value)
                    } catch (e: Exception) {
                        false
                    }
                } ?: true
            }
        }
    }

    private fun isValidDate(value: String): Boolean {
        return try {
            java.time.LocalDate.parse(value)
            true
        } catch (e: Exception) {
            false
        }
    }

    private fun isValidTime(value: String): Boolean {
        return try {
            java.time.LocalTime.parse(value)
            true
        } catch (e: Exception) {
            false
        }
    }

    private fun isValidDateTime(value: String): Boolean {
        return try {
            java.time.LocalDateTime.parse(value)
            true
        } catch (e: Exception) {
            false
        }
    }

    private fun isValidJson(value: String): Boolean {
        return try {
            val trimmed = value.trim()
            (trimmed.startsWith("{") && trimmed.endsWith("}")) ||
            (trimmed.startsWith("[") && trimmed.endsWith("]"))
        } catch (e: Exception) {
            false
        }
    }
}
