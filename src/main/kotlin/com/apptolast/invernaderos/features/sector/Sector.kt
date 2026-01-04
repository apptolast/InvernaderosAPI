package com.apptolast.invernaderos.features.sector

import com.apptolast.invernaderos.features.greenhouse.Greenhouse
import jakarta.persistence.*
import java.util.UUID

/**
 * Subdivisiones logicas de un invernadero para agrupar dispositivos.
 *
 * @property id UUID unico del sector
 * @property greenhouseId UUID del invernadero al que pertenece
 * @property variety Variedad de cultivo en este sector
 */
@Entity
@Table(
    name = "sectors",
    schema = "metadata",
    indexes = [
        Index(name = "idx_sectors_greenhouse", columnList = "greenhouse_id")
    ]
)
data class Sector(
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    val id: UUID? = null,

    @Column(name = "greenhouse_id", nullable = false)
    val greenhouseId: UUID,

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
