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
