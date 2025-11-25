package com.apptolast.invernaderos.features.mqtt

import java.util.UUID
import org.springframework.stereotype.Service

@Service
class MqttUserService(private val mqttUserRepository: MqttUserRepository) {

    fun getAllUsers(): List<MqttUserDto> {
        return mqttUserRepository.findAll().toDtoList()
    }

    fun getMqttUser(id: UUID): MqttUserDto? {
        return mqttUserRepository.findById(id).map { it.toDto() }.orElse(null)
    }
}
