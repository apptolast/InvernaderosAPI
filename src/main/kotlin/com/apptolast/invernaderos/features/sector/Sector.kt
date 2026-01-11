package com.apptolast.invernaderos.features.sector

import com.apptolast.invernaderos.features.greenhouse.Greenhouse
import io.hypersistence.utils.hibernate.id.Tsid
import jakarta.persistence.*

/**
 * Subdivisiones logicas de un invernadero para agrupar dispositivos.
 *
 * @property id ID unico del sector (TSID - Time-Sorted ID, unico global)
 * @property code Codigo unico legible para identificacion externa (ej: SEC-00001)
 * @property greenhouseId ID del invernadero al que pertenece
 * @property variety Variedad de cultivo en este sector
 */
@Entity
@Table(
    name = "sectors",
    schema = "metadata",
    indexes = [
        Index(name = "idx_sectors_greenhouse", columnList = "greenhouse_id"),
        Index(name = "idx_sectors_code", columnList = "code")
    ]
)
data class Sector(
    @Id
    @Tsid
    var id: Long? = null,

    /**
     * Codigo unico legible para identificacion externa.
     * Formato: SEC-{numero_padded} (ej: SEC-00001)
     * Usado por PLCs, APIs externas y para debuggear.
     */
    @Column(nullable = false, length = 50, unique = true)
    var code: String,

    @Column(name = "greenhouse_id", nullable = false)
    val greenhouseId: Long,

    @Column(length = 100)
    var variety: String? = null
) {
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
        name = "greenhouse_id",
        referencedColumnName = "id",
        insertable = false,
        updatable = false
    )
    var greenhouse: Greenhouse? = null

    override fun toString(): String {
        return "Sector(id=$id, greenhouseId=$greenhouseId, variety=$variety)"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Sector) return false
        return id != null && id == other.id
    }

    override fun hashCode(): Int = id?.hashCode() ?: 0
}
