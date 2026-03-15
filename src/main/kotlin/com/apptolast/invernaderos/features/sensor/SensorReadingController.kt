package com.apptolast.invernaderos.features.sensor

import io.swagger.v3.oas.annotations.Operation
import java.time.Instant
import java.time.temporal.ChronoUnit
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/v1/sensors")
class SensorReadingController(private val sensorReadingService: SensorReadingService) {

        /** GET /api/v1/sensors/latest - Obtiene las últimas lecturas */
        @GetMapping("/latest")
        @Operation(summary = "Get latest sensor readings")
        fun getLatestReadings(
                @RequestParam(defaultValue = "10") limit: Int
        ): ResponseEntity<List<SensorReadingResponse>> {
                return ResponseEntity.ok(sensorReadingService.getLatestReadings(limit))
        }

        /** GET /api/v1/sensors/by-code/{code} - Obtiene lecturas de un código específico */
        @GetMapping("/by-code/{code}")
        @Operation(summary = "Get readings by code (e.g., SET-00036, DEV-00031)")
        fun getReadingsByCode(
                @PathVariable code: String,
                @RequestParam(required = false) hoursAgo: Long = 24
        ): ResponseEntity<List<SensorReadingResponse>> {
                val end = Instant.now()
                val start = end.minus(hoursAgo, ChronoUnit.HOURS)
                return ResponseEntity.ok(sensorReadingService.getReadingsByCode(code, start, end))
        }

        /** GET /api/v1/sensors/current - Obtiene el último valor de cada código */
        @GetMapping("/current")
        @Operation(summary = "Get current values for all codes")
        fun getCurrentValues(): ResponseEntity<Map<String, Any?>> {
                return ResponseEntity.ok(sensorReadingService.getCurrentValues())
        }
}
