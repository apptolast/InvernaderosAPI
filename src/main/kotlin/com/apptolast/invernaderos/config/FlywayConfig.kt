package com.apptolast.invernaderos.config

import org.flywaydb.core.Flyway
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.autoconfigure.flyway.FlywayMigrationStrategy
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import javax.sql.DataSource

/**
 * Configuración custom de Flyway para usar explícitamente el datasource de metadata
 *
 * PROBLEMA: TimescaleDB datasource está marcado como @Primary, causando que
 * Flyway auto-configuration lo use por defecto. Esta configuración fuerza
 * Flyway a usar metadataDataSource.
 *
 * Las migraciones SQL (V3-V14) están en: src/main/resources/db/migration/
 * Solo aplican al schema 'metadata' en PostgreSQL (puerto 30433)
 */
@Configuration
class FlywayConfig {

    /**
     * Configura Flyway para usar SOLO el datasource de metadata
     *
     * @param metadataDataSource El datasource de PostgreSQL metadata (puerto 30433)
     * @return Instancia de Flyway configurada
     */
    @Bean(initMethod = "migrate")
    fun flyway(@Qualifier("metadataDataSource") metadataDataSource: DataSource): Flyway {
        return Flyway.configure()
            .dataSource(metadataDataSource)
            .locations("classpath:db/migration")
            .schemas("metadata")
            .baselineOnMigrate(true)  // Necesario para bases de datos existentes con datos
            .baselineVersion("1")
            .validateOnMigrate(true)
            .outOfOrder(false)
            .table("flyway_schema_history")  // Tabla de control de Flyway
            .load()
    }

    /**
     * Estrategia de migración: ejecutar migraciones automáticamente al iniciar
     */
    @Bean
    fun flywayMigrationStrategy(): FlywayMigrationStrategy {
        return FlywayMigrationStrategy { flyway ->
            // Migrar automáticamente
            flyway.migrate()
        }
    }
}
