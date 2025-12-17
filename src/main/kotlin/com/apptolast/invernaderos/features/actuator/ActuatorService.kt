package com.apptolast.invernaderos.features.actuator

import java.util.UUID
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class ActuatorService(private val actuatorRepository: ActuatorRepository) {

    fun findAll(): List<ActuatorResponse> {
        return actuatorRepository.findAll().map { it.toResponse() }
    }

    fun findById(id: UUID): ActuatorResponse? {
        return actuatorRepository.findById(id).map { it.toResponse() }.orElse(null)
    }

    @Transactional
    fun create(request: ActuatorCreateRequest, tenantId: UUID): ActuatorResponse {
        val actuator =
                Actuator(
                        tenantId = tenantId,
                        greenhouseId = request.greenhouseId,
                        actuatorCode = request.actuatorCode,
                        deviceId = request.deviceId,
                        actuatorType = request.actuatorType,
                        unit = request.unit,
                        locationInGreenhouse = request.locationInGreenhouse,
                        isActive = true
                )
        return actuatorRepository.save(actuator).toResponse()
    }

    @Transactional
    fun sendCommand(id: UUID, request: ActuatorCommandRequest): ActuatorResponse? {
        val actuator = actuatorRepository.findById(id).orElse(null) ?: return null

        // In a real scenario, this would publish to MQTT.
        // For the DEMO/Simulation, we immediately update the state in the DB
        // so the SimulationService can react to it (Feedback Loop).

        actuator.currentState = request.command
        actuator.lastStatusUpdate = java.time.Instant.now()

        val saved = actuatorRepository.save(actuator)
        return saved.toResponse()
    }
}
