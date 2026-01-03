package com.apptolast.invernaderos.features.tenant

import com.apptolast.invernaderos.features.greenhouse.Greenhouse
import com.apptolast.invernaderos.features.user.User
import jakarta.persistence.*
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import java.time.Instant
import java.util.UUID

/**
 * Entity que representa un Tenant (Cliente/Empresa) en el sistema multi-tenant.
 * Cada tenant puede tener multiples invernaderos, usuarios y dispositivos.
 *
 * @property id UUID unico del tenant
 * @property name Nombre del tenant (unico, usado como identificador MQTT)
 * @property email Email de contacto principal
 * @property isActive Si el tenant esta activo
 * @property createdAt Fecha de creacion
 * @property updatedAt Fecha de ultima actualizacion
 * @property province Provincia/Estado
 * @property country Pais (default: Espana)
 * @property phone Telefono principal
 * @property location Coordenadas geograficas como objeto LocationDto (lat, lon)
 */
@NamedEntityGraphs(
    NamedEntityGraph(
        name = "Tenant.withGreenhouses",
        attributeNodes = [NamedAttributeNode("greenhouses")]
    ),
    NamedEntityGraph(
        name = "Tenant.withUsers",
        attributeNodes = [NamedAttributeNode("users")]
    )
)
@Entity
@Table(
    name = "tenants",
    schema = "metadata",
    indexes = [
        Index(name = "idx_tenants_active", columnList = "is_active")
    ]
)
data class Tenant(
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    val id: UUID? = null,

    @Column(nullable = false, length = 100, unique = true)
    var name: String,

    @Column(nullable = false, length = 255)
    var email: String,

    @Column(name = "is_active", nullable = true)
    var isActive: Boolean? = true,

    @Column(name = "created_at", nullable = false)
    val createdAt: Instant = Instant.now(),

    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant = Instant.now(),

    @Column(length = 100, nullable = true)
    var province: String? = null,

    @Column(length = 50, nullable = true)
    var country: String? = "España",

    @Column(length = 50, nullable = true)
    var phone: String? = null,

    /**
     * Coordenadas geograficas en formato JSONB: {lat: Double, lon: Double}
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb", nullable = true)
    var location: LocationDto? = null
) {
    /**
     * Relacion con greenhouses (lazy loading).
     * Un tenant puede tener N invernaderos.
     */
    @OneToMany(mappedBy = "tenant", fetch = FetchType.LAZY, cascade = [CascadeType.ALL])
    @OrderBy("name ASC")
    var greenhouses: MutableList<Greenhouse> = mutableListOf()

    /**
     * Relacion con users (lazy loading).
     * Un tenant puede tener N usuarios.
     */
    @OneToMany(mappedBy = "tenant", fetch = FetchType.LAZY, cascade = [CascadeType.ALL])
    @OrderBy("username ASC")
    var users: MutableList<User> = mutableListOf()

    override fun toString(): String {
        return "Tenant(id=$id, name='$name', email='$email', isActive=$isActive)"
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
