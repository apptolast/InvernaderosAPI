package com.apptolast.invernaderos.entities.dtos

import java.math.BigDecimal
import java.time.Instant

data class RealDataDto(
    val timestamp: Instant,
    val TEMPERATURA_INVERNADERO_01: Double?,
    val HUMEDAD_INVERNADERO_01: Double?,
    val TEMPERATURA_INVERNADERO_02: Double?,
    val HUMEDAD_INVERNADERO_02: Double?,
    val TEMPERATURA_INVERNADERO_03: Double?,
    val HUMEDAD_INVERNADERO_03: Double?,
    val INVERNADERO_01_SECTOR_01: Double?,
    val INVERNADERO_01_SECTOR_02: Double?,
    val INVERNADERO_01_SECTOR_03: Double?,
    val INVERNADERO_01_SECTOR_04: Double?,
    val INVERNADERO_02_SECTOR_01: Double?,
    val INVERNADERO_02_SECTOR_02: Double?,
    val INVERNADERO_02_SECTOR_03: Double?,
    val INVERNADERO_02_SECTOR_04: Double?,
    val INVERNADERO_03_SECTOR_01: Double?,
    val INVERNADERO_03_SECTOR_02: Double?,
    val INVERNADERO_03_SECTOR_03: Double?,
    val INVERNADERO_03_SECTOR_04: Double?,
    val INVERNADERO_01_EXTRACTOR: Double?,
    val INVERNADERO_02_EXTRACTOR: Double?,
    val INVERNADERO_03_EXTRACTOR: Double?,
    val RESERVA: Double?,
    val greenhouseId: String? = null,
)
