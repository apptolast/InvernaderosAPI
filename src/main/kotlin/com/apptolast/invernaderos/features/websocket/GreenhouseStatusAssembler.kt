package com.apptolast.invernaderos.features.websocket

import com.apptolast.invernaderos.features.alert.AlertRepository
import com.apptolast.invernaderos.features.device.DeviceRepository
import com.apptolast.invernaderos.features.greenhouse.GreenhouseRepository
import com.apptolast.invernaderos.features.sector.Sector
import com.apptolast.invernaderos.features.sector.SectorRepository
import com.apptolast.invernaderos.features.setting.SettingRepository
import com.apptolast.invernaderos.features.telemetry.timescaledb.entities.DeviceCurrentValue
import com.apptolast.invernaderos.features.telemetry.timeseries.DeviceCurrentValueRepository
import com.apptolast.invernaderos.features.tenant.TenantRepository
import com.apptolast.invernaderos.features.user.UserRepository
import com.apptolast.invernaderos.features.websocket.dto.GreenhouseStatusResponse
import com.apptolast.invernaderos.features.websocket.dto.toResponse
import com.apptolast.invernaderos.features.websocket.dto.toDto
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

/**
 * Servicio que ensambla la respuesta enriquecida del WebSocket.
 *
 * Consulta TimescaleDB para obtener los ultimos valores de cada code (DEV-XXXXX, SET-XXXXX)
 * y PostgreSQL para la jerarquia completa de negocio (tenant > greenhouse > sector > devices/settings/alerts).
 *
 * El front recibe un unico JSON con toda la informacion de negocio + valores actuales del hardware.
 * El front NO sabe que existe TimescaleDB.
 */
@Service
class GreenhouseStatusAssembler(
    private val tenantRepository: TenantRepository,
    private val greenhouseRepository: GreenhouseRepository,
    private val sectorRepository: SectorRepository,
    private val deviceRepository: DeviceRepository,
    private val settingRepository: SettingRepository,
    private val alertRepository: AlertRepository,
    private val userRepository: UserRepository,
    private val deviceCurrentValueRepository: DeviceCurrentValueRepository
) {
    private val logger = LoggerFactory.getLogger(GreenhouseStatusAssembler::class.java)

    /**
     * Ensambla la respuesta completa con datos de negocio enriquecidos.
     *
     * 1. Consulta device_current_values: ultimo valor por code (tabla tiny, instantaneo)
     * 2. Carga jerarquia completa de PostgreSQL con EntityGraphs
     * 3. Para cada device/setting, embebe el currentValue de TimescaleDB
     * 4. Devuelve GreenhouseStatusResponse con toda la informacion
     */
    @Transactional("metadataTransactionManager", readOnly = true)
    fun assembleFullStatus(): GreenhouseStatusResponse {
        val startTime = System.currentTimeMillis()

        val readingsMap = loadReadingsMap()

        // Cargar todos los tenants activos y ensamblar cada rama
        val tenants = tenantRepository.findByIsActive(true)
        val tenantResponses = tenants.map { tenant -> assembleTenantBranch(tenant, readingsMap) }

        val duration = System.currentTimeMillis() - startTime
        logger.info("Assembled full status: {} tenants, {} readings in {}ms",
            tenantResponses.size, readingsMap.size, duration)

        return GreenhouseStatusResponse(
            timestamp = Instant.now(),
            tenants = tenantResponses
        )
    }

    /**
     * Snapshot enriquecido de un único tenant. Mantiene el mismo envelope
     * `GreenhouseStatusResponse` que `assembleFullStatus()` (con `tenants`
     * conteniendo un solo elemento) para que los clientes no tengan que
     * cambiar el contrato — solo cambia la ruta de invocación.
     *
     * Pensado para el broadcast push tras un cambio que afecta sólo a un
     * tenant (sensor flush, alert state change, CRUD admin); evita el coste
     * de re-ensamblar todos los tenants cuando solo uno ha cambiado.
     *
     * Devuelve un envelope con `tenants = []` cuando el tenant no existe o
     * está inactivo, por idempotencia frente al broadcast: el listener no
     * tiene que conocer la regla de "tenants activos" para decidir si publica.
     */
    @Transactional("metadataTransactionManager", readOnly = true)
    fun assembleStatusForTenant(tenantId: Long): GreenhouseStatusResponse {
        val startTime = System.currentTimeMillis()

        val tenant = tenantRepository.findById(tenantId).orElse(null)
        val readingsMap = loadReadingsMap()

        val tenantResponses = if (tenant == null || tenant.isActive != true) {
            emptyList()
        } else {
            listOf(assembleTenantBranch(tenant, readingsMap))
        }

        val duration = System.currentTimeMillis() - startTime
        logger.debug("Assembled tenant status: tenantId={} hits={} readings={} in {}ms",
            tenantId, tenantResponses.size, readingsMap.size, duration)

        return GreenhouseStatusResponse(
            timestamp = Instant.now(),
            tenants = tenantResponses
        )
    }

    /** Carga la tabla `device_current_values` y devuelve un mapa code → row. */
    private fun loadReadingsMap(): Map<String, DeviceCurrentValue> {
        val latestReadings = deviceCurrentValueRepository.findAll()
        logger.debug("Loaded {} current values from device_current_values", latestReadings.size)
        return latestReadings.associateBy { it.code }
    }

    /**
     * Ensambla la rama completa de un tenant: usuarios, invernaderos,
     * sectores y sus children (devices, settings, alerts) con `currentValue`
     * embebido cuando lo hay.
     */
    private fun assembleTenantBranch(
        tenant: com.apptolast.invernaderos.features.tenant.Tenant,
        readingsMap: Map<String, DeviceCurrentValue>
    ): com.apptolast.invernaderos.features.websocket.dto.TenantResponse {
        val tenantId = tenant.id!!
        val users = userRepository.findByTenantId(tenantId)
        val greenhouses = greenhouseRepository.findByTenantId(tenantId)

        val greenhouseResponses = greenhouses.map { greenhouse ->
            val sectors = sectorRepository.findByGreenhouseId(greenhouse.id!!)
            val sectorResponses = sectors.map { sector -> assembleSector(sector, readingsMap) }
            greenhouse.toResponse(sectors = sectorResponses)
        }

        return tenant.toResponse(
            users = users.map { it.toResponse() },
            greenhouses = greenhouseResponses
        )
    }

    /**
     * Ensambla un sector con sus devices, settings y alerts,
     * embebiendo los valores actuales de TimescaleDB.
     */
    private fun assembleSector(
        sector: Sector,
        readingsMap: Map<String, DeviceCurrentValue>
    ): com.apptolast.invernaderos.features.websocket.dto.SectorResponse {
        val sectorId = sector.id!!

        // Cargar devices con catalogo (EntityGraph: Device.withCatalog)
        val devices = deviceRepository.findBySectorId(sectorId)
        val deviceResponses = devices.map { device ->
            val currentValue = readingsMap[device.code]
            device.toResponse(
                currentValue = normalizeCurrentValue(currentValue?.value, device.type?.dataType),
                lastUpdated = currentValue?.lastSeenAt
            )
        }

        // Cargar settings con catalogo (EntityGraph: Setting.withCatalog)
        val settings = settingRepository.findBySectorId(sectorId)
        val settingResponses = settings.map { setting ->
            val currentValue = readingsMap[setting.code]
            setting.toResponse(
                currentValue = normalizeCurrentValue(currentValue?.value, setting.dataType?.name),
                lastUpdated = currentValue?.lastSeenAt
            )
        }

        // Cargar alerts con catalogo (EntityGraph: Alert.context)
        val alerts = alertRepository.findBySectorId(sectorId)
        val alertResponses = alerts.map { it.toResponse() }

        return sector.toResponse(
            devices = deviceResponses,
            settings = settingResponses,
            alerts = alertResponses
        )
    }

    companion object {
        /**
         * Normalises the wire representation of a current value before sending it to the client.
         *
         * The MQTT ingestion path (`DeviceStatusListener`) converts JSON booleans into the
         * strings `"1"` / `"0"` to keep TimescaleDB continuous aggregates that cast value to
         * `double precision` (commit 6c98ca6) working. That choice is correct for the storage
         * layer but breaks UI consumers that read `currentValue` and compare against the
         * conventional `"true"` / `"false"` strings (the case for the mobile app's
         * SetpointBooleanEditor and several view models).
         *
         * For codes whose catalog declares `dataType == "BOOLEAN"`, this method translates
         * the stored `"1"` → `"true"` and `"0"` → `"false"`. Any other value (or non-boolean
         * dataType) is passed through unchanged. The DB layout is not touched; only the
         * representation that travels over the WebSocket changes.
         *
         * @param storedValue The raw value as stored in iot.device_current_values (or null).
         * @param dataType Catalog dataType name (e.g. "BOOLEAN", "DOUBLE", "INTEGER", "STRING").
         */
        internal fun normalizeCurrentValue(storedValue: String?, dataType: String?): String? {
            if (storedValue == null) return null
            if (!dataType.equals("BOOLEAN", ignoreCase = true)) return storedValue
            return when (storedValue) {
                "1" -> "true"
                "0" -> "false"
                else -> storedValue
            }
        }
    }
}
