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

        // 1. Obtener ultimos valores de device_current_values (tabla tiny, sin DISTINCT ON)
        val latestReadings = deviceCurrentValueRepository.findAll()
        val readingsMap = latestReadings.associateBy { it.code }

        logger.debug("Loaded {} current values from device_current_values", latestReadings.size)

        // 2. Cargar todos los tenants activos
        val tenants = tenantRepository.findByIsActive(true)

        // 3. Ensamblar la jerarquia para cada tenant
        val tenantResponses = tenants.map { tenant ->
            val tenantId = tenant.id!!

            // Cargar usuarios del tenant
            val users = userRepository.findByTenantId(tenantId)

            // Cargar greenhouses del tenant
            val greenhouses = greenhouseRepository.findByTenantId(tenantId)

            // Para cada greenhouse, cargar sectores y sus hijos
            val greenhouseResponses = greenhouses.map { greenhouse ->
                val sectors = sectorRepository.findByGreenhouseId(greenhouse.id!!)

                val sectorResponses = sectors.map { sector ->
                    assembleSector(sector, readingsMap)
                }

                greenhouse.toResponse(sectors = sectorResponses)
            }

            tenant.toResponse(
                users = users.map { it.toResponse() },
                greenhouses = greenhouseResponses
            )
        }

        val duration = System.currentTimeMillis() - startTime
        logger.info("Assembled full status: {} tenants, {} readings in {}ms",
            tenantResponses.size, latestReadings.size, duration)

        return GreenhouseStatusResponse(
            timestamp = Instant.now(),
            tenants = tenantResponses
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
                currentValue = currentValue?.value,
                lastUpdated = currentValue?.lastSeenAt
            )
        }

        // Cargar settings con catalogo (EntityGraph: Setting.withCatalog)
        val settings = settingRepository.findBySectorId(sectorId)
        val settingResponses = settings.map { setting ->
            val currentValue = readingsMap[setting.code]
            setting.toResponse(
                currentValue = currentValue?.value,
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
}
