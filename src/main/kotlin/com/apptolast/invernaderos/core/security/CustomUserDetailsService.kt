package com.apptolast.invernaderos.core.security

import com.apptolast.invernaderos.features.user.UserRepository
import org.springframework.security.core.userdetails.User
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.stereotype.Service

@Service
class CustomUserDetailsService(private val userRepository: UserRepository) : UserDetailsService {

    override fun loadUserByUsername(username: String): UserDetails {
        // Buscamos por email o username
        val user =
                userRepository.findByEmail(username)
                        ?: userRepository.findByUsername(username)
                                ?: throw UsernameNotFoundException(
                                "User not found with username or email: $username"
                        )

        if (!user.isActive) {
            throw UsernameNotFoundException("User is not active: $username")
        }

        return User.builder()
                .username(user.email) // Usamos email como principal username en Spring Security
                .password(user.passwordHash)
                .roles(user.role) // "ADMIN", "USER", etc.
                .build()
    }
}
