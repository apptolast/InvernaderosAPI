package com.apptolast.invernaderos.features.sector

import com.apptolast.invernaderos.config.CodeGeneratorService
import com.apptolast.invernaderos.features.greenhouse.GreenhouseRepository
import com.apptolast.invernaderos.features.sector.dto.SectorCreateRequest
import com.apptolast.invernaderos.features.sector.dto.SectorResponse
import com.apptolast.invernaderos.features.sector.dto.SectorUpdateRequest
import com.apptolast.invernaderos.features.sector.dto.toResponse
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class SectorService(
    private val sectorRepository: SectorRepository,
    private val greenhouseRepository: GreenhouseRepository,
    private val codeGeneratorService: CodeGeneratorService
) {

    @Transactional(readOnly = true)
    fun findAllByTenantId(tenantId: Long): List<SectorResponse> {
        return sectorRepository.findByTenantId(tenantId).map { it.toResponse() }
    }

    @Transactional(readOnly = true)
    fun findAllByGreenhouseId(greenhouseId: Long): List<SectorResponse> {
        return sectorRepository.findByGreenhouseId(greenhouseId).map { it.toResponse() }
    }

    @Transactional(readOnly = true)
    fun findByIdAndTenantId(id: Long, tenantId: Long): SectorResponse? {
        val sector = sectorRepository.findById(id).orElse(null) ?: return null
        if (sector.tenantId != tenantId) return null
        return sector.toResponse()
    }

    /**
     * Crea un nuevo sector asociado a un invernadero.
     * El codigo (code) se genera automaticamente en el backend.
     */
    @Transactional
    fun create(tenantId: Long, request: SectorCreateRequest): SectorResponse {
        val greenhouse = greenhouseRepository.findById(request.greenhouseId).orElse(null)
            ?: throw IllegalArgumentException("Invernadero no encontrado")

        if (greenhouse.tenantId != tenantId) {
            throw IllegalArgumentException("El invernadero no pertenece al cliente especificado")
        }

        val sector = Sector(
            code = codeGeneratorService.generateSectorCode(),
            tenantId = tenantId,
            greenhouseId = request.greenhouseId,
            name = request.name
        )
        return sectorRepository.save(sector).toResponse()
    }

    /**
     * Actualiza un sector existente.
     * Si se proporciona un nuevo greenhouseId, valida que el invernadero pertenezca al mismo tenant.
     */
    @Transactional
    fun update(id: Long, tenantId: Long, request: SectorUpdateRequest): SectorResponse? {
        val sector = sectorRepository.findById(id).orElse(null) ?: return null
        if (sector.tenantId != tenantId) return null

        // Validar y obtener el nuevo greenhouseId si se proporciona
        val newGreenhouseId = if (request.greenhouseId != null && request.greenhouseId != sector.greenhouseId) {
            val newGreenhouse = greenhouseRepository.findById(request.greenhouseId).orElse(null)
                ?: throw IllegalArgumentException("Invernadero no encontrado con ID: ${request.greenhouseId}")

            if (newGreenhouse.tenantId != tenantId) {
                throw IllegalArgumentException("El invernadero con ID ${request.greenhouseId} no pertenece al cliente especificado")
            }
            request.greenhouseId
        } else {
            sector.greenhouseId
        }

        sector.greenhouseId = newGreenhouseId
        request.name?.let { sector.name = it }

        return sectorRepository.save(sector).toResponse()
    }

    @Transactional
    fun delete(id: Long, tenantId: Long): Boolean {
        val sector = sectorRepository.findById(id).orElse(null) ?: return false
        if (sector.tenantId != tenantId) return false

        sectorRepository.delete(sector)
        return true
    }
}
