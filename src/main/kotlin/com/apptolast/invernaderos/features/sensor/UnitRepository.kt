package com.apptolast.invernaderos.features.sensor

import com.apptolast.invernaderos.features.catalog.catalog.Unit
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository

@Repository
interface UnitRepository : JpaRepository<Unit, Short> {

    /**
     * Busca una unidad por su símbolo
     * Ej: findBySymbol("°C") → Unit(id=1, symbol="°C", name="Celsius")
     */
    fun findBySymbol(symbol: String): Unit?

    /**
     * Obtiene todas las unidades activas
     */
    fun findByIsActiveTrue(): List<Unit>

    /**
     * Busca unidad por símbolo (case-insensitive)
     */
    @Query("SELECT u FROM Unit u WHERE LOWER(u.symbol) = LOWER(:symbol)")
    fun findBySymbolIgnoreCase(symbol: String): Unit?
}
