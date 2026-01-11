package com.apptolast.invernaderos.config

import io.hypersistence.tsid.TSID
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Configuration
import jakarta.annotation.PostConstruct
import java.net.InetAddress
import java.util.function.Supplier
import kotlin.math.abs

/**
 * Configuracion para TSID (Time-Sorted Unique Identifiers).
 *
 * TSID genera identificadores unicos de 64 bits con las siguientes propiedades:
 * - Unicos globalmente (no se repiten entre tablas ni servidores)
 * - Ordenables por tiempo (IDs mas nuevos = numeros mas grandes)
 * - Eficientes como BIGINT en indices de PostgreSQL
 *
 * Estructura de 64 bits:
 * [42 bits timestamp][10 bits node][12 bits counter]
 *
 * Configuracion del node_id para multiples instancias (K8s):
 * - Variable de entorno: TSID_NODE_ID (0-1023)
 * - Si no existe, calcula hash del hostname % 1024
 *
 * @see <a href="https://vladmihalcea.com/tsid-identifier-jpa-hibernate/">TSID with JPA and Hibernate</a>
 */
@Configuration
class TsidConfig {

    private val logger = LoggerFactory.getLogger(TsidConfig::class.java)

    @PostConstruct
    fun logTsidConfiguration() {
        val nodeId = TsidSupplier.getNodeId()
        logger.info("TSID configured with node_id={} (max capacity: 4096 IDs/ms)", nodeId)
    }
}

/**
 * Supplier para crear instancias de TSID.Factory con configuracion de node_id.
 *
 * Esta clase es usada por la anotacion @Tsid de hypersistence-utils.
 * Se instancia via reflection, por lo que no puede usar inyeccion de dependencias.
 *
 * El node_id se determina de la siguiente manera:
 * 1. Variable de entorno TSID_NODE_ID (si existe)
 * 2. System property tsid.node.id (si existe)
 * 3. Hash del hostname % 1024 (fallback para K8s)
 *
 * Uso en entidades:
 * ```kotlin
 * @Id
 * @Tsid(TsidSupplier::class)
 * var id: Long? = null
 * ```
 */
class TsidSupplier : Supplier<TSID.Factory> {

    override fun get(): TSID.Factory {
        return TSID.Factory.builder()
            .withNode(getNodeId())
            .build()
    }

    companion object {
        private val logger = LoggerFactory.getLogger(TsidSupplier::class.java)
        private var cachedNodeId: Int? = null

        /**
         * Obtiene el node_id para esta instancia.
         * El valor se cachea para evitar calculos repetidos.
         *
         * @return node_id entre 0 y 1023
         */
        fun getNodeId(): Int {
            if (cachedNodeId != null) {
                return cachedNodeId!!
            }

            // 1. Intentar variable de entorno
            val envNodeId = System.getenv("TSID_NODE_ID")
            if (!envNodeId.isNullOrBlank()) {
                val nodeId = envNodeId.toIntOrNull()
                if (nodeId != null && nodeId in 0..1023) {
                    cachedNodeId = nodeId
                    logger.info("TSID node_id from TSID_NODE_ID env var: {}", nodeId)
                    return nodeId
                }
                logger.warn("Invalid TSID_NODE_ID value: {} (must be 0-1023), using fallback", envNodeId)
            }

            // 2. Intentar system property
            val propNodeId = System.getProperty("tsid.node.id")
            if (!propNodeId.isNullOrBlank()) {
                val nodeId = propNodeId.toIntOrNull()
                if (nodeId != null && nodeId in 0..1023) {
                    cachedNodeId = nodeId
                    logger.info("TSID node_id from tsid.node.id property: {}", nodeId)
                    return nodeId
                }
                logger.warn("Invalid tsid.node.id value: {} (must be 0-1023), using fallback", propNodeId)
            }

            // 3. Fallback: hash del hostname
            val nodeId = calculateNodeIdFromHostname()
            cachedNodeId = nodeId
            logger.info("TSID node_id from hostname hash: {}", nodeId)
            return nodeId
        }

        /**
         * Calcula un node_id basado en el hostname.
         * Util para Kubernetes donde cada pod tiene un hostname unico.
         *
         * @return node_id entre 0 y 1023
         */
        private fun calculateNodeIdFromHostname(): Int {
            return try {
                val hostname = InetAddress.getLocalHost().hostName
                // Usar abs() para evitar numeros negativos y mod 1024 para el rango
                abs(hostname.hashCode()) % 1024
            } catch (e: Exception) {
                logger.warn("Could not get hostname for TSID node_id, using default 1: {}", e.message)
                1
            }
        }
    }
}
