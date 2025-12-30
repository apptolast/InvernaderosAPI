package com.apptolast.invernaderos.features.greenhouse

import com.apptolast.invernaderos.features.tenant.Tenant
import jakarta.persistence.*
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

/**
 * Entity que representa un Invernadero en el sistema. Cada greenhouse pertenece a un Tenant y puede
 * tener multiples dispositivos (sensores y actuadores).
 *
 * @property id UUID unico del invernadero
 * @property tenantId UUID del tenant propietario
 * @property name Nombre del invernadero
 * @property greenhouseCode Codigo corto unico del invernadero dentro del tenant (ej: INV01, SARA_01)
 * @property mqttTopic Topic MQTT completo asignado (ej: GREENHOUSE/empresa001/inv01)
 * @property mqttPublishIntervalSeconds Intervalo de publicacion de datos en segundos
 * @property externalId ID externo del sistema de sensores (si aplica)
 * @property sectors Sectores embebidos como JSONB: [{"code": "S01", "name": "Sector Norte", ...}]
 * @property location Ubicacion en formato JSONB
 * @property areaM2 Area en metros cuadrados
 * @property cropType Tipo de cultivo
 * @property timezone Zona horaria
 * @property isActive Si el invernadero esta activo
 * @property createdAt Fecha de creacion
 * @property updatedAt Fecha de ultima actualizacion
 */
@NamedEntityGraph(
        name = "Greenhouse.context",
        attributeNodes = [NamedAttributeNode("tenant")]
)
@Entity
@Table(
        name = "greenhouses",
        schema = "metadata",
        indexes =
                [
                        Index(name = "idx_greenhouses_tenant", columnList = "tenant_id"),
                        Index(name = "idx_greenhouses_code", columnList = "greenhouse_code"),
                        Index(name = "idx_greenhouses_mqtt_topic", columnList = "mqtt_topic"),
                        Index(
                                name = "idx_greenhouses_tenant_active",
                                columnList = "tenant_id, is_active"
                        )],
        uniqueConstraints =
                [
                        UniqueConstraint(
                                name = "uq_greenhouse_code_per_tenant",
                                columnNames = ["tenant_id", "greenhouse_code"]
                        ),
                        UniqueConstraint(
                                name = "uq_greenhouse_mqtt_topic",
                                columnNames = ["mqtt_topic"]
                        )]
)
data class Greenhouse(
        @Id @GeneratedValue(strategy = GenerationType.AUTO) val id: UUID? = null,
        @Column(name = "tenant_id", nullable = false) val tenantId: UUID,
        @Column(nullable = false, length = 100) var name: String,

        /**
         * Código corto único del invernadero dentro del tenant. Ejemplo: "INV01", "SARA_01",
         * "GREENHOUSE_A" Se usa para identificación rápida y routing MQTT.
         */
        @Column(name = "greenhouse_code", length = 50) var greenhouseCode: String? = null,

        /**
         * Topic MQTT completo asignado a este invernadero. Ejemplo: "GREENHOUSE/empresa001",
         * "GREENHOUSE/SARA/inv01" Debe ser único globalmente en el sistema.
         */
        @Column(name = "mqtt_topic", length = 100, unique = true) var mqttTopic: String? = null,

        /** Intervalo de publicación de datos del invernadero en segundos. Default: 5 segundos */
        @Column(name = "mqtt_publish_interval_seconds") var mqttPublishIntervalSeconds: Int? = 5,

        /**
         * ID externo del sistema de sensores o hardware. Util para integracion con sistemas legacy.
         */
        @Column(name = "external_id", length = 100) var externalId: String? = null,

        /**
         * Sectores embebidos como JSONB.
         * Ejemplo: [{"code": "S01", "name": "Sector Norte", "area_m2": 500, "target_temp_min": 18, "target_temp_max": 28}]
         */
        @Column(name = "sectors", columnDefinition = "jsonb")
        var sectors: String? = "[]",

        @Column(columnDefinition = "jsonb") var location: String? = null,
        @Column(name = "area_m2", precision = 10, scale = 2) var areaM2: BigDecimal? = null,
        @Column(name = "crop_type", length = 50) var cropType: String? = null,
        @Column(length = 50) var timezone: String? = null,
        @Column(name = "is_active", nullable = false) var isActive: Boolean = true,
        @Column(name = "created_at", nullable = false) val createdAt: Instant = Instant.now(),
        @Column(name = "updated_at", nullable = false) var updatedAt: Instant = Instant.now()
) {
        /** Relacion ManyToOne con Tenant. Un invernadero pertenece a un tenant. */
        @ManyToOne(fetch = FetchType.LAZY)
        @JoinColumn(
                name = "tenant_id",
                referencedColumnName = "id",
                insertable = false,
                updatable = false
        )
        var tenant: Tenant? = null

        override fun toString(): String {
                return "Greenhouse(id=$id, name='$name', greenhouseCode=$greenhouseCode, mqttTopic=$mqttTopic, tenantId=$tenantId, isActive=$isActive)"
        }

        override fun equals(other: Any?): Boolean {
                if (this === other) return true
                if (other !is Greenhouse) return false
                return id != null && id == other.id
        }

        override fun hashCode(): Int {
                return id?.hashCode() ?: 0
        }
}
