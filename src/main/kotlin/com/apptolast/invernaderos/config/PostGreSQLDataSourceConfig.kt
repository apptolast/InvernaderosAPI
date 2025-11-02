package com.apptolast.invernaderos.config

import com.zaxxer.hikari.HikariDataSource
import jakarta.persistence.EntityManagerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.jpa.repository.config.EnableJpaRepositories
import org.springframework.orm.jpa.JpaTransactionManager
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.annotation.EnableTransactionManagement
import javax.sql.DataSource

@Configuration
@EnableTransactionManagement
@EnableJpaRepositories(
    basePackages = ["com.apptolast.invernaderos.metadata.repository"],
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
        return properties.initializeDataSourceBuilder()
            .type(HikariDataSource::class.java)
            .build()
    }

    @Bean(name = ["metadataEntityManagerFactory"], defaultCandidate = false)
    fun metadataEntityManagerFactory(
        @Qualifier("metadataDataSource") dataSource: DataSource
    ): LocalContainerEntityManagerFactoryBean {
        val entityManager = LocalContainerEntityManagerFactoryBean()
        entityManager.dataSource = dataSource
        entityManager.setPackagesToScan("com.apptolast.invernaderos.metadata.entity")
        entityManager.persistenceUnitName = "metadataPersistenceUnit"

        val vendorAdapter = HibernateJpaVendorAdapter()
        vendorAdapter.setGenerateDdl(false)
        entityManager.jpaVendorAdapter = vendorAdapter

        val properties = hashMapOf<String, Any>(
            "hibernate.dialect" to "org.hibernate.dialect.PostgreSQLDialect",
            "hibernate.hbm2ddl.auto" to "validate",
            "hibernate.show_sql" to "false",
            "hibernate.format_sql" to "true"
        )
        entityManager.setJpaPropertyMap(properties)

        return entityManager
    }

    @Bean(name = ["metadataTransactionManager"], defaultCandidate = false)
    fun metadataTransactionManager(
        @Qualifier("metadataEntityManagerFactory") entityManagerFactory: EntityManagerFactory
    ): PlatformTransactionManager {
        return JpaTransactionManager(entityManagerFactory)
    }
}