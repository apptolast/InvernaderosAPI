package com.apptolast.invernaderos.features.tenant

import com.apptolast.invernaderos.config.CodeGeneratorService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

@Service
class TenantService(
    private val tenantRepository: TenantRepository,
    private val codeGeneratorService: CodeGeneratorService
) {

    fun findAll(search: String? = null, province: String? = null, isActive: Boolean? = null): List<TenantResponse> {
        val tenants = when {
            search != null -> tenantRepository.findByNameContainingIgnoreCaseOrEmailContainingIgnoreCase(search, search)
            province != null -> tenantRepository.findByProvince(province)
            isActive != null -> tenantRepository.findByIsActive(isActive)
            else -> tenantRepository.findAll()
        }
        return tenants.map { it.toResponse() }
    }

    fun findById(id: Long): TenantResponse? {
        return tenantRepository.findById(id).map { it.toResponse() }.orElse(null)
    }

    @Transactional
    fun create(request: TenantCreateRequest): TenantResponse {
        val tenant = Tenant(
            code = codeGeneratorService.generateTenantCode(),
            name = request.name,
            email = request.email,
            phone = request.phone,
            province = request.province,
            country = request.country,
            location = request.location,
            isActive = when (request.status) {
                "Activo" -> true
                "Inactivo" -> false
                else -> null // Pendiente o nulo
            }
        )
        return tenantRepository.save(tenant).toResponse()
    }

    @Transactional
    fun update(id: Long, request: TenantUpdateRequest): TenantResponse? {
        val tenant = tenantRepository.findById(id).orElse(null) ?: return null
        
        request.name?.let { tenant.name = it }
        request.email?.let { tenant.email = it }
        request.phone?.let { tenant.phone = it }
        request.province?.let { tenant.province = it }
        request.country?.let { tenant.country = it }
        request.location?.let { tenant.location = it }
        request.status?.let {
            tenant.isActive = when (it) {
                "Activo" -> true
                "Inactivo" -> false
                else -> null
            }
        }
        
        tenant.updatedAt = Instant.now()
        
        return tenantRepository.save(tenant).toResponse()
    }

    @Transactional
    fun delete(id: Long): Boolean {
        if (tenantRepository.existsById(id)) {
            tenantRepository.deleteById(id)
            return true
        }
        return false
    }
}
