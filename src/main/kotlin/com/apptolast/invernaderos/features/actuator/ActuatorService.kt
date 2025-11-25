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

        // Logic to send MQTT command would go here
        // For now, just update state simulation
        // actuator.currentState = request.command // This would be updated by status feedback
        // usually

        // We might want to log the command or update lastCommandAt
        // actuator.lastCommandAt = Instant.now()
        // actuatorRepository.save(actuator)

        return actuator.toResponse()
    }
}
