package com.apptolast.invernaderos.config

import com.zaxxer.hikari.HikariDataSource
import jakarta.persistence.EntityManagerFactory
import javax.sql.DataSource
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
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
        basePackages = ["com.apptolast.invernaderos.features.telemetry.timeseries"],
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
                return properties
                        .initializeDataSourceBuilder()
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
                entityManager.setPackagesToScan(
                        "com.apptolast.invernaderos.features.telemetry.timescaledb.entities"
                )
                entityManager.persistenceUnitName = "timescalePersistenceUnit"

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

        @Primary
        @Bean(name = ["timescaleTransactionManager"])
        fun timescaleTransactionManager(
                @Qualifier("timescaleEntityManagerFactory")
                entityManagerFactory: EntityManagerFactory
        ): PlatformTransactionManager {
                return JpaTransactionManager(entityManagerFactory)
        }

        /**
         * JdbcTemplate bean for TimescaleDB datasource. Used by StatisticsJdbcDao and other
         * repositories that require native SQL queries.
         */
        @Primary
        @Bean(name = ["timescaleJdbcTemplate"])
        fun timescaleJdbcTemplate(
                @Qualifier("timescaleDataSource") dataSource: DataSource
        ): JdbcTemplate {
                return JdbcTemplate(dataSource)
        }

        @Bean(name = ["statsDaoBean"])
        fun statsDaoBean(
                @Qualifier("timescaleJdbcTemplate") jdbcTemplate: JdbcTemplate
        ): com.apptolast.invernaderos.features.statistics.dao.StatsDao {
                return com.apptolast.invernaderos.features.statistics.dao.StatsDao(jdbcTemplate)
        }
}
