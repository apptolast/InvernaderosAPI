package com.apptolast.invernaderos.features.mqtt

import java.util.*
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/mqttusers")
class MqttUserController(private val mqttUserRepository: MqttUserRepository) {

    @GetMapping
    fun getAllUsers(): List<MqttUserDto> {
        return mqttUserRepository.findAll().toDtoList()
    }

    @GetMapping("/{id}")
    fun getMqttUser(@PathVariable id: UUID): ResponseEntity<MqttUserDto> {
        return mqttUserRepository
                .findById(id)
                .map { user -> ResponseEntity.ok(user.toDto()) }
                .orElse(ResponseEntity.notFound().build())
    }
}
