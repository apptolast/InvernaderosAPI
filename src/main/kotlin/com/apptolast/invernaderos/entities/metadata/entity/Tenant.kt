package com.apptolast.invernaderos.entities.metadata.entity

import jakarta.persistence.*
import java.time.Instant
import java.util.UUID

/**
 * Entity que representa un Tenant (Cliente/Empresa) en el sistema multi-tenant.
 * Cada tenant puede tener múltiples invernaderos, usuarios y dispositivos MQTT.
 *
 * @property id UUID único del tenant
 * @property name Nombre del tenant (persona física o empresa)
 * @property email Email de contacto principal
 * @property companyName Razón social o nombre comercial de la empresa
 * @property taxId CIF/NIF/Tax ID único
 * @property address Dirección completa
 * @property city Ciudad
 * @property postalCode Código postal
 * @property province Provincia/Estado
 * @property country País (default: España)
 * @property phone Teléfono principal
 * @property contactPerson Persona de contacto principal
 * @property contactPhone Teléfono de la persona de contacto
 * @property contactEmail Email de la persona de contacto
 * @property mqttTopicPrefix Prefijo para topics MQTT (ej: "GREENHOUSE/empresaID")
 * @property coordinates Coordenadas geográficas en formato JSONB: {"lat": number, "lon": number}
 * @property notes Notas adicionales sobre el cliente
 * @property isActive Si el tenant está activo
 * @property createdAt Fecha de creación
 * @property updatedAt Fecha de última actualización
 */
@Entity
@Table(
    name = "tenants",
    schema = "metadata",
    indexes = [
        Index(name = "idx_tenants_company_name", columnList = "company_name"),
        Index(name = "idx_tenants_mqtt_topic_prefix", columnList = "mqtt_topic_prefix"),
        Index(name = "idx_tenants_tax_id", columnList = "tax_id"),
        Index(name = "idx_tenants_active", columnList = "is_active")
    ],
    uniqueConstraints = [
        UniqueConstraint(name = "uq_tenants_tax_id", columnNames = ["tax_id"]),
        UniqueConstraint(name = "uq_tenants_mqtt_topic_prefix", columnNames = ["mqtt_topic_prefix"])
    ]
)
data class Tenant(
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    val id: UUID? = null,

    @Column(nullable = false, length = 100)
    val name: String,

    @Column(nullable = false, length = 255, unique = true)
    val email: String,

    // Nuevos campos de información de empresa/cliente
    @Column(name = "company_name", length = 200)
    val companyName: String? = null,

    @Column(name = "tax_id", length = 50, unique = true)
    val taxId: String? = null,

    @Column(columnDefinition = "TEXT")
    val address: String? = null,

    @Column(length = 100)
    val city: String? = null,

    @Column(name = "postal_code", length = 20)
    val postalCode: String? = null,

    @Column(length = 100)
    val province: String? = null,

    @Column(length = 50)
    val country: String? = "España",

    @Column(length = 50)
    val phone: String? = null,

    @Column(name = "contact_person", length = 150)
    val contactPerson: String? = null,

    @Column(name = "contact_phone", length = 50)
    val contactPhone: String? = null,

    @Column(name = "contact_email", length = 255)
    val contactEmail: String? = null,

    /**
     * Prefijo único para topics MQTT de este tenant.
     * Ejemplo: "SARA" para topics como "GREENHOUSE/SARA"
     */
    @Column(name = "mqtt_topic_prefix", length = 50, unique = true)
    val mqttTopicPrefix: String? = null,

    /**
     * Coordenadas geográficas en formato JSON: {"lat": 40.4168, "lon": -3.7038}
     */
    @Column(columnDefinition = "JSONB")
    val coordinates: String? = null,

    @Column(columnDefinition = "TEXT")
    val notes: String? = null,

    @Column(name = "is_active", nullable = false)
    val isActive: Boolean = true,

    @Column(name = "created_at", nullable = false)
    val createdAt: Instant = Instant.now(),

    @Column(name = "updated_at", nullable = false)
    val updatedAt: Instant = Instant.now()
) {
    /**
     * Relación con greenhouses (lazy loading).
     * Un tenant puede tener N invernaderos.
     */
    @OneToMany(mappedBy = "tenant", fetch = FetchType.LAZY, cascade = [CascadeType.ALL])
    @OrderBy("name ASC")
    var greenhouses: MutableList<Greenhouse> = mutableListOf()

    /**
     * Relación con users (lazy loading).
     * Un tenant puede tener N usuarios.
     */
    @OneToMany(mappedBy = "tenant", fetch = FetchType.LAZY, cascade = [CascadeType.ALL])
    @OrderBy("username ASC")
    var users: MutableList<User> = mutableListOf()

    override fun toString(): String {
        return "Tenant(id=$id, name='$name', companyName=$companyName, mqttTopicPrefix=$mqttTopicPrefix, isActive=$isActive)"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Tenant) return false
        return id != null && id == other.id
    }

    override fun hashCode(): Int {
        return id?.hashCode() ?: 0
    }
}