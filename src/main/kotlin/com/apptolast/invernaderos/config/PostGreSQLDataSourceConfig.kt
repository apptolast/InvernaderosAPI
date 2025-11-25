package com.apptolast.invernaderos.config

import com.zaxxer.hikari.HikariDataSource
import jakarta.persistence.EntityManagerFactory
import javax.sql.DataSource
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.jpa.repository.config.EnableJpaRepositories
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.orm.jpa.JpaTransactionManager
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.annotation.EnableTransactionManagement

@Configuration
@EnableTransactionManagement
@EnableJpaRepositories(
        basePackages =
                [
                        "com.apptolast.invernaderos.features.greenhouse",
                        "com.apptolast.invernaderos.features.tenant",
                        "com.apptolast.invernaderos.features.actuator",
                        "com.apptolast.invernaderos.features.alert",
                        "com.apptolast.invernaderos.features.mqtt",
                        "com.apptolast.invernaderos.features.audit",
                        "com.apptolast.invernaderos.features.sensor",
                        "com.apptolast.invernaderos.features.catalog",
                        "com.apptolast.invernaderos.features.user",
                        "com.apptolast.invernaderos.features.simulation",
                        "com.apptolast.invernaderos.features.statistics"],
        entityManagerFactoryRef = "metadataEntityManagerFactory",
        transactionManagerRef = "metadataTransactionManager"
)
class PostGreSQLDataSourceConfig {

    @Bean(name = ["metadataDataSourceProperties"], defaultCandidate = false)
    @ConfigurationProperties("spring.datasource-metadata")
    fun metadataDataSourceProperties(): DataSourceProperties {
        return DataSourceProperties()
    }

    @Bean(name = ["metadataDataSource"], defaultCandidate = false)
    @ConfigurationProperties("spring.datasource-metadata.configuration")
    fun metadataDataSource(
            @Qualifier("metadataDataSourceProperties") properties: DataSourceProperties
    ): HikariDataSource {
        return properties.initializeDataSourceBuilder().type(HikariDataSource::class.java).build()
    }

    @Bean(name = ["metadataEntityManagerFactory"], defaultCandidate = false)
    fun metadataEntityManagerFactory(
            @Qualifier("metadataDataSource") dataSource: DataSource
    ): LocalContainerEntityManagerFactoryBean {
        val entityManager = LocalContainerEntityManagerFactoryBean()
        entityManager.dataSource = dataSource
        entityManager.setPackagesToScan(
                "com.apptolast.invernaderos.features.greenhouse",
                "com.apptolast.invernaderos.features.tenant",
                "com.apptolast.invernaderos.features.actuator",
                "com.apptolast.invernaderos.features.alert",
                "com.apptolast.invernaderos.features.mqtt",
                "com.apptolast.invernaderos.features.audit",
                "com.apptolast.invernaderos.features.sensor",
                "com.apptolast.invernaderos.features.catalog",
                "com.apptolast.invernaderos.features.user",
                "com.apptolast.invernaderos.features.simulation",
                "com.apptolast.invernaderos.features.statistics"
        )
        entityManager.persistenceUnitName = "metadataPersistenceUnit"

        val vendorAdapter = HibernateJpaVendorAdapter()
        vendorAdapter.setGenerateDdl(false)
        entityManager.jpaVendorAdapter = vendorAdapter

        val properties =
                hashMapOf<String, Any>(
                        "hibernate.dialect" to "org.hibernate.dialect.PostgreSQLDialect",
                        "hibernate.hbm2ddl.auto" to "validate",
                        "hibernate.show_sql" to "false",
                        "hibernate.format_sql" to "true"
                )
        entityManager.setJpaPropertyMap(properties)

        return entityManager
    }

    /**
     * Transaction manager for metadata datasource. Bean name includes both
     * "metadataTransactionManager" (recommended) and "postgreSQLTransactionManager" (legacy alias
     * for backward compatibility).
     */
    @Bean(
            name = ["metadataTransactionManager", "postgreSQLTransactionManager"],
            defaultCandidate = false
    )
    fun metadataTransactionManager(
            @Qualifier("metadataEntityManagerFactory") entityManagerFactory: EntityManagerFactory
    ): PlatformTransactionManager {
        return JpaTransactionManager(entityManagerFactory)
    }

    /**
     * JdbcTemplate bean for PostgreSQL metadata datasource. Used by repositories that require
     * native SQL queries on metadata database.
     */
    @Bean(name = ["metadataJdbcTemplate"], defaultCandidate = false)
    fun metadataJdbcTemplate(
            @Qualifier("metadataDataSource") dataSource: DataSource
    ): JdbcTemplate {
        return JdbcTemplate(dataSource)
    }
}
