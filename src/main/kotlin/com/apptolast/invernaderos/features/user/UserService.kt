package com.apptolast.invernaderos.features.user

import com.apptolast.invernaderos.features.tenant.Tenant
import com.apptolast.invernaderos.features.tenant.TenantRepository
import java.util.UUID
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class UserService(
        private val userRepository: UserRepository,
        private val tenantRepository: TenantRepository,
        private val passwordEncoder: PasswordEncoder
) {

    fun findByEmail(email: String): User? {
        return userRepository.findByEmail(email)
    }

    fun existsByEmail(email: String): Boolean {
        return userRepository.existsByEmail(email)
    }

    fun getAllUsers(): List<UserResponse> {
        return userRepository.findAll().map { it.toResponse() }
    }

    @Transactional
    fun createTenantAndAdminUser(
            companyName: String,
            taxId: String,
            email: String,
            passwordRaw: String,
            firstName: String,
            lastName: String,
            phone: String?,
            address: String?
    ): UserResponse {
        // 1. Create Tenant
        val tenant =
                Tenant(
                        name = "$firstName $lastName", // Nombre del tenant (persona/empresa)
                        email = email,
                        companyName = companyName,
                        taxId = taxId,
                        contactPerson = "$firstName $lastName",
                        contactEmail = email,
                        contactPhone = phone,
                        address = address,
                        isActive = true,
                        // Generamos un prefijo MQTT único basado en el nombre de la empresa o un
                        // UUID corto
                        mqttTopicPrefix = generateMqttPrefix(companyName)
                )
        val savedTenant = tenantRepository.save(tenant)

        // 2. Create Admin User
        val user =
                User(
                        tenantId = savedTenant.id!!,
                        username = email, // Usamos email como username
                        email = email,
                        passwordHash = passwordEncoder.encode(passwordRaw),
                        role = "ADMIN",
                        isActive = true
                )
        // Asignamos la relación bidireccional si fuera necesario, pero User tiene tenantId
        // y una relación @ManyToOne con insertable=false, updatable=false.
        // Para que JPA lo asocie correctamente en memoria si se usa, podemos setearlo,
        // pero lo importante es el tenantId en el constructor.

        val savedUser = userRepository.save(user)
        return savedUser.toResponse()
    }

    private fun generateMqttPrefix(companyName: String): String {
        // Simplificación: Tomar las primeras letras, quitar espacios y añadir algo random si hace
        // falta
        // En producción, verificar unicidad.
        val base = companyName.uppercase().replace(Regex("[^A-Z0-9]"), "")
        val prefix = if (base.length > 10) base.substring(0, 10) else base
        return "${prefix}_${UUID.randomUUID().toString().substring(0, 4).uppercase()}"
    }
}
