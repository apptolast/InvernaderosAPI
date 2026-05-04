package com.apptolast.invernaderos.features.notification.infrastructure.config

import net.javacrumbs.shedlock.core.LockProvider
import net.javacrumbs.shedlock.provider.jdbctemplate.JdbcTemplateLockProvider
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.jdbc.core.JdbcTemplate

@Configuration
class ShedLockConfig {

    @Bean
    fun lockProvider(@Qualifier("metadataJdbcTemplate") jdbcTemplate: JdbcTemplate): LockProvider =
        JdbcTemplateLockProvider(
            JdbcTemplateLockProvider.Configuration.builder()
                .withJdbcTemplate(jdbcTemplate)
                .withTableName("metadata.shedlock")
                .usingDbTime()
                .build()
        )
}
