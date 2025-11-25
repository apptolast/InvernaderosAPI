package com.apptolast.invernaderos.features.greenhouse

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema
import java.time.Instant

@Schema(description = "DTO representing real-time greenhouse data")
data class RealDataDto(
        @Schema(description = "Timestamp of the data") val timestamp: Instant,
        @Schema(description = "Temperature of Greenhouse 01")
        @JsonProperty("TEMPERATURA INVERNADERO 01")
        val temperaturaInvernadero01: Double?,
        @Schema(description = "Humidity of Greenhouse 01")
        @JsonProperty("HUMEDAD INVERNADERO 01")
        val humedadInvernadero01: Double?,
        @Schema(description = "Temperature of Greenhouse 02")
        @JsonProperty("TEMPERATURA INVERNADERO 02")
        val temperaturaInvernadero02: Double?,
        @Schema(description = "Humidity of Greenhouse 02")
        @JsonProperty("HUMEDAD INVERNADERO 02")
        val humedadInvernadero02: Double?,
        @Schema(description = "Temperature of Greenhouse 03")
        @JsonProperty("TEMPERATURA INVERNADERO 03")
        val temperaturaInvernadero03: Double?,
        @Schema(description = "Humidity of Greenhouse 03")
        @JsonProperty("HUMEDAD INVERNADERO 03")
        val humedadInvernadero03: Double?,
        @Schema(description = "Sector 01 of Greenhouse 01")
        @JsonProperty("INVERNADERO_01_SECTOR_01")
        val invernadero01Sector01: Double?,
        @Schema(description = "Sector 02 of Greenhouse 01")
        @JsonProperty("INVERNADERO_01_SECTOR_02")
        val invernadero01Sector02: Double?,
        @Schema(description = "Sector 03 of Greenhouse 01")
        @JsonProperty("INVERNADERO_01_SECTOR_03")
        val invernadero01Sector03: Double?,
        @Schema(description = "Sector 04 of Greenhouse 01")
        @JsonProperty("INVERNADERO_01_SECTOR_04")
        val invernadero01Sector04: Double?,
        @Schema(description = "Sector 01 of Greenhouse 02")
        @JsonProperty("INVERNADERO_02_SECTOR_01")
        val invernadero02Sector01: Double?,
        @Schema(description = "Sector 02 of Greenhouse 02")
        @JsonProperty("INVERNADERO_02_SECTOR_02")
        val invernadero02Sector02: Double?,
        @Schema(description = "Sector 03 of Greenhouse 02")
        @JsonProperty("INVERNADERO_02_SECTOR_03")
        val invernadero02Sector03: Double?,
        @Schema(description = "Sector 04 of Greenhouse 02")
        @JsonProperty("INVERNADERO_02_SECTOR_04")
        val invernadero02Sector04: Double?,
        @Schema(description = "Sector 01 of Greenhouse 03")
        @JsonProperty("INVERNADERO_03_SECTOR_01")
        val invernadero03Sector01: Double?,
        @Schema(description = "Sector 02 of Greenhouse 03")
        @JsonProperty("INVERNADERO_03_SECTOR_02")
        val invernadero03Sector02: Double?,
        @Schema(description = "Sector 03 of Greenhouse 03")
        @JsonProperty("INVERNADERO_03_SECTOR_03")
        val invernadero03Sector03: Double?,
        @Schema(description = "Sector 04 of Greenhouse 03")
        @JsonProperty("INVERNADERO_03_SECTOR_04")
        val invernadero03Sector04: Double?,
        @Schema(description = "Extractor of Greenhouse 01")
        @JsonProperty("INVERNADERO_01_EXTRACTOR")
        val invernadero01Extractor: Double?,
        @Schema(description = "Extractor of Greenhouse 02")
        @JsonProperty("INVERNADERO_02_EXTRACTOR")
        val invernadero02Extractor: Double?,
        @Schema(description = "Extractor of Greenhouse 03")
        @JsonProperty("INVERNADERO_03_EXTRACTOR")
        val invernadero03Extractor: Double?,
        @Schema(description = "Reserve value") @JsonProperty("RESERVA") val reserva: Double?,
        @Schema(description = "Greenhouse ID") val greenhouseId: String? = null,

        /**
         * Tenant identifier for multi-tenant isolation Used for Redis cache key segregation:
         * "greenhouse:messages:{tenantId}" Required after PostgreSQL multi-tenant migration
         * (V3-V10)
         */
        @Schema(description = "Tenant ID for multi-tenancy") val tenantId: String? = null
)
