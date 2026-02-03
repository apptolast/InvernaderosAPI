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
 * Configuración custom de Flyway para usar explícitamente el datasource de metadata.
 *
 * ESTRATEGIA POR ENTORNO (según best practices oficiales de Flyway):
 * - DEV:  Auto-migrate habilitado (rápido desarrollo)
 * - PROD: Solo VALIDATE (las migraciones se ejecutan via CI/CD ANTES del deploy)
 *
 * Documentación oficial:
 * - https://documentation.red-gate.com/fd/recommended-practices-150700352.html
 * - https://documentation.red-gate.com/fd/flyway-development-and-deployment-pipelines-180715693.html
 *
 * Las migraciones SQL están en: src/main/resources/db/migration/
 * Solo aplican al schema 'metadata' en PostgreSQL (puerto 30433)
 *
 * REGLAS DE ORO:
 * 1. NUNCA modificar una migración ya aplicada (checksum mismatch)
 * 2. NUNCA eliminar una migración ya aplicada
 * 3. Una migración = un cambio atómico
 * 4. Las migraciones son INMUTABLES una vez committeadas
 * 5. Siempre testear en staging antes de PROD
 */
@Configuration
class FlywayConfig {

    private val logger = LoggerFactory.getLogger(FlywayConfig::class.java)

    /**
     * Controla si Flyway ejecuta migraciones automáticamente al arrancar.
     *
     * - true (default para DEV): Ejecuta migrate() al iniciar
     * - false (recomendado para PROD): Solo valida, NO migra
     *
     * Configurar en application.yaml o variables de entorno:
     *   flyway.auto-migrate: false  # Para PROD
     */
    @Value("\${flyway.auto-migrate:true}")
    private var autoMigrate: Boolean = true

    /**
     * Configura Flyway para usar SOLO el datasource de metadata.
     * NO ejecuta migrate automáticamente - eso lo controla FlywayMigrationStrategy.
     *
     * @param metadataDataSource El datasource de PostgreSQL metadata (puerto 30433)
     * @return Instancia de Flyway configurada
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
            .cleanDisabled(true)  // CRÍTICO: Previene flyway.clean() accidental en PROD
            .table("flyway_schema_history")
            .load()
    }

    /**
     * Estrategia de migración controlada por entorno.
     *
     * - autoMigrate=true (DEV):  Ejecuta migrate() automáticamente
     * - autoMigrate=false (PROD): Solo ejecuta validate(), NO migra
     *
     * En PROD, las migraciones deben ejecutarse via CI/CD ANTES del deploy
     * usando: flyway -url=... -user=... migrate
     */
    @Bean
    fun flywayMigrationStrategy(): FlywayMigrationStrategy {
        return FlywayMigrationStrategy { flyway ->
            if (autoMigrate) {
                logger.info("🔄 Flyway AUTO-MIGRATE habilitado - Ejecutando migraciones...")
                val result = flyway.migrate()
                logger.info("✅ Flyway migración completada: ${result.migrationsExecuted} migraciones ejecutadas")
            } else {
                logger.info("🔒 Flyway AUTO-MIGRATE deshabilitado (PROD mode) - Solo validando...")
                flyway.validate()
                logger.info("✅ Flyway validación completada - Schema está sincronizado")
            }
        }
    }
}
