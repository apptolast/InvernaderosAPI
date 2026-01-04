package com.apptolast.invernaderos.features.sector

import com.apptolast.invernaderos.features.greenhouse.GreenhouseRepository
import com.apptolast.invernaderos.features.sector.dto.SectorCreateRequest
import com.apptolast.invernaderos.features.sector.dto.SectorResponse
import com.apptolast.invernaderos.features.sector.dto.SectorUpdateRequest
import com.apptolast.invernaderos.features.sector.dto.toResponse
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class SectorService(
    private val sectorRepository: SectorRepository,
    private val greenhouseRepository: GreenhouseRepository
) {

    fun findAllByTenantId(tenantId: UUID): List<SectorResponse> {
        val greenhouseIds = greenhouseRepository.findByTenantId(tenantId).mapNotNull { it.id }
        if (greenhouseIds.isEmpty()) return emptyList()
        return sectorRepository.findByGreenhouseIdIn(greenhouseIds).map { it.toResponse() }
    }

    fun findAllByGreenhouseId(greenhouseId: UUID): List<SectorResponse> {
        return sectorRepository.findByGreenhouseId(greenhouseId).map { it.toResponse() }
    }

    fun findByIdAndTenantId(id: UUID, tenantId: UUID): SectorResponse? {
        val sector = sectorRepository.findById(id).orElse(null) ?: return null
        val greenhouse = greenhouseRepository.findById(sector.greenhouseId).orElse(null)
        if (greenhouse?.tenantId != tenantId) return null
        return sector.toResponse()
    }

    @Transactional
    fun create(tenantId: UUID, request: SectorCreateRequest): SectorResponse {
        val greenhouse = greenhouseRepository.findById(request.greenhouseId).orElse(null)
            ?: throw IllegalArgumentException("Invernadero no encontrado")
        
        if (greenhouse.tenantId != tenantId) {
            throw IllegalArgumentException("El invernadero no pertenece al cliente especificado")
        }

        val sector = Sector(
            greenhouseId = request.greenhouseId,
            variety = request.variety
        )
        return sectorRepository.save(sector).toResponse()
    }

    @Transactional
    fun update(id: UUID, tenantId: UUID, request: SectorUpdateRequest): SectorResponse? {
        val sector = sectorRepository.findById(id).orElse(null) ?: return null
        val greenhouse = greenhouseRepository.findById(sector.greenhouseId).orElse(null)
        if (greenhouse?.tenantId != tenantId) return null

        request.variety?.let { sector.variety = it }

        return sectorRepository.save(sector).toResponse()
    }

    @Transactional
    fun delete(id: UUID, tenantId: UUID): Boolean {
        val sector = sectorRepository.findById(id).orElse(null) ?: return false
        val greenhouse = greenhouseRepository.findById(sector.greenhouseId).orElse(null)
        if (greenhouse?.tenantId != tenantId) return false

        sectorRepository.delete(sector)
        return true
    }
}
