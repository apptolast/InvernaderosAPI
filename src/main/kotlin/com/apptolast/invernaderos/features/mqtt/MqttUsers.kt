package com.apptolast.invernaderos.features.mqtt

import com.apptolast.invernaderos.features.greenhouse.Greenhouse
import com.apptolast.invernaderos.features.tenant.Tenant
import jakarta.persistence.*
import java.time.Instant
import java.util.UUID

/**
 * Entity que representa usuarios MQTT del sistema. Usuarios para dispositivos IoT (sensores,
 * actuadores, gateways) que se conectan vía MQTT.
 *
 * @property id UUID único del usuario MQTT
 * @property username Nombre de usuario único para autenticación MQTT
 * @property passwordHash Hash de la contraseña (bcrypt, pbkdf2, etc.)
 * @property salt Salt usado para el hash de la contraseña
 * @property deviceType Tipo de dispositivo: SENSOR, ACTUATOR, GATEWAY, API
 * @property greenhouseId UUID del invernadero asociado (nullable)
 * @property tenantId UUID del tenant asociado (nullable)
 * @property isActive Si el usuario MQTT está activo
 * @property createdAt Fecha de creación
 * @property updatedAt Fecha de última actualización
 * @property lastConnectedAt Última vez que el usuario se conectó al broker MQTT
 */
@NamedEntityGraph(
        name = "MqttUsers.context",
        attributeNodes = [NamedAttributeNode("greenhouse"), NamedAttributeNode("tenant")]
)
@Entity
@Table(
        name = "mqtt_users",
        schema = "metadata",
        indexes =
                [
                        Index(name = "idx_mqtt_users_username", columnList = "username"),
                        Index(name = "idx_mqtt_users_greenhouse", columnList = "greenhouse_id"),
                        Index(name = "idx_mqtt_users_tenant", columnList = "tenant_id"),
                        Index(name = "idx_mqtt_users_device_type", columnList = "device_type"),
                        Index(name = "idx_mqtt_users_active", columnList = "is_active")]
)
data class MqttUsers(
        @Id @GeneratedValue(strategy = GenerationType.AUTO) val id: UUID? = null,
        @Column(nullable = false, unique = true, length = 100) val username: String,
        @Column(name = "password_hash", nullable = false, length = 255) val passwordHash: String,
        @Column(nullable = false, length = 255) val salt: String,

        /** Tipo de dispositivo: SENSOR, ACTUATOR, GATEWAY, API */
        @Column(name = "device_type", length = 50) val deviceType: String? = null,

        /** Invernadero asociado a este usuario MQTT. Permite filtrar permisos por invernadero. */
        @Column(name = "greenhouse_id") val greenhouseId: UUID? = null,

        /** Tenant asociado a este usuario MQTT. Permite multi-tenancy en el broker MQTT. */
        @Column(name = "tenant_id") val tenantId: UUID? = null,
        @Column(name = "is_active", nullable = false) val isActive: Boolean = true,
        @Column(name = "created_at", nullable = false, updatable = false)
        val createdAt: Instant = Instant.now(),
        @Column(name = "updated_at", nullable = false) val updatedAt: Instant = Instant.now(),
        @Column(name = "last_connected_at") val lastConnectedAt: Instant? = null
) {
    /**
     * Relación ManyToOne con Greenhouse. Un usuario MQTT puede estar asociado a un invernadero
     * específico.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "greenhouse_id",
            referencedColumnName = "id",
            insertable = false,
            updatable = false
    )
    var greenhouse: Greenhouse? = null

    /** Relación ManyToOne con Tenant. Un usuario MQTT pertenece a un tenant. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "tenant_id",
            referencedColumnName = "id",
            insertable = false,
            updatable = false
    )
    var tenant: Tenant? = null

    override fun toString(): String {
        return "MqttUsers(id=$id, username='$username', deviceType=$deviceType, greenhouseId=$greenhouseId, tenantId=$tenantId, isActive=$isActive)"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is MqttUsers) return false
        return id != null && id == other.id
    }

    override fun hashCode(): Int {
        return id?.hashCode() ?: 0
    }

    companion object {
        object DeviceType {
            const val SENSOR = "SENSOR"
            const val ACTUATOR = "ACTUATOR"
            const val GATEWAY = "GATEWAY"
            const val API = "API"
        }
    }
}
