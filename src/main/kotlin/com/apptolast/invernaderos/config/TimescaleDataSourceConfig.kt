package com.apptolast.invernaderos.config

import com.zaxxer.hikari.HikariDataSource
import jakarta.persistence.EntityManagerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.orm.jpa.EntityManagerFactoryBuilder
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
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
    basePackages = ["com.apptolast.invernaderos.timeseries.repository"],
    entityManagerFactoryRef = "timescaleEntityManagerFactory",
    transactionManagerRef = "timescaleTransactionManager"
)
class TimescaleDataSourceConfig {

    @Primary
    @Bean(name = ["timescaleDataSourceProperties"])
    @ConfigurationProperties("spring.datasource")
    fun timescaleDataSourceProperties(): DataSourceProperties {
        return DataSourceProperties()
    }

    @Primary
    @Bean(name = ["timescaleDataSource"])
    @ConfigurationProperties("spring.datasource.configuration")
    fun timescaleDataSource(
        @Qualifier("timescaleDataSourceProperties") properties: DataSourceProperties
    ): HikariDataSource {
        return properties.initializeDataSourceBuilder()
            .type(HikariDataSource::class.java)
            .build()
    }

    @Primary
    @Bean(name = ["timescaleEntityManagerFactory"])
    fun timescaleEntityManagerFactory(
        @Qualifier("timescaleDataSource") dataSource: DataSource
    ): LocalContainerEntityManagerFactoryBean {
        val entityManager = LocalContainerEntityManagerFactoryBean()
        entityManager.dataSource = dataSource
        entityManager.setPackagesToScan("com.apptolast.invernaderos.timeseries.entity")
        entityManager.persistenceUnitName = "timescalePersistenceUnit"

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

    @Primary
    @Bean(name = ["timescaleTransactionManager"])
    fun timescaleTransactionManager(
        @Qualifier("timescaleEntityManagerFactory") entityManagerFactory: EntityManagerFactory
    ): PlatformTransactionManager {
        return JpaTransactionManager(entityManagerFactory)
    }
}