package com.apptolast.invernaderos.config

import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * Servicio para generar codigos unicos legibles para todas las entidades.
 * Los codigos tienen formato: {PREFIX}-{NUMERO_PADDED}
 * Ejemplo: TNT-00001, USR-00001, GRH-00001, DEV-00001, etc.
 *
 * Utiliza secuencias de PostgreSQL para garantizar unicidad en entornos concurrentes.
 */
@Service
class CodeGeneratorService(
    private val jdbcTemplate: JdbcTemplate
) {

    companion object {
        // Prefijos para cada tipo de entidad
        const val PREFIX_TENANT = "TNT"
        const val PREFIX_USER = "USR"
        const val PREFIX_GREENHOUSE = "GRH"
        const val PREFIX_SECTOR = "SEC"
        const val PREFIX_DEVICE = "DEV"
        const val PREFIX_ALERT = "ALT"
        const val PREFIX_SETTING = "SET"
        const val PREFIX_COMMAND_HISTORY = "CMD"
    }

    /**
     * Genera el siguiente codigo para Tenant.
     * @return Codigo unico en formato TNT-{5_digitos}
     */
    @Transactional
    fun generateTenantCode(): String {
        return generateCode(PREFIX_TENANT, "metadata.tenants_code_seq")
    }

    /**
     * Genera el siguiente codigo para User.
     * @return Codigo unico en formato USR-{5_digitos}
     */
    @Transactional
    fun generateUserCode(): String {
        return generateCode(PREFIX_USER, "metadata.users_code_seq")
    }

    /**
     * Genera el siguiente codigo para Greenhouse.
     * @return Codigo unico en formato GRH-{5_digitos}
     */
    @Transactional
    fun generateGreenhouseCode(): String {
        return generateCode(PREFIX_GREENHOUSE, "metadata.greenhouses_code_seq")
    }

    /**
     * Genera el siguiente codigo para Sector.
     * @return Codigo unico en formato SEC-{5_digitos}
     */
    @Transactional
    fun generateSectorCode(): String {
        return generateCode(PREFIX_SECTOR, "metadata.sectors_code_seq")
    }

    /**
     * Genera el siguiente codigo para Device.
     * @return Codigo unico en formato DEV-{5_digitos}
     */
    @Transactional
    fun generateDeviceCode(): String {
        return generateCode(PREFIX_DEVICE, "metadata.devices_code_seq")
    }

    /**
     * Genera el siguiente codigo para Alert.
     * @return Codigo unico en formato ALT-{5_digitos}
     */
    @Transactional
    fun generateAlertCode(): String {
        return generateCode(PREFIX_ALERT, "metadata.alerts_code_seq")
    }

    /**
     * Genera el siguiente codigo para Setting.
     * @return Codigo unico en formato SET-{5_digitos}
     */
    @Transactional
    fun generateSettingCode(): String {
        return generateCode(PREFIX_SETTING, "metadata.settings_code_seq")
    }

    /**
     * Genera el siguiente codigo para CommandHistory.
     * @return Codigo unico en formato CMD-{5_digitos}
     */
    @Transactional
    fun generateCommandHistoryCode(): String {
        return generateCode(PREFIX_COMMAND_HISTORY, "metadata.command_history_code_seq")
    }

    /**
     * Genera un codigo unico utilizando una secuencia de PostgreSQL.
     * @param prefix Prefijo del codigo (ej: TNT, USR, GRH)
     * @param sequenceName Nombre completo de la secuencia (ej: metadata.tenants_code_seq)
     * @return Codigo formateado como PREFIX-{5_digitos}
     */
    private fun generateCode(prefix: String, sequenceName: String): String {
        ensureSequenceExists(sequenceName)
        val nextVal = jdbcTemplate.queryForObject(
            "SELECT nextval('$sequenceName')",
            Long::class.java
        ) ?: 1L
        return "$prefix-${nextVal.toString().padStart(5, '0')}"
    }

    /**
     * Crea la secuencia si no existe.
     * Las secuencias inician en el maximo id existente + 1 para evitar colisiones.
     */
    private fun ensureSequenceExists(sequenceName: String) {
        val exists = jdbcTemplate.queryForObject(
            """
            SELECT EXISTS (
                SELECT 1 FROM pg_sequences
                WHERE schemaname = 'metadata'
                AND sequencename = ?
            )
            """,
            Boolean::class.java,
            sequenceName.substringAfter(".")
        ) ?: false

        if (!exists) {
            // Obtener el nombre de la tabla del nombre de la secuencia
            // metadata.tenants_code_seq -> tenants
            val tableName = sequenceName
                .substringAfter(".")
                .substringBefore("_code_seq")

            // Obtener el maximo id existente para iniciar la secuencia
            val maxId = try {
                jdbcTemplate.queryForObject(
                    "SELECT COALESCE(MAX(id), 0) FROM metadata.$tableName",
                    Long::class.java
                ) ?: 0L
            } catch (e: Exception) {
                0L
            }

            jdbcTemplate.execute(
                "CREATE SEQUENCE IF NOT EXISTS $sequenceName START WITH ${maxId + 1}"
            )
        }
    }
}
