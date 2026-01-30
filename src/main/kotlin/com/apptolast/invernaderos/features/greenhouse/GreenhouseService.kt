package com.apptolast.invernaderos.features.greenhouse

import com.apptolast.invernaderos.config.CodeGeneratorService
import com.apptolast.invernaderos.features.greenhouse.dto.GreenhouseCreateRequest
import com.apptolast.invernaderos.features.greenhouse.dto.GreenhouseResponse
import com.apptolast.invernaderos.features.greenhouse.dto.GreenhouseUpdateRequest
import com.apptolast.invernaderos.features.greenhouse.dto.toResponse
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

@Service
class GreenhouseService(
    private val greenhouseRepository: GreenhouseRepository,
    private val codeGeneratorService: CodeGeneratorService
) {

    fun findAllByTenantId(tenantId: Long): List<GreenhouseResponse> {
        return greenhouseRepository.findByTenantId(tenantId).map { it.toResponse() }
    }

    fun findByIdAndTenantId(id: Long, tenantId: Long): GreenhouseResponse? {
        return greenhouseRepository.findByIdAndTenantId(id, tenantId)?.toResponse()
    }

    @Transactional
    fun create(tenantId: Long, request: GreenhouseCreateRequest): GreenhouseResponse {
        // Validar unicidad de nombre por tenant
        if (greenhouseRepository.findByTenantIdAndName(tenantId, request.name) != null) {
            throw IllegalArgumentException("Un invernadero con el nombre '${request.name}' ya existe para este cliente.")
        }

        val greenhouse = Greenhouse(
            code = codeGeneratorService.generateGreenhouseCode(),
            tenantId = tenantId,
            name = request.name,
            location = request.location,
            areaM2 = request.areaM2,
            timezone = request.timezone ?: "Europe/Madrid",
            isActive = request.isActive ?: true
        )

        return greenhouseRepository.save(greenhouse).toResponse()
    }

    @Transactional
    fun update(id: Long, tenantId: Long, request: GreenhouseUpdateRequest): GreenhouseResponse? {
        val greenhouse = greenhouseRepository.findByIdAndTenantId(id, tenantId) ?: return null

        request.name?.let {
            if (it != greenhouse.name && greenhouseRepository.findByTenantIdAndName(tenantId, it) != null) {
                throw IllegalArgumentException("Un invernadero con el nombre '$it' ya existe para este cliente.")
            }
            greenhouse.name = it
        }

        request.location?.let { greenhouse.location = it }
        request.areaM2?.let { greenhouse.areaM2 = it }
        request.timezone?.let { greenhouse.timezone = it }
        request.isActive?.let { greenhouse.isActive = it }

        greenhouse.updatedAt = Instant.now()

        return greenhouseRepository.save(greenhouse).toResponse()
    }

    @Transactional
    fun delete(id: Long, tenantId: Long): Boolean {
        val greenhouse = greenhouseRepository.findByIdAndTenantId(id, tenantId) ?: return false
        greenhouseRepository.delete(greenhouse)
        return true
    }
}
