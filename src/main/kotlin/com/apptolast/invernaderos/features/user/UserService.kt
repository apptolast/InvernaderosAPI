package com.apptolast.invernaderos.features.user

import com.apptolast.invernaderos.config.CodeGeneratorService
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
        private val passwordEncoder: PasswordEncoder,
        private val codeGeneratorService: CodeGeneratorService
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

    fun findAllByTenantId(tenantId: Long): List<UserResponse> {
        return userRepository.findByTenantId(tenantId).map { it.toResponse() }
    }

    fun findByIdAndTenantId(id: Long, tenantId: Long): UserResponse? {
        return userRepository.findByIdAndTenantId(id, tenantId)?.toResponse()
    }

    @Transactional
    fun createUser(tenantId: Long, request: UserCreateRequest): UserResponse {
        validateRole(request.role)
        if (userRepository.existsByUsername(request.username)) {
            throw IllegalArgumentException("Username already exists: ${request.username}")
        }
        if (userRepository.existsByEmail(request.email)) {
            throw IllegalArgumentException("Email already exists: ${request.email}")
        }

        val user = User(
            code = codeGeneratorService.generateUserCode(),
            tenantId = tenantId,
            username = request.username,
            email = request.email,
            passwordHash = passwordEncoder.encode(request.passwordRaw),
            role = request.role.uppercase(),
            isActive = request.isActive ?: true
        )

        return userRepository.save(user).toResponse()
    }

    @Transactional
    fun updateUser(id: Long, tenantId: Long, request: UserUpdateRequest): UserResponse? {
        val user = userRepository.findByIdAndTenantId(id, tenantId) ?: return null

        request.username?.let {
            if (it != user.username && userRepository.existsByUsername(it)) {
                throw IllegalArgumentException("Username already exists: $it")
            }
            user.username = it
        }
        request.email?.let {
            if (it != user.email && userRepository.existsByEmail(it)) {
                throw IllegalArgumentException("Email already exists: $it")
            }
            user.email = it
        }
        request.passwordRaw?.let {
            user.passwordHash = passwordEncoder.encode(it)
        }
        request.role?.let {
            validateRole(it)
            user.role = it.uppercase()
        }
        request.isActive?.let {
            user.isActive = it
        }
        user.updatedAt = java.time.Instant.now()

        return userRepository.save(user).toResponse()
    }

    private fun validateRole(role: String) {
        val allowedRoles = listOf("ADMIN", "OPERATOR", "VIEWER")
        if (!allowedRoles.contains(role.uppercase())) {
            throw IllegalArgumentException("Invalid role: $role. Allowed roles are: $allowedRoles")
        }
    }

    @Transactional
    fun deleteUser(id: Long, tenantId: Long): Boolean {
        val user = userRepository.findByIdAndTenantId(id, tenantId) ?: return false
        userRepository.delete(user)
        return true
    }

        /**
         * Crea un nuevo tenant con su usuario administrador.
         *
         * @param tenantName Nombre único del tenant (usado como identificador MQTT)
         * @param email Email del tenant y del usuario admin
         * @param passwordRaw Contraseña sin encriptar del admin
         * @param firstName Nombre del usuario admin
         * @param lastName Apellido del usuario admin
         * @param phone Teléfono de contacto (opcional)
         * @param province Provincia (opcional)
         * @param country País (default: España)
         * @return UserResponse del usuario admin creado
         */
        @Transactional
        fun createTenantAndAdminUser(
                tenantName: String,
                email: String,
                passwordRaw: String,
                firstName: String,
                lastName: String,
                phone: String? = null,
                province: String? = null,
                country: String? = "España"
        ): UserResponse {
                // 1. Crear Tenant con campos simplificados
                val tenant =
                        Tenant(
                                code = codeGeneratorService.generateTenantCode(),
                                name = tenantName,
                                email = email,
                                phone = phone,
                                province = province,
                                country = country,
                                isActive = true
                        )
                val savedTenant = tenantRepository.save(tenant)

                // 2. Crear usuario Admin asociado al tenant
                val user =
                        User(
                                code = codeGeneratorService.generateUserCode(),
                                tenantId = savedTenant.id!!,
                                username = email,
                                email = email,
                                passwordHash = passwordEncoder.encode(passwordRaw),
                                role = "ADMIN",
                                isActive = true
                        )

                val savedUser = userRepository.save(user)
                return savedUser.toResponse()
        }

        @Transactional
        fun generatePasswordResetToken(email: String): String {
                val user =
                        userRepository.findByEmail(email)
                                ?: throw RuntimeException("User not found with email: $email")

                val token = UUID.randomUUID().toString()
                val expiry = java.time.Instant.now().plusSeconds(900) // 15 minutes

                val updatedUser =
                        user.copy(resetPasswordToken = token, resetPasswordTokenExpiry = expiry)
                userRepository.save(updatedUser)

                return token
        }

        @Transactional
        fun resetPassword(token: String, newPasswordRaw: String) {
                val user =
                        userRepository.findByResetPasswordToken(token)
                                ?: throw RuntimeException("Invalid reset token")

                val expiry = user.resetPasswordTokenExpiry
                if (expiry == null || expiry.isBefore(java.time.Instant.now())) {
                        throw RuntimeException("Reset token has expired")
                }

                val updatedUser =
                        user.copy(
                                passwordHash = passwordEncoder.encode(newPasswordRaw),
                                resetPasswordToken = null,
                                resetPasswordTokenExpiry = null
                        )
                userRepository.save(updatedUser)
        }
}
