package com.apptolast.invernaderos.features.catalog

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface PeriodRepository : JpaRepository<Period, Short> {
    fun findByName(name: String): Period?
}
