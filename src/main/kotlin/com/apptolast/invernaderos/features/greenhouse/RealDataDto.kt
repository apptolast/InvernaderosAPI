package com.apptolast.invernaderos.features.greenhouse

import com.fasterxml.jackson.annotation.JsonProperty
import java.time.Instant

data class RealDataDto(
    val timestamp: Instant,
    @JsonProperty("TEMPERATURA INVERNADERO 01")
    val temperaturaInvernadero01: Double?,
    @JsonProperty("HUMEDAD INVERNADERO 01")
    val humedadInvernadero01: Double?,
    @JsonProperty("TEMPERATURA INVERNADERO 02")
    val temperaturaInvernadero02: Double?,
    @JsonProperty("HUMEDAD INVERNADERO 02")
    val humedadInvernadero02: Double?,
    @JsonProperty("TEMPERATURA INVERNADERO 03")
    val temperaturaInvernadero03: Double?,
    @JsonProperty("HUMEDAD INVERNADERO 03")
    val humedadInvernadero03: Double?,
    @JsonProperty("INVERNADERO_01_SECTOR_01")
    val invernadero01Sector01: Double?,
    @JsonProperty("INVERNADERO_01_SECTOR_02")
    val invernadero01Sector02: Double?,
    @JsonProperty("INVERNADERO_01_SECTOR_03")
    val invernadero01Sector03: Double?,
    @JsonProperty("INVERNADERO_01_SECTOR_04")
    val invernadero01Sector04: Double?,
    @JsonProperty("INVERNADERO_02_SECTOR_01")
    val invernadero02Sector01: Double?,
    @JsonProperty("INVERNADERO_02_SECTOR_02")
    val invernadero02Sector02: Double?,
    @JsonProperty("INVERNADERO_02_SECTOR_03")
    val invernadero02Sector03: Double?,
    @JsonProperty("INVERNADERO_02_SECTOR_04")
    val invernadero02Sector04: Double?,
    @JsonProperty("INVERNADERO_03_SECTOR_01")
    val invernadero03Sector01: Double?,
    @JsonProperty("INVERNADERO_03_SECTOR_02")
    val invernadero03Sector02: Double?,
    @JsonProperty("INVERNADERO_03_SECTOR_03")
    val invernadero03Sector03: Double?,
    @JsonProperty("INVERNADERO_03_SECTOR_04")
    val invernadero03Sector04: Double?,
    @JsonProperty("INVERNADERO_01_EXTRACTOR")
    val invernadero01Extractor: Double?,
    @JsonProperty("INVERNADERO_02_EXTRACTOR")
    val invernadero02Extractor: Double?,
    @JsonProperty("INVERNADERO_03_EXTRACTOR")
    val invernadero03Extractor: Double?,
    @JsonProperty("RESERVA")
    val reserva: Double?,
    val greenhouseId: String? = null,

    /**
     * Tenant identifier for multi-tenant isolation
     * Used for Redis cache key segregation: "greenhouse:messages:{tenantId}"
     * Required after PostgreSQL multi-tenant migration (V3-V10)
     */
    val tenantId: String? = null
)
