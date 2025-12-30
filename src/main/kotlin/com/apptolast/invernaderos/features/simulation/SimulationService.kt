package com.apptolast.invernaderos.features.simulation

import com.apptolast.invernaderos.features.greenhouse.RealDataDto
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.sin
import org.slf4j.LoggerFactory
import org.springframework.messaging.simp.SimpMessagingTemplate
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service

@Service
class SimulationService(
        private val messagingTemplate: SimpMessagingTemplate
) {

    private val logger = LoggerFactory.getLogger(SimulationService::class.java)

    // Tracks active simulations per tenant: TenantID -> StartTimestamp
    private val activeSimulations = ConcurrentHashMap<String, Long>()

    // Constant for the target topic the frontend listens to
    // Assuming standard STOMP convention /topic/greenhouse/{tenantId}
    // We will verify this with the file read, but defaulting to a likely candidate.
    private val TOPIC_PREFIX = "/topic/greenhouse"

    fun startSimulation(tenantId: String) {
        activeSimulations[tenantId] = System.currentTimeMillis()
        logger.info("Started generic simulation for tenant $tenantId")
    }

    fun stopSimulation(tenantId: String) {
        activeSimulations.remove(tenantId)
        logger.info("Stopped simulation for tenant $tenantId")
    }

    fun getSimulationStatus(tenantId: String): SimulationStatus {
        val startTime = activeSimulations[tenantId] ?: return SimulationStatus(isActive = false)
        val elapsedSeconds = (System.currentTimeMillis() - startTime) / 1000
        val currentMinute = (elapsedSeconds / 60).toInt()
        val phase = SimulationPhase.fromMinute(currentMinute)

        return SimulationStatus(
                isActive = true,
                elapsedSeconds = elapsedSeconds,
                phase = phase.name,
                description = phase.description
        )
    }

    @Scheduled(fixedRate = 1000)
    fun tick() {
        if (activeSimulations.isEmpty()) return

        activeSimulations.forEach { (tenantId, startTime) ->
            try {
                processSimulationStep(tenantId, startTime)
            } catch (e: Exception) {
                logger.error("Error in simulation tick for tenant $tenantId", e)
            }
        }
    }

    private fun processSimulationStep(tenantId: String, startTime: Long) {
        val elapsedSeconds = (System.currentTimeMillis() - startTime) / 1000
        val minute = (elapsedSeconds / 60).toInt()
        val phase = SimulationPhase.fromMinute(minute)

        // 1. Calculate Base Values based on Phase
        val (temp, humidity) = calculateEnvironment(phase, elapsedSeconds)

        // 2. Use base temp (actuator adjustment removed)
        val adjustedTemp = temp

        // 3. Construct Data Packet
        val data =
                RealDataDto(
                        timestamp = Instant.now(),
                        tenantId = tenantId,
                        greenhouseId = "SIMULATION_GH_01",
                        temperaturaInvernadero01 = adjustedTemp,
                        humedadInvernadero01 = humidity,
                        temperaturaInvernadero02 = adjustedTemp - 1.0, // Slight variation
                        humedadInvernadero02 = humidity + 5.0,
                        // Fill other fields with noise or defaults
                        invernadero01Sector01 = adjustedTemp,
                        reserva =
                                if (phase == SimulationPhase.CRITICAL) 1.0
                                else 0.0 // Flag for debug
                )

        // 4. Push to WebSocket
        // Destination: /topic/greenhouse/messages (Global topic, frontend filters by tenantId)
        messagingTemplate.convertAndSend("/topic/greenhouse/messages", data)

        // Also trigger alerts if critical (bypass DB, just socket notification)
        if (adjustedTemp > 35.0) {
            // We could push a separate alert message here if the frontend listens for it
            // messagingTemplate.convertAndSend("/topic/alerts/$tenantId", AlertDto(...))
        }
    }

    private fun calculateEnvironment(
            phase: SimulationPhase,
            elapsedSeconds: Long
    ): Pair<Double, Double> {
        val noise = (Math.random() - 0.5) * 0.5 // +/- 0.25 drift
        return when (phase) {
            SimulationPhase.CALM -> {
                // Base 22, varying slightly
                val t = 22.0 + sin(elapsedSeconds * 0.1) + noise
                val h = 60.0 + noise
                Pair(t, h)
            }
            SimulationPhase.HEATING -> {
                // Rises from 22 to 35 over 2 minutes (120 seconds) => +0.1 per sec approx
                val phaseSeconds = elapsedSeconds - (phase.startMinute * 60)
                val t = 22.0 + (phaseSeconds * 0.15) + noise
                val h = 60.0 - (phaseSeconds * 0.1) // Humidity drops as temp rises
                Pair(t, h)
            }
            SimulationPhase.CRITICAL, SimulationPhase.ACTION_WAIT -> {
                // High temp ~40
                val t = 40.0 + noise
                val h = 30.0 + noise
                Pair(t, h)
            }
            SimulationPhase.RECOVERY -> {
                // If NO acutuator logic handled above (default path if no actuators), it stays high
                // Real cooling happens in standard logic if actuator is found.
                // Here we return the "Environment External" temp, which is still hot,
                // but the "Greenhouse internal" (calculated above) might drop.
                val t = 40.0 + noise
                val h = 35.0 + noise
                Pair(t, h)
            }
            SimulationPhase.NORMALIZATION -> {
                // Returning to 22
                val t = 22.0 + noise
                val h = 55.0 + noise
                Pair(t, h)
            }
        }
    }
}

data class SimulationStatus(
        val isActive: Boolean,
        val elapsedSeconds: Long = 0,
        val phase: String = "",
        val description: String = ""
)
