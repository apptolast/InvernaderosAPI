package com.apptolast.invernaderos.features.simulation

import com.apptolast.invernaderos.config.SimulationProperties
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/v1/simulation")
@Tag(
        name = "Simulation",
        description = "Control y gestión de simulación de datos de invernadero (Demo Mode)"
)
class SimulationController(
        private val simulationService: SimulationService,
        private val properties: SimulationProperties
) {

        @PostMapping("/start")
        @Operation(summary = "Start the 10-minute demo simulation for a specific tenant")
        fun startSimulation(
                @RequestHeader(value = "X-Tenant-ID", defaultValue = "DEMO") tenantId: String
        ): ResponseEntity<String> {
                simulationService.startSimulation(tenantId)
                return ResponseEntity.ok("Simulation started for tenant $tenantId. Phase: CALM")
        }

        @PostMapping("/stop")
        @Operation(summary = "Stop the simulation immediately")
        fun stopSimulation(
                @RequestHeader(value = "X-Tenant-ID", defaultValue = "DEMO") tenantId: String
        ): ResponseEntity<String> {
                simulationService.stopSimulation(tenantId)
                return ResponseEntity.ok("Simulation stopped for tenant $tenantId")
        }

        @GetMapping("/status")
        @Operation(summary = "Get current simulation status and phase")
        fun getStatus(
                @RequestHeader(value = "X-Tenant-ID", defaultValue = "DEMO") tenantId: String
        ): ResponseEntity<SimulationStatus> {
                val status = simulationService.getSimulationStatus(tenantId)
                return ResponseEntity.ok(status)
        }

        @PostMapping("/generate")
        @Operation(summary = "Manually trigger a single simulation tick (debugging)")
        fun generateSingleStep(
                @RequestHeader(value = "X-Tenant-ID", defaultValue = "DEMO") tenantId: String
        ): ResponseEntity<String> {
                // Manually trigger a step (mostly for debugging or one-off tests)
                simulationService.tick() // This ticks ALL tenants, but it's fine for debug
                return ResponseEntity.ok("Manual tick executed")
        }
}
