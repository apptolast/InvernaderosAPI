package com.apptolast.invernaderos.core.config

import java.util.Optional
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.domain.AuditorAware
import org.springframework.data.jpa.repository.config.EnableJpaAuditing
import org.springframework.security.core.context.SecurityContextHolder

@Configuration
@EnableJpaAuditing(auditorAwareRef = "auditorProvider")
class JpaAuditingConfig {

    @Bean
    fun auditorProvider(): AuditorAware<String> {
        return AuditorAwareImpl()
    }

    class AuditorAwareImpl : AuditorAware<String> {
        override fun getCurrentAuditor(): Optional<String> {
            val authentication = SecurityContextHolder.getContext().authentication
            if (authentication == null ||
                            !authentication.isAuthenticated ||
                            authentication.principal == "anonymousUser"
            ) {
                return Optional.of("SYSTEM")
            }
            return Optional.of(authentication.name)
        }
    }
}
