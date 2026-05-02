package com.apptolast.invernaderos.config

import org.flywaydb.core.Flyway
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.flyway.FlywayMigrationStrategy
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import javax.sql.DataSource

/**
 * Configuración dual de Flyway para gestionar migraciones en ambas bases de datos:
 *
 * - **Metadata PostgreSQL** (puerto 30433): Schema 'metadata', migraciones en db/migration/
 * - **TimescaleDB** (puerto 30432): Schema 'iot', migraciones en db/migration-timescaledb/
 *
 * ESTRATEGIA POR ENTORNO:
 * - DEV:  Auto-migrate habilitado (rápido desarrollo)
 * - PROD: Solo VALIDATE (las migraciones se ejecutan via CI/CD ANTES del deploy)
 *
 * REGLAS DE ORO:
 * 1. NUNCA modificar una migración ya aplicada (checksum mismatch)
 * 2. NUNCA eliminar una migración ya aplicada
 * 3. Una migración = un cambio atómico
 * 4. Las migraciones son INMUTABLES una vez committeadas
 */
@Configuration
class FlywayConfig {

    private val logger = LoggerFactory.getLogger(FlywayConfig::class.java)

    @Value("\${flyway.auto-migrate:true}")
    private var autoMigrate: Boolean = true

    /**
     * Master switch to skip Flyway entirely. Used by tests so that Testcontainers
     * databases (which start empty) rely on Hibernate `ddl-auto: create-drop`
     * rather than running real migrations against an ephemeral container.
     * Defaults to true in dev/prod where Flyway is the source of truth.
     */
    @Value("\${flyway.enabled:true}")
    private var flywayEnabled: Boolean = true

    /**
     * Flyway principal para PostgreSQL Metadata (schema 'metadata').
     * Migraciones en: classpath:db/migration/
     */
    @Bean
    fun flyway(@Qualifier("metadataDataSource") metadataDataSource: DataSource): Flyway {
        return Flyway.configure()
            .dataSource(metadataDataSource)
            .locations("classpath:db/migration")
            .schemas("metadata")
            .baselineOnMigrate(true)
            .baselineVersion("1")
            .validateOnMigrate(true)
            .outOfOrder(false)
            .cleanDisabled(true)
            .table("flyway_schema_history")
            .load()
    }

    /**
     * Estrategia de migración que ejecuta Flyway en ambas bases de datos.
     *
     * Spring Boot llama esta estrategia con el bean 'flyway' (metadata).
     * Adicionalmente ejecutamos Flyway contra TimescaleDB.
     */
    @Bean
    fun flywayMigrationStrategy(
        @Qualifier("timescaleDataSource") timescaleDataSource: DataSource
    ): FlywayMigrationStrategy {
        return FlywayMigrationStrategy { metadataFlyway ->
            if (!flywayEnabled) {
                logger.info("Flyway DESACTIVADO via flyway.enabled=false (test profile) - skipping migrations.")
                return@FlywayMigrationStrategy
            }

            val timescaleFlyway = Flyway.configure()
                .dataSource(timescaleDataSource)
                .locations("classpath:db/migration-timescaledb")
                .schemas("iot")
                .baselineOnMigrate(true)
                .baselineVersion("31")
                .validateOnMigrate(true)
                .outOfOrder(false)
                .cleanDisabled(true)
                .table("flyway_schema_history")
                .load()

            if (autoMigrate) {
                logger.info("Flyway AUTO-MIGRATE habilitado - Ejecutando migraciones en ambas bases de datos...")
                runMigrate("metadata", metadataFlyway)
                runMigrate("timescaledb", timescaleFlyway)
            } else {
                logger.info("Flyway AUTO-MIGRATE deshabilitado (PROD mode) - Solo validando...")
                runValidate("metadata", metadataFlyway)
                runValidate("timescaledb", timescaleFlyway)
            }
        }
    }

    private fun runMigrate(name: String, flyway: Flyway) {
        try {
            val result = flyway.migrate()
            logger.info("Flyway [$name] completado: ${result.migrationsExecuted} migraciones ejecutadas")
        } catch (e: Exception) {
            logger.error("Flyway [$name] migración falló: ${e.message}", e)
            throw e
        }
    }

    private fun runValidate(name: String, flyway: Flyway) {
        try {
            flyway.validate()
            logger.info("Flyway [$name] validación completada")
        } catch (e: Exception) {
            logger.error("Flyway [$name] validación falló: ${e.message}", e)
            throw e
        }
    }
}
