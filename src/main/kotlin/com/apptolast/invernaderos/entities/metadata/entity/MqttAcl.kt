package com.apptolast.invernaderos.entities.metadata.entity

import jakarta.persistence.*
import java.time.Instant
import java.util.UUID

/**
 * Entity que representa las Access Control Lists (ACL) para MQTT.
 * Define los permisos de publicación/suscripción para usuarios MQTT sobre topics específicos.
 *
 * @property id UUID único del registro ACL
 * @property username Nombre del usuario MQTT al que aplica esta regla
 * @property permission Tipo de permiso: 'allow' o 'deny'
 * @property action Acción permitida/denegada: 'publish', 'subscribe', 'pubsub'
 * @property topic Patrón del topic MQTT (soporta wildcards # y +)
 * @property qos Quality of Service (0, 1, 2) - NULL significa cualquier QoS
 * @property createdAt Fecha de creación del registro
 */
@Entity
@Table(
    name = "mqtt_acl",
    schema = "metadata",
    indexes = [
        Index(name = "idx_mqtt_acl_username", columnList = "username"),
        Index(name = "idx_mqtt_acl_topic", columnList = "topic"),
        Index(name = "idx_mqtt_acl_username_topic", columnList = "username, topic")
    ]
)
data class MqttAcl(
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    val id: UUID? = null,

    @Column(nullable = false, length = 100)
    val username: String,

    /**
     * Tipo de permiso: 'allow' o 'deny'
     */
    @Column(nullable = false, length = 20)
    val permission: String,

    /**
     * Acción: 'publish', 'subscribe', 'pubsub'
     */
    @Column(nullable = false, length = 20)
    val action: String,

    /**
     * Patrón del topic MQTT. Soporta wildcards:
     * - '#' coincide con cualquier número de niveles
     * - '+' coincide con un solo nivel
     * Ejemplos: "GREENHOUSE/#", "GREENHOUSE/+/sensor/+"
     */
    @Column(nullable = false, length = 255)
    val topic: String,

    /**
     * Quality of Service (0, 1, 2).
     * NULL significa que aplica a cualquier QoS.
     */
    @Column
    val qos: Int? = null,

    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: Instant = Instant.now()
) {
    override fun toString(): String {
        return "MqttAcl(id=$id, username='$username', permission='$permission', action='$action', topic='$topic', qos=$qos)"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is MqttAcl) return false
        return id != null && id == other.id
    }

    override fun hashCode(): Int {
        return id?.hashCode() ?: 0
    }

    companion object {
        object Permission {
            const val ALLOW = "allow"
            const val DENY = "deny"
        }

        object Action {
            const val PUBLISH = "publish"
            const val SUBSCRIBE = "subscribe"
            const val PUBSUB = "pubsub"
        }
    }
}
