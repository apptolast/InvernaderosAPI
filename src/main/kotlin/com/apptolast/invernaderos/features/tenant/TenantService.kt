package com.apptolast.invernaderos.features.tenant

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.util.UUID

@Service
class TenantService(
    private val tenantRepository: TenantRepository
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

    fun findById(id: UUID): TenantResponse? {
        return tenantRepository.findById(id).map { it.toResponse() }.orElse(null)
    }

    @Transactional
    fun create(request: TenantCreateRequest): TenantResponse {
        val tenant = Tenant(
            name = request.name,
            email = request.email,
            phone = request.phone,
            province = request.province,
            country = request.country,
            isActive = when (request.status) {
                "Activo" -> true
                "Inactivo" -> false
                else -> null // Pendiente o nulo
            }
        )
        return tenantRepository.save(tenant).toResponse()
    }

    @Transactional
    fun update(id: UUID, request: TenantUpdateRequest): TenantResponse? {
        val tenant = tenantRepository.findById(id).orElse(null) ?: return null
        
        request.name?.let { tenant.name = it }
        request.email?.let { tenant.email = it }
        request.phone?.let { tenant.phone = it }
        request.province?.let { tenant.province = it }
        request.country?.let { tenant.country = it }
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
    fun delete(id: UUID): Boolean {
        if (tenantRepository.existsById(id)) {
            tenantRepository.deleteById(id)
            return true
        }
        return false
    }
}
