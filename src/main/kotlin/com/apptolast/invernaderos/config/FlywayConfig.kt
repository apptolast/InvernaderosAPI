package com.apptolast.invernaderos.config

import jakarta.annotation.PostConstruct
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
class FlywayConfig(
    @Qualifier("metadataDataSource") private val metadataDataSource: DataSource,
    @Qualifier("timescaleDataSource") private val timescaleDataSource: DataSource,
    @Value("\${flyway.auto-migrate:true}") private val autoMigrate: Boolean
) {

    private val logger = LoggerFactory.getLogger(FlywayConfig::class.java)

    /**
     * Flyway para PostgreSQL Metadata (schema 'metadata').
     * Migraciones en: classpath:db/migration/
     */
    @Bean
    fun flyway(): Flyway {
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
     * Flyway para TimescaleDB (schema 'iot').
     * Migraciones en: classpath:db/migration-timescaledb/
     *
     * Baseline en V31 porque las migraciones anteriores (V2, V8, V11)
     * se aplicaron manualmente sobre TimescaleDB.
     */
    @Bean
    fun flywayTimescaledb(): Flyway {
        return Flyway.configure()
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
    }

    /**
     * Estrategia que evita que Spring Boot ejecute automáticamente el Flyway principal.
     * La ejecución real se hace en @PostConstruct para controlar ambos Flyway.
     */
    @Bean
    fun flywayMigrationStrategy(): FlywayMigrationStrategy {
        return FlywayMigrationStrategy {
            // No-op: la ejecución real se hace en runDualMigrations()
        }
    }

    /**
     * Ejecuta migraciones (o validación) en ambas bases de datos al arrancar.
     */
    @PostConstruct
    fun runDualMigrations() {
        if (autoMigrate) {
            logger.info("Flyway AUTO-MIGRATE habilitado - Ejecutando migraciones en ambas bases de datos...")
            runMigrate("metadata", flyway())
            runMigrate("timescaledb", flywayTimescaledb())
        } else {
            logger.info("Flyway AUTO-MIGRATE deshabilitado (PROD mode) - Solo validando...")
            runValidate("metadata", flyway())
            runValidate("timescaledb", flywayTimescaledb())
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
