package com.apptolast.invernaderos.features.sensor.infrastructure.adapter.input

import com.apptolast.invernaderos.features.sensor.domain.port.input.QuerySensorReadingsUseCase
import com.apptolast.invernaderos.features.sensor.dto.mapper.toResponse
import com.apptolast.invernaderos.features.sensor.dto.response.SensorReadingResponse
import io.swagger.v3.oas.annotations.Operation
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.time.Instant
import java.time.temporal.ChronoUnit

@RestController
@RequestMapping("/api/v1/sensors")
class SensorReadingController(
    private val queryUseCase: QuerySensorReadingsUseCase
) {

    @GetMapping("/latest")
    @Operation(summary = "Get latest sensor readings")
    fun getLatestReadings(
        @RequestParam(defaultValue = "10") limit: Int
    ): ResponseEntity<List<SensorReadingResponse>> {
        return ResponseEntity.ok(queryUseCase.getLatestReadings(limit).map { it.toResponse() })
    }

    @GetMapping("/by-code/{code}")
    @Operation(summary = "Get readings by code (e.g., SET-00036, DEV-00031)")
    fun getReadingsByCode(
        @PathVariable code: String,
        @RequestParam(required = false) hoursAgo: Long = 24
    ): ResponseEntity<List<SensorReadingResponse>> {
        val end = Instant.now()
        val start = end.minus(hoursAgo, ChronoUnit.HOURS)
        return ResponseEntity.ok(queryUseCase.getReadingsByCode(code, start, end).map { it.toResponse() })
    }

    @GetMapping("/current")
    @Operation(summary = "Get current values for all codes")
    fun getCurrentValues(): ResponseEntity<Map<String, Any?>> {
        return ResponseEntity.ok(queryUseCase.getCurrentValues())
    }
}
